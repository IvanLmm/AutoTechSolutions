package main;

import controlador.ContextoTaller;
import controlador.ServicioController;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import vista.VentanaPrincipal;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // si falla se usa el aspecto predeterminado de Swing
        }
        SwingUtilities.invokeLater(() -> {
            ContextoTaller ctx = new ContextoTaller();
            new ServicioController(ctx).cargarCatalogoInicial();
            new VentanaPrincipal(ctx).setVisible(true);
        });
    }
}
