package controller;

import engine.SimulationEngine;
import gui.MainGUI;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import model.spacecraft.OrbitalObject;
import model.spacecraft.SpaceStation;
import radar.Radar;

/**
 * ============================================================================
 * CONTROLADOR: SimulationController (Capa Controller en MVC)
 * ============================================================================
 * Es el cerebro coordinador de la aplicación:
 * - Conecta la Vista (MainGUI y sus paneles) con el Modelo (SimulationEngine).
 * - Escucha las acciones del usuario (botones, selección de naves, clics en el radar).
 * - Ejecuta los cambios sobre el Modelo en segundo plano sin congelar la pantalla.
 * - Le indica a la Vista exactamente qué mostrar cuando los datos cambian.
 * ============================================================================
 */
public class SimulationController {

    // Referencias a Vista y Modelo
    private final MainGUI vista;
    private SimulationEngine engine;

    // Timers de la interfaz y simulación
    private Timer simulationTimer;
    private Timer radarSweepTimer;

    // Estado de la simulación
    private boolean simulationRunning = false;
    private volatile boolean tickEnProceso = false;
    private int tickCount = 0;
    private boolean estacionDestruida = false;
    private SpaceStation estacionDestruidaRef = null;

    public SimulationController(MainGUI vista) {
        this.vista = vista;
        configurarTimers();
        registrarEventos();
    }

    private void configurarTimers() {
        // Timer del ciclo de simulación (cada 10 segundos ejecuta un tick)
        simulationTimer = new Timer(10000, e -> ejecutarTickSimulacion());

        // Timer de animación del radar (cada 30 milisegundos avanza la línea de barrido a ~33 FPS)
        radarSweepTimer = new Timer(30, e -> {
            vista.getRadarPanel().avanzarBarrido();
            vista.getRadarPanel().repaint();
        });
    }

    // =========================================================================
    // REGISTRO DE EVENTOS (LISTENERS DE LA VISTA)
    // =========================================================================

