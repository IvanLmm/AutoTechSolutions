package vista;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

/**
 * Gráfico de barras dibujado A MANO con Graphics2D dentro de paintComponent().
 * No usa ninguna biblioteca de gráficos: calcula la escala, dibuja ejes,
 * líneas guía, barras, valores y etiquetas a partir de los datos en memoria.
 */
public class CanvasReporte2D extends JPanel {

    private static final Color[] PALETA = {
        new Color(52, 120, 198), new Color(237, 137, 54), new Color(72, 170, 110),
        new Color(155, 89, 182), new Color(220, 80, 90), new Color(60, 170, 190)
    };
    private static final Color TEXTO = new Color(40, 40, 40);
    private static final Color TEXTO_SUAVE = new Color(110, 110, 110);
    private static final Color GUIA = new Color(228, 228, 228);

    private final String titulo;
    private final String subtitulo;
    private final boolean enteros;          // true: eje Y de cantidades; false: dinero
    private final String mensajeVacio;

    private List<String> etiquetas = new ArrayList<>();
    private List<Double> valores = new ArrayList<>();
    private Color[] colores = PALETA;
    private final List<Rectangle> columnas = new ArrayList<>(); // zonas de cada barra (para el tooltip)

    public CanvasReporte2D(String titulo, String subtitulo, boolean enteros, String mensajeVacio) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.enteros = enteros;
        this.mensajeVacio = mensajeVacio;
        setPreferredSize(new Dimension(520, 280));
        setMinimumSize(new Dimension(300, 200));
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /** Reemplaza los datos (etiqueta -> valor) y vuelve a dibujar. Llamar desde el EDT. */
    public void setDatos(Map<String, Double> datos) {
        etiquetas = new ArrayList<>(datos.keySet());
        valores = new ArrayList<>(datos.values());
        repaint();
    }

    public void setColores(Color... colores) {
        this.colores = colores.length == 0 ? PALETA : colores;
        repaint();
    }

