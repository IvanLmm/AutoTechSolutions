package excepciones;

public class ClienteNoEncontradoException extends Exception {
    public ClienteNoEncontradoException(String codigo) {
        super("No existe un cliente con el código: " + codigo);
    }
}
