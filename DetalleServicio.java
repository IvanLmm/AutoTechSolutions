package modelo;

/** Línea de servicio de una orden. Guarda el precio vigente al crear la orden. */
public record DetalleServicio(Servicio servicio, double precio) {

    public DetalleServicio {
        if (servicio == null || precio <= 0) {
            throw new IllegalArgumentException("Servicio y precio positivo son obligatorios.");
        }
    }
}
