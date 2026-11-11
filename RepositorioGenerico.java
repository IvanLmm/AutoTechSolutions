package repositorio;

import excepciones.RegistroDuplicadoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

public class RepositorioGenerico<T> {

    private enum Tipo { AGREGAR, ELIMINAR, MODIFICAR }

    // Guarda lo necesario para revertir cada operación
    private record Operacion<E>(Tipo tipo, E anterior, E actual, int posicion) { }

    private final List<T> lista = new ArrayList<>();             // listados
    private final Map<String, T> indice = new HashMap<>();       // búsquedas por clave
    private final Stack<Operacion<T>> historial = new Stack<>(); // deshacer
    private final Function<T, String> extractorClave;

    public RepositorioGenerico(Function<T, String> extractorClave) {
        this.extractorClave = extractorClave;
    }

    public void agregar(T elemento) throws RegistroDuplicadoException {
        String clave = extractorClave.apply(elemento);
        if (indice.containsKey(clave)) {
            throw new RegistroDuplicadoException(clave);
        }
        lista.add(elemento);
        indice.put(clave, elemento);
        historial.push(new Operacion<>(Tipo.AGREGAR, null, elemento, lista.size() - 1));
    }

    public T buscar(String clave) {
        return indice.get(clave);
    }

    public boolean existe(String clave) {
        return indice.containsKey(clave);
    }

    public boolean eliminar(String clave) {
        T elemento = indice.get(clave);
        if (elemento == null) {
            return false;
        }
        int pos = lista.indexOf(elemento);
        lista.remove(pos);
        indice.remove(clave);
        historial.push(new Operacion<>(Tipo.ELIMINAR, elemento, null, pos));
        return true;
    }

    /** Reemplaza el registro. La clave no puede cambiar. */
    public boolean modificar(String clave, T nuevo) {
        T anterior = indice.get(clave);
        if (anterior == null) {
            return false;
        }
        if (!clave.equals(extractorClave.apply(nuevo))) {
            throw new IllegalArgumentException("La clave de un registro no se puede modificar.");
        }
        int pos = lista.indexOf(anterior);
        lista.set(pos, nuevo);
        indice.put(clave, nuevo);
        historial.push(new Operacion<>(Tipo.MODIFICAR, anterior, nuevo, pos));
        return true;
    }

    /** Revierte la última operación dejando lista e índice consistentes. */
    public boolean deshacer() {
        if (historial.isEmpty()) {
            return false;
        }
        Operacion<T> op = historial.pop();
        switch (op.tipo()) {
            case AGREGAR -> {
                lista.remove(op.actual());
                indice.remove(extractorClave.apply(op.actual()));
            }
            case ELIMINAR -> {
                lista.add(op.posicion(), op.anterior());
                indice.put(extractorClave.apply(op.anterior()), op.anterior());
            }
            case MODIFICAR -> {
                lista.set(op.posicion(), op.anterior());
                indice.put(extractorClave.apply(op.anterior()), op.anterior());
            }
        }
        return true;
    }

    public List<T> listar() {
        return new ArrayList<>(lista); // copia, protege el encapsulamiento
    }

    public int tamanio() {
        return lista.size();
    }

    public boolean hayCambiosParaDeshacer() {
        return !historial.isEmpty();
    }
}
