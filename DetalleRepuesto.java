package modelo;

/** Línea de repuesto de una orden. Guarda el precio vigente al crear la orden. */
public record DetalleRepuesto(Repuesto repuesto, int cantidad, double precioUnitario) {

    public DetalleRepuesto {
        if (repuesto == null || cantidad <= 0 || precioUnitario <= 0) {
            throw new IllegalArgumentException("Repuesto, cantidad y precio positivos son obligatorios.");
        }
    }

    public double subtotal() {
        return precioUnitario * cantidad;
    }
}
