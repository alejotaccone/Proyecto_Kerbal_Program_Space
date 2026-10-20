package gui;

import controller.CrisisController;
import engine.SimulationEngine;
import java.awt.Component;
import java.util.function.Consumer;
import model.spacecraft.SpaceStation;

/**
 * ============================================================================
 * Fachada / Adaptador de compatibilidad para CrisisDialogHandler.
 * En la arquitectura MVC (Iteración 4), la responsabilidad visual se encuentra
 * en {@link gui.CrisisDialogView} y la lógica de control en {@link controller.CrisisController}.
 * Esta clase se preserva para garantizar compatibilidad hacia atrás delegando
 * directamente en el controlador.
 * ============================================================================
 */
public class CrisisDialogHandler {

    public static boolean evaluarEventosCrisis(
            Component parent, 
            SimulationEngine engine, 
            Runnable onPausarSimulacion, 
            Runnable onReanudarSimulacion, 
            Consumer<String> logConsola) {
        return CrisisController.evaluarEventosCrisis(parent, engine, onPausarSimulacion, onReanudarSimulacion, logConsola, null);
    }

    public static boolean evaluarEventosCrisis(
            Component parent, 
            SimulationEngine engine, 
            Runnable onPausarSimulacion, 
            Runnable onReanudarSimulacion, 
            Consumer<String> logConsola,
            Consumer<SpaceStation> onEstacionDestruida) {
        return CrisisController.evaluarEventosCrisis(parent, engine, onPausarSimulacion, onReanudarSimulacion, logConsola, onEstacionDestruida);
    }
}
