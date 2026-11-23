package restaurante;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Inicio 1: panel del restaurante. Las pestañas visibles dependen del rol del usuario. */
public class AdminFrame extends JFrame {

    private final Datos d = Datos.get();
    private final Usuarios.Usuario usuario;
    private final CardLayout cards = new CardLayout();
    private final JPanel contenido = new JPanel(cards);
    private final Map<String, Estilo.Boton> botonesNav = new LinkedHashMap<>();
    private final JPanel grillaMesas = new JPanel(new GridLayout(0, 2, 10, 10));
    private final DefaultTableModel tPedido = Datos.tabla("Producto", "Cant.", "Nota", "Estado", "Subtotal");
    private final JTable tablaPedido = new JTable(tPedido);
    private final JLabel lblMesa = new JLabel("Mesa 1");
    private final JLabel lblTotal = new JLabel("$0");
    private final JLabel lblOcupadas = new JLabel(" ");
    private int mesaSel = 1;

    public AdminFrame(Usuarios.Usuario usuario) {
        super("Panel del restaurante");
        this.usuario = usuario;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1280, 720);
        setMinimumSize(new Dimension(1100, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Estilo.FONDO);

        Usuarios.Rol rol = usuario.rol();
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        nav.setOpaque(false);
        Estilo.tabla(tablaPedido);
        Estilo.colorearEstado(tablaPedido, 3);
        contenido.setBackground(Estilo.FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        agregarVista(nav, "mesas", "Mesas", vistaMesas());
        agregarVista(nav, "reservas", "Reservas", new VistaReservas());
        if (rol.puedeVerMenu()) agregarVista(nav, "menu", "Menú", new VistaMenu(usuario));
        if (rol.esAdmin()) agregarVista(nav, "promos", "Promos", new VistaPromos());
        if (rol.puedeVerVentas()) {
            agregarVista(nav, "ventas", "Ventas", new VistaVentas(usuario));
            agregarVista(nav, "reportes", "Reportes", new VistaReportes());
        }
        if (rol.esAdmin()) agregarVista(nav, "ajustes", "Ajustes", new VistaAjustes(usuario));

        Estilo.Boton miClave = Estilo.nav("Mi clave");
        miClave.fondo = Estilo.SUAVE;
        miClave.addActionListener(e -> cambiarMiClave());
        Estilo.Boton salir = Estilo.nav("Salir");
        salir.fondo = Estilo.ROJO;
        salir.addActionListener(e -> dispose());
        nav.add(Box.createHorizontalStrut(10));
        nav.add(miClave);
        nav.add(salir);

        add(Estilo.cabecera(d.nombre() + " - Panel", usuario.nombre() + "  ·  " + rol.etiqueta(), nav), BorderLayout.NORTH);
        add(contenido, BorderLayout.CENTER);
        mostrar("mesas");

        d.alCambiar(this::refrescar);
        refrescar();
    }

    private void agregarVista(JPanel nav, String clave, String etiqueta, JComponent vista) {
        contenido.add(vista, clave);
        Estilo.Boton b = Estilo.nav(etiqueta);
        b.addActionListener(e -> mostrar(clave));
        botonesNav.put(clave, b);
        nav.add(b);
    }

    private void mostrar(String clave) {
        cards.show(contenido, clave);
        botonesNav.forEach((k, b) -> {
            b.fondo = k.equals(clave) ? Estilo.ACENTO : Estilo.NAV;
            b.repaint();
        });
    }

    private void cambiarMiClave() {
        JPasswordField actual = Estilo.campo(new JPasswordField());
        JPasswordField nueva = Estilo.campo(new JPasswordField());
        JPasswordField repetir = Estilo.campo(new JPasswordField());
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"Clave actual:", actual, "Clave nueva (mínimo 4):", nueva,
                "Repite la clave nueva:", repetir}, "Mi clave", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        if (Usuarios.entrar(usuario.nombre(), new String(actual.getPassword())) == null) {
            JOptionPane.showMessageDialog(this, "La clave actual no es correcta.");
            return;
        }
        String n = new String(nueva.getPassword());
        if (!n.equals(new String(repetir.getPassword()))) {
            JOptionPane.showMessageDialog(this, "Las claves nuevas no coinciden.");
            return;
        }
        String error = Usuarios.cambiarClave(usuario.nombre(), n);
        JOptionPane.showMessageDialog(this, error != null ? error : "Clave cambiada.");
    }

