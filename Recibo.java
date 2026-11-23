package restaurante;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Arma la cuenta como texto y permite imprimirla o guardarla en TXT o PDF. */
public final class Recibo {
    private Recibo() {}

    private static final int ANCHO = 40;

    private static String fila(String izq, String der) {
        if (izq.length() + der.length() >= ANCHO) izq = izq.substring(0, Math.max(0, ANCHO - der.length() - 2)) + "..";
        return izq + " ".repeat(Math.max(1, ANCHO - izq.length() - der.length())) + der;
    }

    private static String centrado(String s) {
        if (s.length() >= ANCHO) return s;
        return " ".repeat((ANCHO - s.length()) / 2) + s;
    }

    /** Texto de la cuenta, en 40 columnas (ancho de un tiquete). */
    public static String texto(Datos.Venta v, String restaurante, String titulo) {
        String raya = "-".repeat(ANCHO);
        StringBuilder sb = new StringBuilder();
        sb.append(centrado(restaurante.toUpperCase())).append("\n");
        sb.append(centrado(titulo + " - Mesa " + v.mesa())).append("\n");
        sb.append(centrado(v.fecha())).append("\n");
        sb.append(raya).append("\n");
        for (Datos.Detalle d : v.detalle()) {
            sb.append(fila(d.cantidad() + " x " + d.producto(), Datos.dinero(d.valor()))).append("\n");
            if (!d.nota().isBlank()) sb.append("    (").append(d.nota()).append(")\n");
        }
        sb.append(raya).append("\n");
        sb.append(fila("Subtotal", Datos.dinero(v.subtotal()))).append("\n");
        if (v.descuento() > 0) sb.append(fila("Descuento", "-" + Datos.dinero(v.descuento()))).append("\n");
        boolean pre = "Por cobrar".equals(v.pago());
        if (v.propina() > 0) sb.append(fila(pre ? "Propina sugerida" : "Propina", Datos.dinero(v.propina()))).append("\n");
        sb.append(fila("TOTAL", Datos.dinero(v.total()))).append("\n");
        sb.append(raya).append("\n");
        if (v.personas() > 1) {
            sb.append(fila("Dividido entre " + v.personas(), Datos.dinero(Math.ceil(v.total() / v.personas())) + " c/u")).append("\n");
        }
        if (!pre && !v.pago().isBlank()) sb.append("Pago: ").append(v.pago()).append("\n");
        if (!v.usuario().isBlank()) sb.append("Atendio: ").append(v.usuario()).append("\n");
        sb.append("\n").append(centrado("Gracias por su visita"));
        return sb.toString();
    }

    public static void guardarTxt(Path destino, String texto) throws IOException {
        Files.writeString(destino, texto.replace("\n", System.lineSeparator()) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    // ---------- PDF mínimo (una página, texto monoespaciado) ----------
    private static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\\' || c == '(' || c == ')') sb.append('\\').append(c);
            else if (c > 255) sb.append('?');
            else sb.append(c);
        }
        return sb.toString();
    }

    public static void guardarPdf(Path destino, String texto) throws IOException {
        String[] lineas = texto.split("\n", -1);
        int ancho = 260, alto = 48 + lineas.length * 12;
        StringBuilder c = new StringBuilder("BT\n/F1 9 Tf\n12 TL\n20 " + (alto - 28) + " Td\n");
        for (String l : lineas) c.append('(').append(esc(l)).append(") Tj T*\n");
        c.append("ET\n");
        byte[] contenido = c.toString().getBytes(StandardCharsets.ISO_8859_1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> pos = new ArrayList<>();
        out.writeBytes("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        String[] objetos = {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + ancho + " " + alto + "] /Contents 4 0 R "
                        + "/Resources << /Font << /F1 5 0 R >> >> >>"
        };
        for (int i = 0; i < objetos.length; i++) {
            pos.add(out.size());
            out.writeBytes(((i + 1) + " 0 obj\n" + objetos[i] + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
        }
        pos.add(out.size());
        out.writeBytes(("4 0 obj\n<< /Length " + contenido.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
        out.writeBytes(contenido);
        out.writeBytes("endstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        pos.add(out.size());
        out.writeBytes("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>\nendobj\n"
                .getBytes(StandardCharsets.ISO_8859_1));
        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 6\n0000000000 65535 f \n");
        for (int p : pos) x.append(String.format("%010d 00000 n \n", p));
        x.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        out.writeBytes(x.toString().getBytes(StandardCharsets.ISO_8859_1));
        Files.write(destino, out.toByteArray());
    }

    // ---------- Impresora ----------
    public static void imprimir(String texto) throws PrinterException {
        String[] ls = texto.split("\n", -1);
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Cuenta");
        job.setPrintable((Graphics g, PageFormat pf, int pagina) -> {
            int porPagina = Math.max(1, (int) (pf.getImageableHeight() / 13));
            int ini = pagina * porPagina;
            if (ini >= ls.length) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            g2.setColor(Color.BLACK);
            for (int i = 0; i < porPagina && ini + i < ls.length; i++) g2.drawString(ls[ini + i], 0, (i + 1) * 13);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) job.print();
    }

    // ---------- Ventana para ver, imprimir o guardar ----------
    public static void ofrecer(Component parent, Datos.Venta v, String titulo) {
        String texto = texto(v, Datos.get().nombre(), titulo);
        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(Color.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(420, 380));
        String[] opciones = {"Imprimir", "Guardar PDF", "Guardar TXT", "Cerrar"};
        String base = "cuenta_mesa" + v.mesa() + "_" + v.fecha().replaceAll("[^0-9]", "");
        while (true) {
            int r = JOptionPane.showOptionDialog(parent, sp, titulo, JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[3]);
            if (r < 0 || r == 3) return;
            try {
                if (r == 0) imprimir(texto);
                else guardar(parent, texto, base, r == 1);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "No se pudo completar la acción: " + ex.getMessage());
            }
        }
    }

    private static void guardar(Component parent, String texto, String base, boolean pdf) throws IOException {
        String ext = pdf ? ".pdf" : ".txt";
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(base + ext));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(ext)) f = new File(f.getPath() + ext);
        if (pdf) guardarPdf(f.toPath(), texto);
        else guardarTxt(f.toPath(), texto);
        JOptionPane.showMessageDialog(parent, "Guardado en:\n" + f.getAbsolutePath());
    }
}
