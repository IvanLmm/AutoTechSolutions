package vista;

import controlador.ClienteController;
import controlador.OrdenController;
import controlador.RepuestoController;
import controlador.ServicioController;
import controlador.Validador;
import controlador.VehiculoController;
import excepciones.ClienteNoEncontradoException;
import excepciones.StockInsuficienteException;
import excepciones.ValidacionException;
import hilos.ProcesoServicioHilo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modelo.Cliente;
import modelo.DetalleRepuesto;
import modelo.EstadoOrden;
import modelo.OrdenTrabajo;
import modelo.Repuesto;
import modelo.Servicio;
import modelo.Vehiculo;

public class PanelOrdenes extends PanelModulo {

    private static final String TODOS = "Todos los estados";

    private final OrdenController ordenes;
    private final ClienteController clientes;
    private final VehiculoController vehiculos;
    private final ServicioController servicios;
    private final RepuestoController repuestos;

    private final JTabbedPane pestanas = new JTabbedPane();

    // ---- pestaña "Nueva orden" ----
    private final JComboBox<Cliente> cmbCliente = new JComboBox<>();
    private final JComboBox<Vehiculo> cmbVehiculo = new JComboBox<>();
    private final JTextField txtMecanico = new JTextField(18);
    private final JTextField txtDiagnostico = new JTextField(30);
    private final JComboBox<Servicio> cmbServicio = new JComboBox<>();
    private final JComboBox<Repuesto> cmbRepuesto = new JComboBox<>();
    private final JTextField txtCantidadRepuesto = new JTextField(5);
    private final DefaultTableModel modeloServiciosElegidos = modeloNoEditable("Código", "Servicio", "Precio");
    private final JTable tablaServiciosElegidos = new JTable(modeloServiciosElegidos);
    private final DefaultTableModel modeloRepuestosElegidos =
            modeloNoEditable("Código", "Repuesto", "Cantidad", "Precio", "Subtotal");
    private final JTable tablaRepuestosElegidos = new JTable(modeloRepuestosElegidos);
    private final List<Servicio> serviciosElegidos = new ArrayList<>();
    private final Map<Repuesto, Integer> repuestosElegidos = new LinkedHashMap<>();
    private final JLabel lblTotal = new JLabel();

    // ---- pestaña "Órdenes registradas" ----
    private final JComboBox<Object> cmbEstado = new JComboBox<>();
    private final DefaultTableModel modeloOrdenes = modeloNoEditable(
            "Número", "Fecha", "Cliente", "Vehículo", "Mecánico", "Estado", "Costo total", "Facturada");
    private final JTable tablaOrdenes = new JTable(modeloOrdenes);
    private List<OrdenTrabajo> mostradas = new ArrayList<>();
    private final JPanel panelProcesos = new JPanel();
    private final Map<String, JPanel> filasProceso = new LinkedHashMap<>();

    private boolean cargando = false;

    public PanelOrdenes(OrdenController ordenes, ClienteController clientes, VehiculoController vehiculos,
                        ServicioController servicios, RepuestoController repuestos) {
        this.ordenes = ordenes;
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.servicios = servicios;
        this.repuestos = repuestos;

        configurarTabla(tablaServiciosElegidos);
        configurarTabla(tablaRepuestosElegidos);
        configurarTabla(tablaOrdenes);
        cmbEstado.addItem(TODOS);
        for (EstadoOrden e : EstadoOrden.values()) {
            cmbEstado.addItem(e);
        }

        pestanas.addTab("Nueva orden", construirNuevaOrden());
        pestanas.addTab("Órdenes registradas", construirListado());
        add(pestanas, BorderLayout.CENTER);

        cmbCliente.addActionListener(e -> {
            if (!cargando) {
                cargarVehiculos();
            }
        });
        cmbEstado.addActionListener(e -> {
            if (!cargando) {
                actualizarOrdenes();
            }
        });
        refrescarElegidos();
        refrescarProcesos();
        actualizar();
    }

