package restaurante;

import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/** Almacén compartido entre el panel, la cocina y el menú del cliente. */
public class Datos {

    public static final String PENDIENTE = "Pendiente";
    public static final String PREPARANDO = "Preparando";
    public static final String LISTO = "Listo";
    public static final String ENTREGADO = "Entregado";

    /** stock = -1 significa sin límite. */
    public record Producto(int id, String nombre, String categoria, double precio, boolean disponible,
                           String descripcion, String imagen, int stock) {
        public Producto(int id, String nombre, String categoria, double precio) {
            this(id, nombre, categoria, precio, true, "", "", -1);
        }
        public boolean puedePedirse() { return disponible && stock != 0; }
        public String estado() {
            if (!puedePedirse()) return "Agotado";
            if (stock > 0 && stock <= 5) return "Quedan " + stock;
            return "Disponible";
        }
        @Override public String toString() {
            return nombre + " - " + dinero(precio) + (puedePedirse() ? "" : " (agotado)");
        }
    }

    public record Linea(long id, Producto producto, int cantidad, String nota, String estado,
                        String hora, double ahorro, String promo) {
        public Linea(Producto producto, int cantidad) { this(0, producto, cantidad, "", "", "", 0, ""); }
        public double subtotal() { return producto.precio() * cantidad - ahorro; }
        public Linea conEstado(String e) { return new Linea(id, producto, cantidad, nota, e, hora, ahorro, promo); }
    }

    public record Detalle(String producto, String categoria, int cantidad, double valor, String nota) { }

    /** Una cuenta cobrada (o una pre-cuenta, si no se ha guardado). */
    public record Venta(String fecha, int mesa, double subtotal, double descuento, double propina,
                        double total, String pago, String usuario, int personas, List<Detalle> detalle) { }

    public record Promocion(int id, int productoId, String tipo, double valor, boolean activa) {
        public String descripcion() { return "2X1".equals(tipo) ? "2x1" : (long) valor + "% de descuento"; }
    }

    public record Reserva(int id, int mesa, String nombre, int personas, String fecha, String hora, String telefono) { }

    /** Una línea que la cocina debe atender. */
    public record EnCocina(int mesa, Linea linea) { }

    private static final Path ARCHIVO = Path.of("menu.csv");
    private static final Path ARCHIVO_VENTAS = Path.of("ventas.csv");
    private static final Path ARCHIVO_PROMOS = Path.of("promos.csv");
    private static final Path ARCHIVO_RESERVAS = Path.of("reservas.csv");
    private static final Path ARCHIVO_CONFIG = Path.of("config.properties");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Datos INSTANCIA = new Datos();

    public static Datos get() { return INSTANCIA; }

    private final List<Producto> menu = new ArrayList<>();
    private final Map<Integer, List<Linea>> pedidos = new HashMap<>();
    private final Set<Integer> llamadas = new TreeSet<>();
    private final List<Venta> ventas = new ArrayList<>();
    private final List<Promocion> promos = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();
    private final Properties config = new Properties();
    private final List<Runnable> oyentes = new CopyOnWriteArrayList<>();
    private int siguienteId = 1;
    private int siguientePromo = 1;
    private int siguienteReserva = 1;
    private long siguienteLinea = 1;

    private Datos() {
        cargarConfig();
        cargar();
        cargarVentas();
        cargarPromos();
        cargarReservas();
        if (menu.isEmpty()) {
            semilla("Bandeja paisa", "Platos fuertes", 32000, "Frijoles, arroz, carne molida, chicharrón, huevo, plátano y aguacate.");
            semilla("Sopa del día", "Entradas", 12000, "Pregunta al mesero por la sopa de hoy.");
            semilla("Jugo natural", "Bebidas", 7000, "Fruta de temporada, en agua o en leche.");
            semilla("Gaseosa", "Bebidas", 5000, "Botella personal.");
            semilla("Tres leches", "Postres", 11000, "Bizcocho bañado en tres leches con crema.");
            guardar();
        }
    }

    private void semilla(String n, String c, double p, String desc) {
        menu.add(new Producto(siguienteId++, n, c, p, true, desc, "", -1));
    }

    // ---------- Configuración ----------
    public synchronized int mesas() { return entero(config.getProperty("mesas"), 10); }
    public synchronized int propinaSugerida() { return entero(config.getProperty("propina"), 10); }
    public synchronized String nombre() { return config.getProperty("nombre", "Restaurante"); }

