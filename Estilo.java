package restaurante;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Colores, fuentes y componentes con estilo propio. */
public final class Estilo {
    private Estilo() {}

    public static final Color FONDO = new Color(0xFAF6F0);
    public static final Color OSCURO = new Color(0x2B2118);
    public static final Color ACENTO = new Color(0xD9622B);
    public static final Color VERDE = new Color(0x2E8B57);
    public static final Color ROJO = new Color(0xC0392B);
    public static final Color AMBAR = new Color(0xD99A00);
    public static final Color MORADO = new Color(0x6C5B9E);
    public static final Color TEAL = new Color(0x1F7A8C);
    public static final Color GRIS = new Color(0xB8B0A6);
    public static final Color SUAVE = new Color(0x7A6E62);
    public static final Color CHIP = new Color(0xEADFD2);
    public static final Color BORDE = new Color(0xE6DED3);
    public static final Color FILA = new Color(0xFBF8F3);
    public static final Color NAV = new Color(0x4A3B2E);

    /** Colores para gráficas (siempre van acompañados de etiquetas). */
    public static final Color[] PALETA = {ACENTO, VERDE, new Color(0x3B6EA5), AMBAR, MORADO, TEAL, ROJO, GRIS};

    public static final Font NORMAL = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font NEGRITA = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font MEDIA = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font GRANDE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font TITULO = new Font("Segoe UI", Font.BOLD, 26);

