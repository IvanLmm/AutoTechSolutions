package vista;

import controlador.ClienteController;
import excepciones.ClienteNoEncontradoException;
import excepciones.RegistroDuplicadoException;
import excepciones.ValidacionException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modelo.Cliente;

public class PanelClientes extends PanelModulo {

    private final ClienteController controlador;

    private final JTextField txtCodigo = new JTextField(10);
    private final JTextField txtNombre = new JTextField(22);
    private final JTextField txtIdentidad = new JTextField(16);
    private final JTextField txtTelefono = new JTextField(12);
    private final JTextField txtDireccion = new JTextField(22);
    private final JTextField txtBuscar = new JTextField(22);

    private final DefaultTableModel modelo =
            modeloNoEditable("Código", "Nombre completo", "Identidad", "Teléfono", "Dirección");
    private final JTable tabla = new JTable(modelo);
    private List<Cliente> mostrados = new ArrayList<>();

    public PanelClientes(ClienteController controlador) {
        this.controlador = controlador;

        JPanel form = crearFormulario("Datos del cliente");
        agregarCampo(form, 0, 0, "Código:", txtCodigo);
        agregarCampo(form, 0, 1, "Nombre completo:", txtNombre);
        agregarCampo(form, 1, 0, "Identidad:", txtIdentidad);
        agregarCampo(form, 1, 1, "Teléfono:", txtTelefono);
        agregarCampo(form, 2, 0, "Dirección:", txtDireccion);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(boton("Registrar", this::registrar));
        botones.add(boton("Modificar", this::modificar));
        botones.add(boton("Eliminar", this::eliminar));
        botones.add(boton("Limpiar", this::limpiar));

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(form, BorderLayout.CENTER);
        norte.add(botones, BorderLayout.SOUTH);

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busqueda.add(new JLabel("Buscar (código, nombre o identidad):"));
        busqueda.add(txtBuscar);

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
        alCambiarTexto(txtBuscar, this::actualizar);
        actualizar();
    }

    @Override
    public void actualizar() {
        mostrados = controlador.filtrar(txtBuscar.getText());
        modelo.setRowCount(0);
        for (Cliente c : mostrados) {
            modelo.addRow(new Object[]{c.getCodigo(), c.getNombreCompleto(),
                c.getIdentidad(), c.getTelefono(), c.getDireccion()});
        }
    }

    private void cargarSeleccion() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= mostrados.size()) {
            return;
        }
        Cliente c = mostrados.get(fila);
        txtCodigo.setText(c.getCodigo());
        txtNombre.setText(c.getNombreCompleto());
        txtIdentidad.setText(c.getIdentidad());
        txtTelefono.setText(c.getTelefono());
        txtDireccion.setText(c.getDireccion());
        txtCodigo.setEditable(false); // la clave no se modifica
    }

    private void limpiar() {
        txtCodigo.setText("");
        txtNombre.setText("");
        txtIdentidad.setText("");
        txtTelefono.setText("");
        txtDireccion.setText("");
        txtCodigo.setEditable(true);
        tabla.clearSelection();
        txtCodigo.requestFocusInWindow();
    }

    private void registrar() {
        if (!txtCodigo.isEditable()) {
            Mensajes.error(this, "Hay un cliente seleccionado. Pulse 'Limpiar' para registrar uno nuevo.");
            return;
        }
        try {
            controlador.registrar(txtCodigo.getText(), txtNombre.getText(), txtIdentidad.getText(),
                    txtTelefono.getText(), txtDireccion.getText());
            limpiar();
            actualizar();
            Mensajes.info(this, "Cliente registrado correctamente.");
        } catch (ValidacionException | RegistroDuplicadoException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void modificar() {
        if (tabla.getSelectedRow() < 0) {
            Mensajes.error(this, "Seleccione un cliente de la tabla para modificarlo.");
            return;
        }
        try {
            controlador.modificar(txtCodigo.getText(), txtNombre.getText(), txtIdentidad.getText(),
                    txtTelefono.getText(), txtDireccion.getText());
            limpiar();
            actualizar();
            Mensajes.info(this, "Cliente modificado correctamente.");
        } catch (ValidacionException | ClienteNoEncontradoException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Mensajes.error(this, "Seleccione un cliente de la tabla para eliminarlo.");
            return;
        }
        Cliente c = mostrados.get(fila);
        if (!Mensajes.confirmar(this, "¿Eliminar al cliente " + c + "?")) {
            return;
        }
        try {
            controlador.eliminar(c.getCodigo());
            limpiar();
            actualizar();
        } catch (ValidacionException | ClienteNoEncontradoException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }
}
