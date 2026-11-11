package vista;

import controlador.ServicioController;
import excepciones.RegistroDuplicadoException;
import excepciones.ValidacionException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modelo.Servicio;

public class PanelServicios extends PanelModulo {

    private final ServicioController controlador;

    private final JTextField txtCodigo = new JTextField(10);
    private final JTextField txtDescripcion = new JTextField(24);
    private final JTextField txtPrecio = new JTextField(10);
    private final JTextField txtTiempo = new JTextField(8);

    private final DefaultTableModel modelo =
            modeloNoEditable("Código", "Descripción", "Precio base", "Tiempo estimado (min)");
    private final JTable tabla = new JTable(modelo);
    private List<Servicio> mostrados = new ArrayList<>();

    public PanelServicios(ServicioController controlador) {
        this.controlador = controlador;

        JPanel form = crearFormulario("Datos del servicio");
        agregarCampo(form, 0, 0, "Código:", txtCodigo);
        agregarCampo(form, 0, 1, "Descripción:", txtDescripcion);
        agregarCampo(form, 1, 0, "Precio base:", txtPrecio);
        agregarCampo(form, 1, 1, "Tiempo estimado (min):", txtTiempo);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(boton("Registrar", this::registrar));
        botones.add(boton("Modificar", this::modificar));
        botones.add(boton("Eliminar", this::eliminar));
        botones.add(boton("Limpiar", this::limpiar));

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(form, BorderLayout.CENTER);
        norte.add(botones, BorderLayout.SOUTH);

        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        configurarTabla(tabla);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccion();
            }
        });
        actualizar();
    }

    @Override
    public void actualizar() {
        mostrados = controlador.listar();
        modelo.setRowCount(0);
        for (Servicio s : mostrados) {
            modelo.addRow(new Object[]{s.getCodigo(), s.getDescripcion(),
                Formato.dinero(s.getPrecioBase()), s.getTiempoEstimadoMinutos()});
        }
    }

    private void cargarSeleccion() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= mostrados.size()) {
            return;
        }
        Servicio s = mostrados.get(fila);
        txtCodigo.setText(s.getCodigo());
        txtDescripcion.setText(s.getDescripcion());
        txtPrecio.setText(Formato.campo(s.getPrecioBase()));
        txtTiempo.setText(String.valueOf(s.getTiempoEstimadoMinutos()));
        txtCodigo.setEditable(false);
    }

    private void limpiar() {
        txtCodigo.setText("");
        txtDescripcion.setText("");
        txtPrecio.setText("");
        txtTiempo.setText("");
        txtCodigo.setEditable(true);
        tabla.clearSelection();
        txtCodigo.requestFocusInWindow();
    }

    private void registrar() {
        if (!txtCodigo.isEditable()) {
            Mensajes.error(this, "Hay un servicio seleccionado. Pulse 'Limpiar' para registrar uno nuevo.");
            return;
        }
        try {
            controlador.registrar(txtCodigo.getText(), txtDescripcion.getText(),
                    txtPrecio.getText(), txtTiempo.getText());
            limpiar();
            actualizar();
            Mensajes.info(this, "Servicio registrado correctamente.");
        } catch (ValidacionException | RegistroDuplicadoException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void modificar() {
        if (tabla.getSelectedRow() < 0) {
            Mensajes.error(this, "Seleccione un servicio de la tabla para modificarlo.");
            return;
        }
        try {
            controlador.modificar(txtCodigo.getText(), txtDescripcion.getText(),
                    txtPrecio.getText(), txtTiempo.getText());
            limpiar();
            actualizar();
            Mensajes.info(this, "Servicio modificado correctamente.");
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Mensajes.error(this, "Seleccione un servicio de la tabla para eliminarlo.");
            return;
        }
        Servicio s = mostrados.get(fila);
        if (!Mensajes.confirmar(this, "¿Eliminar el servicio " + s + "?")) {
            return;
        }
        try {
            controlador.eliminar(s.getCodigo());
            limpiar();
            actualizar();
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }
}