    private void registrarEventos() {
        // 1. Iniciar Simulación
        vista.getControlPanel().addIniciarListener(e -> iniciarSimulacion());

        // 2. Detener Simulación
        vista.getControlPanel().addDetenerListener(e -> detenerSimulacion());

        // 3. Cambiar Estación Objetivo
        vista.getControlPanel().addCambiarEstacionListener(e -> cambiarEstacion());

        // 4. Maniobra de Evasión Orbital
        vista.getControlPanel().addEvadeShipListener(e -> ejecutarManiobraEvasion());

        // 5. Habilidad Especial de la Nave
        vista.getControlPanel().addSpecialAbilityListener(e -> ejecutarHabilidadEspecial());

        // 6. Recargar Combustible / Acople
        vista.getControlPanel().addRefuelListener(e -> ejecutarRecargaCombustible());

        // 7. Generar Crisis (Inyectar Rogue Debris)
        vista.getControlPanel().addGenerarCrisisListener(e -> generarCrisis());

        // 8. Selector de Nave en ComboBox
        vista.getControlPanel().addSeleccionarNaveListener(e -> {
            if (!vista.getControlPanel().isUpdatingCombo()) {
                actualizarMonitorNave();
                vista.getRadarPanel().setSelectedIndex(vista.getControlPanel().getSelectedShipIndex());
                vista.getRadarPanel().repaint();
            }
        });

        // 9. Clic directo sobre cualquier nave en la pantalla del Radar
        vista.getRadarPanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickedIndex = vista.getRadarPanel().getShipIndexAt(e.getX(), e.getY());
                if (clickedIndex >= 0) {
                    vista.getControlPanel().setSelectedShipIndex(clickedIndex);
                }
            }
        });
    }

    // =========================================================================
    // ACCIONES DEL CONTROLADOR
    // =========================================================================

    public void iniciarSimulacion() {
        int noradId = vista.getControlPanel().getSelectedNoradId();
        String apiKey = vista.getControlPanel().getApiKey();
        vista.getControlPanel().getBtnIniciar().setEnabled(false);

        if (!api.ApiConfig.esClaveValida(apiKey)) {
            apiKey = api.ApiConfig.obtenerApiKey();
        }

        if (!api.ApiConfig.esClaveValida(apiKey)) {
            vista.logConsola("[AVISO]: No se ha detectado ninguna API Key de N2YO configurada.");
            vista.logConsola("  -> Podés ingresarla arriba en el campo 'API Key N2YO'");
            vista.logConsola("  -> O crear el archivo 'config.properties' con: n2yo.api.key=TU_CLAVE");
            vista.logConsola("  -> O definir la variable de entorno N2YO_API_KEY");
            vista.logConsola("[Sistema]: Iniciando simulación en modo local/contingencia...");
        } else {
            vista.logConsola("[Sistema]: Conectando con API N2YO (clave " + api.ApiConfig.enmascararKey(apiKey) + ")...");
            vista.logConsola("[Sistema]: Por favor espera, esto puede tomar unos segundos...");
        }

        final String keyFinal = apiKey;

        // Usamos SwingWorker para no congelar la interfaz gráfica mientras hace la petición HTTP
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                engine = new SimulationEngine(keyFinal, noradId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    simulationRunning = true;
                    tickCount = 0;
                    estacionDestruida = false;
                    estacionDestruidaRef = null;

                    sincronizarDatosRadar();
                    simulationTimer.start();
                    radarSweepTimer.start();

                    vista.getControlPanel().setEstadoOnline(true);
                    actualizarComboNaves();

                    if (engine.getApiClient().tieneClaveConfigurada()) {
                        vista.logConsola("[Sistema]: ¡Conexión establecida con la estación vía N2YO en vivo!");
                    } else {
                        vista.logConsola("[Sistema]: Estación cargada en modo contingencia local (sin API Key).");
                    }
                    vista.logConsola("──────────────────────────────────────────────────────────────");
                } catch (Exception ex) {
                    vista.logConsola("[ERROR]: No se pudo conectar con la API: " + ex.getMessage());
                    vista.getControlPanel().getBtnIniciar().setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    public void detenerSimulacion() {
        simulationRunning = false;
        simulationTimer.stop();
        radarSweepTimer.stop();

        vista.getControlPanel().setEstadoOnline(false);
        vista.getHeaderBarPanel().setTextoEstado("TICK: " + tickCount + " | ESTADO: DETENIDO");
        vista.logConsola("[Sistema]: Simulación detenida por el operador.");
    }

    public void cambiarEstacion() {
        int noradId = vista.getControlPanel().getSelectedNoradId();
        if (engine == null) return;

        vista.logConsola("[Sistema]: Reubicando foco a nueva estación...");
        estacionDestruida = false;
        estacionDestruidaRef = null;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                engine.setTargetStation(noradId);
                return null;
            }

            @Override
            protected void done() {
                sincronizarDatosRadar();
                actualizarComboNaves();
                int navesCount = (engine.getTrackedObjects() != null) ? engine.getTrackedObjects().size() : 0;
                vista.getControlPanel().setNavesRastreadasCount(navesCount);
                vista.logConsola("[Sistema]: Radar reubicado a la nueva estación.");
            }
        };
        worker.execute();
    }

    public void ejecutarManiobraEvasion() {
        int idx = vista.getControlPanel().getSelectedShipIndex();
        if (idx >= 0 && engine != null) {
            boolean success = engine.evadeShip(idx);
            if (success) {
                vista.logConsola("[ÉXITO]: Maniobra de evasión ejecutada.");
            } else {
                vista.logConsola("[FALLO]: Maniobra de evasión fallida (sin motor o combustible insuficiente).");
            }
            actualizarComboNaves();
            actualizarMonitorNave();
            vista.getRadarPanel().repaint();
        }
    }

    public void ejecutarHabilidadEspecial() {
        int idx = vista.getControlPanel().getSelectedShipIndex();
        if (idx >= 0 && engine != null) {
            String resultado = engine.useSpecialAbility(idx);
            vista.logConsola("[HABILIDAD]: " + resultado);
            actualizarMonitorNave();
            vista.getRadarPanel().repaint();
        }
    }

    public void ejecutarRecargaCombustible() {
        int idx = vista.getControlPanel().getSelectedShipIndex();
        if (idx >= 0 && engine != null) {
            boolean success = engine.refuelShipAtStation(idx);
            if (success) {
                vista.logConsola("[ACOPLE]: Nave recargada con éxito en la estación.");
            } else {
                vista.logConsola("[FALLO]: No hay estación espacial en rango de acople (500 km) o la nave no requiere combustible.");
            }
            actualizarMonitorNave();
            vista.getRadarPanel().repaint();
        }
    }

    public void generarCrisis() {
        if (engine != null && engine.getTrackedObjects() != null && !engine.getTrackedObjects().isEmpty()) {
            boolean success = engine.triggerCrisisEvent();
            if (success) {
                vista.logConsola("[ALERTA SISTEMA]: ¡Has inyectado una anomalía cinética (Rogue Debris) en el sector!");
                actualizarComboNaves();
                vista.getRadarPanel().repaint();
            } else {
                vista.logConsola("[INFO]: No se pudo inyectar la anomalía (no hay blancos válidos o ya hay una activa).");
            }
        }
    }

    // =========================================================================
    // CICLO DE TICKS Y ACTUALIZACIÓN VISTA <-> MODELO
    // =========================================================================

    public void ejecutarTickSimulacion() {
        if (engine == null || !simulationRunning) return;
        if (tickEnProceso) return;
        tickEnProceso = true;

        tickCount++;
        final int tickActual = tickCount;
        vista.getHeaderBarPanel().actualizarEstadoTick(tickActual, "EN LÍNEA", obtenerHora());

        SwingWorker<Void, Void> tickWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                engine.tick();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    sincronizarDatosRadar();

                    List<OrbitalObject> naves = engine.getTrackedObjects();
                    String ciudad = engine.getRadar().getObserverCity();

                    vista.logConsola("══════════════════════════════════════════════════════════════");
                    vista.logConsola("  CENTRO DE CONTROL DE " + ciudad.toUpperCase() + " | TICK #" + tickActual);
                    vista.logConsola("══════════════════════════════════════════════════════════════");

                    if (naves != null && !naves.isEmpty()) {
                        vista.logConsola("--- FLOTA Y OBJETOS ORBITALES RASTREADOS ---");
                        for (int i = 0; i < naves.size(); i++) {
                            vista.logConsola("[" + (i + 1) + "] " + naves.get(i).toString());
                        }
                    }

                    List<String> alerts = engine.getRadar().detectCollisionRisks(naves);
                    if (alerts != null && !alerts.isEmpty()) {
                        vista.logConsola("──────────────────────────────────────────────────────────────");
                        vista.logConsola(" ⚠ ALERTAS CRÍTICAS DEL RADAR N2YO DETECTADAS:");
                        for (String alert : alerts) {
                            vista.logConsola("   → " + alert);
                        }
                        vista.logConsola("──────────────────────────────────────────────────────────────");
                    } else {
                        vista.logConsola("[Radar]: Órbita despejada sin alertas de colisión inminente.");
                    }

                    vista.getHeaderBarPanel().actualizarEstadoTick(tickActual, "EN LÍNEA", obtenerHora());
                    int navesCount = (naves != null) ? naves.size() : 0;
                    vista.getControlPanel().setNavesRastreadasCount(navesCount);

                    actualizarComboNaves();
                    vista.getRadarPanel().repaint();

                    // Delegar al CrisisController la evaluación de colisiones inminentes
                    boolean crisisOcurrio = CrisisController.evaluarEventosCrisis(
                            vista,
                            engine,
                            () -> {
                                simulationTimer.stop();
                                radarSweepTimer.stop();
                            },
                            () -> {
                                if (simulationRunning) {
                                    simulationTimer.start();
                                    radarSweepTimer.start();
                                }
                            },
                            vista::logConsola,
                            station -> manejarEstacionDestruida(station)
                    );

                    if (crisisOcurrio) {
                        actualizarComboNaves();
                        actualizarMonitorNave();
                        vista.getRadarPanel().repaint();
                    }

                } catch (Exception ex) {
                    vista.logConsola("[ERROR]: Fallo en tick #" + tickActual + ": " + ex.getMessage());
                } finally {
                    tickEnProceso = false;
                }
            }
        };
        tickWorker.execute();
    }

    private void manejarEstacionDestruida(SpaceStation station) {
        this.estacionDestruida = true;
        this.estacionDestruidaRef = station;
        vista.mostrarSenalPerdida(station);
    }

    public void sincronizarDatosRadar() {
        if (engine == null) return;

        vista.getRadarPanel().setNaves(engine.getTrackedObjects());
        vista.getRadarPanel().setSelectedIndex(vista.getControlPanel().getSelectedShipIndex());

        Radar radar = engine.getRadar();
        if (radar != null) {
            vista.getRadarPanel().setCentro(
                radar.getObserverPosition(),
                radar.getCoverageRadiusKm()
            );
        }
    }

    public void actualizarMonitorNave() {
        if (estacionDestruida) {
            vista.getShipMonitorPanel().mostrarSenalPerdida(estacionDestruidaRef);
            return;
        }

        if (engine == null || engine.getTrackedObjects() == null) {
            vista.getShipMonitorPanel().actualizar(null);
            return;
        }

        int idx = vista.getControlPanel().getSelectedShipIndex();
        if (idx < 0 || idx >= engine.getTrackedObjects().size()) {
            vista.getShipMonitorPanel().actualizar(null);
            return;
        }

        OrbitalObject ship = engine.getTrackedObjects().get(idx);
        vista.getShipMonitorPanel().actualizar(ship);
    }

    public void actualizarComboNaves() {
        if (engine == null || engine.getTrackedObjects() == null) return;
        vista.getControlPanel().actualizarComboNaves(engine.getTrackedObjects(), vista.getSplitPane());
        actualizarMonitorNave();
    }

    private String obtenerHora() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    public SimulationEngine getEngine() {
        return engine;
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
    }

    public int getTickCount() {
        return tickCount;
    }
}

