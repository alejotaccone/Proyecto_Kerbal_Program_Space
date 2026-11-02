package controller;

import engine.SimulationEngine;
import gui.DockingMinigameDialog;
import gui.MissionDialogView;
import java.awt.Component;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import model.spacecraft.CargoShip;
import model.spacecraft.MissionState;
import model.spacecraft.OrbitalObject;
import model.spacecraft.SpaceStation;

/**
 * Controlador especialista (Capa Controller en MVC) para el ciclo de vida
 * de las misiones de reabastecimiento orbital y la orquestación del minijuego de acople.
 * Mantiene la cohesión alta evitando sobrecargar SimulationController.
 */
public class MissionController {

    public static final int MAX_ACTIVE_CARGO_SHIPS = 3;

    private final SimulationEngine engine;
    private final AlertQueue alertQueue;
    private final Consumer<String> logConsola;

    private final Set<CargoShip> misionesCompletadasNotificadas;
    private final Set<CargoShip> misionesAbortadasNotificadas;
    private final Set<CargoShip> misionesDesorbitandoNotificadas;

    public MissionController(SimulationEngine engine, AlertQueue alertQueue, Consumer<String> logConsola) {
        this.engine = engine;
        this.alertQueue = alertQueue;
        this.logConsola = (logConsola != null) ? logConsola : s -> {};
        this.misionesCompletadasNotificadas = new HashSet<>();
        this.misionesAbortadasNotificadas = new HashSet<>();
        this.misionesDesorbitandoNotificadas = new HashSet<>();
    }

    /**
     * Valida y lanza un nuevo carguero de reabastecimiento hacia la estación objetivo.
     *
     * @return true si el lanzamiento fue exitoso; false si falló alguna validación.
     */
    public boolean lanzarCarguero(Component parent, int noradEstacion) {
        if (engine == null || engine.getTrackedObjects() == null) {
            logConsola.accept("[AVISO DE MISIÓN]: El simulador no está inicializado.");
            return false;
        }

        // 1. Validación de estación seleccionada
        SpaceStation station = buscarEstacion(noradEstacion);
        if (station == null) {
            logConsola.accept("[AVISO DE MISIÓN]: No hay una estación espacial seleccionada para la misión.");
            return false;
        }

        // 2. Validación de límite de cargueros activos (máximo 3)
        List<CargoShip> activos = engine.getActiveCargoShips();
        if (activos.size() >= MAX_ACTIVE_CARGO_SHIPS) {
            logConsola.accept(String.format(
                "[AVISO DE MISIÓN]: Límite de cargueros activos alcanzado (máximo %d en vuelo simultáneo).", 
                MAX_ACTIVE_CARGO_SHIPS
            ));
            return false;
        }

        // 3. Validación de suministros de la estación (no lanzar si ya tiene el tanque lleno)
        boolean combustibleLleno = (station.getFuelTank() != null && station.getFuelTank().isFull());
        if (combustibleLleno) {
            logConsola.accept(String.format(
                "[AVISO DE MISIÓN]: La estación [%s] ya tiene sus tanques de combustible al 100%% (no requiere reabastecimiento).",
                station.getName()
            ));
            return false;
        }

        // 4. Spawn del carguero en el modelo
        CargoShip cargo = engine.spawnCargoShip(station);
        if (cargo != null) {
            logConsola.accept(String.format(
                "[MISIÓN]: Carguero [%s] lanzado rumbo a [%s]. Distancia inicial: %.1f km.",
                cargo.getName(), station.getName(), cargo.distanceTo(station)
            ));
            return true;
        }

        return false;
    }

