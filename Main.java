package restaurante;

import javax.swing.*;
import java.awt.*;

/** Pantalla de inicio: panel del restaurante (con clave), cocina o menú del cliente. */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Estilo.iniciar();
            Datos d = Datos.get();
            JFrame f = new JFrame(d.nombre());
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(1040, 460);
            f.setLocationRelativeTo(null);

            JPanel fondo = new JPanel(new BorderLayout());
            fondo.setBackground(Estilo.FONDO);
            fondo.add(Estilo.cabecera(d.nombre(), "Sistema de pedidos y atención", null), BorderLayout.NORTH);

            JPanel cuerpo = new JPanel(new GridLayout(1, 3, 24, 0));
            cuerpo.setOpaque(false);
            cuerpo.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
            cuerpo.add(opcion("Panel del restaurante",
                    "Mesas, cobros, reservas, menú, promociones, ventas y reportes. Requiere usuario y clave.",
                    "Entrar al panel", Estilo.OSCURO, () -> {
                        Usuarios.Usuario u = Login.pedir(f);
                        if (u != null) new AdminFrame(u).setVisible(true);
                    }));
            cuerpo.add(opcion("Cocina",
                    "Ve los pedidos que llegan y márcalos como preparando o listos.",
                    "Abrir cocina", Estilo.TEAL, () -> new CocinaFrame().setVisible(true)));
            cuerpo.add(opcion("Menú del cliente",
                    "Mira la carta con fotos, haz tu pedido, sigue su estado y llama al mesero.",
                    "Ver la carta", Estilo.ACENTO, () -> new ClienteFrame().setVisible(true)));
            fondo.add(cuerpo, BorderLayout.CENTER);

            f.add(fondo);
            f.setVisible(true);
        });
    }

    private static JComponent opcion(String titulo, String descripcion, String boton, Color color, Runnable accion) {
        Estilo.Tarjeta t = new Estilo.Tarjeta(new BorderLayout(0, 12));
        JLabel lt = new JLabel(titulo);
        lt.setFont(Estilo.GRANDE);
        lt.setForeground(Estilo.OSCURO);
        JLabel ld = new JLabel("<html><div style='width:220px'>" + descripcion + "</div></html>");
        ld.setFont(Estilo.NORMAL);
        ld.setForeground(Estilo.SUAVE);
        ld.setVerticalAlignment(SwingConstants.TOP);
        Estilo.Boton b = new Estilo.Boton(boton, color);
        b.addActionListener(e -> accion.run());
        t.add(lt, BorderLayout.NORTH);
        t.add(ld, BorderLayout.CENTER);
        t.add(b, BorderLayout.SOUTH);
        return t;
    }
}