    // =====================================================================
    //  Construcción de las pestañas
    // =====================================================================
    private JPanel construirNuevaOrden() {
        JPanel datos = crearFormulario("Datos de la orden");
        agregarCampo(datos, 0, 0, "Cliente:", cmbCliente);
        agregarCampo(datos, 0, 1, "Vehículo:", cmbVehiculo);
        agregarCampo(datos, 1, 0, "Mecánico:", txtMecanico);
        agregarCampo(datos, 1, 1, "Diagnóstico:", txtDiagnostico);

        cmbServicio.setPreferredSize(new Dimension(230, 26));
        cmbRepuesto.setPreferredSize(new Dimension(230, 26));

        JPanel selServicio = new JPanel(new GridLayout(2, 1));
        JPanel filaComboS = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaComboS.add(cmbServicio);
        JPanel filaBotonesS = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaBotonesS.add(boton("Agregar servicio", this::agregarServicio));
        filaBotonesS.add(boton("Quitar servicio", this::quitarServicio));
        selServicio.add(filaComboS);
        selServicio.add(filaBotonesS);
        JPanel pServicios = new JPanel(new BorderLayout(4, 4));
        pServicios.setBorder(BorderFactory.createTitledBorder("Servicios de la orden"));
        pServicios.add(selServicio, BorderLayout.NORTH);
        pServicios.add(new JScrollPane(tablaServiciosElegidos), BorderLayout.CENTER);

        JPanel selRepuesto = new JPanel(new GridLayout(2, 1));
        JPanel filaComboR = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaComboR.add(cmbRepuesto);
        filaComboR.add(new JLabel("Cant.:"));
        filaComboR.add(txtCantidadRepuesto);
        JPanel filaBotonesR = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaBotonesR.add(boton("Agregar repuesto", this::agregarRepuesto));
        filaBotonesR.add(boton("Quitar repuesto", this::quitarRepuesto));
        selRepuesto.add(filaComboR);
        selRepuesto.add(filaBotonesR);
        JPanel pRepuestos = new JPanel(new BorderLayout(4, 4));
        pRepuestos.setBorder(BorderFactory.createTitledBorder("Repuestos de la orden"));
        pRepuestos.add(selRepuesto, BorderLayout.NORTH);
        pRepuestos.add(new JScrollPane(tablaRepuestosElegidos), BorderLayout.CENTER);

        JPanel listas = new JPanel(new GridLayout(1, 2, 10, 0));
        listas.add(pServicios);
        listas.add(pRepuestos);

        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 14f));
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        pie.add(lblTotal);
        pie.add(boton("Crear orden", this::crear));
        pie.add(boton("Limpiar formulario", this::limpiarFormulario));

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(datos, BorderLayout.NORTH);
        panel.add(listas, BorderLayout.CENTER);
        panel.add(pie, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel construirListado() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barra.add(new JLabel("Estado:"));
        barra.add(cmbEstado);
        barra.add(boton("Ver detalle", this::verDetalle));
        barra.add(boton("Procesar orden", this::procesar));
        barra.add(boton("Cancelar orden", this::cancelar));

        panelProcesos.setLayout(new BoxLayout(panelProcesos, BoxLayout.Y_AXIS));
        JScrollPane scrollProcesos = new JScrollPane(panelProcesos);
        scrollProcesos.setBorder(BorderFactory.createTitledBorder("Procesos en ejecución"));
        scrollProcesos.setPreferredSize(new Dimension(100, 130));

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(barra, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaOrdenes), BorderLayout.CENTER);
        panel.add(scrollProcesos, BorderLayout.SOUTH);
        return panel;
    }

    // =====================================================================
    //  Actualización de datos
    // =====================================================================
    @Override
    public void actualizar() {
        cargando = true;
        recargar(cmbCliente, clientes.listar());
        recargar(cmbServicio, servicios.listar());
        recargar(cmbRepuesto, repuestos.listar());
        cargarVehiculos();
        cargando = false;
        actualizarOrdenes();
    }

    private void cargarVehiculos() {
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();
        List<Vehiculo> lista = new ArrayList<>();
        if (cliente != null) {
            try {
                lista = vehiculos.listarPorCliente(cliente.getCodigo());
            } catch (ClienteNoEncontradoException ex) {
                // el cliente fue eliminado: la lista queda vacía
            }
        }
        recargar(cmbVehiculo, lista);
    }

    private void actualizarOrdenes() {
        String seleccion = numeroSeleccionado();
        Object estado = cmbEstado.getSelectedItem();
        mostradas = estado instanceof EstadoOrden e ? ordenes.listarPorEstado(e) : ordenes.listar();
        modeloOrdenes.setRowCount(0);
        for (OrdenTrabajo o : mostradas) {
            modeloOrdenes.addRow(new Object[]{o.getNumero(), o.getFecha(), o.getCliente().getNombreCompleto(),
                o.getVehiculo().getPlaca(), o.getMecanico(), o.getEstado(),
                Formato.dinero(o.getCostoTotal()), o.isFacturada() ? "Sí" : "No"});
        }
        if (seleccion != null) { // conserva la orden seleccionada
            for (int i = 0; i < mostradas.size(); i++) {
                if (mostradas.get(i).getNumero().equals(seleccion)) {
                    tablaOrdenes.setRowSelectionInterval(i, i);
                }
            }
        }
    }

    private String numeroSeleccionado() {
        int fila = tablaOrdenes.getSelectedRow();
        return fila < 0 || fila >= mostradas.size() ? null : mostradas.get(fila).getNumero();
    }

    private OrdenTrabajo seleccionada() {
        int fila = tablaOrdenes.getSelectedRow();
        if (fila < 0 || fila >= mostradas.size()) {
            Mensajes.error(this, "Seleccione una orden de la tabla.");
            return null;
        }
        return mostradas.get(fila);
    }

    // =====================================================================
    //  Armado de la orden
    // =====================================================================
    private void agregarServicio() {
        Servicio s = (Servicio) cmbServicio.getSelectedItem();
        if (s == null) {
            Mensajes.error(this, "No hay servicios en el catálogo.");
            return;
        }
        if (serviciosElegidos.contains(s)) {
            Mensajes.error(this, "El servicio " + s + " ya fue agregado a la orden.");
            return;
        }
        serviciosElegidos.add(s);
        refrescarElegidos();
    }

    private void quitarServicio() {
        int fila = tablaServiciosElegidos.getSelectedRow();
        if (fila < 0) {
            Mensajes.error(this, "Seleccione en la lista el servicio que desea quitar.");
            return;
        }
        serviciosElegidos.remove(fila);
        refrescarElegidos();
    }

    private void agregarRepuesto() {
        Repuesto r = (Repuesto) cmbRepuesto.getSelectedItem();
        if (r == null) {
            Mensajes.error(this, "No hay repuestos registrados en el inventario.");
            return;
        }
        try {
            int cantidad = Validador.enteroPositivo("Cantidad", txtCantidadRepuesto.getText());
            repuestosElegidos.merge(r, cantidad, Integer::sum);
            txtCantidadRepuesto.setText("");
            refrescarElegidos();
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void quitarRepuesto() {
        int fila = tablaRepuestosElegidos.getSelectedRow();
        if (fila < 0) {
            Mensajes.error(this, "Seleccione en la lista el repuesto que desea quitar.");
            return;
        }
        repuestosElegidos.remove(new ArrayList<>(repuestosElegidos.keySet()).get(fila));
        refrescarElegidos();
    }

    /** Redibuja las listas elegidas y calcula el costo estimado con los datos actuales. */
    private void refrescarElegidos() {
        double total = 0;
        modeloServiciosElegidos.setRowCount(0);
        for (Servicio s : serviciosElegidos) {
            modeloServiciosElegidos.addRow(new Object[]{s.getCodigo(), s.getDescripcion(),
                Formato.dinero(s.getPrecioBase())});
            total += s.getPrecioBase();
        }
        modeloRepuestosElegidos.setRowCount(0);
        for (Map.Entry<Repuesto, Integer> e : repuestosElegidos.entrySet()) {
            Repuesto r = e.getKey();
            double subtotal = r.getPrecioUnitario() * e.getValue();
            modeloRepuestosElegidos.addRow(new Object[]{r.getCodigo(), r.getNombre(), e.getValue(),
                Formato.dinero(r.getPrecioUnitario()), Formato.dinero(subtotal)});
            total += subtotal;
        }
        lblTotal.setText("Costo estimado: " + Formato.dinero(total));
    }

    private void limpiarFormulario() {
        serviciosElegidos.clear();
        repuestosElegidos.clear();
        txtMecanico.setText("");
        txtDiagnostico.setText("");
        txtCantidadRepuesto.setText("");
        refrescarElegidos();
    }

    private void crear() {
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();
        Vehiculo vehiculo = (Vehiculo) cmbVehiculo.getSelectedItem();
        List<String> codigosServicios = new ArrayList<>();
        for (Servicio s : serviciosElegidos) {
            codigosServicios.add(s.getCodigo());
        }
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        for (Map.Entry<Repuesto, Integer> e : repuestosElegidos.entrySet()) {
            cantidades.put(e.getKey().getCodigo(), e.getValue());
        }
        try {
            OrdenTrabajo orden = ordenes.crear(
                    cliente == null ? "" : cliente.getCodigo(),
                    vehiculo == null ? "" : vehiculo.getPlaca(),
                    txtDiagnostico.getText(), txtMecanico.getText(), codigosServicios, cantidades);
            limpiarFormulario();
            actualizarOrdenes();
            Mensajes.info(this, "Orden " + orden.getNumero() + " creada en estado Pendiente.\n"
                    + "Costo total: " + Formato.dinero(orden.getCostoTotal()) + "\n"
                    + "Use la pestaña 'Órdenes registradas' para procesarla.");
        } catch (ValidacionException | ClienteNoEncontradoException | StockInsuficienteException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    // =====================================================================
    //  Acciones sobre órdenes registradas
    // =====================================================================
    private void verDetalle() {
        OrdenTrabajo o = seleccionada();
        if (o == null) {
            return;
        }
        JTextArea area = new JTextArea(Detalles.orden(o), 20, 52);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Detalle de la orden " + o.getNumero(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelar() {
        OrdenTrabajo o = seleccionada();
        if (o == null) {
            return;
        }
        if (!Mensajes.confirmar(this, "¿Cancelar la orden " + o.getNumero() + "?")) {
            return;
        }
        try {
            ordenes.cancelar(o.getNumero());
            actualizarOrdenes();
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    /** Inicia el procesamiento en un hilo de fondo mostrando el avance sin bloquear la ventana. */
    private void procesar() {
        OrdenTrabajo o = seleccionada();
        if (o == null) {
            return;
        }
        String numero = o.getNumero();
        try {
            ordenes.iniciarProceso(numero); // marca En proceso y evita duplicados
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
            return;
        }
        JProgressBar barra = new JProgressBar(0, 100);
        barra.setStringPainted(true);
        agregarFilaProceso(numero, barra);
        actualizarOrdenes();

        new ProcesoServicioHilo(numero, barra::setValue,
                () -> terminarProceso(numero),
                motivo -> fallarProceso(numero, motivo)).execute();
    }

    /** Se ejecuta en el EDT cuando el hilo terminó: aquí se finaliza la orden y se descuenta el stock. */
    private void terminarProceso(String numero) {
        String mensaje;
        boolean exito = false;
        try {
            ordenes.finalizarOrden(numero);
            OrdenTrabajo o = ordenes.buscar(numero);
            StringBuilder sb = new StringBuilder("La orden " + numero
                    + " fue finalizada y se descontaron los repuestos del inventario.");
            for (DetalleRepuesto d : o.getRepuestos()) {
                if (d.repuesto().tieneStockBajo()) {
                    sb.append("\nALERTA: stock bajo de ").append(d.repuesto().getNombre())
                      .append(" (").append(d.repuesto().getCantidad()).append(" unidades).");
                }
            }
            mensaje = sb.toString();
            exito = true;
        } catch (ValidacionException | StockInsuficienteException ex) {
            mensaje = ex.getMessage() + "\nLa orden permanece En proceso: reponga el stock y vuelva a procesarla.";
        } finally {
            ordenes.liberarProceso(numero);
            quitarFilaProceso(numero);
            actualizarOrdenes();
        }
        if (exito) {
            Mensajes.info(this, mensaje);
        } else {
            Mensajes.error(this, mensaje);
        }
    }

    private void fallarProceso(String numero, String motivo) {
        ordenes.liberarProceso(numero);
        quitarFilaProceso(numero);
        actualizarOrdenes();
        Mensajes.error(this, "Se interrumpió el procesamiento de la orden " + numero + ": " + motivo);
    }

    // ---- lista visual de procesos activos ----
    private void agregarFilaProceso(String numero, JProgressBar barra) {
        JPanel fila = new JPanel(new BorderLayout(10, 0));
        fila.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        fila.add(new JLabel("Procesando " + numero), BorderLayout.WEST);
        fila.add(barra, BorderLayout.CENTER);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        filasProceso.put(numero, fila);
        refrescarProcesos();
    }

    private void quitarFilaProceso(String numero) {
        filasProceso.remove(numero);
        refrescarProcesos();
    }

    private void refrescarProcesos() {
        panelProcesos.removeAll();
        if (filasProceso.isEmpty()) {
            panelProcesos.add(new JLabel("  Sin procesos activos."));
        }
        for (JPanel fila : filasProceso.values()) {
            panelProcesos.add(fila);
        }
        panelProcesos.revalidate();
        panelProcesos.repaint();
    }
}
