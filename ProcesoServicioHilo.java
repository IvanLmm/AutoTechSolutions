package hilos;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.SwingWorker;

/**
 * Simula el procesamiento de una orden en un hilo de fondo (SwingWorker).
 *
 * - doInBackground() corre FUERA del EDT: solo espera y avisa el avance; no toca
 *   ningún dato del sistema ni componentes Swing, así la interfaz no se bloquea.
 * - process() y done() corren EN el EDT: ahí se actualiza la barra de progreso
 *   y se ejecuta la finalización de la orden (descuento de stock incluido).
 */
public class ProcesoServicioHilo extends SwingWorker<Void, Integer> {

    /** Duración de la simulación (el enunciado exige al menos 5 segundos). */
    public static final int DURACION_SEGUNDOS = 6;
    private static final int PASOS = 100;

    private final String numeroOrden;
    private final IntConsumer alProgresar;
    private final Runnable alTerminar;
    private final Consumer<String> alFallar;

    public ProcesoServicioHilo(String numeroOrden, IntConsumer alProgresar,
                               Runnable alTerminar, Consumer<String> alFallar) {
        this.numeroOrden = numeroOrden;
        this.alProgresar = alProgresar;
        this.alTerminar = alTerminar;
        this.alFallar = alFallar;
    }

    public String getNumeroOrden() {
        return numeroOrden;
    }

    @Override
    protected Void doInBackground() throws Exception {
        long pausaMs = DURACION_SEGUNDOS * 1000L / PASOS;
        for (int paso = 1; paso <= PASOS; paso++) {
            Thread.sleep(pausaMs);
            publish(paso);
        }
        return null;
    }

    @Override
    protected void process(List<Integer> avances) {
        alProgresar.accept(avances.get(avances.size() - 1)); // el más reciente
    }

    @Override
    protected void done() {
        try {
            get(); // lanza excepción si doInBackground falló
            alTerminar.run();
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            alFallar.accept(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }
}