    // ---------- Vista de mesas ----------
    private JComponent vistaMesas() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);

        grillaMesas.setOpaque(false);
        JPanel contenedorGrilla = new Estilo.Rejilla(new BorderLayout());
        contenedorGrilla.add(grillaMesas, BorderLayout.NORTH);
        JScrollPane scrollMesas = new JScrollPane(contenedorGrilla);
        scrollMesas.setBorder(BorderFactory.createEmptyBorder());
        scrollMesas.setOpaque(false);
        scrollMesas.getViewport().setOpaque(false);
        scrollMesas.getVerticalScrollBar().setUnitIncrement(24);
        scrollMesas.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        lblOcupadas.setFont(Estilo.NEGRITA);
        lblOcupadas.setForeground(Estilo.SUAVE);
        lblOcupadas.setBorder(BorderFactory.createEmptyBorder(8, 2, 0, 0));
        JPanel izq = new JPanel(new BorderLayout());
        izq.setOpaque(false);
        izq.setPreferredSize(new Dimension(360, 0));
        izq.add(scrollMesas, BorderLayout.CENTER);
        izq.add(lblOcupadas, BorderLayout.SOUTH);

        lblMesa.setFont(Estilo.GRANDE);
        lblMesa.setForeground(Estilo.OSCURO);
        lblTotal.setFont(Estilo.GRANDE);
        lblTotal.setForeground(Estilo.ACENTO);
        JPanel enc = new JPanel(new BorderLayout());
        enc.setOpaque(false);
        enc.add(lblMesa, BorderLayout.WEST);
        enc.add(lblTotal, BorderLayout.EAST);

        Estilo.Boton agregar = new Estilo.Boton("Agregar", Estilo.ACENTO);
        Estilo.Boton cantidad = new Estilo.Boton("Cambiar cant.", Estilo.NAV);
        Estilo.Boton quitar = new Estilo.Boton("Quitar", Estilo.SUAVE);
        Estilo.Boton entregado = new Estilo.Boton("Entregado", Estilo.TEAL);
        Estilo.Boton transferir = new Estilo.Boton("Cambiar de mesa", Estilo.NAV);
        Estilo.Boton atender = new Estilo.Boton("Atender llamado", Estilo.AMBAR);
        Estilo.Boton precuenta = new Estilo.Boton("Pre-cuenta", Estilo.MORADO);
        Estilo.Boton cobrar = new Estilo.Boton("Cobrar cuenta", Estilo.VERDE);
        agregar.addActionListener(e -> agregarAMesa());
        cantidad.addActionListener(e -> cambiarCantidad());
        quitar.addActionListener(e -> quitar());
        entregado.addActionListener(e -> entregar());
        transferir.addActionListener(e -> transferirMesa());
        atender.addActionListener(e -> d.atender(mesaSel));
        precuenta.addActionListener(e -> preCuenta());
        cobrar.addActionListener(e -> cobrar());
        if (!usuario.rol().puedeCobrar()) {
            cobrar.setEnabled(false);
            cobrar.setToolTipText("Solo caja o administrador pueden cobrar");
        }
        JPanel botones = new JPanel(new GridLayout(2, 4, 8, 8));
        botones.setOpaque(false);
        for (JButton b : new JButton[]{agregar, cantidad, quitar, entregado, transferir, atender, precuenta, cobrar}) botones.add(b);

        Estilo.Tarjeta der = new Estilo.Tarjeta(new BorderLayout(0, 12));
        der.add(enc, BorderLayout.NORTH);
        der.add(Estilo.scroll(tablaPedido), BorderLayout.CENTER);
        der.add(botones, BorderLayout.SOUTH);

        p.add(izq, BorderLayout.WEST);
        p.add(der, BorderLayout.CENTER);
        return p;
    }

    private Datos.Linea lineaSeleccionada() {
        int fila = tablaPedido.getSelectedRow();
        List<Datos.Linea> ls = d.lineas(mesaSel);
        if (fila < 0 || fila >= ls.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona una línea del pedido.");
            return null;
        }
        return ls.get(fila);
    }

    private void agregarAMesa() {
        List<Datos.Producto> disponibles = new ArrayList<>();
        for (Datos.Producto p : d.menu()) if (p.puedePedirse()) disponibles.add(p);
        if (disponibles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos disponibles.");
            return;
        }
        JComboBox<Datos.Producto> cb = new JComboBox<>(disponibles.toArray(new Datos.Producto[0]));
        JSpinner cant = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        JTextField nota = Estilo.campo(new JTextField());
        int r = JOptionPane.showConfirmDialog(this,
                new Object[]{"Producto:", cb, "Cantidad:", cant, "Nota (opcional, ej. sin cebolla):", nota},
                "Agregar a mesa " + mesaSel, JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        Datos.Producto p = (Datos.Producto) cb.getSelectedItem();
        if (!d.pedir(mesaSel, p, (int) cant.getValue(), nota.getText())) {
            JOptionPane.showMessageDialog(this, "No alcanzan las existencias de \"" + p.nombre() + "\".");
        }
    }

    private void cambiarCantidad() {
        Datos.Linea l = lineaSeleccionada();
        if (l == null) return;
        if (!Datos.PENDIENTE.equals(l.estado())) {
            JOptionPane.showMessageDialog(this, "Eso ya está en preparación. Agrega otra línea si piden más.");
            return;
        }
        JSpinner cant = new JSpinner(new SpinnerNumberModel(l.cantidad(), 1, 50, 1));
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"Nueva cantidad:", cant}, "Cambiar cantidad",
                JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION && !d.cambiarCantidad(mesaSel, tablaPedido.getSelectedRow(), (int) cant.getValue())) {
            JOptionPane.showMessageDialog(this, "No alcanzan las existencias para esa cantidad.");
        }
    }

    private void quitar() {
        Datos.Linea l = lineaSeleccionada();
        if (l == null) return;
        if (!Datos.PENDIENTE.equals(l.estado()) && JOptionPane.showConfirmDialog(this,
                "Ya está en \"" + l.estado() + "\". ¿Quitarlo de todos modos?", "Confirmar",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        d.quitarLinea(mesaSel, tablaPedido.getSelectedRow());
    }

    private void entregar() {
        Datos.Linea l = lineaSeleccionada();
        if (l == null) return;
        if (!Datos.LISTO.equals(l.estado()) && JOptionPane.showConfirmDialog(this,
                "La cocina aún no lo marca como listo (" + l.estado() + "). ¿Marcarlo como entregado?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        d.cambiarEstado(l.id(), Datos.ENTREGADO);
    }

    private void transferirMesa() {
        if (d.lineas(mesaSel).isEmpty()) {
            JOptionPane.showMessageDialog(this, "La mesa " + mesaSel + " no tiene pedidos para mover.");
            return;
        }
        JComboBox<Integer> destino = new JComboBox<>();
        for (int m = 1; m <= d.mesas(); m++) if (m != mesaSel) destino.addItem(m);
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"Mover el pedido a la mesa:", destino},
                "Cambiar de mesa", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            int nueva = (int) destino.getSelectedItem();
            d.transferir(mesaSel, nueva);
            mesaSel = nueva;
            refrescar();
        }
    }

    private void preCuenta() {
        Datos.Venta v = d.previsualizar(mesaSel, null, 0, d.propinaSugerida(), "Por cobrar", 1);
        if (v == null) {
            JOptionPane.showMessageDialog(this, "La mesa " + mesaSel + " no tiene pedidos.");
            return;
        }
        Recibo.ofrecer(this, v, "Pre-cuenta");
    }

    private void cobrar() {
        if (d.lineas(mesaSel).isEmpty()) {
            JOptionPane.showMessageDialog(this, "La mesa " + mesaSel + " no tiene pedidos.");
            return;
        }
        Datos.Venta v = CobroDialog.mostrar(this, mesaSel, usuario);
        if (v != null) Recibo.ofrecer(this, v, "Cuenta cobrada");
    }

    // ---------- Refresco ----------
    private void refrescar() {
        if (mesaSel > d.mesas()) mesaSel = 1;
        grillaMesas.removeAll();
        for (int m = 1; m <= d.mesas(); m++) {
            final int mesa = m;
            double total = d.total(m);
            Datos.Reserva reserva = d.reservaDeHoy(m);
            String sub;
            Color color;
            if (d.llamando(m)) { sub = "Llama al mesero"; color = Estilo.ROJO; }
            else if (d.hayListo(m)) { sub = "Listo · " + Datos.dinero(total); color = Estilo.TEAL; }
            else if (total > 0) { sub = Datos.dinero(total); color = Estilo.ACENTO; }
            else if (reserva != null) { sub = "Reservada " + reserva.hora(); color = Estilo.MORADO; }
            else { sub = "Libre"; color = Estilo.VERDE; }
            Estilo.Boton b = new Estilo.Boton("<html><center><b>Mesa " + m + "</b><br>" + sub + "</center></html>", color);
            if (reserva != null) b.setToolTipText("Reserva de " + reserva.nombre() + " (" + reserva.personas() + " personas) a las " + reserva.hora());
            b.seleccionado = (m == mesaSel);
            b.addActionListener(e -> { mesaSel = mesa; refrescar(); });
            grillaMesas.add(b);
        }
        grillaMesas.revalidate();
        grillaMesas.repaint();
        lblOcupadas.setText(d.mesasOcupadas() + " de " + d.mesas() + " mesas ocupadas");

        Estilo.conservar(tablaPedido, () -> {
            tPedido.setRowCount(0);
            for (Datos.Linea l : d.lineas(mesaSel)) {
                String nombre = l.producto().nombre() + (l.promo().isBlank() ? "" : " (" + l.promo() + ")");
                tPedido.addRow(new Object[]{nombre, l.cantidad(), l.nota(), l.estado(), Datos.dinero(l.subtotal())});
            }
        });
        lblMesa.setText("Mesa " + mesaSel + (d.llamando(mesaSel) ? "  (llama al mesero)" : ""));
        lblTotal.setText(Datos.dinero(d.total(mesaSel)));
    }
}