    public synchronized boolean fijarMesas(int n) {
        if (n < 1 || n > 60) return false;
        for (Map.Entry<Integer, List<Linea>> e : pedidos.entrySet()) {
            if (e.getKey() > n && !e.getValue().isEmpty()) return false;
        }
        pedidos.keySet().removeIf(k -> k > n);
        llamadas.removeIf(k -> k > n);
        reservas.removeIf(r -> r.mesa() > n);
        guardarReservas();
        config.setProperty("mesas", String.valueOf(n));
        guardarConfig();
        notificar();
        return true;
    }

    public synchronized void fijarAjustes(String nombre, int propina) {
        config.setProperty("nombre", limpiar(nombre).isBlank() ? "Restaurante" : limpiar(nombre).trim());
        config.setProperty("propina", String.valueOf(Math.max(0, Math.min(100, propina))));
        guardarConfig();
        notificar();
    }

    // ---------- Menú ----------
    public synchronized List<Producto> menu() { return new ArrayList<>(menu); }

    public synchronized Producto buscar(int id) {
        return menu.stream().filter(p -> p.id() == id).findFirst().orElse(null);
    }

    public synchronized List<String> categorias() {
        return menu.stream().map(Producto::categoria).filter(c -> !c.isBlank())
                .distinct().sorted().toList();
    }

    /** Crea (id = 0) o edita un producto. Devuelve el id. En edición conserva el estado manual de agotado. */
    public synchronized int guardarProducto(int id, String nombre, String categoria, double precio,
                                            String descripcion, String imagen, int stock) {
        nombre = limpiar(nombre).trim();
        categoria = limpiar(categoria).trim();
        descripcion = limpiar(descripcion).trim();
        imagen = limpiar(imagen == null ? "" : imagen).trim();
        if (stock < 0) stock = -1;
        if (id == 0) {
            id = siguienteId++;
            menu.add(new Producto(id, nombre, categoria, precio, true, descripcion, imagen, stock));
        } else {
            for (int i = 0; i < menu.size(); i++) {
                Producto a = menu.get(i);
                if (a.id() == id) menu.set(i, new Producto(id, nombre, categoria, precio, a.disponible(), descripcion, imagen, stock));
            }
        }
        guardar();
        notificar();
        return id;
    }

    public synchronized void alternarDisponible(int id) {
        for (int i = 0; i < menu.size(); i++) {
            Producto p = menu.get(i);
            if (p.id() == id) menu.set(i, new Producto(id, p.nombre(), p.categoria(), p.precio(), !p.disponible(),
                    p.descripcion(), p.imagen(), p.stock()));
        }
        guardar();
        notificar();
    }

    public synchronized void eliminarProducto(int id) {
        menu.removeIf(p -> p.id() == id);
        promos.removeIf(p -> p.productoId() == id);
        guardar();
        guardarPromos();
        notificar();
    }

    private void ponerStock(int id, int nuevo) {
        for (int i = 0; i < menu.size(); i++) {
            Producto p = menu.get(i);
            if (p.id() == id) menu.set(i, new Producto(id, p.nombre(), p.categoria(), p.precio(), p.disponible(),
                    p.descripcion(), p.imagen(), Math.max(0, nuevo)));
        }
        guardar();
    }

    private void devolverStock(int id, int cantidad) {
        Producto p = buscar(id);
        if (p != null && p.stock() >= 0) ponerStock(id, p.stock() + cantidad);
    }

