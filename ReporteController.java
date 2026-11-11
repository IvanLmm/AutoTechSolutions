package controlador;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import modelo.DetalleServicio;
import modelo.EstadoOrden;
import modelo.OrdenTrabajo;
import modelo.Servicio;

public class ReporteController {

    private final ContextoTaller ctx;

    public ReporteController(ContextoTaller ctx) {
        this.ctx = ctx;
    }

    public Map<String, Double> ingresosPorServicio() {
        Map<String, Double> ingresos = new LinkedHashMap<>();
        for (Servicio s : ctx.getServicios().listar()) {
            ingresos.put(s.getDescripcion(), 0.0);
        }
        for (OrdenTrabajo o : ctx.getOrdenes().listar()) {
            if (o.getEstado() == EstadoOrden.FINALIZADA) {
                for (DetalleServicio d : o.getServicios()) {
                    ingresos.merge(d.servicio().getDescripcion(), d.precio(), Double::sum);
                }
            }
        }
        return ingresos;
    }

    public Map<EstadoOrden, Integer> ordenesPorEstado() {
        Map<EstadoOrden, Integer> conteo = new EnumMap<>(EstadoOrden.class);
        for (EstadoOrden e : EstadoOrden.values()) {
            conteo.put(e, 0);
        }
        for (OrdenTrabajo o : ctx.getOrdenes().listar()) {
            conteo.merge(o.getEstado(), 1, Integer::sum);
        }
        return conteo;
    }
}
