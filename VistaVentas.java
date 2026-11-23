package restaurante;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Historial de ventas por periodo, con producto más vendido y exportación a CSV (Excel). */
public class VistaVentas extends JPanel {

    private final Datos d = Datos.get();
    private final Usuarios.Usuario usuario;
    private final Periodo periodo = new Periodo(0, this::refrescar);
    private final DefaultTableModel tVentas = Datos.tabla("Fecha", "Mesa", "Subtotal", "Desc.", "Propina", "Total", "Pago", "Atendió");
    private final JTable tabla = new JTable(tVentas);
    private final JLabel lblVendido = new JLabel("$0");
    private final JLabel lblCuentas = new JLabel("0");
    private final JLabel lblTicket = new JLabel("$0");
    private final JLabel lblPropinas = new JLabel("$0");
    private final JLabel lblTop = new JLabel("Sin datos");
    private List<Datos.Venta> visibles = new ArrayList<>();

    public VistaVentas(Usuarios.Usuario usuario) {
        super(new BorderLayout(0, 12));
        this.usuario = usuario;
        setOpaque(false);
        Estilo.tabla(tabla);

        JPanel stats = new JPanel(new GridLayout(1, 5, 12, 0));
        stats.setOpaque(false);
        stats.add(Estilo.dato("Vendido", lblVendido, Estilo.ACENTO));
        stats.add(Estilo.dato("Cuentas cobradas", lblCuentas, Estilo.OSCURO));
        stats.add(Estilo.dato("Ticket promedio", lblTicket, Estilo.VERDE));
        stats.add(Estilo.dato("Propinas", lblPropinas, Estilo.AMBAR));
        stats.add(Estilo.dato("Producto más vendido", lblTop, Estilo.MORADO));
        lblTop.setFont(Estilo.MEDIA);

        JPanel norte = new JPanel(new BorderLayout(0, 12));
        norte.setOpaque(false);
        norte.add(periodo, BorderLayout.NORTH);
        norte.add(stats, BorderLayout.CENTER);

        Estilo.Boton recibo = new Estilo.Boton("Ver recibo", Estilo.ACENTO);
        Estilo.Boton exportar = new Estilo.Boton("Exportar a Excel (CSV)", Estilo.VERDE);
        recibo.addActionListener(e -> verRecibo());
        exportar.addActionListener(e -> exportar());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(recibo);
        botones.add(exportar);
        if (usuario.rol().esAdmin()) {
            Estilo.Boton borrar = new Estilo.Boton("Borrar historial", Estilo.ROJO);
            borrar.addActionListener(e -> {
                if (JOptionPane.showConfirmDialog(this, "¿Borrar TODO el historial de ventas? No se puede deshacer.",
                        "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    d.borrarVentas();
                }
            });
            botones.add(borrar);
        }
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) verRecibo(); }
        });

        Estilo.Tarjeta t = new Estilo.Tarjeta(new BorderLayout(0, 12));
        t.add(Estilo.titulo("Cuentas cobradas"), BorderLayout.NORTH);
        t.add(Estilo.scroll(tabla), BorderLayout.CENTER);
        t.add(botones, BorderLayout.SOUTH);

        add(norte, BorderLayout.NORTH);
        add(t, BorderLayout.CENTER);
        d.alCambiar(this::refrescar);
        refrescar();
    }

    private void verRecibo() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= visibles.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona una cuenta de la lista (o haz doble clic en ella).");
            return;
        }
        Recibo.ofrecer(this, visibles.get(fila), "Recibo");
    }

    private static String csv(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }

    private void exportar() {
        if (visibles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay ventas en este periodo para exportar.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("ventas_" + periodo.nombreArchivo() + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getPath() + ".csv");
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("Fecha;Mesa;Subtotal;Descuento;Propina;Total;Pago;Atendió;Personas;Productos\r\n");
        double sub = 0, desc = 0, prop = 0, total = 0;
        List<Datos.Venta> orden = new ArrayList<>(visibles);
        Collections.reverse(orden);
        for (Datos.Venta v : orden) {
            String productos = v.detalle().stream().map(x -> x.cantidad() + "x " + x.producto())
                    .collect(Collectors.joining(" | "));
            sb.append(v.fecha()).append(';').append(v.mesa()).append(';').append(Math.round(v.subtotal())).append(';')
              .append(Math.round(v.descuento())).append(';').append(Math.round(v.propina())).append(';')
              .append(Math.round(v.total())).append(';').append(csv(v.pago())).append(';').append(csv(v.usuario()))
              .append(';').append(v.personas()).append(';').append(csv(productos)).append("\r\n");
            sub += v.subtotal();
            desc += v.descuento();
            prop += v.propina();
            total += v.total();
        }
        sb.append("TOTAL;;").append(Math.round(sub)).append(';').append(Math.round(desc)).append(';')
          .append(Math.round(prop)).append(';').append(Math.round(total)).append(";;;;\r\n");
        try {
            Files.writeString(f.toPath(), sb.toString(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Exportadas " + orden.size() + " cuentas a:\n" + f.getAbsolutePath()
                    + "\n\nÁbrelo con Excel (separado por punto y coma).");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo: " + ex.getMessage());
        }
    }

    private void refrescar() {
        visibles = new ArrayList<>(d.ventasEntre(periodo.desde(), periodo.hasta()));
        Collections.reverse(visibles);
        double total = 0, propinas = 0;
        Map<String, Integer> cantidades = new HashMap<>();
        for (Datos.Venta v : visibles) {
            total += v.total();
            propinas += v.propina();
            for (Datos.Detalle x : v.detalle()) cantidades.merge(x.producto(), x.cantidad(), Integer::sum);
        }
        final List<Datos.Venta> lista = visibles;
        Estilo.conservar(tabla, () -> {
            tVentas.setRowCount(0);
            for (Datos.Venta v : lista) {
                tVentas.addRow(new Object[]{v.fecha(), "Mesa " + v.mesa(), Datos.dinero(v.subtotal()),
                        Datos.dinero(v.descuento()), Datos.dinero(v.propina()), Datos.dinero(v.total()), v.pago(), v.usuario()});
            }
        });
        lblVendido.setText(Datos.dinero(total));
        lblCuentas.setText(String.valueOf(visibles.size()));
        lblTicket.setText(Datos.dinero(visibles.isEmpty() ? 0 : total / visibles.size()));
        lblPropinas.setText(Datos.dinero(propinas));
        Map.Entry<String, Integer> top = cantidades.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
        lblTop.setText(top == null ? "Sin datos" : top.getKey() + " (" + top.getValue() + ")");
    }
}
