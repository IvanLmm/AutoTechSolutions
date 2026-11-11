package controlador;

import excepciones.PagoInsuficienteException;
import excepciones.RegistroDuplicadoException;
import excepciones.ValidacionException;
import java.time.LocalDate;
import java.util.List;
import modelo.EstadoOrden;
import modelo.Factura;
import modelo.OrdenTrabajo;

public class FacturaController {

    private final ContextoTaller ctx;

    public FacturaController(ContextoTaller ctx) {
        this.ctx = ctx;
    }

    public Factura generar(String numeroOrden) throws ValidacionException {
        String clave = Validador.requerido("Número de orden", numeroOrden);
        OrdenTrabajo orden = ctx.getOrdenes().buscar(clave);
        if (orden == null) {
            throw new ValidacionException("No existe la orden " + clave + ".");
        }
        if (orden.getEstado() != EstadoOrden.FINALIZADA) {
            throw new ValidacionException("Solo se pueden facturar órdenes Finalizadas "
                    + "(estado actual: " + orden.getEstado() + ").");
        }
        if (orden.isFacturada()) {
            throw new ValidacionException("La orden " + clave + " ya fue facturada.");
        }

        Factura factura = new Factura(ctx.siguienteNumeroFactura(), LocalDate.now(),
                orden, ctx.getTasaImpuesto());
        try {
            ctx.getFacturas().agregar(factura);
        } catch (RegistroDuplicadoException e) {
            throw new IllegalStateException(e); // no debería ocurrir: el número es automático
        }
        orden.setFacturada(true);
        ctx.getGestorDeshacer().registrar("Generación de la factura " + factura.getNumero(), () -> {
            ctx.getFacturas().deshacer();
            orden.setFacturada(false);
            ctx.retrocederNumeroFactura();
        });
        return factura;
    }

   
    public double pagar(String numeroFactura, String efectivoTexto)
            throws ValidacionException, PagoInsuficienteException {
        Factura factura = buscar(numeroFactura);
        if (factura.isPagada()) {
            throw new ValidacionException("La factura " + factura.getNumero() + " ya fue pagada.");
        }
        double efectivo = Validador.decimal("Efectivo recibido", efectivoTexto);
        if (efectivo < 0) {
            throw new ValidacionException("El campo 'Efectivo recibido' no puede ser negativo.");
        }
        double cambio = factura.pagarEnEfectivo(efectivo); // lanza PagoInsuficienteException
        ctx.getGestorDeshacer().registrar("Pago de la factura " + factura.getNumero(),
                factura::anularPago);
        return cambio;
    }

    public void configurarImpuesto(String porcentajeTexto) throws ValidacionException {
        double porcentaje = Validador.decimal("Impuesto (%)", porcentajeTexto);
        if (porcentaje < 0 || porcentaje > 100) {
            throw new ValidacionException("El impuesto debe estar entre 0 y 100 %.");
        }
        ctx.setTasaImpuesto(porcentaje / 100.0);
    }

    public double getImpuestoPorcentaje() {
        return ctx.getTasaImpuesto() * 100.0;
    }

    public Factura buscar(String numeroFactura) throws ValidacionException {
        String clave = Validador.requerido("Número de factura", numeroFactura);
        Factura f = ctx.getFacturas().buscar(clave);
        if (f == null) {
            throw new ValidacionException("No existe la factura " + clave + ".");
        }
        return f;
    }

    public List<Factura> listar() {
        return ctx.getFacturas().listar();
    }
}
