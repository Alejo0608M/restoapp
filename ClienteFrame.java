package restaurante;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Inicio 2: carta para el cliente (fotos, promos, notas, estado del pedido, cuenta y llamado al mesero). */
public class ClienteFrame extends JFrame {

    private final Datos d = Datos.get();
    private final JComboBox<Integer> cbMesa = new JComboBox<>();
    private final DefaultTableModel tMenu = Datos.tabla("Producto", "Categoría", "Precio", "Promo", "Estado");
    private final JTable tablaMenu = new JTable(tMenu);
    private final DefaultTableModel tCarrito = Datos.tabla("Producto", "Cant.", "Nota", "Subtotal");
    private final JTable tablaCarrito = new JTable(tCarrito);
    private final DefaultTableModel tEnviados = Datos.tabla("Producto", "Cant.", "Estado");
    private final JTable tablaEnviados = new JTable(tEnviados);
    private final JLabel lblTotal = new JLabel("Total del pedido: $0");
    private final JLabel lblEnMesa = new JLabel("Ya consumido en tu mesa: $0");
    private final JLabel lblFoto = new JLabel();
    private final JLabel lblInfo = new JLabel();
    private final JTextField txtBuscar = Estilo.campo(new JTextField());
    private final JTextField txtNota = Estilo.campo(new JTextField(14));
    private final JSpinner cant = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final List<Datos.Linea> carrito = new ArrayList<>();
    private List<Datos.Producto> menuActual = List.of();
    private String categoriaSel = null; // null = todas
    private boolean reconstruyendo;

