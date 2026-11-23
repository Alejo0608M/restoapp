package restaurante;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** Gráficas sencillas dibujadas a mano: barras, barras horizontales y torta. */
public class Grafica extends JComponent {

    public enum Tipo { BARRAS, BARRAS_H, TORTA }

    private final Tipo tipo;
    private Map<String, Double> datos = new LinkedHashMap<>();

    public Grafica(Tipo tipo) {
        this.tipo = tipo;
        setPreferredSize(new Dimension(380, 230));
        setFont(Estilo.NORMAL.deriveFont(12f));
    }

    public void setDatos(Map<String, Double> d) {
        datos = new LinkedHashMap<>(d);
        repaint();
    }

    /** Dinero abreviado: $950, $1.5k, $96k, $1.2M. */
    public static String corto(double v) {
        if (v >= 1_000_000) {
            String s = String.format("%.1f", v / 1_000_000);
            return "$" + (s.endsWith(".0") || s.endsWith(",0") ? s.substring(0, s.length() - 2) : s) + "M";
        }
        if (v >= 1_000) {
            double k = v / 1_000;
            return "$" + (k < 10 && k != Math.floor(k) ? String.format("%.1f", k) : String.format("%.0f", k)) + "k";
        }
        return String.format("$%.0f", v);
    }

    private static double bonito(double max) {
        double exp = Math.pow(10, Math.floor(Math.log10(max)));
        double f = max / exp;
        double nf = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
        return nf * exp;
    }

