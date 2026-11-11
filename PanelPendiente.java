package vista;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/** Marcador temporal de los módulos que se completan en la siguiente entrega. */
public class PanelPendiente extends PanelModulo {

    public PanelPendiente(String titulo) {
        add(new JLabel("<html><div style='text-align:center'><h2>" + titulo
                + "</h2>Este módulo se habilita en la siguiente entrega.</div></html>",
                SwingConstants.CENTER), BorderLayout.CENTER);
    }

    @Override
    public void actualizar() {
        // sin datos que refrescar
    }
}
