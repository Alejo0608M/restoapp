package restaurante;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/** Reportes con gráficas: por día, por hora, por categoría, por forma de pago, top productos y por usuario. */
public class VistaReportes extends JPanel {

    private final Datos d = Datos.get();
    private final Periodo periodo = new Periodo(1, this::refrescar);
    private final JLabel lblResumen = new JLabel(" ");
    private final Grafica gDia = new Grafica(Grafica.Tipo.BARRAS);
    private final Grafica gHora = new Grafica(Grafica.Tipo.BARRAS);
    private final Grafica gCategoria = new Grafica(Grafica.Tipo.BARRAS_H);
    private final Grafica gPago = new Grafica(Grafica.Tipo.TORTA);
    private final Grafica gTop = new Grafica(Grafica.Tipo.BARRAS_H);
    private final Grafica gUsuario = new Grafica(Grafica.Tipo.TORTA);

    public VistaReportes() {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        lblResumen.setFont(Estilo.MEDIA);
        lblResumen.setForeground(Estilo.OSCURO);
        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.setOpaque(false);
        norte.add(periodo, BorderLayout.NORTH);
        norte.add(lblResumen, BorderLayout.CENTER);

        Estilo.Rejilla rejilla = new Estilo.Rejilla(new GridLayout(0, 2, 16, 16));
        rejilla.add(tarjeta("Ventas por día", gDia));
        rejilla.add(tarjeta("Ventas por hora", gHora));
        rejilla.add(tarjeta("Ventas por categoría", gCategoria));
        rejilla.add(tarjeta("Formas de pago", gPago));
        rejilla.add(tarjeta("Productos más vendidos", gTop));
        rejilla.add(tarjeta("Ventas por usuario", gUsuario));
        JScrollPane sp = new JScrollPane(rejilla);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(24);

        add(norte, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        d.alCambiar(this::refrescar);
        refrescar();
    }

    private static JComponent tarjeta(String titulo, Grafica g) {
        Estilo.Tarjeta t = new Estilo.Tarjeta(new BorderLayout(0, 8));
        JLabel l = new JLabel(titulo);
        l.setFont(Estilo.MEDIA);
        l.setForeground(Estilo.OSCURO);
        t.add(l, BorderLayout.NORTH);
        t.add(g, BorderLayout.CENTER);
        return t;
    }

    private static Map<String, Double> ordenar(Map<String, Double> m, int limite) {
        return m.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(limite)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private void refrescar() {
        List<Datos.Venta> vs = d.ventasEntre(periodo.desde(), periodo.hasta());
        double total = vs.stream().mapToDouble(Datos.Venta::total).sum();
        lblResumen.setText(vs.size() + " cuentas  ·  " + Datos.dinero(total) + "  ·  " + periodo.texto());

        // por día (o por mes si el periodo es muy largo)
        TreeMap<String, Double> dia = new TreeMap<>();
        for (Datos.Venta v : vs) dia.merge(v.fecha().substring(0, 10), v.total(), Double::sum);
        Map<String, Double> serieDia = new LinkedHashMap<>();
        if (!dia.isEmpty()) {
            LocalDate primero = LocalDate.parse(dia.firstKey()), ultimo = LocalDate.parse(dia.lastKey());
            LocalDate a = periodo.desde().compareTo("1000") > 0 ? LocalDate.parse(periodo.desde()) : primero;
            LocalDate b = periodo.hasta().compareTo("9000") < 0 ? LocalDate.parse(periodo.hasta()) : ultimo;
            if (b.isBefore(a)) b = a;
            if (ChronoUnit.DAYS.between(a, b) > 62) {
                TreeMap<String, Double> mes = new TreeMap<>();
                for (Map.Entry<String, Double> e : dia.entrySet()) mes.merge(e.getKey().substring(0, 7), e.getValue(), Double::sum);
                serieDia.putAll(mes);
            } else {
                for (LocalDate x = a; !x.isAfter(b); x = x.plusDays(1)) {
                    serieDia.put(x.toString().substring(5), dia.getOrDefault(x.toString(), 0.0));
                }
            }
        }
        gDia.setDatos(serieDia);

        // por hora
        TreeMap<Integer, Double> hora = new TreeMap<>();
        for (Datos.Venta v : vs) hora.merge(Integer.parseInt(v.fecha().substring(11, 13)), v.total(), Double::sum);
        Map<String, Double> serieHora = new LinkedHashMap<>();
        if (!hora.isEmpty()) {
            for (int h = hora.firstKey(); h <= hora.lastKey(); h++) serieHora.put(h + "h", hora.getOrDefault(h, 0.0));
        }
        gHora.setDatos(serieHora);

        // por categoría, top productos, pago y usuario
        Map<String, Double> cat = new HashMap<>(), pago = new HashMap<>(), usuario = new HashMap<>();
        Map<String, Double> valorProd = new HashMap<>();
        Map<String, Integer> cantProd = new HashMap<>();
        for (Datos.Venta v : vs) {
            pago.merge(v.pago().isBlank() ? "—" : v.pago(), v.total(), Double::sum);
            usuario.merge(v.usuario().isBlank() ? "(sin dato)" : v.usuario(), v.total(), Double::sum);
            for (Datos.Detalle x : v.detalle()) {
                cat.merge(x.categoria().isBlank() ? "Sin categoría" : x.categoria(), x.valor(), Double::sum);
                valorProd.merge(x.producto(), x.valor(), Double::sum);
                cantProd.merge(x.producto(), x.cantidad(), Integer::sum);
            }
        }
        gCategoria.setDatos(ordenar(cat, 8));
        gPago.setDatos(ordenar(pago, 8));
        gUsuario.setDatos(ordenar(usuario, 8));
        Map<String, Double> top = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : ordenar(valorProd, 6).entrySet()) {
            top.put(e.getKey() + " ×" + cantProd.get(e.getKey()), e.getValue());
        }
        gTop.setDatos(top);
    }
}