    private static String recortar(String s, int px, FontMetrics fm) {
        if (fm.stringWidth(s) <= px) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > px) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        double total = datos.values().stream().mapToDouble(Double::doubleValue).sum();
        if (datos.isEmpty() || total <= 0) {
            String m = "Sin datos en este periodo";
            g.setColor(Estilo.SUAVE);
            g.drawString(m, (getWidth() - fm.stringWidth(m)) / 2, getHeight() / 2);
            g.dispose();
            return;
        }
        switch (tipo) {
            case BARRAS -> barras(g, fm);
            case BARRAS_H -> barrasH(g, fm);
            case TORTA -> torta(g, fm, total);
        }
        g.dispose();
    }

    private void barras(Graphics2D g, FontMetrics fm) {
        int izq = 48, der = 8, arriba = 16, abajo = 26;
        int w = getWidth() - izq - der, h = getHeight() - arriba - abajo;
        double max = bonito(datos.values().stream().mapToDouble(Double::doubleValue).max().orElse(1));
        for (int i = 0; i <= 4; i++) {
            int y = arriba + h - h * i / 4;
            g.setColor(Estilo.BORDE);
            g.drawLine(izq, y, izq + w, y);
            String t = corto(max * i / 4);
            g.setColor(Estilo.SUAVE);
            g.drawString(t, izq - 6 - fm.stringWidth(t), y + fm.getAscent() / 2 - 1);
        }
        int n = datos.size();
        double paso = (double) w / n;
        int bw = (int) Math.max(2, Math.min(48, paso * 0.68));
        int anchoEtiqueta = 0;
        for (String k : datos.keySet()) anchoEtiqueta = Math.max(anchoEtiqueta, fm.stringWidth(k) + 8);
        int cada = (int) Math.max(1, Math.ceil(anchoEtiqueta / paso));
        int i = 0;
        for (Map.Entry<String, Double> e : datos.entrySet()) {
            double cx = izq + paso * i + paso / 2;
            int bh = (int) Math.round(e.getValue() / max * h);
            g.setColor(Estilo.ACENTO);
            if (bh > 0) g.fillRoundRect((int) (cx - bw / 2.0), arriba + h - bh, bw, bh, 4, 4);
            String v = corto(e.getValue());
            if (e.getValue() > 0 && paso >= fm.stringWidth(v) + 4) {
                g.setColor(Estilo.OSCURO);
                g.drawString(v, (int) (cx - fm.stringWidth(v) / 2.0), arriba + h - bh - 4);
            }
            if (i % cada == 0) {
                g.setColor(Estilo.SUAVE);
                String l = recortar(e.getKey(), (int) Math.max(paso * cada, 30), fm);
                g.drawString(l, (int) (cx - fm.stringWidth(l) / 2.0), arriba + h + fm.getAscent() + 6);
            }
            i++;
        }
    }

    private void barrasH(Graphics2D g, FontMetrics fm) {
        int n = Math.min(datos.size(), 8);
        int margen = 6;
        int etiqueta = 0, valor = 0;
        for (Map.Entry<String, Double> e : datos.entrySet()) {
            etiqueta = Math.max(etiqueta, fm.stringWidth(e.getKey()));
            valor = Math.max(valor, fm.stringWidth(corto(e.getValue())));
        }
        etiqueta = Math.min(etiqueta + 10, Math.max(90, getWidth() / 3));
        int barMax = getWidth() - margen * 2 - etiqueta - valor - 10;
        double max = datos.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        int fila = Math.min(30, (getHeight() - margen * 2) / n);
        int i = 0;
        for (Map.Entry<String, Double> e : datos.entrySet()) {
            if (i >= n) break;
            int y = margen + i * fila;
            g.setColor(Estilo.OSCURO);
            g.drawString(recortar(e.getKey(), etiqueta - 8, fm), margen, y + (fila + fm.getAscent()) / 2 - 2);
            int bw = (int) Math.max(2, Math.round(e.getValue() / max * barMax));
            g.setColor(Estilo.ACENTO);
            g.fillRoundRect(margen + etiqueta, y + 4, bw, fila - 8, 6, 6);
            g.setColor(Estilo.OSCURO);
            g.drawString(corto(e.getValue()), margen + etiqueta + bw + 6, y + (fila + fm.getAscent()) / 2 - 2);
            i++;
        }
    }

    private void torta(Graphics2D g, FontMetrics fm, double total) {
        int lado = Math.max(40, Math.min(getHeight() - 20, (int) (getWidth() * 0.45)));
        int x0 = 10, y0 = (getHeight() - lado) / 2;
        double ang = 90;
        int i = 0;
        for (double v : datos.values()) {
            double extent = -v / total * 360.0;
            g.setColor(Estilo.PALETA[i % Estilo.PALETA.length]);
            g.fill(new Arc2D.Double(x0, y0, lado, lado, ang, extent, Arc2D.PIE));
            ang += extent;
            i++;
        }
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        ang = 90;
        for (double v : datos.values()) {
            g.draw(new Arc2D.Double(x0, y0, lado, lado, ang, -v / total * 360.0, Arc2D.PIE));
            ang += -v / total * 360.0;
        }
        int hueco = (int) (lado * 0.55);
        g.fill(new Ellipse2D.Double(x0 + (lado - hueco) / 2.0, y0 + (lado - hueco) / 2.0, hueco, hueco));
        String t = corto(total);
        g.setColor(Estilo.OSCURO);
        g.setFont(getFont().deriveFont(Font.BOLD));
        FontMetrics fb = g.getFontMetrics();
        g.drawString(t, x0 + (lado - fb.stringWidth(t)) / 2, y0 + lado / 2 + fb.getAscent() / 2 - 2);
        g.setFont(getFont());

        int lx = x0 + lado + 18;
        int fila = 22;
        int max = Math.max(1, (getHeight() - 10) / fila);
        int ly = Math.max(10, (getHeight() - Math.min(datos.size(), max) * fila) / 2);
        i = 0;
        for (Map.Entry<String, Double> e : datos.entrySet()) {
            if (i >= max) break;
            g.setColor(Estilo.PALETA[i % Estilo.PALETA.length]);
            g.fillRoundRect(lx, ly + i * fila + 4, 11, 11, 3, 3);
            g.setColor(Estilo.OSCURO);
            String s = e.getKey() + "  " + corto(e.getValue()) + " (" + Math.round(e.getValue() / total * 100) + "%)";
            g.drawString(recortar(s, getWidth() - lx - 22, fm), lx + 18, ly + i * fila + 14);
            i++;
        }
    }
}