    public static void iniciar() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) { }
        for (String k : new String[]{"OptionPane.messageFont", "OptionPane.buttonFont", "Label.font",
                "ComboBox.font", "Spinner.font", "TextField.font", "Button.font", "CheckBox.font",
                "RadioButton.font", "TextArea.font", "PasswordField.font"}) {
            UIManager.put(k, NORMAL);
        }
    }

    /** Botón redondeado con efecto al pasar el mouse. */
    public static class Boton extends JButton {
        public Color fondo;
        public boolean seleccionado;
        private boolean hover;

        public Boton(String texto, Color fondo) {
            super(texto);
            this.fondo = fondo;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(NEGRITA);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = isEnabled() ? (hover ? fondo.darker() : fondo) : GRIS;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            if (seleccionado) {
                g2.setColor(OSCURO);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 14, 14);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Panel blanco con esquinas redondeadas. */
    public static class Tarjeta extends JPanel {
        public Tarjeta(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.setColor(BORDE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Panel que se ajusta al ancho del scroll (para rejillas dentro de un JScrollPane). */
    public static class Rejilla extends JPanel implements Scrollable {
        public Rejilla(LayoutManager l) { super(l); setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(40, r.height - 40); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Barra superior oscura con título, subtítulo y un componente opcional a la derecha. */
    public static JPanel cabecera(String titulo, String subtitulo, JComponent derecha) {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(OSCURO);
        p.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        JLabel a = new JLabel(titulo);
        a.setFont(TITULO);
        a.setForeground(Color.WHITE);
        JLabel b = new JLabel(subtitulo);
        b.setFont(NORMAL);
        b.setForeground(new Color(0xCFC3B5));
        textos.add(a);
        textos.add(b);
        p.add(textos, BorderLayout.WEST);
        if (derecha != null) p.add(derecha, BorderLayout.EAST);
        return p;
    }

    /** Botón pequeño tipo "chip" para filtros (activo = resaltado). */
    public static Boton chip(String texto, boolean activo) {
        Boton b = new Boton(texto, activo ? ACENTO : CHIP);
        b.setForeground(activo ? Color.WHITE : OSCURO);
        b.setFont(NORMAL);
        b.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        return b;
    }

    /** Botón compacto para la barra de navegación. */
    public static Boton nav(String texto) {
        Boton b = new Boton(texto, NAV);
        b.setFont(NORMAL);
        b.setBorder(BorderFactory.createEmptyBorder(7, 13, 7, 13));
        return b;
    }

    /** Campo de texto con borde suave. */
    public static <T extends javax.swing.text.JTextComponent> T campo(T c) {
        c.setFont(NORMAL);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return c;
    }

    /** Tarjeta pequeña con un título y un valor grande (para estadísticas). */
    public static JComponent dato(String titulo, JLabel valor, Color color) {
        Tarjeta t = new Tarjeta(new BorderLayout(0, 4));
        JLabel a = new JLabel(titulo);
        a.setFont(NORMAL);
        a.setForeground(SUAVE);
        valor.setFont(GRANDE);
        valor.setForeground(color);
        t.add(a, BorderLayout.NORTH);
        t.add(valor, BorderLayout.CENTER);
        return t;
    }

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(GRANDE);
        l.setForeground(OSCURO);
        return l;
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createLineBorder(BORDE));
        s.getViewport().setBackground(Color.WHITE);
        return s;
    }

    /** Vuelve a llenar una tabla sin perder la fila seleccionada. */
    public static void conservar(JTable t, Runnable llenar) {
        int sel = t.getSelectedRow();
        llenar.run();
        if (sel >= 0 && sel < t.getRowCount()) t.setRowSelectionInterval(sel, sel);
    }

    public static Color colorEstado(String estado) {
        if (estado == null) return OSCURO;
        switch (estado) {
            case "Pendiente": return SUAVE;
            case "Preparando": case "Pausada": return AMBAR;
            case "Listo": case "Disponible": case "Activa": return VERDE;
            case "Entregado": return NAV;
            case "Agotado": return ROJO;
            default: return estado.startsWith("Quedan") ? AMBAR : OSCURO;
        }
    }

    /** Pinta en negrita y con color la columna de estado. */
    public static void colorearEstado(JTable t, int col) {
        t.getColumnModel().getColumn(col).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                                                                     boolean foco, int fila, int c) {
                super.getTableCellRendererComponent(tb, v, sel, false, fila, c);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setFont(NEGRITA);
                setForeground(colorEstado(String.valueOf(v)));
                if (!sel) setBackground(fila % 2 == 0 ? Color.WHITE : FILA);
                return this;
            }
        });
    }

    public static void tabla(JTable t) {
        t.setRowHeight(36);
        t.setFont(NORMAL);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(0xFBE3D5));
        t.setSelectionForeground(OSCURO);
        t.setFillsViewportHeight(true);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                                                                     boolean foco, int fila, int col) {
                super.getTableCellRendererComponent(tb, v, sel, false, fila, col);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                setForeground(OSCURO);
                if (!sel) setBackground(fila % 2 == 0 ? Color.WHITE : FILA);
                return this;
            }
        });
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 38));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                                                                     boolean foco, int fila, int col) {
                super.getTableCellRendererComponent(tb, v, false, false, fila, col);
                setBackground(OSCURO);
                setForeground(Color.WHITE);
                setFont(NEGRITA);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
    }

    // ---------- Fotos de los platos ----------
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    /** Foto cuadrada con esquinas redondeadas; si no hay foto, dibuja la inicial del plato. */
    public static ImageIcon foto(String ruta, String nombre, int tam) {
        BufferedImage src = null;
        if (ruta != null && !ruta.isBlank()) {
            synchronized (CACHE) {
                src = CACHE.get(ruta);
                if (src == null) {
                    try {
                        src = ImageIO.read(new File(ruta));
                        if (src != null) CACHE.put(ruta, src);
                    } catch (Exception ignorada) { }
                }
            }
        }
        BufferedImage out = new BufferedImage(tam, tam, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setClip(new RoundRectangle2D.Float(0, 0, tam, tam, 18, 18));
        if (src != null) {
            double esc = Math.max((double) tam / src.getWidth(), (double) tam / src.getHeight());
            int w = (int) Math.ceil(src.getWidth() * esc), h = (int) Math.ceil(src.getHeight() * esc);
            g.drawImage(src, (tam - w) / 2, (tam - h) / 2, w, h, null);
        } else {
            g.setColor(CHIP);
            g.fillRect(0, 0, tam, tam);
            g.setColor(ACENTO);
            g.setFont(new Font("Segoe UI", Font.BOLD, tam / 2));
            String ini = (nombre == null || nombre.isBlank()) ? "?" : nombre.substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(ini, (tam - fm.stringWidth(ini)) / 2, (tam - fm.getHeight()) / 2 + fm.getAscent());
        }
        g.dispose();
        return new ImageIcon(out);
    }
}
