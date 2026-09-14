package gui;

import engine.SimulationEngine;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import model.spacecraft.OrbitalObject;
import model.spacecraft.SpaceStation;
import radar.Radar;

/**
 * ============================================================================
 * MainGUI - Dashboard Táctico del Simulador de Tráfico Orbital
 * ============================================================================
 * 
 * Ventana principal coordinadora de la aplicación.
 * Diseñada siguiendo el patrón Composite UI / Alta Cohesión GRASP:
 * delega la construcción y gestión visual a componentes especialistas:
 *   - NORTH:  HeaderBarPanel (Título, estado del tick, reloj en vivo)
 *   - WEST:   ControlPanel (Configuración de estación, API key, controles y leyenda)
 *   - CENTER: RadarPanel (Pantalla animada del radar táctico)
 *   - EAST:   ShipMonitorPanel (Consola lateral de telemetría de nave)
 *   - SOUTH:  ConsolePanel (Registro de actividad de telemetría en tiempo real)
 * ============================================================================
 */
public class MainGUI extends JFrame {

    private static final Color COLOR_BG_DARK = new Color(10, 12, 18);
    private static final Color COLOR_BORDER  = new Color(0, 80, 30);
    private static final Color COLOR_TEXT_DIM = new Color(0, 120, 40);

    // Paneles Especialistas Modulares
    private HeaderBarPanel headerBarPanel;
    private ControlPanel controlPanel;
    private RadarPanel radarPanel;
    private ShipMonitorPanel shipMonitorPanel;
    private ConsolePanel consolePanel;
    private JSplitPane splitPane;

    // Backend, Estado y Timers
    private SimulationEngine engine;
    private javax.swing.Timer simulationTimer;
    private javax.swing.Timer radarSweepTimer;
    private boolean simulationRunning = false;
    private volatile boolean tickEnProceso = false;
    private int tickCount = 0;
    private boolean estacionDestruida = false;
    private SpaceStation estacionDestruidaRef = null;

