package gui;

import engine.SimulationEngine;
import java.awt.Component;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import model.spacecraft.OrbitalObject;
import model.spacecraft.RogueDebris;
import model.spacecraft.Spacecraft;

/**
 * Controlador de diálogos interactivos y lógica de decisión ante eventos de crisis y colisiones.
 * Desacopla la lógica de juego y manejo de alertas de impacto fuera de MainGUI (God Class).
 */
public class CrisisDialogHandler {

    /**
     * Revisa si hay una amenaza activa (RogueDebris) cerca de su objetivo y lanza el diálogo interactivo.
     * Retorna true si se procesó una crisis (para actualizar la vista).
     */
    public static boolean evaluarEventosCrisis(
            Component parent, 
            SimulationEngine engine, 
            Runnable onPausarSimulacion, 
            Runnable onReanudarSimulacion, 
            Consumer<String> logConsola) {

        if (engine == null || engine.getTrackedObjects() == null) return false;

        RogueDebris threat = buscarAmenazaRogue(engine);
        if (threat == null || threat.getTarget() == null) return false;

        OrbitalObject target = threat.getTarget();
        double dist = threat.getPosition().distanceTo(target.getPosition());

        // Si está a menos de 100km, se activa la crisis interactiva
        if (dist < 100.0) {
            if (onPausarSimulacion != null) onPausarSimulacion.run();

            int seleccion = mostrarDialogoCrisis(parent, threat, target, dist);
            procesarDecisionCrisis(seleccion, target, engine, logConsola);
            finalizarCrisis(threat, engine, onReanudarSimulacion);
            return true;
        }

        return false;
    }

    private static RogueDebris buscarAmenazaRogue(SimulationEngine engine) {
        for (OrbitalObject nave : engine.getTrackedObjects()) {
            if (nave instanceof RogueDebris) {
                return (RogueDebris) nave;
            }
        }
        return null;
    }

    private static int mostrarDialogoCrisis(Component parent, RogueDebris threat, OrbitalObject target, double dist) {
        String mensaje = "¡ALERTA DE IMPACTO INMINENTE!\n\n"
                + "La Basura Espacial Hostil [" + threat.getName() + "]\n"
                + "se encuentra a " + String.format("%.1f", dist) + " km de la nave [" + target.getName() + "].\n\n"
                + "¿Qué orden de emergencia desea ejecutar, Comandante?";

        Object[] opciones = {
            "Forzar Evasión (-15L Combustible)",
            "Ignorar (Aceptar Impacto Crítico)"
        };

        return JOptionPane.showOptionDialog(
                parent,
                mensaje,
                "CRISIS DE COLISIÓN DETECTADA",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                opciones,
                opciones[0]);
    }

    private static void procesarDecisionCrisis(
            int seleccion, 
            OrbitalObject target, 
            SimulationEngine engine, 
            Consumer<String> logConsola) {

        if (seleccion == 0) { // Evasión
            boolean success = false;
            if (target instanceof Spacecraft) {
                success = ((Spacecraft) target).evade(0.5, 0.5);
            }
            if (success) {
                if (logConsola != null) logConsola.accept("[CRISIS]: ¡Evasión exitosa! [" + target.getName() + "] maniobró a tiempo.");
            } else {
                if (logConsola != null) logConsola.accept("[CRISIS FATAL]: [" + target.getName() + "] no pudo evadir el impacto (sin propulsión/combustible). OBJETO DESTRUIDO.");
                engine.removeShip(target);
            }
        } else { // Ignorar
            if (logConsola != null) logConsola.accept("[CRISIS FATAL]: [" + target.getName() + "] ha recibido un impacto directo. OBJETO DESTRUIDO.");
            engine.removeShip(target);
        }
    }

    private static void finalizarCrisis(
            RogueDebris threat, 
            SimulationEngine engine, 
            Runnable onReanudarSimulacion) {

        // La amenaza desaparece tras el encuentro
        engine.removeShip(threat);

        if (onReanudarSimulacion != null) {
            onReanudarSimulacion.run();
        }
    }
}