    // =====================================================================
    //  Dibujo
    // =====================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            dibujar(g2, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
    }

    private void dibujar(Graphics2D g2, int w, int h) {
        // fondo y marco
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(205, 205, 205));
        g2.drawRect(0, 0, w - 1, h - 1);

        // título y subtítulo
        Font base = getFont();
        g2.setFont(base.deriveFont(Font.BOLD, 15f));
        g2.setColor(TEXTO);
        g2.drawString(titulo, 16, 24);
        g2.setFont(base.deriveFont(Font.PLAIN, 11f));
        g2.setColor(TEXTO_SUAVE);
        g2.drawString(subtitulo, 16, 40);

        // área de dibujo
        int izquierda = 78, derecha = 24, arriba = 58, abajo = 64;
        int px = izquierda, py = arriba;
        int pw = w - izquierda - derecha, ph = h - arriba - abajo;
        columnas.clear();
        if (pw < 60 || ph < 50) {
            return; // ventana demasiado pequeña
        }

        double maximo = 0;
        for (double v : valores) {
            maximo = Math.max(maximo, v);
        }
        if (valores.isEmpty() || maximo <= 0) {
            dibujarVacio(g2, px, py, pw, ph);
            return;
        }

        // escala "agradable": 1, 2, 5 x 10^n
        double paso = pasoAgradable(maximo / 5.0);
        if (enteros) {
            paso = Math.max(1, Math.ceil(paso));
        }
        double tope = Math.ceil(maximo / paso - 1e-9) * paso;

        // líneas guía y valores del eje Y
        g2.setFont(base.deriveFont(Font.PLAIN, 11f));
        FontMetrics fm = g2.getFontMetrics();
        g2.setStroke(new BasicStroke(1f));
        for (double t = 0; t <= tope + paso * 1e-6; t += paso) {
            int y = py + ph - (int) Math.round(t / tope * ph);
            g2.setColor(GUIA);
            g2.drawLine(px, y, px + pw, y);
            String texto = formatoEje(t, paso);
            g2.setColor(TEXTO_SUAVE);
            g2.drawString(texto, px - 8 - fm.stringWidth(texto), y + fm.getAscent() / 2 - 1);
        }

        // barras
        int n = valores.size();
        double ranura = pw / (double) n;
        double anchoBarra = Math.min(ranura * 0.6, 96);
        for (int i = 0; i < n; i++) {
            double valor = valores.get(i);
            int altura = (int) Math.round(valor / tope * ph);
            int x = (int) Math.round(px + i * ranura + (ranura - anchoBarra) / 2);
            int yArriba = py + ph - altura;
            int ancho = (int) Math.round(anchoBarra);
            columnas.add(new Rectangle((int) Math.round(px + i * ranura), py, (int) Math.round(ranura), ph));

            Color color = colores[i % colores.length];
            if (altura > 0) {
                g2.setPaint(new GradientPaint(0, yArriba, color.brighter(), 0, yArriba + altura, color.darker()));
                g2.fillRect(x, yArriba, ancho, altura);
                g2.setPaint(null);
                g2.setColor(color.darker().darker());
                g2.drawRect(x, yArriba, ancho, altura);
            }

            // valor sobre la barra
            g2.setFont(base.deriveFont(Font.BOLD, 12f));
            FontMetrics fv = g2.getFontMetrics();
            String textoValor = formatoValor(valor);
            g2.setColor(TEXTO);
            g2.drawString(textoValor, x + (ancho - fv.stringWidth(textoValor)) / 2, yArriba - 6);

            // etiqueta de categoría (con ajuste de líneas)
            g2.setFont(base.deriveFont(Font.PLAIN, 11f));
            FontMetrics fe = g2.getFontMetrics();
            List<String> lineas = ajustar(etiquetas.get(i), fe, (int) ranura - 8, 3);
            g2.setColor(TEXTO);
            for (int l = 0; l < lineas.size(); l++) {
                String linea = lineas.get(l);
                g2.drawString(linea, x + (ancho - fe.stringWidth(linea)) / 2, py + ph + 18 + l * 14);
            }
        }

        // ejes
        g2.setColor(TEXTO);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(px, py, px, py + ph);
        g2.drawLine(px, py + ph, px + pw, py + ph);
    }

    private void dibujarVacio(Graphics2D g2, int px, int py, int pw, int ph) {
        g2.setColor(TEXTO);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(px, py, px, py + ph);
        g2.drawLine(px, py + ph, px + pw, py + ph);
        g2.setFont(getFont().deriveFont(Font.ITALIC, 13f));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(TEXTO_SUAVE);
        g2.drawString(mensajeVacio, px + (pw - fm.stringWidth(mensajeVacio)) / 2, py + ph / 2);
    }

    // =====================================================================
    //  Cálculos auxiliares
    // =====================================================================
    private static double pasoAgradable(double x) {
        double exponente = Math.floor(Math.log10(x));
        double potencia = Math.pow(10, exponente);
        double f = x / potencia;
        double nf = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
        return nf * potencia;
    }

    private String formatoEje(double valor, double paso) {
        if (enteros) {
            return String.valueOf(Math.round(valor));
        }
        return paso < 1 ? String.format("%,.2f", valor) : String.format("%,.0f", valor);
    }

    private String formatoValor(double valor) {
        return enteros ? String.valueOf(Math.round(valor)) : Formato.dinero(valor);
    }

    /** Parte un texto en hasta maxLineas líneas que quepan en el ancho dado. */
    private static List<String> ajustar(String texto, FontMetrics fm, int ancho, int maxLineas) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (fm.stringWidth(prueba) <= ancho) {
                actual = new StringBuilder(prueba);
            } else {
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                }
                actual = new StringBuilder(palabra);
            }
        }
        if (actual.length() > 0) {
            lineas.add(actual.toString());
        }
        List<String> resultado = new ArrayList<>();
        for (int i = 0; i < lineas.size() && i < maxLineas; i++) {
            String linea = lineas.get(i);
            if (i == maxLineas - 1 && lineas.size() > maxLineas) {
                linea += "…";
            }
            resultado.add(recortar(linea, fm, ancho));
        }
        return resultado;
    }

    private static String recortar(String texto, FontMetrics fm, int ancho) {
        if (fm.stringWidth(texto) <= ancho) {
            return texto;
        }
        String r = texto;
        while (r.length() > 1 && fm.stringWidth(r + "…") > ancho) {
            r = r.substring(0, r.length() - 1);
        }
        return r + "…";
    }

    /** Al pasar el mouse sobre una columna muestra su etiqueta y valor exactos. */
    @Override
    public String getToolTipText(MouseEvent e) {
        for (int i = 0; i < columnas.size() && i < valores.size(); i++) {
            if (columnas.get(i).contains(e.getPoint())) {
                return etiquetas.get(i) + ": " + formatoValor(valores.get(i));
            }
        }
        return null;
    }
}