    public MainGUI() {
        super("KERBAL PROGRAM SPACE - Centro de Control y Monitoreo Orbital");

        configurarVentana();
        construirInterfaz();
        configurarTimers();
        registrarEventos();
        mostrarMensajeBienvenida();
    }

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG_DARK);
        setLayout(new BorderLayout(4, 4));
    }

    private void construirInterfaz() {
        headerBarPanel = new HeaderBarPanel();
        controlPanel = new ControlPanel();
        radarPanel = new RadarPanel();
        shipMonitorPanel = new ShipMonitorPanel();
        consolePanel = new ConsolePanel();

        // Panel central con JSplitPane para redimensionar el radar y panel de control
        JPanel radarWrapper = new JPanel(new BorderLayout());
        radarWrapper.setBackground(COLOR_BG_DARK);
        radarWrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        radarWrapper.add(radarPanel, BorderLayout.CENTER);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, radarWrapper);
        splitPane.setDividerLocation(340);
        splitPane.setDividerSize(6);
        splitPane.setResizeWeight(0.0);
        splitPane.setContinuousLayout(true);
        splitPane.setBackground(COLOR_BG_DARK);
        splitPane.setBorder(null);

        controlPanel.setMinimumSize(new Dimension(280, 0));
        radarWrapper.setMinimumSize(new Dimension(400, 0));
        estilizarSplitPane(splitPane);

        add(headerBarPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(shipMonitorPanel, BorderLayout.EAST);
        add(consolePanel, BorderLayout.SOUTH);
    }

    private void estilizarSplitPane(JSplitPane sp) {
        sp.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(COLOR_BORDER);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(COLOR_TEXT_DIM);
                        int cy = getHeight() / 2;
                        g.fillRect(1, cy - 15, getWidth() - 2, 2);
                        g.fillRect(1, cy, getWidth() - 2, 2);
                        g.fillRect(1, cy + 15, getWidth() - 2, 2);
                    }
                };
            }
        });
    }

    private void configurarTimers() {
        simulationTimer = new javax.swing.Timer(10000, e -> ejecutarTickSimulacion());
        radarSweepTimer = new javax.swing.Timer(30, e -> {
            radarPanel.avanzarBarrido();
            radarPanel.repaint();
        });
    }

    private void mostrarMensajeBienvenida() {
        logConsola("════════════════════════════════════════════════════════════════");
        logConsola("   KERBAL PROGRAM SPACE - CENTRO DE CONTROL Y MONITOREO ORBITAL");
        logConsola("   Dashboard Táctico v2.0 - Integración N2YO NORAD & Radar");
        logConsola("════════════════════════════════════════════════════════════════");
        logConsola("[Sistema]: Interfaz gráfica inicializada correctamente.");
        logConsola("[Sistema]: Selecciona una estación y presiona 'Iniciar Simulación'.");
        logConsola("");
    }

    // =========================================================================
    // REGISTRO DE EVENTOS (CONTROLADOR DE VISTA)
    // =========================================================================

    private void registrarEventos() {
        // Iniciar Simulación
        controlPanel.addIniciarListener(e -> {
            int noradId = controlPanel.getSelectedNoradId();
            String apiKey = controlPanel.getApiKey();
            controlPanel.getBtnIniciar().setEnabled(false);

            logConsola("[Sistema]: Conectando con API N2YO y contactando estación...");
            logConsola("[Sistema]: Por favor espera, esto puede tomar unos segundos...");

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    engine = new SimulationEngine(apiKey, noradId);
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

                        controlPanel.setEstadoOnline(true);
                        actualizarComboNaves();

                        logConsola("[Sistema]: ¡Conexión establecida con la estación!");
                        logConsola("──────────────────────────────────────────────────────────────");
                    } catch (Exception ex) {
                        logConsola("[ERROR]: No se pudo conectar con la API: " + ex.getMessage());
                        controlPanel.getBtnIniciar().setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        // Detener Simulación
        controlPanel.addDetenerListener(e -> detenerSimulacion());

        // Cambiar Estación
        controlPanel.addCambiarEstacionListener(e -> {
            int noradId = controlPanel.getSelectedNoradId();
            if (engine == null) return;

            logConsola("[Sistema]: Reubicando foco a nueva estación...");
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
                    controlPanel.setNavesRastreadasCount(navesCount);
                    logConsola("[Sistema]: Radar reubicado a la nueva estación.");
                }
            };
            worker.execute();
        });

        // Maniobra de Evasión
        controlPanel.addEvadeShipListener(e -> {
            int idx = controlPanel.getSelectedShipIndex();
            if (idx >= 0 && engine != null) {
                boolean success = engine.evadeShip(idx);
                if (success) {
                    logConsola("[ÉXITO]: Maniobra de evasión ejecutada.");
                } else {
                    logConsola("[FALLO]: Maniobra de evasión fallida (sin motor o combustible insuficiente).");
                }
                actualizarComboNaves();
                actualizarMonitorNave();
                radarPanel.repaint();
            }
        });

        // Habilidad Especial
        controlPanel.addSpecialAbilityListener(e -> {
            int idx = controlPanel.getSelectedShipIndex();
            if (idx >= 0 && engine != null) {
                String resultado = engine.useSpecialAbility(idx);
                logConsola("[HABILIDAD]: " + resultado);
                actualizarMonitorNave();
                radarPanel.repaint();
            }
        });

        // Recargar Combustible
        controlPanel.addRefuelListener(e -> {
            int idx = controlPanel.getSelectedShipIndex();
            if (idx >= 0 && engine != null) {
                boolean success = engine.refuelShipAtStation(idx);
                if (success) {
                    logConsola("[ACOPLE]: Nave recargada con éxito en la estación.");
                } else {
                    logConsola("[FALLO]: No hay estación espacial en rango de acople (500 km) o la nave no requiere combustible.");
                }
                actualizarMonitorNave();
                radarPanel.repaint();
            }
        });

        // Generar Crisis (Anomalía Cinética)
        controlPanel.addGenerarCrisisListener(e -> {
            if (engine != null && engine.getTrackedObjects() != null && !engine.getTrackedObjects().isEmpty()) {
                boolean success = engine.triggerCrisisEvent();
                if (success) {
                    logConsola("[ALERTA SISTEMA]: ¡Has inyectado una anomalía cinética (Rogue Debris) en el sector!");
                    actualizarComboNaves();
                    radarPanel.repaint();
                } else {
                    logConsola("[INFO]: No se pudo inyectar la anomalía (no hay blancos válidos o ya hay una activa).");
                }
            }
        });

        // Selector de Nave
        controlPanel.addSeleccionarNaveListener(e -> {
            if (!controlPanel.isUpdatingCombo()) {
                actualizarMonitorNave();
                radarPanel.setSelectedIndex(controlPanel.getSelectedShipIndex());
                radarPanel.repaint();
            }
        });

        // Clics interactivos en el Radar
        radarPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickedIndex = radarPanel.getShipIndexAt(e.getX(), e.getY());
                if (clickedIndex >= 0) {
                    controlPanel.setSelectedShipIndex(clickedIndex);
                }
            }
        });
    }

    // =========================================================================
    // LÓGICA DE SIMULACIÓN Y ACTUALIZACIÓN DE PANELES
    // =========================================================================

    private void ejecutarTickSimulacion() {
        if (engine == null || !simulationRunning) return;
        if (tickEnProceso) return;
        tickEnProceso = true;

        tickCount++;
        final int tickActual = tickCount;
        headerBarPanel.actualizarEstadoTick(tickActual, "EN LÍNEA", obtenerHora());

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

                    logConsola("══════════════════════════════════════════════════════════════");
                    logConsola("  CENTRO DE CONTROL DE " + ciudad.toUpperCase() + " | TICK #" + tickActual);
                    logConsola("══════════════════════════════════════════════════════════════");

                    if (naves != null && !naves.isEmpty()) {
                        logConsola("--- FLOTA Y OBJETOS ORBITALES RASTREADOS ---");
                        for (int i = 0; i < naves.size(); i++) {
                            logConsola("[" + (i + 1) + "] " + naves.get(i).toString());
                        }
                    }

                    List<String> alerts = engine.getRadar().detectCollisionRisks(naves);
                    if (alerts != null && !alerts.isEmpty()) {
                        logConsola("──────────────────────────────────────────────────────────────");
                        logConsola(" ⚠ ALERTAS CRÍTICAS DEL RADAR N2YO DETECTADAS:");
                        for (String alert : alerts) {
                            logConsola("   → " + alert);
                        }
                        logConsola("──────────────────────────────────────────────────────────────");
                    } else {
                        logConsola("[Radar]: Órbita despejada sin alertas de colisión inminente.");
                    }

                    headerBarPanel.actualizarEstadoTick(tickActual, "EN LÍNEA", obtenerHora());
                    int navesCount = (naves != null) ? naves.size() : 0;
                    controlPanel.setNavesRastreadasCount(navesCount);

                    actualizarComboNaves();
                    radarPanel.repaint();

                    boolean crisisOcurrio = CrisisDialogHandler.evaluarEventosCrisis(
                            MainGUI.this,
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
                            MainGUI.this::logConsola,
                            station -> MainGUI.this.mostrarSenalPerdida(station)
                    );

                    if (crisisOcurrio) {
                        actualizarComboNaves();
                        actualizarMonitorNave();
                        radarPanel.repaint();
                    }

                } catch (Exception ex) {
                    logConsola("[ERROR]: Fallo en tick #" + tickActual + ": " + ex.getMessage());
                } finally {
                    tickEnProceso = false;
                }
            }
        };
        tickWorker.execute();
    }

    private void detenerSimulacion() {
        simulationRunning = false;
        simulationTimer.stop();
        radarSweepTimer.stop();

        controlPanel.setEstadoOnline(false);
        headerBarPanel.setTextoEstado("TICK: " + tickCount + " | ESTADO: DETENIDO");
        logConsola("[Sistema]: Simulación detenida por el operador.");
    }

    private void sincronizarDatosRadar() {
        if (engine == null) return;

        radarPanel.setNaves(engine.getTrackedObjects());
        radarPanel.setSelectedIndex(controlPanel.getSelectedShipIndex());

        Radar radar = engine.getRadar();
        if (radar != null) {
            radarPanel.setCentro(
                radar.getObserverPosition(),
                radar.getCoverageRadiusKm()
            );
        }
    }

    private void actualizarMonitorNave() {
        if (estacionDestruida) {
            shipMonitorPanel.mostrarSenalPerdida(estacionDestruidaRef);
            return;
        }

        if (engine == null || engine.getTrackedObjects() == null) {
            shipMonitorPanel.actualizar(null);
            return;
        }

        int idx = controlPanel.getSelectedShipIndex();
        if (idx < 0 || idx >= engine.getTrackedObjects().size()) {
            shipMonitorPanel.actualizar(null);
            return;
        }

        OrbitalObject ship = engine.getTrackedObjects().get(idx);
        shipMonitorPanel.actualizar(ship);
    }

    public void mostrarSenalPerdida(SpaceStation station) {
        this.estacionDestruida = true;
        this.estacionDestruidaRef = station;
        shipMonitorPanel.mostrarSenalPerdida(station);
    }

    private void actualizarComboNaves() {
        if (engine == null || engine.getTrackedObjects() == null) return;
        controlPanel.actualizarComboNaves(engine.getTrackedObjects(), splitPane);
        actualizarMonitorNave();
    }

    private void logConsola(String mensaje) {
        consolePanel.log(mensaje);
    }

    private String obtenerHora() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainGUI gui = new MainGUI();
            gui.setVisible(true);
        });
    }
}
