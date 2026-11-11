package controlador;

import excepciones.ClienteNoEncontradoException;
import excepciones.RegistroDuplicadoException;
import excepciones.StockInsuficienteException;
import excepciones.ValidacionException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import modelo.Cliente;
import modelo.EstadoOrden;
import modelo.OrdenTrabajo;
import modelo.Repuesto;
import modelo.Servicio;
import modelo.Vehiculo;

public class OrdenController {

    private final ContextoTaller ctx;

    public OrdenController(ContextoTaller ctx) {
        this.ctx = ctx;
    }

    /**
     * Crea una orden completa en estado Pendiente.
     * @param codigosServicios códigos de los servicios del catálogo
     * @param cantidadesRepuestos código de repuesto -> cantidad (puede ser null o vacío)
     */
    public OrdenTrabajo crear(String codigoCliente, String placa, String diagnostico, String mecanico,
                              List<String> codigosServicios, Map<String, Integer> cantidadesRepuestos)
            throws ValidacionException, ClienteNoEncontradoException, StockInsuficienteException {

        String codigo = Validador.requerido("Cliente", codigoCliente);
        Cliente cliente = ctx.getClientes().buscar(codigo);
        if (cliente == null) {
            throw new ClienteNoEncontradoException(codigo);
        }

        String placaLimpia = Validador.requerido("Vehículo", placa).toUpperCase();
        Vehiculo vehiculo = ctx.getVehiculos().buscar(placaLimpia);
        if (vehiculo == null) {
            throw new ValidacionException("No existe un vehículo con la placa " + placaLimpia + ".");
        }
        if (vehiculo.getPropietario() != cliente) {
            throw new ValidacionException("El vehículo " + placaLimpia
                    + " no pertenece al cliente " + codigo + ".");
        }

        String diagnosticoLimpio = Validador.requerido("Diagnóstico", diagnostico);
        String mecanicoLimpio = Validador.requerido("Mecánico", mecanico);

        if (codigosServicios == null || codigosServicios.isEmpty()) {
            throw new ValidacionException("La orden debe incluir al menos un servicio.");
        }
        List<Servicio> servicios = new ArrayList<>();
        for (String codigoServicio : codigosServicios) {
            Servicio s = ctx.getServicios().buscar(codigoServicio);
            if (s == null) {
                throw new ValidacionException("No existe el servicio " + codigoServicio + ".");
            }
            servicios.add(s);
        }

        Map<Repuesto, Integer> solicitados = new LinkedHashMap<>();
        if (cantidadesRepuestos != null) {
            for (Map.Entry<String, Integer> entrada : cantidadesRepuestos.entrySet()) {
                Repuesto r = ctx.getRepuestos().buscar(entrada.getKey());
                if (r == null) {
                    throw new ValidacionException("No existe el repuesto " + entrada.getKey() + ".");
                }
                int cantidad = entrada.getValue();
                if (cantidad <= 0) {
                    throw new ValidacionException("La cantidad de " + r.getNombre()
                            + " debe ser mayor que cero.");
                }
                if (cantidad > r.getCantidad()) {
                    throw new StockInsuficienteException(r.getNombre(), r.getCantidad(), cantidad);
                }
                solicitados.put(r, cantidad);
            }
        }

        OrdenTrabajo orden = new OrdenTrabajo(ctx.siguienteNumeroOrden(), LocalDate.now(),
                cliente, vehiculo, diagnosticoLimpio, mecanicoLimpio);
        for (Servicio s : servicios) {
            orden.agregarServicio(s);
        }
        for (Map.Entry<Repuesto, Integer> entrada : solicitados.entrySet()) {
            orden.agregarRepuesto(entrada.getKey(), entrada.getValue());
        }

        try {
            ctx.getOrdenes().agregar(orden);
        } catch (RegistroDuplicadoException e) {
            throw new IllegalStateException(e); // no debería ocurrir: el número es automático
        }
        ctx.getGestorDeshacer().registrar("Creación de la orden " + orden.getNumero(), () -> {
            ctx.getOrdenes().deshacer();
            ctx.retrocederNumeroOrden();
        });
        return orden;
    }

    /**
     * Marca la orden En proceso y la reserva para que no se procese dos veces.
     * Quien la llame debe liberar con {@link #liberarProceso(String)} al terminar.
     */
    public OrdenTrabajo iniciarProceso(String numero) throws ValidacionException {
        OrdenTrabajo orden = buscar(numero);
        if (ctx.estaEnProcesamiento(orden.getNumero())) {
            throw new ValidacionException("La orden " + orden.getNumero() + " ya se está procesando.");
        }
        switch (orden.getEstado()) {
            case PENDIENTE -> orden.iniciarProceso();
            case EN_PROCESO -> { }
            default -> throw new ValidacionException("La orden " + orden.getNumero() + " está "
                    + orden.getEstado() + " y no puede procesarse.");
        }
        ctx.reservarProcesamiento(orden.getNumero());
        return orden;
    }

    /** Finaliza la orden y descuenta el stock una sola vez. */
    public void finalizarOrden(String numero) throws ValidacionException, StockInsuficienteException {
        OrdenTrabajo orden = buscar(numero);
        if (orden.getEstado() != EstadoOrden.EN_PROCESO) {
            throw new ValidacionException("Solo una orden En proceso puede finalizarse "
                    + "(estado actual: " + orden.getEstado() + ").");
        }
        orden.finalizar();
        ctx.getGestorDeshacer().registrar("Finalización de la orden " + orden.getNumero(),
                () -> orden.deshacerCambioEstado(EstadoOrden.EN_PROCESO));
    }

    public void liberarProceso(String numero) {
        ctx.liberarProcesamiento(numero);
    }

    public void cancelar(String numero) throws ValidacionException {
        OrdenTrabajo orden = buscar(numero);
        if (ctx.estaEnProcesamiento(orden.getNumero())) {
            throw new ValidacionException("No se puede cancelar la orden mientras se procesa.");
        }
        if (orden.getEstado() == EstadoOrden.FINALIZADA || orden.getEstado() == EstadoOrden.CANCELADA) {
            throw new ValidacionException("No se puede cancelar una orden "
                    + orden.getEstado() + ".");
        }
        EstadoOrden anterior = orden.getEstado();
        orden.cancelar();
        ctx.getGestorDeshacer().registrar("Cancelación de la orden " + orden.getNumero(),
                () -> orden.deshacerCambioEstado(anterior));
    }

    public OrdenTrabajo buscar(String numero) throws ValidacionException {
        String clave = Validador.requerido("Número de orden", numero);
        OrdenTrabajo o = ctx.getOrdenes().buscar(clave);
        if (o == null) {
            throw new ValidacionException("No existe la orden " + clave + ".");
        }
        return o;
    }

    public List<OrdenTrabajo> listar() {
        return ctx.getOrdenes().listar();
    }

    public List<OrdenTrabajo> listarPorEstado(EstadoOrden estado) {
        List<OrdenTrabajo> resultado = new ArrayList<>();
        for (OrdenTrabajo o : ctx.getOrdenes().listar()) {
            if (o.getEstado() == estado) {
                resultado.add(o);
            }
        }
        return resultado;
    }

    public List<OrdenTrabajo> listarPendientesDeFacturar() {
        List<OrdenTrabajo> resultado = new ArrayList<>();
        for (OrdenTrabajo o : ctx.getOrdenes().listar()) {
            if (o.getEstado() == EstadoOrden.FINALIZADA && !o.isFacturada()) {
                resultado.add(o);
            }
        }
        return resultado;
    }
}
