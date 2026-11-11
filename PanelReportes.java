package vista;

import controlador.ReporteController;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import modelo.EstadoOrden;

/** Pantalla de reportes: dos gráficos Java 2D alimentados con los datos en memoria. */
public class PanelReportes extends PanelModulo {

    private final ReporteController reportes;

    private final CanvasReporte2D graficoIngresos = new CanvasReporte2D(
            "Ingresos por servicio", "Suma de servicios de las órdenes Finalizadas", false,
            "Sin ingresos todavía: finalice órdenes para ver este gráfico.");
    private final CanvasReporte2D graficoEstados = new CanvasReporte2D(
            "Cantidad de órdenes por estado", "Todas las órdenes registradas", true,
            "Aún no hay órdenes registradas.");
    private final JLabel lblResumen = new JLabel(" ");

    public PanelReportes(ReporteController reportes) {
        this.reportes = reportes;

        // mismo orden que EstadoOrden: Pendiente, En proceso, Finalizada, Cancelada
        graficoEstados.setColores(new Color(237, 176, 40), new Color(52, 120, 198),
                new Color(72, 170, 110), new Color(200, 80, 80));

        lblResumen.setFont(lblResumen.getFont().deriveFont(Font.BOLD, 13f));
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        barra.add(boton("Actualizar reportes", this::actualizar));
        barra.add(lblResumen);

        JPanel graficos = new JPanel(new GridLayout(2, 1, 0, 10));
        graficos.add(graficoIngresos);
        graficos.add(graficoEstados);

        add(barra, BorderLayout.NORTH);
        add(graficos, BorderLayout.CENTER);
        actualizar();
    }

    @Override
    public void actualizar() {
        Map<String, Double> ingresos = reportes.ingresosPorServicio();
        graficoIngresos.setDatos(ingresos);

        Map<String, Double> estados = new LinkedHashMap<>();
        int totalOrdenes = 0;
        for (Map.Entry<EstadoOrden, Integer> e : reportes.ordenesPorEstado().entrySet()) {
            estados.put(e.getKey().toString(), e.getValue().doubleValue());
            totalOrdenes += e.getValue();
        }
        graficoEstados.setDatos(estados);

        double totalIngresos = 0;
        for (double v : ingresos.values()) {
            totalIngresos += v;
        }
        lblResumen.setText("Ingresos por servicios: " + Formato.dinero(totalIngresos)
                + "     |     Órdenes registradas: " + totalOrdenes);
    }
}
