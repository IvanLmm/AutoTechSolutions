package controlador;

import excepciones.ClienteNoEncontradoException;
import excepciones.RegistroDuplicadoException;
import excepciones.ValidacionException;
import java.util.ArrayList;
import java.util.List;
import modelo.Cliente;
import modelo.Vehiculo;

public class ClienteController {

    private final ContextoTaller ctx;

    public ClienteController(ContextoTaller ctx) {
        this.ctx = ctx;
    }

    public void registrar(String codigo, String nombre, String identidad, String telefono, String direccion)
            throws ValidacionException, RegistroDuplicadoException {
        Cliente cliente = new Cliente(
                Validador.requerido("Código", codigo),
                Validador.requerido("Nombre completo", nombre),
                Validador.requerido("Identidad", identidad),
                Validador.requerido("Teléfono", telefono),
                Validador.requerido("Dirección", direccion));
        ctx.getClientes().agregar(cliente);
        ctx.getGestorDeshacer().registrar("Registro del cliente " + cliente.getCodigo(),
                () -> ctx.getClientes().deshacer());
    }

    /**
     * Modifica el cliente EN SITIO (con setters). No se usa repo.modificar porque
     * reemplazaría el objeto y los vehículos/órdenes quedarían apuntando al viejo.
     */
    public void modificar(String codigo, String nombre, String identidad, String telefono, String direccion)
            throws ValidacionException, ClienteNoEncontradoException {
        String nuevoNombre = Validador.requerido("Nombre completo", nombre);
        String nuevaIdentidad = Validador.requerido("Identidad", identidad);
        String nuevoTelefono = Validador.requerido("Teléfono", telefono);
        String nuevaDireccion = Validador.requerido("Dirección", direccion);
        Cliente c = buscar(codigo);

        String nombreAnterior = c.getNombreCompleto();
        String identidadAnterior = c.getIdentidad();
        String telefonoAnterior = c.getTelefono();
        String direccionAnterior = c.getDireccion();

        c.setNombreCompleto(nuevoNombre);
        c.setIdentidad(nuevaIdentidad);
        c.setTelefono(nuevoTelefono);
        c.setDireccion(nuevaDireccion);

        ctx.getGestorDeshacer().registrar("Modificación del cliente " + c.getCodigo(), () -> {
            c.setNombreCompleto(nombreAnterior);
            c.setIdentidad(identidadAnterior);
            c.setTelefono(telefonoAnterior);
            c.setDireccion(direccionAnterior);
        });
    }

    public void eliminar(String codigo) throws ClienteNoEncontradoException, ValidacionException {
        Cliente c = buscar(codigo);
        for (Vehiculo v : ctx.getVehiculos().listar()) {
            if (v.getPropietario() == c) {
                throw new ValidacionException("No se puede eliminar al cliente "
                        + c.getCodigo() + ": tiene vehículos registrados.");
            }
        }
        ctx.getClientes().eliminar(c.getCodigo());
        ctx.getGestorDeshacer().registrar("Eliminación del cliente " + c.getCodigo(),
                () -> ctx.getClientes().deshacer());
    }

    public Cliente buscar(String codigo) throws ClienteNoEncontradoException {
        String clave = codigo == null ? "" : codigo.trim();
        Cliente c = ctx.getClientes().buscar(clave);
        if (c == null) {
            throw new ClienteNoEncontradoException(clave);
        }
        return c;
    }

    public List<Cliente> listar() {
        return ctx.getClientes().listar();
    }

    /** Filtra por código, nombre o identidad (texto vacío = todos). */
    public List<Cliente> filtrar(String texto) {
        String t = texto == null ? "" : texto.trim().toLowerCase();
        if (t.isEmpty()) {
            return listar();
        }
        List<Cliente> resultado = new ArrayList<>();
        for (Cliente c : ctx.getClientes().listar()) {
            if (c.getCodigo().toLowerCase().contains(t)
                    || c.getNombreCompleto().toLowerCase().contains(t)
                    || c.getIdentidad().toLowerCase().contains(t)) {
                resultado.add(c);
            }
        }
        return resultado;
    }
}
