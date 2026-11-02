package controller;

import java.util.ArrayDeque;
import java.util.Queue;
import javax.swing.SwingUtilities;

/**
 * Gestor centralizado de eventos y alertas modales (FIFO Queue).
 * Garantiza que nunca se abran dos ventanas modales de forma simultánea,
 * manteniendo pausada la simulación hasta que la cola se vacíe completamente.
 */
public class AlertQueue {

    private final Queue<Runnable> colaAlertas;
    private final Runnable onPausarSimulacion;
    private final Runnable onReanudarSimulacion;
    private boolean modalEnPantalla;

    public AlertQueue(Runnable onPausarSimulacion, Runnable onReanudarSimulacion) {
        this.colaAlertas = new ArrayDeque<>();
        this.onPausarSimulacion = onPausarSimulacion;
        this.onReanudarSimulacion = onReanudarSimulacion;
        this.modalEnPantalla = false;
    }

    /**
     * Encola una tarea modal para su presentación ordenada.
     * Si no hay ningún diálogo activo, pausa los timers e inicia la presentación de inmediato.
     */
    public synchronized void encolarAlerta(Runnable tareaModal) {
        if (tareaModal == null) return;

        colaAlertas.offer(tareaModal);
        procesarSiguienteSiCorresponde();
    }

    /**
     * Encola una alerta prioritaria (ej. peligro inminente de colisión) al frente de la cola.
     */
    public synchronized void encolarAlertaPrioritaria(Runnable tareaModal) {
        if (tareaModal == null) return;

        if (colaAlertas instanceof ArrayDeque) {
            ((ArrayDeque<Runnable>) colaAlertas).addFirst(tareaModal);
        } else {
            colaAlertas.offer(tareaModal);
        }
        procesarSiguienteSiCorresponde();
    }

    private synchronized void procesarSiguienteSiCorresponde() {
        if (modalEnPantalla || colaAlertas.isEmpty()) return;

        modalEnPantalla = true;

        // Pausar simulación si aún no lo estaba
        if (onPausarSimulacion != null) {
            SwingUtilities.invokeLater(onPausarSimulacion);
        }

        Runnable siguiente = colaAlertas.poll();
        if (siguiente != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    siguiente.run();
                } finally {
                    cerrarModalYContinuar();
                }
            });
        }
    }

    private synchronized void cerrarModalYContinuar() {
        modalEnPantalla = false;

        if (!colaAlertas.isEmpty()) {
            procesarSiguienteSiCorresponde();
        } else {
            // Cola vacía: reanudar la simulación
            if (onReanudarSimulacion != null) {
                SwingUtilities.invokeLater(onReanudarSimulacion);
            }
        }
    }

    public synchronized boolean hayModalActivo() {
        return modalEnPantalla || !colaAlertas.isEmpty();
    }

    public synchronized void limpiarCola() {
        colaAlertas.clear();
        modalEnPantalla = false;
    }
}