    public ClienteFrame() {
        super("Menú del cliente");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 740);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Estilo.FONDO);

        // ---- Cabecera con mesa y acciones rápidas ----
        reconstruirMesas();
        cbMesa.addActionListener(e -> { if (!reconstruyendo) refrescarPedido(); });
        JLabel lblMesa = new JLabel("Mi mesa:");
        lblMesa.setFont(Estilo.NEGRITA);
        lblMesa.setForeground(Color.WHITE);
        Estilo.Boton llamar = new Estilo.Boton("Llamar al mesero", Estilo.AMBAR);
        Estilo.Boton cuenta = new Estilo.Boton("Ver mi cuenta", Estilo.ACENTO);
        llamar.addActionListener(e -> {
            d.llamar(mesa());
            JOptionPane.showMessageDialog(this, "Avisamos al mesero. Enseguida te atienden.");
        });
        cuenta.addActionListener(e -> verCuenta());
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setOpaque(false);
        derecha.add(lblMesa);
        derecha.add(cbMesa);
        derecha.add(llamar);
        derecha.add(cuenta);
        add(Estilo.cabecera(d.nombre(), "Elige, personaliza y pide desde tu mesa", derecha), BorderLayout.NORTH);

        Estilo.tabla(tablaMenu);
        Estilo.tabla(tablaCarrito);
        Estilo.tabla(tablaEnviados);
        Estilo.colorearEstado(tablaMenu, 4);
        Estilo.colorearEstado(tablaEnviados, 2);
        tablaEnviados.setRowHeight(32);

        // ---- Carta (izquierda) ----
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refrescarMenu(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refrescarMenu(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refrescarMenu(); }
        });
        JPanel filaBuscar = new JPanel(new BorderLayout(8, 0));
        filaBuscar.setOpaque(false);
        filaBuscar.add(new JLabel("Buscar:"), BorderLayout.WEST);
        filaBuscar.add(txtBuscar, BorderLayout.CENTER);
        chips.setOpaque(false);
        JPanel filtros = new JPanel(new BorderLayout(0, 6));
        filtros.setOpaque(false);
        filtros.add(filaBuscar, BorderLayout.NORTH);
        filtros.add(chips, BorderLayout.CENTER);

        tablaMenu.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) agregarSeleccionado(1); }
        });
        tablaMenu.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) mostrarDetalle(); });

        // detalle del plato: foto + descripción
        lblFoto.setPreferredSize(new Dimension(96, 96));
        lblInfo.setVerticalAlignment(SwingConstants.TOP);
        JPanel detalle = new JPanel(new BorderLayout(14, 0));
        detalle.setOpaque(false);
        detalle.setPreferredSize(new Dimension(0, 104));
        detalle.add(lblFoto, BorderLayout.WEST);
        detalle.add(lblInfo, BorderLayout.CENTER);

        Estilo.Boton agregar = new Estilo.Boton("Agregar al pedido", Estilo.ACENTO);
        agregar.addActionListener(e -> agregarSeleccionado((int) cant.getValue()));
        JPanel cantidad = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        cantidad.setOpaque(false);
        cantidad.add(new JLabel("Cant.:"));
        cantidad.add(cant);
        JPanel controles = new JPanel(new BorderLayout(8, 0));
        controles.setOpaque(false);
        controles.add(cantidad, BorderLayout.WEST);
        txtNota.setToolTipText("Ej.: sin cebolla, sin hielo, término medio");
        JPanel filaNota = new JPanel(new BorderLayout(6, 0));
        filaNota.setOpaque(false);
        filaNota.add(new JLabel("Nota:"), BorderLayout.WEST);
        filaNota.add(txtNota, BorderLayout.CENTER);
        controles.add(filaNota, BorderLayout.CENTER);
        controles.add(agregar, BorderLayout.EAST);

        JPanel sur = new JPanel(new BorderLayout(0, 10));
        sur.setOpaque(false);
        sur.add(detalle, BorderLayout.CENTER);
        sur.add(controles, BorderLayout.SOUTH);

        JPanel norteCarta = new JPanel(new BorderLayout(0, 8));
        norteCarta.setOpaque(false);
        norteCarta.add(Estilo.titulo("Carta"), BorderLayout.NORTH);
        norteCarta.add(filtros, BorderLayout.CENTER);

        Estilo.Tarjeta carta = new Estilo.Tarjeta(new BorderLayout(0, 10));
        carta.add(norteCarta, BorderLayout.NORTH);
        carta.add(Estilo.scroll(tablaMenu), BorderLayout.CENTER);
        carta.add(sur, BorderLayout.SOUTH);

        // ---- Pedido (derecha) ----
        Estilo.Boton mas = new Estilo.Boton("+", Estilo.NAV);
        Estilo.Boton menos = new Estilo.Boton("−", Estilo.NAV);
        Estilo.Boton quitar = new Estilo.Boton("Quitar", Estilo.SUAVE);
        Estilo.Boton vaciar = new Estilo.Boton("Vaciar", Estilo.ROJO);
        Estilo.Boton enviar = new Estilo.Boton("Enviar pedido", Estilo.VERDE);
        mas.addActionListener(e -> modificar(1));
        menos.addActionListener(e -> modificar(-1));
        quitar.addActionListener(e -> {
            int fila = tablaCarrito.getSelectedRow();
            if (fila >= 0) { carrito.remove(fila); refrescarPedido(); }
        });
        vaciar.addActionListener(e -> {
            if (!carrito.isEmpty() && JOptionPane.showConfirmDialog(this, "¿Vaciar todo tu pedido?",
                    "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                carrito.clear();
                refrescarPedido();
            }
        });
        enviar.addActionListener(e -> enviarPedido());

        lblTotal.setFont(Estilo.GRANDE);
        lblTotal.setForeground(Estilo.ACENTO);
        lblEnMesa.setFont(Estilo.NORMAL);
        lblEnMesa.setForeground(Estilo.SUAVE);
        JPanel totales = new JPanel(new GridLayout(2, 1));
        totales.setOpaque(false);
        totales.add(lblTotal);
        totales.add(lblEnMesa);
        JPanel botonesPedido = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botonesPedido.setOpaque(false);
        botonesPedido.add(menos);
        botonesPedido.add(mas);
        botonesPedido.add(quitar);
        botonesPedido.add(vaciar);
        botonesPedido.add(enviar);
        JPanel surPedido = new JPanel(new BorderLayout(0, 10));
        surPedido.setOpaque(false);
        surPedido.add(totales, BorderLayout.NORTH);
        surPedido.add(botonesPedido, BorderLayout.CENTER);

        JPanel tablas = new JPanel(new GridLayout(2, 1, 0, 12));
        tablas.setOpaque(false);
        tablas.add(seccion("Por enviar", tablaCarrito));
        tablas.add(seccion("En cocina y en tu mesa", tablaEnviados));

        Estilo.Tarjeta pedido = new Estilo.Tarjeta(new BorderLayout(0, 12));
        pedido.add(Estilo.titulo("Mi pedido"), BorderLayout.NORTH);
        pedido.add(tablas, BorderLayout.CENTER);
        pedido.add(surPedido, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 16, 0));
        centro.setOpaque(false);
        centro.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        centro.add(carta);
        centro.add(pedido);
        add(centro, BorderLayout.CENTER);

        d.alCambiar(() -> { reconstruirMesas(); refrescarMenu(); refrescarPedido(); });
        refrescarMenu();
        refrescarPedido();
        mostrarDetalle();
    }

    private JComponent seccion(String titulo, JTable t) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(titulo);
        l.setFont(Estilo.NEGRITA);
        l.setForeground(Estilo.SUAVE);
        p.add(l, BorderLayout.NORTH);
        p.add(Estilo.scroll(t), BorderLayout.CENTER);
        return p;
    }

    private int mesa() { return cbMesa.getSelectedItem() == null ? 1 : (int) cbMesa.getSelectedItem(); }

    /** Ajusta la lista de mesas si el administrador cambió su número. */
    private void reconstruirMesas() {
        if (cbMesa.getItemCount() == d.mesas()) return;
        reconstruyendo = true;
        Object previa = cbMesa.getSelectedItem();
        cbMesa.removeAllItems();
        for (int i = 1; i <= d.mesas(); i++) cbMesa.addItem(i);
        if (previa != null && (int) previa <= d.mesas()) cbMesa.setSelectedItem(previa);
        reconstruyendo = false;
    }

    private void mostrarDetalle() {
        int fila = tablaMenu.getSelectedRow();
        if (fila < 0 || fila >= menuActual.size()) {
            lblFoto.setIcon(null);
            lblInfo.setText("<html><div style='width:300px;color:#7A6E62'>Selecciona un plato para ver su foto y su "
                    + "descripción. También puedes hacer doble clic para agregarlo.</div></html>");
            return;
        }
        Datos.Producto p = menuActual.get(fila);
        lblFoto.setIcon(Estilo.foto(p.imagen(), p.nombre(), 96));
        Datos.Promocion promo = d.promoPara(p.id());
        String extra = promo == null ? "" : "<br><span style='color:#C0392B'><b>Promoción: " + promo.descripcion() + "</b></span>";
        String desc = p.descripcion().isBlank() ? "Sin descripción." : p.descripcion();
        lblInfo.setText("<html><div style='width:300px'><b style='font-size:15px'>" + p.nombre() + "</b>  ·  "
                + Datos.dinero(p.precio()) + "<br><span style='color:#7A6E62'>" + desc + "</span>" + extra + "</div></html>");
    }

    private int enCarrito(int productoId) {
        int n = 0;
        for (Datos.Linea l : carrito) if (l.producto().id() == productoId) n += l.cantidad();
        return n;
    }

    private void agregarSeleccionado(int cantidad) {
        int fila = tablaMenu.getSelectedRow();
        if (fila < 0 || fila >= menuActual.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto de la carta.");
            return;
        }
        Datos.Producto p = menuActual.get(fila);
        if (!p.puedePedirse()) {
            JOptionPane.showMessageDialog(this, "\"" + p.nombre() + "\" está agotado por hoy.");
            return;
        }
        if (p.stock() >= 0 && enCarrito(p.id()) + cantidad > p.stock()) {
            JOptionPane.showMessageDialog(this, "Solo quedan " + p.stock() + " de \"" + p.nombre() + "\".");
            return;
        }
        carrito.add(d.cotizar(p, cantidad, txtNota.getText()));
        txtNota.setText("");
        cant.setValue(1);
        refrescarPedido();
    }

    /** Sube o baja la cantidad de la línea seleccionada del pedido. */
    private void modificar(int delta) {
        int fila = tablaCarrito.getSelectedRow();
        if (fila < 0) return;
        Datos.Linea l = carrito.get(fila);
        int nueva = l.cantidad() + delta;
        if (nueva <= 0) carrito.remove(fila);
        else {
            Datos.Producto p = d.buscar(l.producto().id());
            if (p != null && p.stock() >= 0 && enCarrito(p.id()) + delta > p.stock()) {
                JOptionPane.showMessageDialog(this, "Solo quedan " + p.stock() + " de \"" + p.nombre() + "\".");
                return;
            }
            carrito.set(fila, d.cotizar(l.producto(), Math.min(nueva, 50), l.nota()));
        }
        refrescarPedido();
        if (fila < carrito.size()) tablaCarrito.setRowSelectionInterval(fila, fila);
    }

    private void enviarPedido() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tu pedido está vacío.");
            return;
        }
        List<String> rechazados = new ArrayList<>();
        for (Datos.Linea l : carrito) {
            if (!d.pedir(mesa(), l.producto(), l.cantidad(), l.nota())) rechazados.add(l.producto().nombre());
        }
        carrito.clear();
        refrescarPedido();
        if (rechazados.isEmpty()) {
            JOptionPane.showMessageDialog(this, "¡Pedido enviado a la cocina! Puedes ver cómo va en \"En cocina y en tu mesa\".");
        } else {
            JOptionPane.showMessageDialog(this, "Pedido enviado, excepto lo que se agotó: " + String.join(", ", rechazados));
        }
    }

    private void verCuenta() {
        Datos.Venta v = d.previsualizar(mesa(), null, 0, d.propinaSugerida(), "Por cobrar", 1);
        if (v == null) {
            JOptionPane.showMessageDialog(this, "Aún no has pedido nada en la mesa " + mesa() + ".");
            return;
        }
        Recibo.ofrecer(this, v, "Mi cuenta");
    }

    private void refrescarMenu() {
        List<String> categorias = d.categorias();
        if (categoriaSel != null && !categorias.contains(categoriaSel)) categoriaSel = null;
        chips.removeAll();
        chips.add(chip("Todas", null));
        for (String c : categorias) chips.add(chip(c, c));
        chips.revalidate();
        chips.repaint();

        Datos.Producto elegido = null;
        int sel = tablaMenu.getSelectedRow();
        if (sel >= 0 && sel < menuActual.size()) elegido = menuActual.get(sel);

        String texto = txtBuscar.getText().trim().toLowerCase();
        menuActual = new ArrayList<>();
        for (Datos.Producto p : d.menu()) {
            boolean okCat = categoriaSel == null || categoriaSel.equals(p.categoria());
            boolean okTxt = texto.isEmpty() || p.nombre().toLowerCase().contains(texto);
            if (okCat && okTxt) menuActual.add(p);
        }
        tMenu.setRowCount(0);
        int reselect = -1;
        for (int i = 0; i < menuActual.size(); i++) {
            Datos.Producto p = menuActual.get(i);
            Datos.Promocion promo = d.promoPara(p.id());
            tMenu.addRow(new Object[]{p.nombre(), p.categoria(), Datos.dinero(p.precio()),
                    promo == null ? "" : promo.descripcion(), p.estado()});
            if (elegido != null && p.id() == elegido.id()) reselect = i;
        }
        if (reselect >= 0) tablaMenu.setRowSelectionInterval(reselect, reselect);
        mostrarDetalle();
    }

    private Estilo.Boton chip(String texto, String categoria) {
        boolean activo = Objects.equals(categoria, categoriaSel);
        Estilo.Boton b = Estilo.chip(texto, activo);
        b.addActionListener(e -> { categoriaSel = categoria; refrescarMenu(); });
        return b;
    }

    private void refrescarPedido() {
        tCarrito.setRowCount(0);
        double total = 0;
        for (Datos.Linea l : carrito) {
            String nombre = l.producto().nombre() + (l.promo().isBlank() ? "" : " (" + l.promo() + ")");
            tCarrito.addRow(new Object[]{nombre, l.cantidad(), l.nota(), Datos.dinero(l.subtotal())});
            total += l.subtotal();
        }
        lblTotal.setText("Total del pedido: " + Datos.dinero(total));
        lblEnMesa.setText("Ya consumido en tu mesa: " + Datos.dinero(d.total(mesa())));
        tEnviados.setRowCount(0);
        for (Datos.Linea l : d.lineas(mesa())) {
            tEnviados.addRow(new Object[]{l.producto().nombre(), l.cantidad(), l.estado()});
        }
    }
}
