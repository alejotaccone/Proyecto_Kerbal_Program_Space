package gui;

import controller.SimulationController;
import java.awt.*;
import javax.swing.*;
import model.spacecraft.SpaceStation;

/**
 * ============================================================================
 * VISTA PRINCIPAL: MainGUI (Capa GUI / Vista en MVC)
 * ============================================================================
 * Ventana principal contenedora del Dashboard Táctico.
 * 
 * En el patrón MVC (Iteración 4):
 * - Es estrictamente la VISTA: se encarga únicamente de construir y estilizar
 *   la interfaz gráfica y ensamblar los paneles especialistas (Composite UI).
 * - Delega toda la lógica de eventos, temporizadores y llamadas al motor
 *   al CONTROLADOR ({@link controller.SimulationController}).
 * - Expone getters hacia sus paneles para que el controlador pueda consultar
 *   entradas del usuario y actualizar los componentes en pantalla.
 * ============================================================================
 */
public class MainGUI extends JFrame {

    private static final Color COLOR_BG_DARK = new Color(10, 12, 18);
    private static final Color COLOR_BORDER  = new Color(0, 80, 30);
    private static final Color COLOR_TEXT_DIM = new Color(0, 120, 40);

    // Paneles Especialistas Modulares (Componentes de la Vista)
    private HeaderBarPanel headerBarPanel;
    private ControlPanel controlPanel;
    private RadarPanel radarPanel;
    private ShipMonitorPanel shipMonitorPanel;
    private ConsolePanel consolePanel;
    private JSplitPane splitPane;

    // Controlador enlazado a esta Vista
    private SimulationController controller;

    public MainGUI() {
        super("KERBAL PROGRAM SPACE - Centro de Control y Monitoreo Orbital");

        configurarVentana();
        construirInterfaz();
        mostrarMensajeBienvenida();

        // Inicializar y enlazar el Controlador MVC
        this.controller = new SimulationController(this);
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

    private void mostrarMensajeBienvenida() {
        logConsola("════════════════════════════════════════════════════════════════");
        logConsola("   KERBAL PROGRAM SPACE - CENTRO DE CONTROL Y MONITOREO ORBITAL");
        logConsola("   Dashboard Táctico v2.0 - Arquitectura MVC (Iteración 4)");
        logConsola("════════════════════════════════════════════════════════════════");
        logConsola("[Sistema]: Interfaz gráfica inicializada correctamente.");
        logConsola("[Sistema]: Selecciona una estación y presiona 'Iniciar Simulación'.");
        logConsola("");
    }

    // =========================================================================
    // MÉTODOS DE PRESENTACIÓN / VISTA
    // =========================================================================

    public void logConsola(String mensaje) {
        consolePanel.log(mensaje);
    }

    public void mostrarSenalPerdida(SpaceStation station) {
        shipMonitorPanel.mostrarSenalPerdida(station);
    }

    // =========================================================================
    // GETTERS PARA ACCESO DEL CONTROLADOR (MVC)
    // =========================================================================

    public HeaderBarPanel getHeaderBarPanel() {
        return headerBarPanel;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    public RadarPanel getRadarPanel() {
        return radarPanel;
    }

    public ShipMonitorPanel getShipMonitorPanel() {
        return shipMonitorPanel;
    }

    public ConsolePanel getConsolePanel() {
        return consolePanel;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public SimulationController getController() {
        return controller;
    }

    // =========================================================================
    // PUNTO DE ENTRADA PRINCIPAL DE LA GUI
    // =========================================================================

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
