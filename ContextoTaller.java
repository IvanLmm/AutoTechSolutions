package controlador;

import java.util.HashSet;
import java.util.Set;
import modelo.Cliente;
import modelo.Factura;
import modelo.OrdenTrabajo;
import modelo.Repuesto;
import modelo.Servicio;
import modelo.Vehiculo;
import repositorio.RepositorioGenerico;

/**
 * Estado compartido del taller en memoria: todos los repositorios, la pila de
 * deshacer, el impuesto y los contadores. Los controladores trabajan sobre
 * este mismo objeto, así comparten los mismos datos.
 *
 * Toda modificación de datos debe hacerse desde el hilo de Swing (EDT);
 * los hilos de fondo solo simulan trabajo y no tocan estas estructuras.
 */
public class ContextoTaller {

    private final RepositorioGenerico<Cliente> clientes = new RepositorioGenerico<>(Cliente::getCodigo);
    private final RepositorioGenerico<Vehiculo> vehiculos = new RepositorioGenerico<>(Vehiculo::getPlaca);
    private final RepositorioGenerico<Servicio> servicios = new RepositorioGenerico<>(Servicio::getCodigo);
    private final RepositorioGenerico<Repuesto> repuestos = new RepositorioGenerico<>(Repuesto::getCodigo);
    private final RepositorioGenerico<OrdenTrabajo> ordenes = new RepositorioGenerico<>(OrdenTrabajo::getNumero);
    private final RepositorioGenerico<Factura> facturas = new RepositorioGenerico<>(Factura::getNumero);

    private final GestorDeshacer gestorDeshacer = new GestorDeshacer();
    private final Set<String> ordenesEnProcesamiento = new HashSet<>();

    private double tasaImpuesto = 0.15; // configurable (0.15 = 15 %)
    private int ultimoNumeroOrden = 0;
    private int ultimoNumeroFactura = 0;

    public RepositorioGenerico<Cliente> getClientes() { return clientes; }
    public RepositorioGenerico<Vehiculo> getVehiculos() { return vehiculos; }
    public RepositorioGenerico<Servicio> getServicios() { return servicios; }
    public RepositorioGenerico<Repuesto> getRepuestos() { return repuestos; }
    public RepositorioGenerico<OrdenTrabajo> getOrdenes() { return ordenes; }
    public RepositorioGenerico<Factura> getFacturas() { return facturas; }
    public GestorDeshacer getGestorDeshacer() { return gestorDeshacer; }

    public double getTasaImpuesto() { return tasaImpuesto; }
    public void setTasaImpuesto(double tasaImpuesto) { this.tasaImpuesto = tasaImpuesto; }

    // ---- Numeración automática (garantiza números únicos) ----
    public String siguienteNumeroOrden() {
        ultimoNumeroOrden++;
        return String.format("OT-%04d", ultimoNumeroOrden);
    }

    public void retrocederNumeroOrden() {
        if (ultimoNumeroOrden > 0) {
            ultimoNumeroOrden--;
        }
    }

    public String siguienteNumeroFactura() {
        ultimoNumeroFactura++;
        return String.format("F-%04d", ultimoNumeroFactura);
    }

    public void retrocederNumeroFactura() {
        if (ultimoNumeroFactura > 0) {
            ultimoNumeroFactura--;
        }
    }

    // ---- Control de procesamiento (evita procesar la misma orden dos veces) ----
    /** @return false si la orden ya estaba siendo procesada. */
    public boolean reservarProcesamiento(String numeroOrden) {
        return ordenesEnProcesamiento.add(numeroOrden);
    }

    public void liberarProcesamiento(String numeroOrden) {
        ordenesEnProcesamiento.remove(numeroOrden);
    }

    public boolean estaEnProcesamiento(String numeroOrden) {
        return ordenesEnProcesamiento.contains(numeroOrden);
    }

    public boolean hayProcesosActivos() {
        return !ordenesEnProcesamiento.isEmpty();
    }
}
