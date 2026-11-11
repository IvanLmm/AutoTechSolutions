package vista;

import controlador.FacturaController;
import controlador.OrdenController;
import excepciones.PagoInsuficienteException;
import excepciones.ValidacionException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modelo.Factura;
import modelo.OrdenTrabajo;

public class PanelFacturacion extends PanelModulo {

    private final FacturaController facturas;
    private final OrdenController ordenes;

    private final JComboBox<OrdenTrabajo> cmbOrden = new JComboBox<>();
    private final JTextField txtImpuesto = new JTextField(6);
    private final JLabel lblImpuesto = new JLabel();
    private final JTextField txtEfectivo = new JTextField(10);
    private final JLabel lblCambio = new JLabel(" ");
    private final JTextArea txtDetalle = new JTextArea();

    private final DefaultTableModel modelo = modeloNoEditable(
            "Factura", "Fecha", "Orden", "Cliente", "Total", "Estado");
    private final JTable tabla = new JTable(modelo);
    private List<Factura> mostradas = new ArrayList<>();

    public PanelFacturacion(FacturaController facturas, OrdenController ordenes) {
        this.facturas = facturas;
        this.ordenes = ordenes;

        cmbOrden.setPreferredSize(new Dimension(300, 26));

        // ---- parte superior: generar factura e impuesto ----
        JPanel generar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        generar.setBorder(BorderFactory.createTitledBorder("Generar factura (órdenes Finalizadas sin facturar)"));
        generar.add(new JLabel("Orden:"));
        generar.add(cmbOrden);
        generar.add(boton("Generar factura", this::generar));

        JPanel impuesto = new JPanel(new FlowLayout(FlowLayout.LEFT));
        impuesto.setBorder(BorderFactory.createTitledBorder("Impuesto (se aplica a las facturas nuevas)"));
        impuesto.add(new JLabel("Porcentaje:"));
        impuesto.add(txtImpuesto);
        impuesto.add(new JLabel("%"));
        impuesto.add(boton("Aplicar", this::aplicarImpuesto));
        impuesto.add(lblImpuesto);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(generar, BorderLayout.NORTH);
        norte.add(impuesto, BorderLayout.SOUTH);

        // ---- parte derecha: detalle y pago ----
        txtDetalle.setEditable(false);
        txtDetalle.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel pago = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pago.setBorder(BorderFactory.createTitledBorder("Pago en efectivo"));
        pago.add(new JLabel("Efectivo recibido:"));
        pago.add(txtEfectivo);
        pago.add(boton("Pagar", this::pagar));
        lblCambio.setFont(lblCambio.getFont().deriveFont(Font.BOLD, 14f));
        pago.add(lblCambio);

        JPanel derecha = new JPanel(new BorderLayout(6, 6));
        derecha.add(new JScrollPane(txtDetalle), BorderLayout.CENTER);
        derecha.add(pago, BorderLayout.SOUTH);

        configurarTabla(tabla);
        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tabla), derecha);
        division.setResizeWeight(0.5);

        add(norte, BorderLayout.NORTH);
        add(division, BorderLayout.CENTER);

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalle();
            }
        });
        actualizar();
    }

    @Override
    public void actualizar() {
        String seleccion = numeroSeleccionado();
        recargar(cmbOrden, ordenes.listarPendientesDeFacturar());
        lblImpuesto.setText("Vigente: " + Formato.campo(facturas.getImpuestoPorcentaje()) + " %");
        if (txtImpuesto.getText().isBlank()) {
            txtImpuesto.setText(Formato.campo(facturas.getImpuestoPorcentaje()));
        }
        mostradas = facturas.listar();
        modelo.setRowCount(0);
        for (Factura f : mostradas) {
            modelo.addRow(new Object[]{f.getNumero(), f.getFecha(), f.getOrden().getNumero(),
                f.getOrden().getCliente().getNombreCompleto(), Formato.dinero(f.getTotal()),
                f.isPagada() ? "Pagada" : "Pendiente de pago"});
        }
        seleccionar(seleccion);
        mostrarDetalle();
    }

    private String numeroSeleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 || fila >= mostradas.size() ? null : mostradas.get(fila).getNumero();
    }

    private void seleccionar(String numero) {
        if (numero == null) {
            return;
        }
        for (int i = 0; i < mostradas.size(); i++) {
            if (mostradas.get(i).getNumero().equals(numero)) {
                tabla.setRowSelectionInterval(i, i);
            }
        }
    }

    private void mostrarDetalle() {
        int fila = tabla.getSelectedRow();
        txtDetalle.setText(fila < 0 || fila >= mostradas.size()
                ? "Seleccione una factura para ver su detalle."
                : Detalles.factura(mostradas.get(fila)));
        txtDetalle.setCaretPosition(0);
    }

    private void generar() {
        OrdenTrabajo orden = (OrdenTrabajo) cmbOrden.getSelectedItem();
        if (orden == null) {
            Mensajes.error(this, "No hay órdenes Finalizadas pendientes de facturar.");
            return;
        }
        try {
            Factura f = facturas.generar(orden.getNumero());
            lblCambio.setText(" ");
            txtEfectivo.setText("");
            actualizar();
            seleccionar(f.getNumero());
            Mensajes.info(this, "Factura " + f.getNumero() + " generada.\nTotal a pagar: "
                    + Formato.dinero(f.getTotal()));
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void aplicarImpuesto() {
        try {
            facturas.configurarImpuesto(txtImpuesto.getText());
            actualizar();
            Mensajes.info(this, "Impuesto configurado en " + Formato.campo(facturas.getImpuestoPorcentaje())
                    + " % para las próximas facturas.");
        } catch (ValidacionException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }

    private void pagar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= mostradas.size()) {
            Mensajes.error(this, "Seleccione la factura que desea pagar.");
            return;
        }
        Factura f = mostradas.get(fila);
        try {
            double cambio = facturas.pagar(f.getNumero(), txtEfectivo.getText());
            lblCambio.setText("Cambio: " + Formato.dinero(cambio));
            txtEfectivo.setText("");
            actualizar();
            Mensajes.info(this, "Pago registrado. Cambio a entregar: " + Formato.dinero(cambio));
        } catch (ValidacionException | PagoInsuficienteException ex) {
            Mensajes.error(this, ex.getMessage());
        }
    }
}
