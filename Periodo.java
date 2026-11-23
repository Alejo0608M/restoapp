package restaurante;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/** Selector de periodo (hoy, 7 días, mes, todo o fechas a elección) para ventas y reportes. */
public class Periodo extends JPanel {

    private static final String[] OPCIONES = {"Hoy", "Últimos 7 días", "Este mes", "Todo", "Personalizado"};
    private final JComboBox<String> cb = new JComboBox<>(OPCIONES);
    private final JTextField txtDesde = Estilo.campo(new JTextField(8));
    private final JTextField txtHasta = Estilo.campo(new JTextField(8));
    private final Runnable alCambiar;
    private String desde = "0000-01-01";
    private String hasta = "9999-12-31";

    public Periodo(int inicial, Runnable alCambiar) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 0));
        setOpaque(false);
        this.alCambiar = alCambiar;
        Estilo.Boton aplicar = new Estilo.Boton("Aplicar", Estilo.ACENTO);
        aplicar.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        txtDesde.setToolTipText("AAAA-MM-DD");
        txtHasta.setToolTipText("AAAA-MM-DD");
        add(new JLabel("Periodo:"));
        add(cb);
        add(new JLabel("Desde:"));
        add(txtDesde);
        add(new JLabel("Hasta:"));
        add(txtHasta);
        add(aplicar);
        cb.setSelectedIndex(inicial);
        calcular();
        cb.addActionListener(e -> {
            calcular();
            if (!personalizado()) alCambiar.run();
        });
        aplicar.addActionListener(e -> aplicar());
    }

    private boolean personalizado() { return cb.getSelectedIndex() == 4; }

    private void calcular() {
        LocalDate hoy = LocalDate.now();
        switch (cb.getSelectedIndex()) {
            case 0 -> { desde = hoy.toString(); hasta = hoy.toString(); }
            case 1 -> { desde = hoy.minusDays(6).toString(); hasta = hoy.toString(); }
            case 2 -> { desde = hoy.withDayOfMonth(1).toString(); hasta = hoy.toString(); }
            case 3 -> { desde = "0000-01-01"; hasta = "9999-12-31"; }
            default -> {
                if (txtDesde.getText().isBlank()) txtDesde.setText(hoy.withDayOfMonth(1).toString());
                if (txtHasta.getText().isBlank()) txtHasta.setText(hoy.toString());
                desde = txtDesde.getText().trim();
                hasta = txtHasta.getText().trim();
            }
        }
        if (!personalizado()) {
            boolean todo = cb.getSelectedIndex() == 3;
            txtDesde.setText(todo ? "" : desde);
            txtHasta.setText(todo ? "" : hasta);
        }
        txtDesde.setEditable(personalizado());
        txtHasta.setEditable(personalizado());
    }

    private void aplicar() {
        if (personalizado()) {
            try {
                LocalDate d = LocalDate.parse(txtDesde.getText().trim());
                LocalDate h = LocalDate.parse(txtHasta.getText().trim());
                if (h.isBefore(d)) {
                    JOptionPane.showMessageDialog(this, "La fecha final es anterior a la inicial.");
                    return;
                }
                desde = d.toString();
                hasta = h.toString();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Escribe las fechas como AAAA-MM-DD, por ejemplo 2026-11-04.");
                return;
            }
        }
        alCambiar.run();
    }

    public String desde() { return desde; }
    public String hasta() { return hasta; }

    public String texto() {
        return cb.getSelectedIndex() == 3 ? "todo el historial" : (desde.equals(hasta) ? desde : desde + " a " + hasta);
    }

    public String nombreArchivo() {
        return cb.getSelectedIndex() == 3 ? "todo" : (desde.equals(hasta) ? desde : desde + "_a_" + hasta);
    }
}
