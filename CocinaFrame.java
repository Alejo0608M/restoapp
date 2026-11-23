package restaurante;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Inicio 3: pantalla de cocina. Muestra los pedidos y los marca como Preparando o Listo. */
public class CocinaFrame extends JFrame {

    private final Datos d = Datos.get();
    private final DefaultTableModel modelo = Datos.tabla("Mesa", "Cant.", "Producto", "Nota", "Hora", "Estado");
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblResumen = new JLabel(" ");
    private List<Datos.EnCocina> actuales = new ArrayList<>();
    private int pendientesAntes = -1;

    public CocinaFrame() {
        super("Cocina");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(980, 600);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Estilo.FONDO);

        lblResumen.setFont(Estilo.MEDIA);
        lblResumen.setForeground(Color.WHITE);
        add(Estilo.cabecera("Cocina", "Pedidos por preparar", lblResumen), BorderLayout.NORTH);

        Estilo.tabla(tabla);
        tabla.setFont(Estilo.MEDIA);
        tabla.setRowHeight(44);
        Estilo.colorearEstado(tabla, 5);
        tabla.getColumnModel().getColumn(0).setMinWidth(100);
        tabla.getColumnModel().getColumn(0).setMaxWidth(120);
        tabla.getColumnModel().getColumn(1).setMaxWidth(80);
        tabla.getColumnModel().getColumn(4).setMaxWidth(90);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(130);

        Estilo.Boton preparando = new Estilo.Boton("Preparando", Estilo.AMBAR);
        Estilo.Boton listo = new Estilo.Boton("Listo", Estilo.VERDE);
        Estilo.Boton pendiente = new Estilo.Boton("Volver a pendiente", Estilo.SUAVE);
        preparando.addActionListener(e -> marcar(Datos.PREPARANDO));
        listo.addActionListener(e -> marcar(Datos.LISTO));
        pendiente.addActionListener(e -> marcar(Datos.PENDIENTE));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(preparando);
        botones.add(listo);
        botones.add(pendiente);
        JLabel ayuda = new JLabel("Puedes seleccionar varias filas a la vez (Ctrl o Shift).");
        ayuda.setForeground(Estilo.SUAVE);
        ayuda.setFont(Estilo.NORMAL);

        Estilo.Tarjeta tarjeta = new Estilo.Tarjeta(new BorderLayout(0, 12));
        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        sur.add(botones, BorderLayout.WEST);
        sur.add(ayuda, BorderLayout.EAST);
        tarjeta.add(Estilo.scroll(tabla), BorderLayout.CENTER);
        tarjeta.add(sur, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        centro.add(tarjeta);
        add(centro, BorderLayout.CENTER);

        d.alCambiar(this::refrescar);
        refrescar();
    }

    private void marcar(String estado) {
        int[] filas = tabla.getSelectedRows();
        if (filas.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona uno o más pedidos de la lista.");
            return;
        }
        for (int f : filas) d.cambiarEstado(actuales.get(f).linea().id(), estado);
    }

    private void refrescar() {
        Set<Long> seleccion = new HashSet<>();
        for (int f : tabla.getSelectedRows()) if (f < actuales.size()) seleccion.add(actuales.get(f).linea().id());

        actuales = d.cocina();
        modelo.setRowCount(0);
        int pend = 0, prep = 0, listos = 0;
        for (Datos.EnCocina c : actuales) {
            Datos.Linea l = c.linea();
            modelo.addRow(new Object[]{"Mesa " + c.mesa(), l.cantidad(), l.producto().nombre(), l.nota(), l.hora(), l.estado()});
            switch (l.estado()) {
                case Datos.PENDIENTE -> pend++;
                case Datos.PREPARANDO -> prep++;
                case Datos.LISTO -> listos++;
                default -> { }
            }
        }
        for (int i = 0; i < actuales.size(); i++) {
            if (seleccion.contains(actuales.get(i).linea().id())) tabla.addRowSelectionInterval(i, i);
        }
        lblResumen.setText("Pendientes: " + pend + "   Preparando: " + prep + "   Listos: " + listos);
        if (pendientesAntes >= 0 && pend > pendientesAntes) {
            try { Toolkit.getDefaultToolkit().beep(); } catch (Exception ignorada) { }
        }
        pendientesAntes = pend;
    }
}
