package controlador;

import excepciones.RegistroDuplicadoException;
import excepciones.ValidacionException;
import java.util.List;
import modelo.DetalleServicio;
import modelo.OrdenTrabajo;
import modelo.Servicio;

public class ServicioController {

    private final ContextoTaller ctx;

    public ServicioController(ContextoTaller ctx) {
        this.ctx = ctx;
    }

    /**
     * Catálogo mínimo exigido por el enunciado. Los precios y tiempos son
     * valores base que se pueden modificar desde la pantalla de Servicios.
     * No se registra en el historial de deshacer (es la carga inicial).
     */
    public void cargarCatalogoInicial() {
        sembrar("SRV-001", "Cambio de aceite", 350.0, 30);
        sembrar("SRV-002", "Revisión de frenos", 500.0, 45);
        sembrar("SRV-003", "Diagnóstico electrónico", 800.0, 60);
        sembrar("SRV-004", "Alineamiento", 450.0, 60);
        sembrar("SRV-005", "Mantenimiento general", 1500.0, 120);
    }

    private void sembrar(String codigo, String descripcion, double precio, int minutos) {
        try {
            ctx.getServicios().agregar(new Servicio(codigo, descripcion, precio, minutos));
        } catch (RegistroDuplicadoException e) {
            // ya estaba cargado: no hacer nada
        }
    }

    public void registrar(String codigo, String descripcion, String precioTexto, String tiempoTexto)
            throws ValidacionException, RegistroDuplicadoException {
        Servicio s = new Servicio(
                Validador.requerido("Código", codigo),
                Validador.requerido("Descripción", descripcion),
                Validador.decimalPositivo("Precio base", precioTexto),
                Validador.enteroPositivo("Tiempo estimado (min)", tiempoTexto));
        ctx.getServicios().agregar(s);
        ctx.getGestorDeshacer().registrar("Registro del servicio " + s.getCodigo(),
                () -> ctx.getServicios().deshacer());
    }

    /** Modifica en sitio. Las órdenes ya creadas conservan el precio con que se crearon. */
    public void modificar(String codigo, String descripcion, String precioTexto, String tiempoTexto)
            throws ValidacionException {
        String nuevaDescripcion = Validador.requerido("Descripción", descripcion);
        double nuevoPrecio = Validador.decimalPositivo("Precio base", precioTexto);
        int nuevoTiempo = Validador.enteroPositivo("Tiempo estimado (min)", tiempoTexto);
        Servicio s = buscar(codigo);

        String descripcionAnterior = s.getDescripcion();
        double precioAnterior = s.getPrecioBase();
        int tiempoAnterior = s.getTiempoEstimadoMinutos();

        s.setDescripcion(nuevaDescripcion);
        s.setPrecioBase(nuevoPrecio);
        s.setTiempoEstimadoMinutos(nuevoTiempo);

        ctx.getGestorDeshacer().registrar("Modificación del servicio " + s.getCodigo(), () -> {
            s.setDescripcion(descripcionAnterior);
            s.setPrecioBase(precioAnterior);
            s.setTiempoEstimadoMinutos(tiempoAnterior);
        });
    }

    public void eliminar(String codigo) throws ValidacionException {
        Servicio s = buscar(codigo);
        for (OrdenTrabajo o : ctx.getOrdenes().listar()) {
            for (DetalleServicio d : o.getServicios()) {
                if (d.servicio() == s) {
                    throw new ValidacionException("No se puede eliminar el servicio "
                            + s.getCodigo() + ": está usado en la orden " + o.getNumero() + ".");
                }
            }
        }
        ctx.getServicios().eliminar(s.getCodigo());
        ctx.getGestorDeshacer().registrar("Eliminación del servicio " + s.getCodigo(),
                () -> ctx.getServicios().deshacer());
    }

    public Servicio buscar(String codigo) throws ValidacionException {
        String clave = Validador.requerido("Código", codigo);
        Servicio s = ctx.getServicios().buscar(clave);
        if (s == null) {
            throw new ValidacionException("No existe un servicio con el código " + clave + ".");
        }
        return s;
    }

    public List<Servicio> listar() {
        return ctx.getServicios().listar();
    }
}
