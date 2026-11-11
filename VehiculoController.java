package controlador;

import excepciones.ClienteNoEncontradoException;
import excepciones.RegistroDuplicadoException;
import excepciones.ValidacionException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import modelo.Cliente;
import modelo.OrdenTrabajo;
import modelo.Vehiculo;

public class VehiculoController {

    private final ContextoTaller ctx;

    public VehiculoController(ContextoTaller ctx) {
        this.ctx = ctx;
    }

    public void registrar(String placa, String marca, String modelo, String anioTexto,
                          String color, String codigoCliente)
            throws ValidacionException, RegistroDuplicadoException, ClienteNoEncontradoException {
        String placaLimpia = Validador.requerido("Placa", placa).toUpperCase();
        String marcaLimpia = Validador.requerido("Marca", marca);
        String modeloLimpio = Validador.requerido("Modelo", modelo);
        int anio = Validador.enteroPositivo("Año", anioTexto);
        int anioMaximo = Year.now().getValue() + 1;
        if (anio < 1900 || anio > anioMaximo) {
            throw new ValidacionException("El año debe estar entre 1900 y " + anioMaximo + ".");
        }
        String colorLimpio = Validador.requerido("Color", color);
        String codigo = Validador.requerido("Propietario", codigoCliente);

        Cliente propietario = ctx.getClientes().buscar(codigo);
        if (propietario == null) {
            throw new ClienteNoEncontradoException(codigo);
        }

        Vehiculo vehiculo = new Vehiculo(placaLimpia, marcaLimpia, modeloLimpio, anio, colorLimpio, propietario);
        ctx.getVehiculos().agregar(vehiculo);
        ctx.getGestorDeshacer().registrar("Registro del vehículo " + placaLimpia,
                () -> ctx.getVehiculos().deshacer());
    }

    public void eliminar(String placa) throws ValidacionException {
        Vehiculo v = buscarPorPlaca(placa);
        for (OrdenTrabajo o : ctx.getOrdenes().listar()) {
            if (o.getVehiculo() == v) {
                throw new ValidacionException("No se puede eliminar el vehículo "
                        + v.getPlaca() + ": tiene órdenes de trabajo.");
            }
        }
        ctx.getVehiculos().eliminar(v.getPlaca());
        ctx.getGestorDeshacer().registrar("Eliminación del vehículo " + v.getPlaca(),
                () -> ctx.getVehiculos().deshacer());
    }

    public Vehiculo buscarPorPlaca(String placa) throws ValidacionException {
        String clave = Validador.requerido("Placa", placa).toUpperCase();
        Vehiculo v = ctx.getVehiculos().buscar(clave);
        if (v == null) {
            throw new ValidacionException("No existe un vehículo con la placa " + clave + ".");
        }
        return v;
    }

    public List<Vehiculo> listarPorCliente(String codigoCliente) throws ClienteNoEncontradoException {
        String codigo = codigoCliente == null ? "" : codigoCliente.trim();
        Cliente cliente = ctx.getClientes().buscar(codigo);
        if (cliente == null) {
            throw new ClienteNoEncontradoException(codigo);
        }
        List<Vehiculo> resultado = new ArrayList<>();
        for (Vehiculo v : ctx.getVehiculos().listar()) {
            if (v.getPropietario() == cliente) {
                resultado.add(v);
            }
        }
        return resultado;
    }

    public List<Vehiculo> listar() {
        return ctx.getVehiculos().listar();
    }
}
