package controlador;

import excepciones.ValidacionException;

/** Validaciones y conversiones reutilizables para los datos que escribe el usuario. */
public final class Validador {

    private Validador() { }

    public static String requerido(String campo, String valor) throws ValidacionException {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ValidacionException("El campo '" + campo + "' es obligatorio.");
        }
        return valor.trim();
    }

    public static int entero(String campo, String texto) throws ValidacionException {
        String t = requerido(campo, texto);
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo '" + campo + "' debe ser un número entero.");
        }
    }

    public static int enteroNoNegativo(String campo, String texto) throws ValidacionException {
        int v = entero(campo, texto);
        if (v < 0) {
            throw new ValidacionException("El campo '" + campo + "' no puede ser negativo.");
        }
        return v;
    }

    public static int enteroPositivo(String campo, String texto) throws ValidacionException {
        int v = enteroNoNegativo(campo, texto);
        if (v == 0) {
            throw new ValidacionException("El campo '" + campo + "' debe ser mayor que cero.");
        }
        return v;
    }

    public static double decimal(String campo, String texto) throws ValidacionException {
        String t = requerido(campo, texto).replace(',', '.');
        try {
            double v = Double.parseDouble(t);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException e) {
            throw new ValidacionException("El campo '" + campo + "' debe ser un número válido.");
        }
    }

    public static double decimalPositivo(String campo, String texto) throws ValidacionException {
        double v = decimal(campo, texto);
        if (v < 0) {
            throw new ValidacionException("El campo '" + campo + "' no puede ser negativo.");
        }
        if (v == 0) {
            throw new ValidacionException("El campo '" + campo + "' debe ser mayor que cero.");
        }
        return v;
    }
}
