package vista;

import controlador.ClienteController;
import controlador.VehiculoController;
import excepciones.ClienteNoEncontradoException;
import excepciones.RegistroDuplicadoException;
import excepciones.ValidacionException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modelo.Cliente;
import modelo.Vehiculo;

public class PanelVehiculos extends PanelModulo {

    private static final String TODOS = "Todos los clientes";

    private final VehiculoController controlador;
    private final ClienteController clientes;

    private final JTextField txtPlaca = new JTextField(10);
    private final JTextField txtMarca = new JTextField(12);
    private final JTextField txtModelo = new JTextField(12);
    private final JTextField txtAnio = new JTextField(6);
    private final JTextField txtColor = new JTextField(10);
    private final JComboBox<Cliente> cmbPropietario = new JComboBox<>();
    private final JComboBox<Object> cmbFiltro = new JComboBox<>();
    private final JTextField txtBuscarPlaca = new JTextField(10);

    private final DefaultTableModel modelo =
            modeloNoEditable("Placa", "Marca", "Modelo", "Año", "Color", "Propietario");
    private final JTable tabla = new JTable(modelo);
    private List<Vehiculo> mostrados = new ArrayList<>();
    private boolean cargando = false;

    public PanelVehiculos(VehiculoController controlador, ClienteController clientes) {
        this.controlador = controlador;
        this.clientes = clientes;

        JPanel form = crearFormulario("Datos del vehículo");
        agregarCampo(form, 0, 0, "Placa:", txtPlaca);
        agregarCampo(form, 0, 1, "Marca:", txtMarca);
        agregarCampo(form, 1, 0, "Modelo:", txtModelo);
        agregarCampo(form, 1, 1, "Año:", txtAnio);
        agregarCampo(form, 2, 0, "Color:", txtColor);
        agregarCampo(form, 2, 1, "Propietario:", cmbPropietario);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(boton("Registrar", this::registrar));
        botones.add(boton("Eliminar", this::eliminar));
        botones.add(boton("Limpiar", this::limpiar));

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(form, BorderLayout.CENTER);
        norte.add(botones, BorderLayout.SOUTH);

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busqueda.add(new JLabel("Buscar por placa:"));
        busqueda.add(txtBuscarPlaca);
        busqueda.add(boton("Buscar", this::buscarPorPlaca));
        busqueda.add(new JLabel("   Listar vehículos de:"));
        busqueda.add(cmbFiltro);

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(busqueda, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);

        add(norte, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        configurarTabla(tabla);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccion();
            }
        });
        cmbFiltro.addActionListener(e -> {
            if (!cargando) {
                actualizarTabla();
            }
        });
        actualizar();
    }

    /** Recarga los combos de clientes (conservando la selección) y la tabla. */
    @Override
    public void actualizar() {
        cargando = true;
        Object propietario = cmbPropietario.getSelectedItem();
        Object filtro = cmbFiltro.getSelectedItem();

        DefaultComboBoxModel<Cliente> modeloPropietario = new DefaultComboBoxModel<>();
        DefaultComboBoxModel<Object> modeloFiltro = new DefaultComboBoxModel<>();
        modeloFiltro.addElement(TODOS);
        for (Cliente c : clientes.listar()) {
            modeloPropietario.addElement(c);
            modeloFiltro.addElement(c);
        }
        cmbPropietario.setModel(modeloPropietario);
        cmbFiltro.setModel(modeloFiltro);
        if (propietario != null && modeloPropietario.getIndexOf(propietario) >= 0) {
            cmbPropietario.setSelectedItem(propietario);
        }
        cmbFiltro.setSelectedItem(filtro != null && modeloFiltro.getIndexOf(filtro) >= 0 ? filtro : TODOS);
        cargando = false;
        actualizarTabla();
    }

    private List<Vehiculo> obtenerVehiculos() {
        Object filtro = cmbFiltro.getSelectedItem();
        if (filtro instanceof Cliente cliente) {
            try {
                return controlador.listarPorCliente(cliente.getCodigo());
            } catch (ClienteNoEncontradoException ex) {
                return new ArrayList<>();
            }
        }
        return controlador.listar();
    }

    private void actualizarTabla() {
        mostrados = obtenerVehiculos();
        modelo.setRowCount(0);
        for (Vehiculo v : mostrados) {
            modelo.addRow(new Object[]{v.getPlaca(), v.getMarca(), v.getModelo(), v.getAnio(),
                v.getColor(), v.getPropietario().getNombreCompleto()});
        }
    }

    private void cargarSeleccion() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= mostrados.size()) {
            return;
        }
        Vehiculo v = mostrados.get(fila);
        txtPlaca.setText(v.getPlaca());
        txtMarca.setText(v.getMarca());
        txtModelo.setText(v.getModelo());
        txtAnio.setText(String.valueOf(v.getAnio()));
        txtColor.setText(v.getColor());
        cmbPropietario.setSelectedItem(v.getPropietario());
        txtPlaca.setEditable(false);
    }

    private void limpiar() {
        txtPlaca.setText("");
        txtMarca.setText("");
        txtModelo.setText("");
        txtAnio.setText("");
        txtColor.setText("");
        txtPlaca.setEditable(true);
        tabla.clearSelection();
        txtPlaca.requestFocusInWindow();
    }

    private void registrar() {
        if (!txtPlaca.isEditable()) {
            Mensajes.error(this, "Hay un vehículo seleccionado. Pulse 'Limpiar' para registrar uno nuevo.");
            return;
        }
        Cliente propietario = (Cliente) cmbPropietario.getSelectedItem();
        String codigoCliente = propietario == null ? "" : propietario.getCodigo();
        try {
            controlador.registrar(txtPlaca.getText(), txtMarca.getText(), txtModelo.getText(),
                    txtAnio.getText(), txtColor.getText(), codigoCliente);
            limpiar();
            actualizarTabla();
            Mensajes.info(this, "Vehículo registrado correctamente.");
        } catch (ValidacionException | RegistroDuplicadoException | ClienteNoEncontradoException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Mensajes.error(this, "Seleccione un vehículo de la tabla para eliminarlo.");
            return;
        }
        Vehiculo v = mostrados.get(fila);
        if (!Mensajes.confirmar(this, "¿Eliminar el vehículo " + v + "?")) {
            return;
        }
        try {
            controlador.eliminar(v.getPlaca());
            limpiar();
            actualizarTabla();
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void buscarPorPlaca() {
        try {
            Vehiculo v = controlador.buscarPorPlaca(txtBuscarPlaca.getText());
            cargando = true;
            cmbFiltro.setSelectedIndex(0); // mostrar todos para poder ubicar la fila
            cargando = false;
            actualizarTabla();
            int fila = mostrados.indexOf(v);
            if (fila >= 0) {
                tabla.setRowSelectionInterval(fila, fila);
                tabla.scrollRectToVisible(tabla.getCellRect(fila, 0, true));
            }
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }
}