    /** Copia una foto a la carpeta fotos/ y devuelve su ruta relativa (o null si falla). */
    public static String copiarFoto(File origen) {
        try {
            Path dir = Path.of("fotos");
            Files.createDirectories(dir);
            String nombre = System.currentTimeMillis() + "_" + origen.getName().replaceAll("[^A-Za-z0-9._-]", "_");
            Files.copy(origen.toPath(), dir.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
            return "fotos/" + nombre;
        } catch (IOException e) {
            return null;
        }
    }

    // ---------- Promociones ----------
    public synchronized List<Promocion> promos() { return new ArrayList<>(promos); }

    public synchronized Promocion promoPara(int productoId) {
        return promos.stream().filter(p -> p.activa() && p.productoId() == productoId).findFirst().orElse(null);
    }

    public synchronized void crearPromo(int productoId, String tipo, double valor) {
        promos.add(new Promocion(siguientePromo++, productoId, tipo, valor, true));
        guardarPromos();
        notificar();
    }

    public synchronized void alternarPromo(int id) {
        for (int i = 0; i < promos.size(); i++) {
            Promocion p = promos.get(i);
            if (p.id() == id) promos.set(i, new Promocion(id, p.productoId(), p.tipo(), p.valor(), !p.activa()));
        }
        guardarPromos();
        notificar();
    }

    public synchronized void eliminarPromo(int id) {
        promos.removeIf(p -> p.id() == id);
        guardarPromos();
        notificar();
    }

    private Linea armar(long id, Producto p, int cant, String nota, String estado, String hora) {
        Promocion pr = promoPara(p.id());
        double ahorro = 0;
        String etiqueta = "";
        if (pr != null) {
            if ("2X1".equals(pr.tipo())) {
                ahorro = (cant / 2) * p.precio();
                etiqueta = "2x1";
            } else {
                ahorro = Math.round(p.precio() * cant * pr.valor() / 100.0);
                etiqueta = (long) pr.valor() + "% dto";
            }
        }
        if (ahorro <= 0) { ahorro = 0; etiqueta = ""; }
        return new Linea(id, p, cant, nota, estado, hora, ahorro, etiqueta);
    }

    /** Calcula una línea (con promoción) sin guardarla: sirve para el carrito del cliente. */
    public synchronized Linea cotizar(Producto p, int cantidad, String nota) {
        Producto fresco = buscar(p.id());
        return armar(0, fresco != null ? fresco : p, cantidad, nota == null ? "" : limpiar(nota.trim()), "", "");
    }

    // ---------- Pedidos y cuentas ----------
    /** Agrega a la mesa. Devuelve false si el producto no existe, está agotado o no alcanza el stock. */
    public synchronized boolean pedir(int mesa, Producto p, int cantidad, String nota) {
        Producto actual = buscar(p.id());
        if (actual == null || !actual.puedePedirse() || cantidad <= 0) return false;
        if (actual.stock() >= 0) {
            if (cantidad > actual.stock()) return false;
            ponerStock(actual.id(), actual.stock() - cantidad);
        }
        Producto despues = buscar(actual.id());
        pedidos.computeIfAbsent(mesa, k -> new ArrayList<>()).add(armar(siguienteLinea++, despues, cantidad,
                nota == null ? "" : limpiar(nota.trim()), PENDIENTE, HORA.format(LocalTime.now())));
        notificar();
        return true;
    }

    public synchronized List<Linea> lineas(int mesa) {
        return new ArrayList<>(pedidos.getOrDefault(mesa, List.of()));
    }

    public synchronized void quitarLinea(int mesa, int indice) {
        List<Linea> l = pedidos.get(mesa);
        if (l != null && indice >= 0 && indice < l.size()) {
            Linea x = l.remove(indice);
            if (PENDIENTE.equals(x.estado())) devolverStock(x.producto().id(), x.cantidad());
        }
        notificar();
    }

    /** Solo se puede cambiar la cantidad de lo que aún está pendiente en cocina. */
    public synchronized boolean cambiarCantidad(int mesa, int indice, int cantidad) {
        List<Linea> l = pedidos.get(mesa);
        if (l == null || indice < 0 || indice >= l.size() || cantidad <= 0) return false;
        Linea x = l.get(indice);
        if (!PENDIENTE.equals(x.estado())) return false;
        Producto p = buscar(x.producto().id());
        int dif = cantidad - x.cantidad();
        if (p != null && p.stock() >= 0) {
            if (dif > p.stock()) return false;
            ponerStock(p.id(), p.stock() - dif);
        }
        l.set(indice, armar(x.id(), p != null ? p : x.producto(), cantidad, x.nota(), x.estado(), x.hora()));
        notificar();
        return true;
    }

    public synchronized void cambiarEstado(long idLinea, String estado) {
        for (List<Linea> l : pedidos.values()) {
            for (int i = 0; i < l.size(); i++) {
                if (l.get(i).id() == idLinea) l.set(i, l.get(i).conEstado(estado));
            }
        }
        notificar();
    }

    /** Pedidos que la cocina todavía no ha entregado, del más antiguo al más nuevo. */
    public synchronized List<EnCocina> cocina() {
        List<EnCocina> r = new ArrayList<>();
        for (Map.Entry<Integer, List<Linea>> e : pedidos.entrySet()) {
            for (Linea l : e.getValue()) if (!ENTREGADO.equals(l.estado())) r.add(new EnCocina(e.getKey(), l));
        }
        r.sort(Comparator.comparingLong(c -> c.linea().id()));
        return r;
    }

    public synchronized double total(int mesa) {
        return lineas(mesa).stream().mapToDouble(Linea::subtotal).sum();
    }

    public synchronized boolean hayListo(int mesa) {
        return lineas(mesa).stream().anyMatch(l -> LISTO.equals(l.estado()));
    }

    public synchronized void transferir(int origen, int destino) {
        if (origen == destino) return;
        List<Linea> l = pedidos.remove(origen);
        if (l != null) pedidos.computeIfAbsent(destino, k -> new ArrayList<>()).addAll(l);
        if (llamadas.remove(origen)) llamadas.add(destino);
        notificar();
    }

    public synchronized int mesasOcupadas() {
        int n = 0;
        for (int m = 1; m <= mesas(); m++) if (total(m) > 0) n++;
        return n;
    }

    private Venta armarVenta(int mesa, Set<Long> ids, double descPct, double propPct, String pago,
                             String usuario, int personas) {
        List<Linea> sel = new ArrayList<>();
        for (Linea l : lineas(mesa)) if (ids == null || ids.contains(l.id())) sel.add(l);
        if (sel.isEmpty()) return null;
        double sub = sel.stream().mapToDouble(Linea::subtotal).sum();
        double desc = Math.round(sub * descPct / 100.0);
        double prop = Math.round((sub - desc) * propPct / 100.0);
        List<Detalle> det = new ArrayList<>();
        for (Linea l : sel) {
            det.add(new Detalle(l.producto().nombre(), l.producto().categoria(), l.cantidad(), l.subtotal(), l.nota()));
        }
        return new Venta(FECHA.format(LocalDateTime.now()), mesa, sub, desc, prop, sub - desc + prop,
                limpiar(pago), limpiar(usuario), Math.max(1, personas), det);
    }

    /** Calcula una cuenta sin cobrarla (ids = null: toda la mesa). */
    public synchronized Venta previsualizar(int mesa, Set<Long> ids, double descPct, double propPct,
                                            String pago, int personas) {
        return armarVenta(mesa, ids, descPct, propPct, pago, "", personas);
    }

    /** Cobra las líneas indicadas (null = todas), guarda la venta y libera lo cobrado de la mesa. */
    public synchronized Venta cobrar(int mesa, Set<Long> ids, double descPct, double propPct, String pago,
                                     String usuario, int personas) {
        Venta v = armarVenta(mesa, ids, descPct, propPct, pago, usuario, personas);
        if (v == null) return null;
        ventas.add(v);
        guardarVentas();
        List<Linea> l = pedidos.get(mesa);
        if (l != null) {
            l.removeIf(x -> ids == null || ids.contains(x.id()));
            if (l.isEmpty()) {
                pedidos.remove(mesa);
                llamadas.remove(mesa);
            }
        }
        notificar();
        return v;
    }

    // ---------- Historial de ventas ----------
    public synchronized List<Venta> ventas() { return new ArrayList<>(ventas); }

    /** Ventas cuya fecha (yyyy-MM-dd) está entre desde y hasta, ambas incluidas. */
    public synchronized List<Venta> ventasEntre(String desde, String hasta) {
        return ventas.stream().filter(v -> {
            String dia = v.fecha().substring(0, 10);
            return dia.compareTo(desde) >= 0 && dia.compareTo(hasta) <= 0;
        }).collect(Collectors.toList());
    }

    public synchronized void borrarVentas() {
        ventas.clear();
        guardarVentas();
        notificar();
    }

    public static String hoy() { return LocalDate.now().toString(); }

    // ---------- Reservas ----------
    public synchronized List<Reserva> reservas() {
        List<Reserva> r = new ArrayList<>(reservas);
        r.sort(Comparator.comparing(Reserva::fecha).thenComparing(Reserva::hora));
        return r;
    }

    /** Devuelve un mensaje de error, o null si la reserva quedó guardada. */
    public synchronized String crearReserva(int mesa, String nombre, int personas, String fecha, String hora,
                                            String telefono) {
        if (mesa < 1 || mesa > mesas()) return "Esa mesa no existe.";
        if (nombre == null || nombre.isBlank()) return "Escribe el nombre de quien reserva.";
        if (personas < 1) return "Indica cuántas personas son.";
        LocalDate dia;
        try { dia = LocalDate.parse(fecha.trim()); } catch (Exception e) { return "Fecha inválida (usa AAAA-MM-DD)."; }
        if (dia.isBefore(LocalDate.now())) return "La fecha ya pasó.";
        LocalTime h;
        try {
            String[] p = hora.trim().split(":");
            h = LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
        } catch (Exception e) { return "Hora inválida (usa HH:MM, por ejemplo 19:30)."; }
        for (Reserva r : reservas) {
            if (r.mesa() == mesa && r.fecha().equals(dia.toString())) {
                long dif = Math.abs(java.time.Duration.between(LocalTime.parse(r.hora()), h).toMinutes());
                if (dif < 90) return "La mesa " + mesa + " ya está reservada a las " + r.hora() + ".";
            }
        }
        reservas.add(new Reserva(siguienteReserva++, mesa, limpiar(nombre).trim(), personas, dia.toString(),
                HORA.format(h), limpiar(telefono == null ? "" : telefono).trim()));
        guardarReservas();
        notificar();
        return null;
    }

    public synchronized void eliminarReserva(int id) {
        reservas.removeIf(r -> r.id() == id);
        guardarReservas();
        notificar();
    }

    /** La primera reserva de hoy para esa mesa, o null. */
    public synchronized Reserva reservaDeHoy(int mesa) {
        return reservas().stream().filter(r -> r.mesa() == mesa && r.fecha().equals(hoy())).findFirst().orElse(null);
    }

    // ---------- Llamado al mesero ----------
    public synchronized void llamar(int mesa) { llamadas.add(mesa); notificar(); }
    public synchronized void atender(int mesa) { llamadas.remove(mesa); notificar(); }
    public synchronized boolean llamando(int mesa) { return llamadas.contains(mesa); }

    // ---------- Notificaciones a las pantallas ----------
    public void alCambiar(Runnable r) { oyentes.add(r); }

    private void notificar() {
        for (Runnable r : oyentes) SwingUtilities.invokeLater(r);
    }

    // ---------- Persistencia ----------
    private static List<String> leer(Path p) {
        try { return Files.readAllLines(p, StandardCharsets.UTF_8); } catch (IOException e) { return List.of(); }
    }

    private static void escribir(Path p, List<String> lineas) {
        try {
            Files.write(p, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo guardar " + p + ": " + e.getMessage());
        }
    }

    private void cargar() {
        for (String linea : leer(ARCHIVO)) {
            try {
                String[] p = linea.split(";", -1);
                if (p.length < 4) continue;
                int id = Integer.parseInt(p[0].trim());
                boolean disp = p.length < 5 || !p[4].trim().equals("0");
                String desc = p.length > 5 ? p[5] : "";
                String img = p.length > 6 ? p[6] : "";
                int stock = p.length > 7 && !p[7].isBlank() ? Integer.parseInt(p[7].trim()) : -1;
                menu.add(new Producto(id, p[1], p[2], Double.parseDouble(p[3]), disp, desc, img, stock));
                siguienteId = Math.max(siguienteId, id + 1);
            } catch (NumberFormatException e) {
                // línea dañada: se ignora
            }
        }
    }

    private void guardar() {
        List<String> l = new ArrayList<>();
        for (Producto p : menu) {
            l.add(p.id() + ";" + p.nombre() + ";" + p.categoria() + ";" + Math.round(p.precio()) + ";"
                    + (p.disponible() ? "1" : "0") + ";" + p.descripcion() + ";" + p.imagen() + ";" + p.stock());
        }
        escribir(ARCHIVO, l);
    }

    private void cargarVentas() {
        for (String linea : leer(ARCHIVO_VENTAS)) {
            try {
                String[] p = linea.split(";", -1);
                if (p.length < 7) continue;
                String usuario = p.length > 7 ? p[7] : "";
                int personas = p.length > 8 && !p[8].isBlank() ? Integer.parseInt(p[8]) : 1;
                List<Detalle> det = new ArrayList<>();
                if (p.length > 9 && !p[9].isBlank()) {
                    for (String d : p[9].split("~")) {
                        String[] x = d.split("\\^", -1);
                        if (x.length >= 4) det.add(new Detalle(x[0], x[1], Integer.parseInt(x[2]),
                                Double.parseDouble(x[3]), x.length > 4 ? x[4] : ""));
                    }
                }
                ventas.add(new Venta(p[0], Integer.parseInt(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                        Double.parseDouble(p[4]), Double.parseDouble(p[5]), p[6], usuario, personas, det));
            } catch (NumberFormatException e) {
                // línea dañada: se ignora
            }
        }
    }

    private void guardarVentas() {
        List<String> l = new ArrayList<>();
        for (Venta v : ventas) {
            String det = v.detalle().stream()
                    .map(d -> d.producto() + "^" + d.categoria() + "^" + d.cantidad() + "^" + Math.round(d.valor()) + "^" + d.nota())
                    .collect(Collectors.joining("~"));
            l.add(v.fecha() + ";" + v.mesa() + ";" + Math.round(v.subtotal()) + ";" + Math.round(v.descuento()) + ";"
                    + Math.round(v.propina()) + ";" + Math.round(v.total()) + ";" + v.pago() + ";" + v.usuario() + ";"
                    + v.personas() + ";" + det);
        }
        escribir(ARCHIVO_VENTAS, l);
    }

    private void cargarPromos() {
        for (String linea : leer(ARCHIVO_PROMOS)) {
            try {
                String[] p = linea.split(";", -1);
                if (p.length < 5) continue;
                int id = Integer.parseInt(p[0]);
                promos.add(new Promocion(id, Integer.parseInt(p[1]), p[2], Double.parseDouble(p[3]), p[4].equals("1")));
                siguientePromo = Math.max(siguientePromo, id + 1);
            } catch (NumberFormatException e) {
                // ignorar
            }
        }
    }

    private void guardarPromos() {
        List<String> l = new ArrayList<>();
        for (Promocion p : promos) {
            l.add(p.id() + ";" + p.productoId() + ";" + p.tipo() + ";" + p.valor() + ";" + (p.activa() ? "1" : "0"));
        }
        escribir(ARCHIVO_PROMOS, l);
    }

    private void cargarReservas() {
        for (String linea : leer(ARCHIVO_RESERVAS)) {
            try {
                String[] p = linea.split(";", -1);
                if (p.length < 7) continue;
                int id = Integer.parseInt(p[0]);
                siguienteReserva = Math.max(siguienteReserva, id + 1);
                if (p[4].compareTo(hoy()) < 0) continue; // reservas de días pasados se descartan
                reservas.add(new Reserva(id, Integer.parseInt(p[1]), p[2], Integer.parseInt(p[3]), p[4], p[5], p[6]));
            } catch (NumberFormatException e) {
                // ignorar
            }
        }
    }

    private void guardarReservas() {
        List<String> l = new ArrayList<>();
        for (Reserva r : reservas) {
            l.add(r.id() + ";" + r.mesa() + ";" + r.nombre() + ";" + r.personas() + ";" + r.fecha() + ";" + r.hora() + ";" + r.telefono());
        }
        escribir(ARCHIVO_RESERVAS, l);
    }

    private void cargarConfig() {
        if (!Files.exists(ARCHIVO_CONFIG)) return;
        try (Reader r = Files.newBufferedReader(ARCHIVO_CONFIG, StandardCharsets.UTF_8)) {
            config.load(r);
        } catch (IOException e) {
            // se usan los valores por defecto
        }
    }

    private void guardarConfig() {
        try (Writer w = Files.newBufferedWriter(ARCHIVO_CONFIG, StandardCharsets.UTF_8)) {
            config.store(w, "Ajustes del restaurante");
        } catch (IOException e) {
            System.err.println("No se pudo guardar la configuración: " + e.getMessage());
        }
    }

    private static int entero(String s, int defecto) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return defecto; }
    }

    private static String limpiar(String s) {
        return s.replace(";", ",").replace("~", "-").replace("^", "-").replace("\n", " ").replace("\r", " ");
    }

    // ---------- Utilidades de interfaz ----------
    public static String dinero(double v) { return String.format("$%,.0f", v); }

    public static DefaultTableModel tabla(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}
