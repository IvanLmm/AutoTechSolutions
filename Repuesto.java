package modelo;

import excepciones.StockInsuficienteException;

public class Repuesto {
    private final String codigo;
    private String nombre;
    private int cantidad;
    private double precioUnitario;
    private int stockMinimo;

    public Repuesto(String codigo, String nombre, int cantidad, double precioUnitario, int stockMinimo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.stockMinimo = stockMinimo;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public int getCantidad() { return cantidad; }
    public double getPrecioUnitario() { return precioUnitario; }
    public int getStockMinimo() { return stockMinimo; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    /** Entrada de inventario. Rechaza cantidades no positivas. */
    public void agregarStock(int unidades) {
        if (unidades <= 0) {
            throw new IllegalArgumentException("La cantidad a ingresar debe ser mayor que cero.");
        }
        cantidad += unidades;
    }

    /** Descuento de inventario. Lanza excepción si no alcanza. */
    public void descontarStock(int unidades) throws StockInsuficienteException {
        if (unidades <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor que cero.");
        }
        if (unidades > cantidad) {
            throw new StockInsuficienteException(nombre, cantidad, unidades);
        }
        cantidad -= unidades;
    }

    public boolean tieneStockBajo() {
        return cantidad <= stockMinimo;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
