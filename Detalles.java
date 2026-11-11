package vista;

import modelo.DetalleRepuesto;
import modelo.DetalleServicio;
import modelo.Factura;
import modelo.OrdenTrabajo;

/** Arma los textos de detalle de órdenes y facturas a partir de los datos reales. */
public final class Detalles {

    private Detalles() { }

    public static String orden(OrdenTrabajo o) {
        StringBuilder sb = new StringBuilder();
        sb.append("Orden:        ").append(o.getNumero()).append('\n');
        sb.append("Fecha:        ").append(o.getFecha()).append('\n');
        sb.append("Estado:       ").append(o.getEstado()).append('\n');
        sb.append("Cliente:      ").append(o.getCliente().getNombreCompleto()).append('\n');
        sb.append("Vehículo:     ").append(o.getVehiculo()).append('\n');
        sb.append("Mecánico:     ").append(o.getMecanico()).append('\n');
        sb.append("Diagnóstico:  ").append(o.getDiagnostico()).append("\n\n");
        lineas(sb, o);
        sb.append(String.format("%nCOSTO TOTAL: %s%n", Formato.dinero(o.getCostoTotal())));
        return sb.toString();
    }

    public static String factura(Factura f) {
        OrdenTrabajo o = f.getOrden();
        StringBuilder sb = new StringBuilder();
        sb.append("FACTURA ").append(f.getNumero()).append('\n');
        sb.append("Fecha:     ").append(f.getFecha()).append('\n');
        sb.append("Orden:     ").append(o.getNumero()).append('\n');
        sb.append("Cliente:   ").append(o.getCliente().getNombreCompleto()).append('\n');
        sb.append("Identidad: ").append(o.getCliente().getIdentidad()).append('\n');
        sb.append("Vehículo:  ").append(o.getVehiculo()).append("\n\n");
        lineas(sb, o);
        sb.append('\n');
        sb.append(String.format("Subtotal servicios: %14s%n", Formato.dinero(f.getSubtotalServicios())));
        sb.append(String.format("Subtotal repuestos: %14s%n", Formato.dinero(f.getSubtotalRepuestos())));
        sb.append(String.format("Impuesto (%.2f%%):   %14s%n", f.getTasaImpuesto() * 100, Formato.dinero(f.getImpuesto())));
        sb.append(String.format("TOTAL:              %14s%n", Formato.dinero(f.getTotal())));
        sb.append('\n');
        if (f.isPagada()) {
            sb.append(String.format("PAGADA en efectivo%nEfectivo recibido: %s%nCambio:            %s%n",
                    Formato.dinero(f.getMontoPagado()), Formato.dinero(f.getCambio())));
        } else {
            sb.append("PENDIENTE DE PAGO\n");
        }
        return sb.toString();
    }

    private static void lineas(StringBuilder sb, OrdenTrabajo o) {
        sb.append("SERVICIOS\n");
        for (DetalleServicio d : o.getServicios()) {
            sb.append(String.format("  %-28s %12s%n", d.servicio().getDescripcion(), Formato.dinero(d.precio())));
        }
        sb.append("REPUESTOS\n");
        if (o.getRepuestos().isEmpty()) {
            sb.append("  (ninguno)\n");
        }
        for (DetalleRepuesto d : o.getRepuestos()) {
            sb.append(String.format("  %-20s %3d x %9s = %12s%n", d.repuesto().getNombre(), d.cantidad(),
                    Formato.dinero(d.precioUnitario()), Formato.dinero(d.subtotal())));
        }
    }
}
