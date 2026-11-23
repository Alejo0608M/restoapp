package restaurante;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Ajustes del restaurante (nombre, número de mesas, propina) y administración de usuarios. Solo administrador. */
public class VistaAjustes extends JPanel {

    private final Datos d = Datos.get();
    private final Usuarios.Usuario actual;
    private final JTextField txtNombre = Estilo.campo(new JTextField(d.nombre(), 18));
    private final JSpinner spMesas = new JSpinner(new SpinnerNumberModel(d.mesas(), 1, 60, 1));
    private final JSpinner spPropina = new JSpinner(new SpinnerNumberModel(d.propinaSugerida(), 0, 30, 1));
    private final DefaultTableModel tUsuarios = Datos.tabla("Usuario", "Rol");
    private final JTable tabla = new JTable(tUsuarios);
    private List<Usuarios.Usuario> usuarios = new ArrayList<>();

    public VistaAjustes(Usuarios.Usuario actual) {
        super(new BorderLayout(16, 0));
        this.actual = actual;
        setOpaque(false);
        Estilo.tabla(tabla);

        // ---- Restaurante ----
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        fila(form, 0, "Nombre del restaurante:", txtNombre);
        fila(form, 1, "Número de mesas:", spMesas);
        fila(form, 2, "Propina sugerida (%):", spPropina);
        Estilo.Boton guardar = new Estilo.Boton("Guardar ajustes", Estilo.VERDE);
        guardar.addActionListener(e -> guardar());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pie.setOpaque(false);
        pie.add(guardar);
        JPanel contenido = new JPanel(new BorderLayout(0, 14));
        contenido.setOpaque(false);
        contenido.add(form, BorderLayout.NORTH);
        contenido.add(pie, BorderLayout.CENTER);
        Estilo.Tarjeta izq = new Estilo.Tarjeta(new BorderLayout(0, 12));
        izq.setPreferredSize(new Dimension(400, 0));
        izq.add(Estilo.titulo("Restaurante"), BorderLayout.NORTH);
        izq.add(contenido, BorderLayout.CENTER);

        // ---- Usuarios ----
        Estilo.Boton crear = new Estilo.Boton("Crear", Estilo.VERDE);
        Estilo.Boton clave = new Estilo.Boton("Cambiar clave", Estilo.ACENTO);
        Estilo.Boton rol = new Estilo.Boton("Cambiar rol", Estilo.NAV);
        Estilo.Boton eliminar = new Estilo.Boton("Eliminar", Estilo.ROJO);
        crear.addActionListener(e -> crear());
        clave.addActionListener(e -> cambiarClave());
        rol.addActionListener(e -> cambiarRol());
        eliminar.addActionListener(e -> eliminar());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(crear);
        botones.add(clave);
        botones.add(rol);
        botones.add(eliminar);
        JLabel ayuda = new JLabel("<html><div style='width:420px;color:#7A6E62'>Administrador: todo. Cajero: mesas, cobros, "
                + "ventas, reportes y reservas. Mesero: mesas y reservas (no cobra).</div></html>");
        JPanel norte = new JPanel(new BorderLayout(0, 6));
        norte.setOpaque(false);
        norte.add(Estilo.titulo("Usuarios y roles"), BorderLayout.NORTH);
        norte.add(ayuda, BorderLayout.CENTER);
        Estilo.Tarjeta der = new Estilo.Tarjeta(new BorderLayout(0, 12));
        der.add(norte, BorderLayout.NORTH);
        der.add(Estilo.scroll(tabla), BorderLayout.CENTER);
        der.add(botones, BorderLayout.SOUTH);

        add(izq, BorderLayout.WEST);
        add(der, BorderLayout.CENTER);
        cargarUsuarios();
    }

    private static void fila(JPanel f, int y, String etiqueta, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = y; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(6, 0, 6, 12);
        f.add(new JLabel(etiqueta), g);
        g = new GridBagConstraints();
        g.gridx = 1; g.gridy = y; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(6, 0, 6, 0);
        f.add(c, g);
    }

    private void guardar() {
        d.fijarAjustes(txtNombre.getText(), (int) spPropina.getValue());
        if (!d.fijarMesas((int) spMesas.getValue())) {
            JOptionPane.showMessageDialog(this, "No se pueden quitar mesas que tienen pedidos abiertos. Cóbralas primero.");
            spMesas.setValue(d.mesas());
            return;
        }
        JOptionPane.showMessageDialog(this, "Ajustes guardados.");
    }

    private void cargarUsuarios() {
        usuarios = Usuarios.lista();
        tUsuarios.setRowCount(0);
        for (Usuarios.Usuario u : usuarios) tUsuarios.addRow(new Object[]{u.nombre(), u.rol().etiqueta()});
    }

    private Usuarios.Usuario seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= usuarios.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario de la lista.");
            return null;
        }
        return usuarios.get(fila);
    }

    private void resultado(String error) {
        if (error != null) JOptionPane.showMessageDialog(this, error);
        cargarUsuarios();
    }

    private void crear() {
        JTextField nombre = Estilo.campo(new JTextField());
        JPasswordField clave = Estilo.campo(new JPasswordField());
        JComboBox<Usuarios.Rol> rol = new JComboBox<>(Usuarios.Rol.values());
        rol.setSelectedItem(Usuarios.Rol.MESERO);
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"Usuario (sin espacios):", nombre,
                "Clave (mínimo 4 caracteres):", clave, "Rol:", rol}, "Nuevo usuario", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            resultado(Usuarios.crear(nombre.getText(), new String(clave.getPassword()), (Usuarios.Rol) rol.getSelectedItem()));
        }
    }

    private void cambiarClave() {
        Usuarios.Usuario u = seleccionado();
        if (u == null) return;
        JPasswordField a = Estilo.campo(new JPasswordField());
        JPasswordField b = Estilo.campo(new JPasswordField());
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"Nueva clave para " + u.nombre() + ":", a,
                "Repite la clave:", b}, "Cambiar clave", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        String x = new String(a.getPassword());
        if (!x.equals(new String(b.getPassword()))) {
            JOptionPane.showMessageDialog(this, "Las claves no coinciden.");
            return;
        }
        resultado(Usuarios.cambiarClave(u.nombre(), x));
    }

    private void cambiarRol() {
        Usuarios.Usuario u = seleccionado();
        if (u == null) return;
        JComboBox<Usuarios.Rol> rol = new JComboBox<>(Usuarios.Rol.values());
        rol.setSelectedItem(u.rol());
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"Rol de " + u.nombre() + ":", rol}, "Cambiar rol",
                JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) resultado(Usuarios.cambiarRol(u.nombre(), (Usuarios.Rol) rol.getSelectedItem()));
    }

    private void eliminar() {
        Usuarios.Usuario u = seleccionado();
        if (u == null) return;
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar al usuario \"" + u.nombre() + "\"?", "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            resultado(Usuarios.eliminar(u.nombre(), actual.nombre()));
        }
    }
}
