package restaurante;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Promociones (2x1, descuento) y menú del día / combos con precio fijo. */
public class VistaPromos extends Estilo.Tarjeta {

    private final Datos d = Datos.get();
    private final DefaultTableModel tPromos = Datos.tabla("Producto", "Promoción", "Estado");
    private final JTable tabla = new JTable(tPromos);
    private List<Datos.Promocion> actuales = new ArrayList<>();

    public VistaPromos() {
        super(new BorderLayout(0, 12));
        Estilo.tabla(tabla);
        Estilo.colorearEstado(tabla, 2);

        JLabel ayuda = new JLabel("<html><div style='width:700px;color:#7A6E62'>Las promociones se aplican solas cuando el "
                + "cliente o el mesero piden el producto. <b>2x1</b>: por cada 2 unidades se cobra 1. "
                + "El <b>menú del día</b> es un plato combo con precio fijo que aparece en la carta.</div></html>");
        JPanel norte = new JPanel(new BorderLayout(0, 6));
        norte.setOpaque(false);
        norte.add(Estilo.titulo("Promociones y menú del día"), BorderLayout.NORTH);
        norte.add(ayuda, BorderLayout.CENTER);

        Estilo.Boton nueva = new Estilo.Boton("Nueva promoción", Estilo.ACENTO);
        Estilo.Boton pausar = new Estilo.Boton("Pausar / Activar", Estilo.AMBAR);
        Estilo.Boton eliminar = new Estilo.Boton("Eliminar", Estilo.ROJO);
        Estilo.Boton combo = new Estilo.Boton("Nuevo menú del día", Estilo.VERDE);
        nueva.addActionListener(e -> nuevaPromo());
        pausar.addActionListener(e -> { Datos.Promocion p = seleccionada(); if (p != null) d.alternarPromo(p.id()); });
        eliminar.addActionListener(e -> { Datos.Promocion p = seleccionada(); if (p != null) d.eliminarPromo(p.id()); });
        combo.addActionListener(e -> nuevoCombo());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        botones.add(nueva);
        botones.add(pausar);
        botones.add(eliminar);
        botones.add(combo);

        add(norte, BorderLayout.NORTH);
        add(Estilo.scroll(tabla), BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        d.alCambiar(this::refrescar);
        refrescar();
    }

    private Datos.Promocion seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= actuales.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona una promoción de la lista.");
            return null;
        }
        return actuales.get(fila);
    }

    private void nuevaPromo() {
        List<Datos.Producto> menu = d.menu();
        if (menu.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Primero crea productos en el menú.");
            return;
        }
        JComboBox<Datos.Producto> cbP = new JComboBox<>(menu.toArray(new Datos.Producto[0]));
        JComboBox<String> cbT = new JComboBox<>(new String[]{"2x1", "Descuento %"});
        JSpinner valor = new JSpinner(new SpinnerNumberModel(20, 5, 90, 5));
        valor.setEnabled(false);
        cbT.addActionListener(e -> valor.setEnabled(cbT.getSelectedIndex() == 1));
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"Producto:", cbP, "Tipo de promoción:", cbT,
                "Porcentaje de descuento:", valor}, "Nueva promoción", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        Datos.Producto p = (Datos.Producto) cbP.getSelectedItem();
        if (cbT.getSelectedIndex() == 0) d.crearPromo(p.id(), "2X1", 0);
        else d.crearPromo(p.id(), "PCT", ((Number) valor.getValue()).doubleValue());
    }

    private void nuevoCombo() {
        JTextField nombre = Estilo.campo(new JTextField("Menú del día", 22));
        JTextArea desc = new JTextArea("Sopa + plato fuerte + jugo", 3, 22);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFont(Estilo.NORMAL);
        JTextField precio = Estilo.campo(new JTextField());
        JTextField stock = Estilo.campo(new JTextField());
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"Nombre:", nombre, "Qué incluye:", new JScrollPane(desc),
                "Precio fijo:", precio, "Existencias del día (vacío = sin límite):", stock},
                "Nuevo menú del día / combo", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        try {
            double valor = Double.parseDouble(precio.getText().trim());
            int existencias = stock.getText().isBlank() ? -1 : Integer.parseInt(stock.getText().trim());
            if (nombre.getText().isBlank() || valor < 0) throw new NumberFormatException();
            d.guardarProducto(0, nombre.getText(), "Menú del día", valor, desc.getText(), "", existencias);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Revisa el nombre, el precio y las existencias.");
        }
    }

    private void refrescar() {
        actuales = d.promos();
        Estilo.conservar(tabla, () -> {
            tPromos.setRowCount(0);
            for (Datos.Promocion p : actuales) {
                Datos.Producto prod = d.buscar(p.productoId());
                tPromos.addRow(new Object[]{prod == null ? "(eliminado)" : prod.nombre(), p.descripcion(),
                        p.activa() ? "Activa" : "Pausada"});
            }
        });
    }
}
