package controlador;

import excepciones.ValidacionException;

public class DeshacerController {

    private final ContextoTaller ctx;

    public DeshacerController(ContextoTaller ctx) {
        this.ctx = ctx;
    }

    /** Revierte la última operación y devuelve su descripción. */
    public String deshacer() throws ValidacionException {
        if (ctx.hayProcesosActivos()) {
            throw new ValidacionException("No se puede deshacer mientras hay órdenes procesándose.");
        }
        String descripcion = ctx.getGestorDeshacer().deshacer();
        if (descripcion == null) {
            throw new ValidacionException("No hay operaciones para deshacer.");
        }
        return descripcion;
    }

    public boolean hayAcciones() {
        return ctx.getGestorDeshacer().hayAcciones();
    }

    public String descripcionProxima() {
        return ctx.getGestorDeshacer().descripcionProxima();
    }
}
