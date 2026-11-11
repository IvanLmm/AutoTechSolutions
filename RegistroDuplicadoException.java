package excepciones;

public class RegistroDuplicadoException extends Exception {
    public RegistroDuplicadoException(String clave) {
        super("Ya existe un registro con la clave: " + clave);
    }
}
