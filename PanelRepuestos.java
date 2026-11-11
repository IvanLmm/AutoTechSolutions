package vista;

import controlador.RepuestoController;
import excepciones.RegistroDuplicadoException;
import excepciones.ValidacionException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import modelo.Repuesto;

public class PanelRepuestos extends PanelModulo {

    private final RepuestoController controlador;

    private final JTextField txtCodigo = new JTextField(10);
    private final JTextField txtNombre = new JTextField(22);
    private final JTextField txtCantidad = new JTextField(8);
    private final JTextField txtPrecio = new JTextField(10);
    private final JTextField txtStockMinimo = new JTextField(8);
    private final JTextField txtEntrada = new JTextField(8);
    private final JCheckBox chkSoloAlertas = new JCheckBox("Mostrar solo alertas de stock bajo");
    private final JLabel lblAlertas = new JLabel(" ");

    private final DefaultTableModel modelo =
            modeloNoEditable("Código", "Nombre", "Existencias", "Precio unitario", "Stock mínimo", "Estado");
    private final JTable tabla = new JTable(modelo);
    private List<Repuesto> mostrados = new ArrayList<>();

    public PanelRepuestos(RepuestoController controlador) {
        this.controlador = controlador;

        JPanel form = crearFormulario("Datos del repuesto");
        agregarCampo(form, 0, 0, "Código:", txtCodigo);
        agregarCampo(form, 0, 1, "Nombre:", txtNombre);
        agregarCampo(form, 1, 0, "Cantidad inicial:", txtCantidad);
        agregarCampo(form, 1, 1, "Precio unitario:", txtPrecio);
        agregarCampo(form, 2, 0, "Stock mínimo:", txtStockMinimo);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(boton("Registrar", this::registrar));
        botones.add(boton("Modificar", this::modificar));
        botones.add(boton("Eliminar", this::eliminar));
        botones.add(boton("Limpiar", this::limpiar));

        JPanel entrada = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entrada.setBorder(javax.swing.BorderFactory.createTitledBorder("Entrada de inventario (repuesto seleccionado)"));
        entrada.add(new JLabel("Unidades a ingresar:"));
        entrada.add(txtEntrada);
        entrada.add(boton("Registrar entrada", this::registrarEntrada));

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(form, BorderLayout.NORTH);
        JPanel abajo = new JPanel(new BorderLayout());
        abajo.add(botones, BorderLayout.NORTH);
        abajo.add(entrada, BorderLayout.SOUTH);
        norte.add(abajo, BorderLayout.CENTER);

        lblAlertas.setForeground(new Color(180, 0, 0));
        JPanel estado = new JPanel(new FlowLayout(FlowLayout.LEFT));
        estado.add(chkSoloAlertas);
        estado.add(lblAlertas);

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(estado, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);

        add(norte, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        configurarTabla(tabla);
        // Resalta en rojo claro los repuestos con existencias bajas
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                           boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                if (!seleccionada) {
                    boolean bajo = fila < mostrados.size() && mostrados.get(fila).tieneStockBajo();
                    c.setBackground(bajo ? new Color(255, 205, 205) : t.getBackground());
                    c.setForeground(bajo ? Color.BLACK : t.getForeground());
                }
                return c;
            }
        });
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarSeleccion();
            }
        });
        chkSoloAlertas.addActionListener(e -> actualizar());
        actualizar();
    }

    @Override
    public void actualizar() {
        mostrados = chkSoloAlertas.isSelected() ? controlador.listarAlertas() : controlador.listar();
        modelo.setRowCount(0);
        for (Repuesto r : mostrados) {
            modelo.addRow(new Object[]{r.getCodigo(), r.getNombre(), r.getCantidad(),
                Formato.dinero(r.getPrecioUnitario()), r.getStockMinimo(),
                r.tieneStockBajo() ? "STOCK BAJO" : "Normal"});
        }
        int alertas = controlador.listarAlertas().size();
        lblAlertas.setText(alertas == 0 ? " " : "ALERTA: " + alertas + " repuesto(s) con stock bajo");
    }

    private void cargarSeleccion() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= mostrados.size()) {
            return;
        }
        Repuesto r = mostrados.get(fila);
        txtCodigo.setText(r.getCodigo());
        txtNombre.setText(r.getNombre());
        txtCantidad.setText(String.valueOf(r.getCantidad()));
        txtPrecio.setText(Formato.campo(r.getPrecioUnitario()));
        txtStockMinimo.setText(String.valueOf(r.getStockMinimo()));
        txtCodigo.setEditable(false);
        txtCantidad.setEditable(false); // las existencias cambian solo con entradas u órdenes
    }

    private void limpiar() {
        txtCodigo.setText("");
        txtNombre.setText("");
        txtCantidad.setText("");
        txtPrecio.setText("");
        txtStockMinimo.setText("");
        txtEntrada.setText("");
        txtCodigo.setEditable(true);
        txtCantidad.setEditable(true);
        tabla.clearSelection();
        txtCodigo.requestFocusInWindow();
    }

    private void registrar() {
        if (!txtCodigo.isEditable()) {
            Mensajes.error(this, "Hay un repuesto seleccionado. Pulse 'Limpiar' para registrar uno nuevo.");
            return;
        }
        try {
            controlador.registrar(txtCodigo.getText(), txtNombre.getText(), txtCantidad.getText(),
                    txtPrecio.getText(), txtStockMinimo.getText());
            limpiar();
            actualizar();
            Mensajes.info(this, "Repuesto registrado correctamente.");
        } catch (ValidacionException | RegistroDuplicadoException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void modificar() {
        if (tabla.getSelectedRow() < 0) {
            Mensajes.error(this, "Seleccione un repuesto de la tabla para modificarlo.");
            return;
        }
        try {
            controlador.modificar(txtCodigo.getText(), txtNombre.getText(),
                    txtPrecio.getText(), txtStockMinimo.getText());
            limpiar();
            actualizar();
            Mensajes.info(this, "Repuesto modificado correctamente.");
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void registrarEntrada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Mensajes.error(this, "Seleccione en la tabla el repuesto que recibe la entrada.");
            return;
        }
        String codigo = mostrados.get(fila).getCodigo();
        try {
            controlador.registrarEntrada(codigo, txtEntrada.getText());
            int existencias = controlador.buscar(codigo).getCantidad();
            limpiar();
            actualizar();
            Mensajes.info(this, "Entrada registrada. Existencias actuales: " + existencias + ".");
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Mensajes.error(this, "Seleccione un repuesto de la tabla para eliminarlo.");
            return;
        }
        Repuesto r = mostrados.get(fila);
        if (!Mensajes.confirmar(this, "¿Eliminar el repuesto " + r + "?")) {
            return;
        }
        try {
            controlador.eliminar(r.getCodigo());
            limpiar();
            actualizar();
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }
}
