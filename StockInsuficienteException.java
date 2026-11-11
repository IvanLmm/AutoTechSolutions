package excepciones;

public class StockInsuficienteException extends Exception {
    public StockInsuficienteException(String repuesto, int disponible, int solicitado) {
        super("Stock insuficiente de '" + repuesto + "'. Disponible: "
                + disponible + ", solicitado: " + solicitado + ".");
    }
}
