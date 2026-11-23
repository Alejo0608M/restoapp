package restaurante;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;

/** Gestión del menú: productos, stock, descripción y foto. */
public class VistaMenu extends Estilo.Tarjeta {

    private final Datos d = Datos.get();
    private final Usuarios.Usuario usuario;
    private final DefaultTableModel tMenu = Datos.tabla("ID", "Nombre", "Categoría", "Precio", "Stock", "Estado");
    private final JTable tabla = new JTable(tMenu);
    private final JTextField txtBuscar = Estilo.campo(new JTextField(14));
    private final JComboBox<String> cbCategoria = new JComboBox<>();
    private boolean actualizando;

    public VistaMenu(Usuarios.Usuario usuario) {
        super(new BorderLayout(0, 12));
        this.usuario = usuario;
        Estilo.tabla(tabla);
        Estilo.colorearEstado(tabla, 5);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Buscar:"));
        filtros.add(txtBuscar);
        filtros.add(cbCategoria);
        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(Estilo.titulo("Productos del menú"), BorderLayout.WEST);
        norte.add(filtros, BorderLayout.EAST);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setOpaque(false);
        if (usuario.rol().puedeEditarMenu()) {
            Estilo.Boton crear = new Estilo.Boton("Crear", Estilo.VERDE);
            Estilo.Boton editar = new Estilo.Boton("Editar", Estilo.ACENTO);
            Estilo.Boton eliminar = new Estilo.Boton("Eliminar", Estilo.ROJO);
            crear.addActionListener(e -> formulario(null));
            editar.addActionListener(e -> { Datos.Producto p = seleccionado(); if (p != null) formulario(p); });
            eliminar.addActionListener(e -> {
                Datos.Producto p = seleccionado();
                if (p != null && JOptionPane.showConfirmDialog(this, "¿Eliminar \"" + p.nombre() + "\"?",
                        "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) d.eliminarProducto(p.id());
            });
            botones.add(crear);
            botones.add(editar);
            botones.add(eliminar);
        }
        Estilo.Boton agotado = new Estilo.Boton("Agotado / Disponible", Estilo.AMBAR);
        agotado.addActionListener(e -> { Datos.Producto p = seleccionado(); if (p != null) d.alternarDisponible(p.id()); });
        botones.add(agotado);

        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refrescar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refrescar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refrescar(); }
        });
        cbCategoria.addActionListener(e -> { if (!actualizando) refrescar(); });

        add(norte, BorderLayout.NORTH);
        add(Estilo.scroll(tabla), BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        d.alCambiar(this::refrescar);
        refrescar();
    }

    private Datos.Producto seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto primero.");
            return null;
        }
        return d.buscar((int) tMenu.getValueAt(fila, 0));
    }

    private static void fila(JPanel f, int y, String etiqueta, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = y; g.anchor = GridBagConstraints.NORTHWEST; g.insets = new Insets(5, 4, 5, 10);
        f.add(new JLabel(etiqueta), g);
        g = new GridBagConstraints();
        g.gridx = 1; g.gridy = y; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(5, 0, 5, 4);
        f.add(c, g);
    }

    private void formulario(Datos.Producto p) {
        JTextField nombre = Estilo.campo(new JTextField(p == null ? "" : p.nombre(), 24));
        JComboBox<String> cat = new JComboBox<>(d.categorias().toArray(new String[0]));
        cat.setEditable(true);
        cat.setSelectedItem(p == null ? "" : p.categoria());
        JTextField precio = Estilo.campo(new JTextField(p == null ? "" : String.valueOf(Math.round(p.precio()))));
        JTextField stock = Estilo.campo(new JTextField(p == null || p.stock() < 0 ? "" : String.valueOf(p.stock())));
        stock.setToolTipText("Déjalo vacío si no quieres controlar existencias");
        JTextArea desc = new JTextArea(p == null ? "" : p.descripcion(), 3, 24);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFont(Estilo.NORMAL);
        JScrollPane descSp = new JScrollPane(desc);

        String[] foto = {p == null ? "" : p.imagen()};
        JLabel vista = new JLabel(Estilo.foto(foto[0], nombre.getText(), 72));
        Estilo.Boton elegir = new Estilo.Boton("Elegir foto...", Estilo.NAV);
        Estilo.Boton quitar = new Estilo.Boton("Quitar", Estilo.SUAVE);
        elegir.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        quitar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        elegir.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Imágenes (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File f = fc.getSelectedFile();
            String ruta = Datos.copiarFoto(f);
            if (ruta == null) {
                JOptionPane.showMessageDialog(this, "No se pudo copiar la foto.");
                return;
            }
            foto[0] = ruta;
            vista.setIcon(Estilo.foto(ruta, nombre.getText(), 72));
        });
        quitar.addActionListener(e -> {
            foto[0] = "";
            vista.setIcon(Estilo.foto("", nombre.getText(), 72));
        });
        JPanel filaFoto = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filaFoto.add(vista);
        filaFoto.add(elegir);
        filaFoto.add(quitar);

        JPanel form = new JPanel(new GridBagLayout());
        fila(form, 0, "Nombre:", nombre);
        fila(form, 1, "Categoría:", cat);
        fila(form, 2, "Precio:", precio);
        fila(form, 3, "Existencias:", stock);
        fila(form, 4, "Descripción:", descSp);
        fila(form, 5, "Foto:", filaFoto);

        int r = JOptionPane.showConfirmDialog(this, form, p == null ? "Nuevo producto" : "Editar producto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        try {
            double valor = Double.parseDouble(precio.getText().trim());
            int existencias = stock.getText().isBlank() ? -1 : Integer.parseInt(stock.getText().trim());
            if (nombre.getText().isBlank() || valor < 0 || (!stock.getText().isBlank() && existencias < 0)) {
                throw new NumberFormatException();
            }
            d.guardarProducto(p == null ? 0 : p.id(), nombre.getText(), String.valueOf(cat.getEditor().getItem()),
                    valor, desc.getText(), foto[0], existencias);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Revisa el nombre, el precio y las existencias (números sin letras).");
        }
    }

    private void refrescar() {
        actualizando = true;
        Object previa = cbCategoria.getSelectedItem();
        cbCategoria.removeAllItems();
        cbCategoria.addItem("Todas");
        for (String c : d.categorias()) cbCategoria.addItem(c);
        cbCategoria.setSelectedItem(previa != null ? previa : "Todas");
        if (cbCategoria.getSelectedItem() == null) cbCategoria.setSelectedIndex(0);
        actualizando = false;

        String texto = txtBuscar.getText().trim().toLowerCase();
        String cat = (String) cbCategoria.getSelectedItem();
        Estilo.conservar(tabla, () -> {
            tMenu.setRowCount(0);
            for (Datos.Producto p : d.menu()) {
                boolean okCat = cat == null || cat.equals("Todas") || cat.equals(p.categoria());
                boolean okTxt = texto.isEmpty() || p.nombre().toLowerCase().contains(texto);
                if (okCat && okTxt) {
                    tMenu.addRow(new Object[]{p.id(), p.nombre(), p.categoria(), Datos.dinero(p.precio()),
                            p.stock() < 0 ? "Sin límite" : String.valueOf(p.stock()), p.estado()});
                }
            }
        });
    }
}
