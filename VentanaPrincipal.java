package vista;

import controlador.ClienteController;
import controlador.ContextoTaller;
import controlador.DeshacerController;
import controlador.FacturaController;
import controlador.OrdenController;
import controlador.ReporteController;
import controlador.RepuestoController;
import controlador.ServicioController;
import controlador.VehiculoController;
import excepciones.ValidacionException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/** Ventana principal: menú lateral de navegación + zona central con un panel por módulo. */
public class VentanaPrincipal extends JFrame {

    private final CardLayout cartas = new CardLayout();
    private final JPanel contenido = new JPanel(cartas);
    private final Map<String, PanelModulo> paneles = new LinkedHashMap<>();
    private final DeshacerController deshacerCtrl;

    public VentanaPrincipal(ContextoTaller ctx) {
        super("AutoTech Solutions - Gestión del taller");

        ClienteController clientes = new ClienteController(ctx);
        VehiculoController vehiculos = new VehiculoController(ctx);
        ServicioController servicios = new ServicioController(ctx);
        RepuestoController repuestos = new RepuestoController(ctx);
        OrdenController ordenes = new OrdenController(ctx);
        FacturaController facturas = new FacturaController(ctx);
        ReporteController reportes = new ReporteController(ctx);
        DeshacerController deshacer = new DeshacerController(ctx);
        this.deshacerCtrl = deshacer;

        agregarModulo("Clientes", new PanelClientes(clientes));
        agregarModulo("Vehículos", new PanelVehiculos(vehiculos, clientes));
        agregarModulo("Servicios", new PanelServicios(servicios));
        agregarModulo("Repuestos", new PanelRepuestos(repuestos));
        agregarModulo("Órdenes", new PanelOrdenes(ordenes, clientes, vehiculos, servicios, repuestos));
        agregarModulo("Facturación", new PanelFacturacion(facturas, ordenes));
        agregarModulo("Reportes", new PanelReportes(reportes));

        // ---- menú lateral ----
        JPanel botonesMenu = new JPanel(new GridLayout(0, 1, 0, 6));
        ButtonGroup grupo = new ButtonGroup();
        boolean primero = true;
        for (String nombre : paneles.keySet()) {
            JToggleButton b = new JToggleButton(nombre);
            b.setPreferredSize(new Dimension(170, 38));
            b.addActionListener(e -> mostrar(nombre));
            grupo.add(b);
            botonesMenu.add(b);
            if (primero) {
                b.setSelected(true);
                primero = false;
            }
        }

        JButton btnDeshacer = new JButton("Deshacer última acción") {
            @Override
            public String getToolTipText() {
                String d = deshacer.descripcionProxima();
                return d == null ? "No hay operaciones para deshacer" : "Deshacer: " + d;
            }
        };
        btnDeshacer.setToolTipText("Deshacer");
        btnDeshacer.addActionListener(e -> deshacerUltima());

        JLabel titulo = new JLabel("AutoTech Solutions");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JPanel menu = new JPanel(new BorderLayout());
        menu.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(titulo, BorderLayout.NORTH);
        arriba.add(botonesMenu, BorderLayout.CENTER);
        menu.add(arriba, BorderLayout.NORTH);
        menu.add(btnDeshacer, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(menu, BorderLayout.WEST);
        add(contenido, BorderLayout.CENTER);

        // Los datos viven solo en memoria: se avisa antes de cerrar
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (Mensajes.confirmar(VentanaPrincipal.this,
                        "Los datos se guardan solo en memoria y se perderán al cerrar.\n¿Desea salir?")) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        setMinimumSize(new Dimension(1000, 640));
        setSize(1150, 700);
        setLocationRelativeTo(null);
    }

    private void agregarModulo(String nombre, PanelModulo panel) {
        paneles.put(nombre, panel);
        contenido.add(panel, nombre);
    }

    private void mostrar(String nombre) {
        paneles.get(nombre).actualizar();
        cartas.show(contenido, nombre);
    }

    private void actualizarTodo() {
        for (PanelModulo p : paneles.values()) {
            p.actualizar();
        }
    }

    private void deshacerUltima() {
        try {
            String descripcion = deshacerCtrl.deshacer();
            actualizarTodo();
            Mensajes.info(this, "Se deshizo: " + descripcion + ".");
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }
}
