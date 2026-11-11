package vista;

import java.util.Locale;

public final class Formato {

    private Formato() { }

    /** Para mostrar en tablas y etiquetas (con separador de miles). */
    public static String dinero(double valor) {
        return String.format("%,.2f", valor);
    }

    /** Para volver a escribir el valor en un campo de texto editable. */
    public static String campo(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }
}
