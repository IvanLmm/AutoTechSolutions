package modelo;

import excepciones.StockInsuficienteException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrdenTrabajo {
    private final String numero;
    private final LocalDate fecha;
    private final Cliente cliente;
    private final Vehiculo vehiculo;
    private final String diagnostico;
    private final String mecanico;
    private final List<DetalleServicio> servicios = new ArrayList<>();
    private final List<DetalleRepuesto> repuestos = new ArrayList<>();
    private EstadoOrden estado = EstadoOrden.PENDIENTE;
    private boolean stockDescontado = false; // garantiza descuento una sola vez
    private boolean facturada = false;

    public OrdenTrabajo(String numero, LocalDate fecha, Cliente cliente, Vehiculo vehiculo,
                        String diagnostico, String mecanico) {
        this.numero = numero;
        this.fecha = fecha;
        this.cliente = cliente;
        this.vehiculo = vehiculo;
        this.diagnostico = diagnostico;
        this.mecanico = mecanico;
    }

    public String getNumero() { return numero; }
    public LocalDate getFecha() { return fecha; }
    public Cliente getCliente() { return cliente; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public String getDiagnostico() { return diagnostico; }
    public String getMecanico() { return mecanico; }
    public EstadoOrden getEstado() { return estado; }
    public boolean isStockDescontado() { return stockDescontado; }
    public boolean isFacturada() { return facturada; }
    public void setFacturada(boolean facturada) { this.facturada = facturada; }

    public List<DetalleServicio> getServicios() { return Collections.unmodifiableList(servicios); }
    public List<DetalleRepuesto> getRepuestos() { return Collections.unmodifiableList(repuestos); }

    // ---- Edición (solo mientras la orden esté Pendiente) ----
    /** Agrega el servicio congelando su precio actual. */
    public void agregarServicio(Servicio s) {
        exigirPendiente();
        servicios.add(new DetalleServicio(s, s.getPrecioBase()));
    }

    /** Agrega el repuesto congelando su precio actual. */
    public void agregarRepuesto(Repuesto r, int cantidad) {
        exigirPendiente();
        repuestos.add(new DetalleRepuesto(r, cantidad, r.getPrecioUnitario()));
    }

    private void exigirPendiente() {
        if (estado != EstadoOrden.PENDIENTE) {
            throw new IllegalStateException("Solo se puede editar una orden Pendiente.");
        }
    }

    // ---- Costos calculados automáticamente ----
    public double getCostoServicios() {
        double total = 0;
        for (DetalleServicio d : servicios) {
            total += d.precio();
        }
        return total;
    }

    public double getCostoRepuestos() {
        double total = 0;
        for (DetalleRepuesto d : repuestos) {
            total += d.subtotal();
        }
        return total;
    }

    public double getCostoTotal() {
        return getCostoServicios() + getCostoRepuestos();
    }

    // ---- Cambios de estado ----
    public void iniciarProceso() {
        if (estado != EstadoOrden.PENDIENTE) {
            throw new IllegalStateException("Solo una orden Pendiente puede pasar a En proceso.");
        }
        estado = EstadoOrden.EN_PROCESO;
    }

    /**
     * Finaliza la orden y descuenta el stock UNA sola vez.
     * Primero verifica todos los repuestos y luego descuenta, para que
     * no quede un descuento a medias si falta stock de alguno.
     */
    public void finalizar() throws StockInsuficienteException {
        if (estado != EstadoOrden.EN_PROCESO) {
            throw new IllegalStateException("Solo una orden En proceso puede finalizarse.");
        }
        if (!stockDescontado) {
            for (DetalleRepuesto d : repuestos) {
                if (d.cantidad() > d.repuesto().getCantidad()) {
                    throw new StockInsuficienteException(d.repuesto().getNombre(),
                            d.repuesto().getCantidad(), d.cantidad());
                }
            }
            for (DetalleRepuesto d : repuestos) {
                d.repuesto().descontarStock(d.cantidad());
            }
            stockDescontado = true;
        }
        estado = EstadoOrden.FINALIZADA;
    }

    public void cancelar() {
        if (estado == EstadoOrden.FINALIZADA || estado == EstadoOrden.CANCELADA) {
            throw new IllegalStateException("No se puede cancelar una orden " + estado + ".");
        }
        estado = EstadoOrden.CANCELADA;
    }

    /**
     * Uso exclusivo del "deshacer": vuelve al estado anterior y, si la orden
     * ya había descontado stock, lo devuelve al inventario.
     */
    public void deshacerCambioEstado(EstadoOrden anterior) {
        if (estado == EstadoOrden.FINALIZADA && stockDescontado) {
            for (DetalleRepuesto d : repuestos) {
                d.repuesto().agregarStock(d.cantidad());
            }
            stockDescontado = false;
        }
        estado = anterior;
    }

    @Override
    public String toString() {
        return numero + " - " + cliente.getNombreCompleto() + " (" + estado + ")";
    }
}
