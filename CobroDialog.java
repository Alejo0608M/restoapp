package restaurante;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Cobro de una mesa: cuenta completa, partes iguales entre varias personas, o solo algunos productos. */
public class CobroDialog extends JDialog {

    private final Datos d = Datos.get();
    private final int mesa;
    private final Usuarios.Usuario usuario;
    private final List<Datos.Linea> lineas;
    private final List<JCheckBox> checks = new ArrayList<>();
    private final JRadioButton rbTodo = new JRadioButton("Cuenta completa", true);
    private final JRadioButton rbPartes = new JRadioButton("Dividir en partes iguales");
    private final JRadioButton rbSel = new JRadioButton("Pagar solo lo seleccionado");
    private final JSpinner personas = new JSpinner(new SpinnerNumberModel(2, 2, 30, 1));
    private final JSpinner desc = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
    private final JSpinner prop;
    private final JComboBox<String> pago = new JComboBox<>(new String[]{"Efectivo", "Tarjeta", "Transferencia"});
    private final JTextArea resumen = new JTextArea(7, 28);
    private Datos.Venta resultado;

    private CobroDialog(Frame owner, int mesa, Usuarios.Usuario usuario) {
        super(owner, "Cobrar cuenta - Mesa " + mesa, true);
        this.mesa = mesa;
        this.usuario = usuario;
        this.lineas = d.lineas(mesa);
        this.prop = new JSpinner(new SpinnerNumberModel(d.propinaSugerida(), 0, 100, 5));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // ---- Productos (izquierda) ----
        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setOpaque(false);
        for (Datos.Linea l : lineas) {
            JCheckBox c = new JCheckBox(l.cantidad() + " x " + l.producto().nombre() + "  -  " + Datos.dinero(l.subtotal()), true);
            c.setOpaque(false);
            c.setFont(Estilo.NORMAL);
            c.setEnabled(false);
            c.addItemListener(e -> actualizar());
            checks.add(c);
            lista.add(c);
        }
        Estilo.Tarjeta izq = new Estilo.Tarjeta(new BorderLayout(0, 10));
        izq.add(Estilo.titulo("Productos de la cuenta"), BorderLayout.NORTH);
        JScrollPane sp = Estilo.scroll(lista);
        sp.setPreferredSize(new Dimension(340, 300));
        izq.add(sp, BorderLayout.CENTER);

        // ---- Opciones (derecha) ----
        ButtonGroup grupo = new ButtonGroup();
        for (JRadioButton r : new JRadioButton[]{rbTodo, rbPartes, rbSel}) {
            r.setOpaque(false);
            r.setFont(Estilo.NORMAL);
            grupo.add(r);
            r.addActionListener(e -> cambiarModo());
        }
        personas.setEnabled(false);
        personas.addChangeListener(e -> actualizar());
        desc.addChangeListener(e -> actualizar());
        prop.addChangeListener(e -> actualizar());
        resumen.setEditable(false);
        resumen.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        resumen.setBackground(Estilo.FONDO);
        resumen.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel opciones = new JPanel();
        opciones.setLayout(new BoxLayout(opciones, BoxLayout.Y_AXIS));
        opciones.setOpaque(false);
        opciones.add(alinear(rbTodo));
        opciones.add(alinear(fila(rbPartes, personas, new JLabel("personas"))));
        opciones.add(alinear(rbSel));
        opciones.add(Box.createVerticalStrut(10));
        opciones.add(alinear(fila(new JLabel("Descuento (%):"), desc)));
        opciones.add(alinear(fila(new JLabel("Propina (%):"), prop)));
        opciones.add(alinear(fila(new JLabel("Forma de pago:"), pago)));
        opciones.add(Box.createVerticalStrut(10));
        opciones.add(alinear(resumen));

        Estilo.Tarjeta der = new Estilo.Tarjeta(new BorderLayout(0, 10));
        der.add(Estilo.titulo("Cómo se paga"), BorderLayout.NORTH);
        der.add(opciones, BorderLayout.CENTER);

        // ---- Botones ----
        Estilo.Boton cancelar = new Estilo.Boton("Cancelar", Estilo.SUAVE);
        Estilo.Boton cobrar = new Estilo.Boton("Cobrar", Estilo.VERDE);
        cancelar.addActionListener(e -> dispose());
        cobrar.addActionListener(e -> cobrar());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(cancelar);
        botones.add(cobrar);

        JPanel centro = new JPanel(new BorderLayout(16, 0));
        centro.setOpaque(false);
        centro.add(izq, BorderLayout.WEST);
        centro.add(der, BorderLayout.CENTER);
        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Estilo.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);
        actualizar();
        pack();
        setLocationRelativeTo(owner);
    }

    private static JPanel fila(Component... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        p.setOpaque(false);
        for (Component c : cs) {
            if (c instanceof JLabel l) l.setFont(Estilo.NORMAL);
            p.add(c);
        }
        return p;
    }

    private static JComponent alinear(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private void cambiarModo() {
        boolean sel = rbSel.isSelected();
        for (JCheckBox c : checks) {
            if (!sel) c.setSelected(true);
            c.setEnabled(sel);
        }
        personas.setEnabled(rbPartes.isSelected());
        actualizar();
    }

    private Set<Long> seleccionados() {
        Set<Long> ids = new LinkedHashSet<>();
        for (int i = 0; i < lineas.size(); i++) if (checks.get(i).isSelected()) ids.add(lineas.get(i).id());
        return ids;
    }

    private static double pct(JSpinner s) { return ((Number) s.getValue()).doubleValue(); }

    private int cuantas() { return rbPartes.isSelected() ? (int) personas.getValue() : 1; }

    private void actualizar() {
        Datos.Venta v = d.previsualizar(mesa, seleccionados(), pct(desc), pct(prop), "", 1);
        if (v == null) {
            resumen.setText("Selecciona al menos un producto.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Subtotal:   %12s%n", Datos.dinero(v.subtotal())));
        sb.append(String.format("Descuento:  %12s%n", "-" + Datos.dinero(v.descuento())));
        sb.append(String.format("Propina:     %12s%n", Datos.dinero(v.propina())));
        sb.append(String.format("TOTAL:       %12s", Datos.dinero(v.total())));
        if (cuantas() > 1) {
            sb.append(String.format("%n%nCada persona (%d): %s", cuantas(), Datos.dinero(Math.ceil(v.total() / cuantas()))));
        }
        resumen.setText(sb.toString());
    }

    private void cobrar() {
        Set<Long> ids = seleccionados();
        if (ids.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona al menos un producto.");
            return;
        }
        resultado = d.cobrar(mesa, ids, pct(desc), pct(prop), (String) pago.getSelectedItem(),
                usuario.nombre(), cuantas());
        dispose();
    }

    /** Muestra el cobro y devuelve la venta registrada, o null si se canceló. */
    public static Datos.Venta mostrar(Frame owner, int mesa, Usuarios.Usuario usuario) {
        CobroDialog dlg = new CobroDialog(owner, mesa, usuario);
        dlg.setVisible(true);
        return dlg.resultado;
    }
}
