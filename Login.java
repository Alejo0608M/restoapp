package restaurante;

import javax.swing.*;
import java.awt.*;

/** Pide usuario y clave antes de abrir el panel del restaurante. */
public final class Login {
    private Login() {}

    /** Devuelve el usuario que entró, o null si canceló o se equivocó 3 veces. */
    public static Usuarios.Usuario pedir(Component parent) {
        JTextField usuario = Estilo.campo(new JTextField(16));
        JPasswordField clave = Estilo.campo(new JPasswordField(16));
        for (int intento = 0; intento < 3; intento++) {
            java.util.List<Object> msg = new java.util.ArrayList<>();
            msg.add("Usuario:");
            msg.add(usuario);
            msg.add("Clave:");
            msg.add(clave);
            if (Usuarios.claveInicial()) {
                JLabel aviso = new JLabel("<html><div style='width:230px;color:#7A6E62'>Primera vez: usuario <b>admin</b>, "
                        + "clave <b>admin123</b>. Cámbiala después en \"Mi clave\".</div></html>");
                msg.add(aviso);
            }
            SwingUtilities.invokeLater(usuario::requestFocusInWindow);
            int r = JOptionPane.showConfirmDialog(parent, msg.toArray(), "Iniciar sesión",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return null;
            Usuarios.Usuario u = Usuarios.entrar(usuario.getText(), new String(clave.getPassword()));
            if (u != null) return u;
            clave.setText("");
            JOptionPane.showMessageDialog(parent, "Usuario o clave incorrectos.", "No se pudo entrar",
                    JOptionPane.WARNING_MESSAGE);
        }
        return null;
    }
}
