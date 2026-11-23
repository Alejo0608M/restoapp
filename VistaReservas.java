package restaurante;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Reservas de mesa por fecha y hora. La mesa aparece como "Reservada" el día de la reserva. */
public class VistaReservas extends Estilo.Tarjeta {

    private final Datos d = Datos.get();
    private final DefaultTableModel tReservas = Datos.tabla("Fecha", "Hora", "Mesa", "Nombre", "Personas", "Teléfono");
    private final JTable tabla = new JTable(tReservas);
    private List<Datos.Reserva> actuales = new ArrayList<>();

    public VistaReservas() {
        super(new BorderLayout(0, 12));
        Estilo.tabla(tabla);

        Estilo.Boton nueva = new Estilo.Boton("Nueva reserva", Estilo.VERDE);
        Estilo.Boton llegaron = new Estilo.Boton("Ya llegaron", Estilo.ACENTO);
        Estilo.Boton cancelar = new Estilo.Boton("Cancelar reserva", Estilo.ROJO);
        nueva.addActionListener(e -> nueva());
        llegaron.addActionListener(e -> {
            Datos.Reserva r = seleccionada();
            if (r == null) return;
            d.eliminarReserva(r.id());
            JOptionPane.showMessageDialog(this, "Reserva de " + r.nombre() + " atendida. La mesa " + r.mesa()
                    + " queda libre para tomar su pedido.");
        });
        cancelar.addActionListener(e -> {
            Datos.Reserva r = seleccionada();
            if (r != null && JOptionPane.showConfirmDialog(this, "¿Cancelar la reserva de " + r.nombre() + "?",
                    "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) d.eliminarReserva(r.id());
        });
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(nueva);
        botones.add(llegaron);
        botones.add(cancelar);

        add(Estilo.titulo("Reservas"), BorderLayout.NORTH);
        add(Estilo.scroll(tabla), BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        d.alCambiar(this::refrescar);
        refrescar();
    }

    private Datos.Reserva seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= actuales.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona una reserva de la lista.");
            return null;
        }
        return actuales.get(fila);
    }

    private void nueva() {
        JComboBox<Integer> mesa = new JComboBox<>();
        for (int m = 1; m <= d.mesas(); m++) mesa.addItem(m);
        JTextField nombre = Estilo.campo(new JTextField());
        JSpinner personas = new JSpinner(new SpinnerNumberModel(2, 1, 30, 1));
        JTextField fecha = Estilo.campo(new JTextField(Datos.hoy()));
        JTextField hora = Estilo.campo(new JTextField("20:00"));
        JTextField tel = Estilo.campo(new JTextField());
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"Mesa:", mesa, "A nombre de:", nombre,
                "Personas:", personas, "Fecha (AAAA-MM-DD):", fecha, "Hora (HH:MM):", hora, "Teléfono (opcional):", tel},
                "Nueva reserva", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        String error = d.crearReserva((int) mesa.getSelectedItem(), nombre.getText(), (int) personas.getValue(),
                fecha.getText(), hora.getText(), tel.getText());
        if (error != null) JOptionPane.showMessageDialog(this, error);
    }

    private void refrescar() {
        actuales = d.reservas();
        Estilo.conservar(tabla, () -> {
            tReservas.setRowCount(0);
            for (Datos.Reserva r : actuales) {
                String cuando = r.fecha().equals(Datos.hoy()) ? "Hoy" : r.fecha();
                tReservas.addRow(new Object[]{cuando, r.hora(), "Mesa " + r.mesa(), r.nombre(), r.personas(), r.telefono()});
            }
        });
    }
}
