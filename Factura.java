package modelo;

import excepciones.PagoInsuficienteException;
import java.time.LocalDate;

public class Factura {
    private final String numero;
    private final LocalDate fecha;
    private final OrdenTrabajo orden;
    private final double tasaImpuesto;      // por ejemplo 0.15 = 15 %
    private final double subtotalServicios;
    private final double subtotalRepuestos;
    private final double impuesto;
    private final double total;
    private boolean pagada = false;
    private double montoPagado;
    private double cambio;

    public Factura(String numero, LocalDate fecha, OrdenTrabajo orden, double tasaImpuesto) {
        this.numero = numero;
        this.fecha = fecha;
        this.orden = orden;
        this.tasaImpuesto = tasaImpuesto;
        this.subtotalServicios = redondear(orden.getCostoServicios());
        this.subtotalRepuestos = redondear(orden.getCostoRepuestos());
        double subtotal = subtotalServicios + subtotalRepuestos;
        this.impuesto = redondear(subtotal * tasaImpuesto);
        this.total = redondear(subtotal + impuesto);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /** Simula pago en efectivo. Devuelve el cambio o lanza excepción si no alcanza. */
    public double pagarEnEfectivo(double efectivo) throws PagoInsuficienteException {
        if (pagada) {
            throw new IllegalStateException("La factura ya fue pagada.");
        }
        if (efectivo < total) {
            throw new PagoInsuficienteException(total, efectivo);
        }
        montoPagado = efectivo;
        cambio = redondear(efectivo - total);
        pagada = true;
        return cambio;
    }

    /** Uso exclusivo del "deshacer": revierte un pago realizado. */
    public void anularPago() {
        pagada = false;
        montoPagado = 0;
        cambio = 0;
    }

    public String getNumero() { return numero; }
    public LocalDate getFecha() { return fecha; }
    public OrdenTrabajo getOrden() { return orden; }
    public double getTasaImpuesto() { return tasaImpuesto; }
    public double getSubtotalServicios() { return subtotalServicios; }
    public double getSubtotalRepuestos() { return subtotalRepuestos; }
    public double getImpuesto() { return impuesto; }
    public double getTotal() { return total; }
    public boolean isPagada() { return pagada; }
    public double getMontoPagado() { return montoPagado; }
    public double getCambio() { return cambio; }

    @Override
    public String toString() {
        return numero + " - Total: " + String.format("%.2f", total) + (pagada ? " (Pagada)" : " (Pendiente de pago)");
    }
}
