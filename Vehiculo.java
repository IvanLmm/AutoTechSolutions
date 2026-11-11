package modelo;

public class Vehiculo {
    private final String placa;
    private String marca;
    private String modelo;
    private int anio;
    private String color;
    private Cliente propietario;

    public Vehiculo(String placa, String marca, String modelo, int anio,
                    String color, Cliente propietario) {
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.anio = anio;
        this.color = color;
        this.propietario = propietario;
    }

    public String getPlaca() { return placa; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public int getAnio() { return anio; }
    public String getColor() { return color; }
    public Cliente getPropietario() { return propietario; }

    public void setMarca(String marca) { this.marca = marca; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setAnio(int anio) { this.anio = anio; }
    public void setColor(String color) { this.color = color; }
    public void setPropietario(Cliente propietario) { this.propietario = propietario; }

    @Override
    public String toString() {
        return placa + " - " + marca + " " + modelo + " (" + anio + ")";
    }
}
