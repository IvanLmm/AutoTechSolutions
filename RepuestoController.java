package controlador;

import excepciones.RegistroDuplicadoException;
import excepciones.StockInsuficienteException;
import excepciones.ValidacionException;
import java.util.ArrayList;
import java.util.List;
import modelo.DetalleRepuesto;
import modelo.OrdenTrabajo;
import modelo.Repuesto;

public class RepuestoController {

    private final ContextoTaller ctx;

    public RepuestoController(ContextoTaller ctx) {
        this.ctx = ctx;
    }

    public void registrar(String codigo, String nombre, String cantidadTexto,
                          String precioTexto, String stockMinimoTexto)
            throws ValidacionException, RegistroDuplicadoException {
        Repuesto r = new Repuesto(
                Validador.requerido("Código", codigo),
                Validador.requerido("Nombre", nombre),
                Validador.enteroNoNegativo("Cantidad", cantidadTexto),
                Validador.decimalPositivo("Precio unitario", precioTexto),
                Validador.enteroNoNegativo("Stock mínimo", stockMinimoTexto));
        ctx.getRepuestos().agregar(r);
        ctx.getGestorDeshacer().registrar("Registro del repuesto " + r.getCodigo(),
                () -> ctx.getRepuestos().deshacer());
    }

    /** Entrada de inventario (compra o reposición). */
    public void registrarEntrada(String codigo, String cantidadTexto) throws ValidacionException {
        int unidades = Validador.enteroPositivo("Cantidad", cantidadTexto);
        Repuesto r = buscar(codigo);
        r.agregarStock(unidades);
        ctx.getGestorDeshacer().registrar(
                "Entrada de " + unidades + " unidad(es) de " + r.getNombre(), () -> {
                    try {
                        r.descontarStock(unidades);
                    } catch (StockInsuficienteException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    /** Modifica en sitio nombre, precio y stock mínimo (la cantidad solo cambia con entradas/órdenes). */
    public void modificar(String codigo, String nombre, String precioTexto, String stockMinimoTexto)
            throws ValidacionException {
        String nuevoNombre = Validador.requerido("Nombre", nombre);
        double nuevoPrecio = Validador.decimalPositivo("Precio unitario", precioTexto);
        int nuevoMinimo = Validador.enteroNoNegativo("Stock mínimo", stockMinimoTexto);
        Repuesto r = buscar(codigo);

        String nombreAnterior = r.getNombre();
        double precioAnterior = r.getPrecioUnitario();
        int minimoAnterior = r.getStockMinimo();

        r.setNombre(nuevoNombre);
        r.setPrecioUnitario(nuevoPrecio);
        r.setStockMinimo(nuevoMinimo);

        ctx.getGestorDeshacer().registrar("Modificación del repuesto " + r.getCodigo(), () -> {
            r.setNombre(nombreAnterior);
            r.setPrecioUnitario(precioAnterior);
            r.setStockMinimo(minimoAnterior);
        });
    }

    public void eliminar(String codigo) throws ValidacionException {
        Repuesto r = buscar(codigo);
        for (OrdenTrabajo o : ctx.getOrdenes().listar()) {
            for (DetalleRepuesto d : o.getRepuestos()) {
                if (d.repuesto() == r) {
                    throw new ValidacionException("No se puede eliminar el repuesto "
                            + r.getCodigo() + ": está usado en la orden " + o.getNumero() + ".");
                }
            }
        }
        ctx.getRepuestos().eliminar(r.getCodigo());
        ctx.getGestorDeshacer().registrar("Eliminación del repuesto " + r.getCodigo(),
                () -> ctx.getRepuestos().deshacer());
    }

    public Repuesto buscar(String codigo) throws ValidacionException {
        String clave = Validador.requerido("Código", codigo);
        Repuesto r = ctx.getRepuestos().buscar(clave);
        if (r == null) {
            throw new ValidacionException("No existe un repuesto con el código " + clave + ".");
        }
        return r;
    }

    public List<Repuesto> listar() {
        return ctx.getRepuestos().listar();
    }

    /** Repuestos con existencias iguales o por debajo del mínimo. */
    public List<Repuesto> listarAlertas() {
        List<Repuesto> alertas = new ArrayList<>();
        for (Repuesto r : ctx.getRepuestos().listar()) {
            if (r.tieneStockBajo()) {
                alertas.add(r);
            }
        }
        return alertas;
    }
}
