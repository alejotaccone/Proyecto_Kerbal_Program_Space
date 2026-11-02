package controller;

import engine.SimulationEngine;
import gui.CrisisDialogView;
import java.awt.Component;
import java.util.Locale;
import java.util.function.Consumer;
import model.spacecraft.EvasiveCapable;
import model.spacecraft.OrbitalObject;
import model.spacecraft.RogueDebris;
import model.spacecraft.SpaceStation;

/**
 * ============================================================================
 * CONTROLADOR: CrisisController (Capa Controller en MVC)
 * ============================================================================
 * Maneja la lógica de control ante eventos críticos de colisión orbital.
 * Desacopla la lógica de decisión del renderizado visual (CrisisDialogView).
 * 
 * Flujo MVC:
 * 1. Monitorea el Modelo (SimulationEngine) buscando amenazas inminentes.
 * 2. Si detecta peligro (< 100 km), solicita a la Vista (CrisisDialogView)
 *    mostrar el diálogo táctico y pedir la orden al usuario.
 * 3. Ejecuta la orden en el Modelo (maniobra evasiva o destrucción y remoción de nave).
 * ============================================================================
 */
public class CrisisController {

    /**
     * Evalúa si existe una anomalía cinética (RogueDebris) a corta distancia de su objetivo.
     * Si detecta una crisis, pausa la simulación, solicita decisión al usuario y aplica los efectos.
     * 
     * @return true si ocurrió y se procesó un evento de crisis; false si no hubo peligro inminente.
     */
    public static boolean evaluarEventosCrisis(
            Component parent, 
            SimulationEngine engine, 
            Runnable onPausarSimulacion, 
            Runnable onReanudarSimulacion, 
            Consumer<String> logConsola,
            Consumer<SpaceStation> onEstacionDestruida) {

        if (engine == null || engine.getTrackedObjects() == null) return false;

        RogueDebris threat = buscarAmenazaRogue(engine);
        if (threat == null || threat.getTarget() == null) return false;

        OrbitalObject target = threat.getTarget();
        double dist = threat.distanceTo(target);

        // Si está a menos de 100km, se activa la crisis interactiva
        if (dist < 100.0) {
            if (onPausarSimulacion != null) onPausarSimulacion.run();

            double consumoEvasion = (target instanceof EvasiveCapable)
                    ? ((EvasiveCapable) target).getConsumoEvasionCrisis()
                    : 200.0;

            // 1. Delegar a la Vista la presentación del diálogo y obtención de la orden
            int seleccion = CrisisDialogView.mostrarDialogoDecisionCrisis(
                    parent, 
                    threat.getName(), 
                    dist, 
                    target.getName(),
                    consumoEvasion
            );

            // 2. Procesar la decisión sobre el Modelo
            procesarDecisionCrisis(parent, seleccion, target, engine, logConsola, onEstacionDestruida);

            // 3. Finalizar la crisis y reanudar la simulación
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

    private static void procesarDecisionCrisis(
            Component parent,
            int seleccion, 
            OrbitalObject target, 
            SimulationEngine engine, 
            Consumer<String> logConsola,
            Consumer<SpaceStation> onEstacionDestruida) {

        if (seleccion == 0) { // Opción 0: Forzar evasión
            boolean success = false;
            double consumo = 200.0;
            if (target instanceof EvasiveCapable) {
                EvasiveCapable evasive = (EvasiveCapable) target;
                consumo = evasive.getConsumoEvasionCrisis();
                if (engine != null) {
                    success = engine.forzarEvasionCrisis(target);
                } else {
                    success = evasive.evade(0.8, 0.8, consumo);
                }
            }
            if (success) {
                if (logConsola != null) {
                    logConsola.accept(String.format(Locale.US,
                            "[CRISIS]: ¡Evasión crítica forzada exitosa! [%s] maniobró de emergencia (-%.0fL Combustible).",
                            target.getName(), consumo));
                }
            } else {
                if (logConsola != null) logConsola.accept("[CRISIS FATAL]: [" + target.getName() + "] no pudo evadir el impacto (sin propulsión/combustible). OBJETO DESTRUIDO.");
                manejarDestruccionObjeto(parent, target, engine, onEstacionDestruida);
            }
        } else { // Opción 1: Ignorar y aceptar impacto
            if (logConsola != null) logConsola.accept("[CRISIS FATAL]: [" + target.getName() + "] ha recibido un impacto directo. OBJETO DESTRUIDO.");
            manejarDestruccionObjeto(parent, target, engine, onEstacionDestruida);
        }
    }

    private static void manejarDestruccionObjeto(
            Component parent, 
            OrbitalObject target, 
            SimulationEngine engine, 
            Consumer<SpaceStation> onEstacionDestruida) {

        if (target instanceof SpaceStation) {
            SpaceStation station = (SpaceStation) target;

            // Desacoplar y abortar misiones de cargueros vinculados si la estación fue destruida
            if (engine != null && engine.getTrackedObjects() != null) {
                for (OrbitalObject obj : engine.getTrackedObjects()) {
                    if (obj instanceof model.spacecraft.CargoShip) {
                        model.spacecraft.CargoShip cargo = (model.spacecraft.CargoShip) obj;
                        if (cargo.getTargetStation() == station) {
                            cargo.abortMission("Estación espacial destruida en colisión orbital.");
                        }
                    }
                }
            }

            // Notificar a la Vista principal para actualizar el monitor lateral (Señal_Perdida) y detener simulación
            if (onEstacionDestruida != null) {
                onEstacionDestruida.accept(station);
            }

            // Delegar a la Vista la presentación del cartel de destrucción
            CrisisDialogView.mostrarDialogoEstacionDestruida(
                    parent, 
                    station.getImagenDestruida(), 
                    station.getTextoDestruccion()
            );

            // Mostrar cartel final indicando que la simulación concluyó por destrucción de la estación
            CrisisDialogView.mostrarDialogoSimulacionFinalizada(parent, station.getName());
        }

        // Modificar el Modelo eliminando el objeto destruido
        engine.removeShip(target);
    }

    private static void finalizarCrisis(
            RogueDebris threat, 
            SimulationEngine engine, 
            Runnable onReanudarSimulacion) {

        // La amenaza se desintegra tras el encuentro
        engine.removeShip(threat);

        if (onReanudarSimulacion != null) {
            onReanudarSimulacion.run();
        }
    }
}
