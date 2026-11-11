package excepciones;

/** Error de validación de datos ingresados (campos vacíos, valores negativos, etc.). */
public class ValidacionException extends Exception {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
