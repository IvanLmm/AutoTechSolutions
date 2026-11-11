package modelo;

public class Servicio {
    private final String codigo;
    private String descripcion;
    private double precioBase;
    private int tiempoEstimadoMinutos;

    public Servicio(String codigo, String descripcion, double precioBase, int tiempoEstimadoMinutos) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.precioBase = precioBase;
        this.tiempoEstimadoMinutos = tiempoEstimadoMinutos;
    }

    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public double getPrecioBase() { return precioBase; }
    public int getTiempoEstimadoMinutos() { return tiempoEstimadoMinutos; }

    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }
    public void setTiempoEstimadoMinutos(int t) { this.tiempoEstimadoMinutos = t; }

    @Override
    public String toString() {
        return codigo + " - " + descripcion;
    }
}
