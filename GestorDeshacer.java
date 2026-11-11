package controlador;

import java.util.Stack;

/**
 * Pila global de operaciones deshacibles. Cada acción guarda cómo revertirse,
 * de modo que al deshacer se restauran TODAS las estructuras afectadas
 * (lista, mapa, stock, estados...), no solo una.
 */
public class GestorDeshacer {

    private record Accion(String descripcion, Runnable reversion) { }

    private final Stack<Accion> pila = new Stack<>();

    public void registrar(String descripcion, Runnable reversion) {
        pila.push(new Accion(descripcion, reversion));
    }

    public boolean hayAcciones() {
        return !pila.isEmpty();
    }

    public String descripcionProxima() {
        return pila.isEmpty() ? null : pila.peek().descripcion();
    }

    /** Revierte la última acción y devuelve su descripción (null si no había). */
    public String deshacer() {
        if (pila.isEmpty()) {
            return null;
        }
        Accion accion = pila.pop();
        accion.reversion().run();
        return accion.descripcion();
    }
}