    /**
     * Evalúa el estado de las misiones en cada tick y encola los eventos modales necesarios.
     */
    public void evaluarMisionesPorTick(Component parent, Runnable onRefreshUI) {
        if (engine == null || engine.getTrackedObjects() == null) return;

        List<OrbitalObject> objetos = engine.getTrackedObjects();
        for (int i = 0; i < objetos.size(); i++) {
            OrbitalObject craft = objetos.get(i);
            if (!(craft instanceof CargoShip)) continue;

            CargoShip cargo = (CargoShip) craft;
            MissionState estado = cargo.getMissionState();

            // A. Carguero en Rango de Acople: disparar el minijuego de precisión interactivo
            if (estado == MissionState.EN_RANGO_DE_ACOPLE) {
                cargo.setMissionState(MissionState.ACOPLE_EN_CURSO);

                final String nombreCarguero = cargo.getName();
                final String nombreEstacion = (cargo.getTargetStation() != null) ? cargo.getTargetStation().getName() : "Estación";

                alertQueue.encolarAlerta(() -> {
                    int intentosAntes = cargo.getDockingAttemptsRemaining();
                    boolean acierto = DockingMinigameDialog.mostrarMinijuego(
                        parent,
                        cargo,
                        logConsola
                    );
                    int intentosFalladosEnSesion = intentosAntes - cargo.getDockingAttemptsRemaining();
                    if (engine.getFlightLogbook() != null) {
                        for (int f = 0; f < intentosFalladosEnSesion; f++) {
                            engine.getFlightLogbook().registrarIntentoAcople(false);
                        }
                        if (acierto) {
                            engine.getFlightLogbook().registrarIntentoAcople(true);
                        } else {
                            engine.getFlightLogbook().registrarMisionAbortada();
                        }
                    }

                    if (acierto) {
                        cargo.registrarAcopleExitoso();
                        logConsola.accept(String.format(
                            "[MISIÓN]: ¡Acople exitoso! Manguera conectada entre [%s] y [%s].",
                            nombreCarguero, nombreEstacion
                        ));
                        MissionDialogView.mostrarAcopleExitoso(parent, nombreCarguero, nombreEstacion);
                    } else {
                        engine.removeShip(cargo);
                        logConsola.accept(String.format(
                            "[MISIÓN]: Carguero [%s] retirado de órbita tras fallar los 3 intentos de acople.",
                            nombreCarguero
                        ));
                        MissionDialogView.mostrarAcopleFallido(parent, nombreCarguero, nombreEstacion);
                    }

                    if (onRefreshUI != null) onRefreshUI.run();
                });
            }

            // B. Carguero sin combustible en tránsito
            else if (estado == MissionState.ABORTADA && !misionesAbortadasNotificadas.contains(cargo)) {
                misionesAbortadasNotificadas.add(cargo);
                if (engine.getFlightLogbook() != null) {
                    engine.getFlightLogbook().registrarMisionAbortada();
                }
                if (cargo.getAbortReason() != null && cargo.getAbortReason().toLowerCase().contains("sin combustible")) {
                    final String nombreCarguero = cargo.getName();
                    alertQueue.encolarAlerta(() -> MissionDialogView.mostrarSinCombustible(parent, nombreCarguero));
                    logConsola.accept(String.format(
                        "[MISIÓN]: [%s] ha quedado a la deriva sin combustible de maniobra.",
                        cargo.getName()
                    ));
                }
            }

            // C. Misión de Reabastecimiento Completada -> Iniciar Desorbitado Cinemático
            else if (estado == MissionState.COMPLETADA && !misionesCompletadasNotificadas.contains(cargo)) {
                misionesCompletadasNotificadas.add(cargo);
                if (engine.getFlightLogbook() != null) {
                    engine.getFlightLogbook().registrarMisionCompletada(cargo.getTotalFuelTransferred());
                }
                cargo.iniciarDesorbitado();
                logConsola.accept(String.format(
                    "[DESORBITADO]: Suministros entregados por [%s] (+%.1f L). Desacoplando e iniciando ignición de retropropulsión para reingreso atmosférico.",
                    cargo.getName(), cargo.getTotalFuelTransferred()
                ));
                if (onRefreshUI != null) onRefreshUI.run();
            }

            // D. Desorbitado Concluido -> Modal de Reingreso y Remoción de Nave
            else if (estado == MissionState.REINGRESO_CONCLUIDO && !misionesDesorbitandoNotificadas.contains(cargo)) {
                misionesDesorbitandoNotificadas.add(cargo);
                final String nombreCarguero = cargo.getName();
                final String nombreEstacion = (cargo.getTargetStation() != null) ? cargo.getTargetStation().getName() : "Estación";
                final double fuel = cargo.getTotalFuelTransferred();

                alertQueue.encolarAlerta(() -> {
                    MissionDialogView.mostrarMisionCompletada(
                        parent, nombreCarguero, nombreEstacion, fuel
                    );
                    engine.removeShip(cargo);
                    if (onRefreshUI != null) onRefreshUI.run();
                });
                logConsola.accept(String.format(
                    "[REINGRESO CONCLUIDO]: Carguero [%s] completó su desorbitado e incineración atmosférica controlada sobre el océano. Misión cerrada con honores.",
                    nombreCarguero
                ));
            }
        }
    }

    private SpaceStation buscarEstacion(int noradId) {
        for (OrbitalObject craft : engine.getTrackedObjects()) {
            if (craft instanceof SpaceStation && (noradId <= 0 || craft.getNoradId() == noradId)) {
                return (SpaceStation) craft;
            }
        }
        return engine.getTargetStation();
    }
}