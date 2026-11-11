package excepciones;

public class PagoInsuficienteException extends Exception {
    public PagoInsuficienteException(double total, double pagado) {
        super(String.format("Pago insuficiente. Total: %.2f, recibido: %.2f, faltan: %.2f",
                total, pagado, total - pagado));
    }
}
