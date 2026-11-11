package modelo;

public enum EstadoOrden {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoOrden(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
