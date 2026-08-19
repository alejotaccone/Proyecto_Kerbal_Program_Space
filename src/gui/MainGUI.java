package gui;

import engine.SimulationEngine;
import model.spacecraft.Spacecraft;
import model.spacecraft.SpaceDebris;
import model.spacecraft.SpaceStation;
import model.spacecraft.CrewShuttle;
import model.geometry.GeoPosition;
import radar.Radar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ============================================================================
 * MainGUI - Dashboard Táctico del Simulador de Tráfico Orbital
 * ============================================================================
 * 
 * Interfaz gráfica principal que reemplaza la consola de texto del Main original.
 * Provee tres zonas:
 *   - WEST:   Panel de configuración (ciudad, radio, API key, acciones)
 *   - CENTER: Pantalla del Radar animada con barrido y blips de naves
 *   - SOUTH:  Consola de telemetría con scroll automático
 * 
 * Utiliza javax.swing.Timer para los ticks de simulación (1000ms)
 * y un segundo timer para la animación de barrido del radar (30ms).
 * 
 * No modifica ninguna clase del backend existente.
 * ============================================================================
 */
public class MainGUI extends JFrame {

    // =========================================================================
    // CONSTANTES DE DISEÑO - Paleta de colores del Dashboard Táctico
    // =========================================================================
    private static final Color COLOR_BG_DARK       = new Color(10, 12, 18);       // Fondo principal ultra oscuro
    private static final Color COLOR_PANEL_BG      = new Color(16, 20, 30);       // Fondo de paneles
    private static final Color COLOR_RADAR_BG      = new Color(5, 10, 5);         // Fondo verde oscuro del radar
    private static final Color COLOR_RADAR_GRID    = new Color(0, 60, 0, 120);    // Círculos concéntricos del radar
    private static final Color COLOR_RADAR_LINE    = new Color(0, 255, 80, 200);  // Línea de barrido del radar
    private static final Color COLOR_RADAR_GLOW    = new Color(0, 255, 80, 30);   // Resplandor del barrido
    private static final Color COLOR_TEXT_PRIMARY   = new Color(0, 230, 80);       // Texto verde principal (estilo terminal)
    private static final Color COLOR_TEXT_SECONDARY = new Color(0, 180, 60);       // Texto verde secundario
    private static final Color COLOR_TEXT_DIM       = new Color(0, 120, 40);       // Texto verde tenue
    private static final Color COLOR_ACCENT_CYAN    = new Color(0, 200, 220);      // Acento cian para estaciones
    private static final Color COLOR_ACCENT_RED     = new Color(255, 60, 60);      // Rojo para alertas/basura
    private static final Color COLOR_ACCENT_YELLOW  = new Color(255, 200, 0);      // Amarillo para transbordadores
    private static final Color COLOR_ACCENT_ORANGE  = new Color(255, 140, 0);      // Naranja para cargo
    private static final Color COLOR_BORDER         = new Color(0, 80, 30);        // Borde de paneles
    private static final Color COLOR_BUTTON_BG      = new Color(0, 60, 25);        // Fondo de botones
    private static final Color COLOR_BUTTON_HOVER   = new Color(0, 90, 40);        // Hover de botones
    private static final Color COLOR_CONSOLE_BG     = new Color(8, 12, 8);         // Fondo de la consola

    // =========================================================================
    // COMPONENTES DE LA INTERFAZ
    // =========================================================================
    
    // Panel de configuración (WEST)
    private JComboBox<String> cmbCiudad;
    private JTextField txtRadio;
    private JTextField txtApiKey;
    private JButton btnIniciar;
    private JButton btnDetener;
    private JButton btnCambiarCiudad;
    private JLabel lblEstado;
    private JLabel lblTickCount;
    private JLabel lblNavesDetectadas;
    
    // Botones de acciones rápidas
    private JComboBox<String> cmbNaves;
    private JButton btnEvadeShip;
    private JButton btnSpecialAbility;
    private JButton btnRefuel;
    private JButton btnToggleShield;
    private JButton btnGenerarCrisis;

    // Panel central del radar (CENTER)
    private RadarPanel radarPanel;
    private JSplitPane splitPane;  // SplitPane para redimensionar panel izquierdo

    // Consola de telemetría (SOUTH)
    private JTextArea txtConsola;
    private JScrollPane scrollConsola;

    // Monitor de Nave Seleccionada (EAST)
    private JTextArea txtMonitorNave;
    private boolean isUpdatingCombo = false;

    // =========================================================================
    // LÓGICA DE BACKEND Y TIMERS
    // =========================================================================
    private SimulationEngine engine;
    private javax.swing.Timer simulationTimer;  // Timer principal de ticks (1000ms)
    private javax.swing.Timer radarSweepTimer;  // Timer de animación del barrido (30ms)
    private boolean simulationRunning = false;
    private volatile boolean tickEnProceso = false;  // Flag anti-solapamiento para ticks con HTTP lento
    private int tickCount = 0;

    // =========================================================================
    // CONSTRUCTOR PRINCIPAL
    // =========================================================================
    
    /**
     * Inicializa la ventana principal del Dashboard Táctico.
     * Configura el layout BorderLayout con las tres zonas requeridas.
     */
    public MainGUI() {
        super("KERBAL PROGRAM SPACE - Centro de Control y Monitoreo Orbital");
        
        // Configuración de la ventana principal
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG_DARK);
        setLayout(new BorderLayout(4, 4));

        // Construir las tres zonas del dashboard
        // Usar JSplitPane para que el panel izquierdo sea redimensionable por el usuario
        JPanel panelConfig = crearPanelConfiguracion();
        JPanel panelRadar = crearPanelRadar();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelConfig, panelRadar);
        splitPane.setDividerLocation(340);          // Ancho inicial del panel izquierdo
        splitPane.setDividerSize(6);                // Ancho del divisor arrastrable
        splitPane.setResizeWeight(0.0);             // Al redimensionar ventana, el espacio extra va al radar
        splitPane.setContinuousLayout(true);        // Redibujar mientras se arrastra
        splitPane.setBackground(COLOR_BG_DARK);
        splitPane.setBorder(null);

        // Mínimos: el panel de config no se puede achicar más de 280px,
        // y el radar necesita al menos 400px para verse bien
        panelConfig.setMinimumSize(new Dimension(280, 0));
        panelRadar.setMinimumSize(new Dimension(400, 0));

        // Estilizar el divisor para que combine con el tema oscuro
        splitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(COLOR_BORDER);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        // Dibuja una línea central como indicador de arrastre
                        g.setColor(COLOR_TEXT_DIM);
                        int cy = getHeight() / 2;
                        g.fillRect(1, cy - 15, getWidth() - 2, 2);
                        g.fillRect(1, cy,      getWidth() - 2, 2);
                        g.fillRect(1, cy + 15, getWidth() - 2, 2);
                    }
                };
            }
        });

        add(splitPane, BorderLayout.CENTER);
        add(crearPanelConsola(), BorderLayout.SOUTH);
        add(crearBarraSuperior(), BorderLayout.NORTH);
        add(crearPanelMonitorNave(), BorderLayout.EAST);

        // Configurar los timers
        configurarTimers();

        // Registrar la lógica de los botones
        registrarEventos();

        // Mensaje de bienvenida en la consola
        logConsola("════════════════════════════════════════════════════════════════");
        logConsola("   KERBAL PROGRAM SPACE - CENTRO DE CONTROL Y MONITOREO ORBITAL");
        logConsola("   Dashboard Táctico v2.0 - Integración N2YO NORAD & Radar");
        logConsola("════════════════════════════════════════════════════════════════");
        logConsola("[Sistema]: Interfaz gráfica inicializada correctamente.");
        logConsola("[Sistema]: Ingresa una ciudad y presiona 'Iniciar Simulación'.");
        logConsola("");
    }

    // =========================================================================
    // CONSTRUCCIÓN DE PANELES
    // =========================================================================

    /**
     * Crea la barra superior con el título del programa y la hora actual.
     */
    private JPanel crearBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(new Color(0, 40, 15));
        barra.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_TEXT_PRIMARY),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        // Título izquierdo
        JLabel lblTitulo = new JLabel("◆ KERBAL PROGRAM SPACE — DASHBOARD TÁCTICO");
        lblTitulo.setFont(new Font("Consolas", Font.BOLD, 16));
        lblTitulo.setForeground(COLOR_TEXT_PRIMARY);
        barra.add(lblTitulo, BorderLayout.WEST);

        // Estado del tick a la derecha
        lblTickCount = new JLabel("TICK: 0 | ESTADO: INACTIVO");
        lblTickCount.setFont(new Font("Consolas", Font.PLAIN, 13));
        lblTickCount.setForeground(COLOR_TEXT_SECONDARY);
        barra.add(lblTickCount, BorderLayout.EAST);

        return barra;
    }

    /**
     * Crea el panel izquierdo (WEST) con los controles de configuración
     * y las acciones rápidas sobre las naves.
     */
    private JPanel crearPanelConfiguracion() {
        JPanel panelOuter = new JPanel(new BorderLayout());
        panelOuter.setBackground(COLOR_PANEL_BG);
        panelOuter.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Contenedor con scroll para cuando la ventana sea pequeña
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_PANEL_BG);

        // ---- SECCIÓN: CONFIGURACIÓN DEL RADAR ----
        panel.add(crearEtiquetaSeccion(">> CONFIGURACIÓN DEL RADAR"));
        panel.add(Box.createVerticalStrut(8));

        // Campo: Ciudad (ComboBox editable con ciudades predefinidas)
        panel.add(crearEtiquetaCampo("Ciudad de la Estación Terrena:"));
        String[] ciudadesPredefinidas = {
            "Buenos Aires", "Nueva York", "Tokio", "Londres",
            "Madrid", "París", "Sídney", "El Cairo",
            "Río de Janeiro", "Ciudad de México", "Berlín",
            "Moscú", "Pekín", "Seúl", "Toronto"
        };
        cmbCiudad = new JComboBox<>(ciudadesPredefinidas);
        cmbCiudad.setEditable(true);  // Permite escribir una ciudad personalizada
        cmbCiudad.setSelectedIndex(0);
        cmbCiudad.setBackground(new Color(20, 30, 20));
        cmbCiudad.setForeground(COLOR_TEXT_PRIMARY);
        cmbCiudad.setFont(new Font("Consolas", Font.PLAIN, 13));
        cmbCiudad.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cmbCiudad.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Estilizar el editor del combo (la parte donde se escribe)
        JTextField editorCiudad = (JTextField) cmbCiudad.getEditor().getEditorComponent();
        editorCiudad.setBackground(new Color(20, 28, 20));
        editorCiudad.setForeground(COLOR_TEXT_PRIMARY);
        editorCiudad.setCaretColor(COLOR_TEXT_PRIMARY);
        editorCiudad.setFont(new Font("Consolas", Font.PLAIN, 13));
        panel.add(cmbCiudad);
        panel.add(Box.createVerticalStrut(6));

        // Campo: Radio
        panel.add(crearEtiquetaCampo("Radio del Radar (km):"));
        txtRadio = crearCampoTexto("100");
        panel.add(txtRadio);
        panel.add(Box.createVerticalStrut(6));

        // Campo: API Key
        panel.add(crearEtiquetaCampo("API Key N2YO:"));
        txtApiKey = crearCampoTexto(api.N2YOApiClient.DEFAULT_KEY);
        txtApiKey.setFont(new Font("Consolas", Font.PLAIN, 10));
        panel.add(txtApiKey);
        panel.add(Box.createVerticalStrut(12));

        // Botones de control de simulación
        btnIniciar = crearBoton(">> INICIAR SIMULACIÓN", COLOR_TEXT_PRIMARY);
        panel.add(btnIniciar);
        panel.add(Box.createVerticalStrut(6));

        btnDetener = crearBoton("[X] DETENER SIMULACIÓN", COLOR_ACCENT_RED);
        btnDetener.setEnabled(false);
        panel.add(btnDetener);
        panel.add(Box.createVerticalStrut(6));

        btnCambiarCiudad = crearBoton("[R] CAMBIAR CIUDAD", COLOR_ACCENT_CYAN);
        btnCambiarCiudad.setEnabled(false);
        panel.add(btnCambiarCiudad);
        panel.add(Box.createVerticalStrut(15));

        // ---- SECCIÓN: ESTADO ----
        panel.add(crearEtiquetaSeccion(">> ESTADO DEL SISTEMA"));
        panel.add(Box.createVerticalStrut(6));

        lblEstado = new JLabel("● Desconectado");
        lblEstado.setFont(new Font("Consolas", Font.BOLD, 12));
        lblEstado.setForeground(COLOR_ACCENT_RED);
        lblEstado.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblEstado);
        panel.add(Box.createVerticalStrut(4));

        lblNavesDetectadas = new JLabel("Naves rastreadas: 0");
        lblNavesDetectadas.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblNavesDetectadas.setForeground(COLOR_TEXT_DIM);
        lblNavesDetectadas.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lblNavesDetectadas);
        panel.add(Box.createVerticalStrut(15));

        // ---- SECCIÓN: ACCIONES RÁPIDAS ----
        panel.add(crearEtiquetaSeccion(">> ACCIONES SOBRE NAVES"));
        panel.add(Box.createVerticalStrut(8));

        panel.add(crearEtiquetaCampo("Seleccionar Nave:"));
        cmbNaves = new JComboBox<>();
        cmbNaves.setBackground(new Color(20, 30, 20));
        cmbNaves.setForeground(COLOR_TEXT_PRIMARY);
        cmbNaves.setFont(new Font("Consolas", Font.PLAIN, 11));
        cmbNaves.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        cmbNaves.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(cmbNaves);
        panel.add(Box.createVerticalStrut(8));

        btnEvadeShip = crearBoton("[~] Maniobra Evasión", COLOR_ACCENT_YELLOW);
        btnEvadeShip.setEnabled(false);
        panel.add(btnEvadeShip);
        panel.add(Box.createVerticalStrut(4));

        btnSpecialAbility = crearBoton("[*] Habilidad Especial", COLOR_ACCENT_CYAN);
        btnSpecialAbility.setEnabled(false);
        panel.add(btnSpecialAbility);
        panel.add(Box.createVerticalStrut(4));

        btnRefuel = crearBoton("[+] Acoplar y Recargar", COLOR_ACCENT_ORANGE);
        btnRefuel.setEnabled(false);
        panel.add(btnRefuel);
        panel.add(Box.createVerticalStrut(4));

        btnToggleShield = crearBoton("[O] Activar/Desact. Escudo", COLOR_TEXT_SECONDARY);
        btnToggleShield.setEnabled(false);
        panel.add(btnToggleShield);
        panel.add(Box.createVerticalStrut(8));
        
        btnGenerarCrisis = crearBoton("[!] Generar Anomalía", COLOR_ACCENT_RED);
        btnGenerarCrisis.setEnabled(false);
        panel.add(btnGenerarCrisis);
        panel.add(Box.createVerticalStrut(15));

        // ---- SECCIÓN: LEYENDA DEL RADAR ----
        panel.add(crearEtiquetaSeccion(">> LEYENDA DEL RADAR"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(crearItemLeyenda("●", COLOR_ACCENT_CYAN, "Estación Espacial"));
        panel.add(crearItemLeyenda("●", COLOR_TEXT_PRIMARY, "Sonda / Satélite"));
        panel.add(crearItemLeyenda("●", COLOR_ACCENT_YELLOW, "Transbordador Tripulado"));
        panel.add(crearItemLeyenda("●", COLOR_ACCENT_ORANGE, "Nave de Carga"));
        panel.add(crearItemLeyenda("▲", COLOR_ACCENT_RED, "Basura Espacial"));
        panel.add(crearItemLeyenda("◎", new Color(0, 200, 255), "Tu Ubicación (Radar)"));

        // Meter el panel en un ScrollPane para ventanas pequeñas
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.setBackground(COLOR_PANEL_BG);
        scroll.getViewport().setBackground(COLOR_PANEL_BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelOuter.add(scroll, BorderLayout.CENTER);

        return panelOuter;
    }

    /**
     * Crea el panel central (CENTER) que contiene la pantalla del radar.
     */
    private JPanel crearPanelRadar() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(COLOR_BG_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        radarPanel = new RadarPanel();
        wrapper.add(radarPanel, BorderLayout.CENTER);

        return wrapper;
    }

    /**
     * Crea el panel inferior (SOUTH) con la consola de telemetría.
     */
    private JPanel crearPanelConsola() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(0, 200));
        panel.setBackground(COLOR_CONSOLE_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, COLOR_BORDER),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Etiqueta de título de la consola
        JLabel lblConsolaTitulo = new JLabel("  ◆ CONSOLA DE TELEMETRÍA — Registro de Actividad en Tiempo Real");
        lblConsolaTitulo.setFont(new Font("Consolas", Font.BOLD, 12));
        lblConsolaTitulo.setForeground(COLOR_TEXT_SECONDARY);
        lblConsolaTitulo.setBackground(new Color(0, 30, 10));
        lblConsolaTitulo.setOpaque(true);
        lblConsolaTitulo.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
        panel.add(lblConsolaTitulo, BorderLayout.NORTH);

        // Área de texto de la consola con scroll
        txtConsola = new JTextArea();
        txtConsola.setEditable(false);
        txtConsola.setBackground(COLOR_CONSOLE_BG);
        txtConsola.setForeground(COLOR_TEXT_PRIMARY);
        txtConsola.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtConsola.setCaretColor(COLOR_TEXT_PRIMARY);
        txtConsola.setLineWrap(true);
        txtConsola.setWrapStyleWord(true);
        txtConsola.setMargin(new Insets(5, 10, 5, 10));

        scrollConsola = new JScrollPane(txtConsola);
        scrollConsola.setBorder(null);
        scrollConsola.setBackground(COLOR_CONSOLE_BG);
        scrollConsola.getViewport().setBackground(COLOR_CONSOLE_BG);

        // Personalizar la barra de scroll
        scrollConsola.getVerticalScrollBar().setBackground(COLOR_PANEL_BG);

        panel.add(scrollConsola, BorderLayout.CENTER);

        return panel;
    }
    
    /**
     * Crea el panel lateral (EAST) que actúa como consola secundaria estática 
     * para la nave seleccionada.
     */
    private JPanel crearPanelMonitorNave() {
        JPanel panelOuter = new JPanel(new BorderLayout());
        panelOuter.setPreferredSize(new Dimension(320, 0));
        panelOuter.setBackground(COLOR_PANEL_BG);
        panelOuter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 2, 0, 0, COLOR_BORDER),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        panelOuter.add(crearEtiquetaSeccion(">> CONSOLA DE NAVE SELECCIONADA"), BorderLayout.NORTH);

        txtMonitorNave = new JTextArea();
        txtMonitorNave.setEditable(false);
        txtMonitorNave.setBackground(new Color(8, 12, 8));
        txtMonitorNave.setForeground(new Color(150, 255, 150));
        txtMonitorNave.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtMonitorNave.setMargin(new Insets(10, 10, 10, 10));
        txtMonitorNave.setText("\nNo hay nave seleccionada");

        JScrollPane scroll = new JScrollPane(txtMonitorNave);
        scroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        scroll.setBackground(COLOR_PANEL_BG);
        scroll.getViewport().setBackground(COLOR_PANEL_BG);

        // Separador para que no quede pegado al título
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(COLOR_PANEL_BG);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        centerPanel.add(scroll, BorderLayout.CENTER);

        panelOuter.add(centerPanel, BorderLayout.CENTER);
        return panelOuter;
    }

    /**
     * Actualiza el contenido estático del monitor de nave secundaria.
     * Solo se llama cuando se selecciona una nave manualmente o se realiza una acción.
     */
    private void actualizarMonitorNave() {
        if (engine == null || engine.getTrackedObjects() == null) {
            txtMonitorNave.setText("\nNo hay nave seleccionada");
            return;
        }
        
        int idx = cmbNaves.getSelectedIndex();
        if (idx < 0 || idx >= engine.getTrackedObjects().size()) {
            txtMonitorNave.setText("\nNo hay nave seleccionada");
            return;
        }

        Spacecraft ship = engine.getTrackedObjects().get(idx);
        StringBuilder sb = new StringBuilder();
        sb.append("=== DATOS DE LA NAVE ===\n\n");
        sb.append("Nombre: ").append(ship.getName()).append("\n");
        sb.append("Tipo:   ").append(ship.getType()).append("\n");
        sb.append("NORAD:  ").append(ship.getNoradId()).append("\n\n");
        
        sb.append("--- ÚLTIMA POSICIÓN REGISTRADA ---\n");
        sb.append(String.format("Latitud:  %.4f°\n", ship.getPosition().getLatitude()));
        sb.append(String.format("Longitud: %.4f°\n", ship.getPosition().getLongitude()));
        sb.append(String.format("Altitud:  %.1f km\n\n", ship.getPosition().getAltitude()));
        
        sb.append("--- ESTADO ---\n");
        sb.append("Velocidad: ").append(String.format("%.1f km/h\n", ship.getVelocityKmH()));
        sb.append("Escudo:    ").append(ship.isShieldActive() ? "ACTIVADO\n" : "DESACTIVADO\n");
        
        if (ship.getFuelTank() != null) {
            sb.append(String.format("Combust:   %.1f%%\n", ship.getFuelTank().getPercentage()));
            sb.append(String.format("Nivel:     %.1f / %.1f L\n", ship.getFuelTank().getCurrentLevel(), ship.getFuelTank().getCapacity()));
        } else {
            sb.append("Combust:   N/A (Sin motor)\n");
        }

        if (ship instanceof CrewShuttle) {
            sb.append("\n--- TRIPULACIÓN ---\n");
            CrewShuttle shuttle = (CrewShuttle) ship;
            if (shuttle.getCrew().isEmpty()) {
                sb.append("Sin tripulación a bordo\n");
            } else {
                for (model.components.Kerbal k : shuttle.getCrew()) {
                    sb.append("- ").append(k.getName()).append(" (").append(k.getRole()).append(")\n");
                }
            }
        } else if (ship instanceof model.spacecraft.CargoShip) {
            model.spacecraft.CargoShip cargo = (model.spacecraft.CargoShip) ship;
            sb.append("\n--- CARGA ---\n");
            sb.append(String.format("Capacidad: %.1f ton\n", cargo.getCargoCapacityTons()));
            sb.append(String.format("Actual:    %.1f ton\n", cargo.getCurrentCargoTons()));
        } else if (ship instanceof model.spacecraft.ExplorationProbe) {
            model.spacecraft.ExplorationProbe probe = (model.spacecraft.ExplorationProbe) ship;
            sb.append("\n--- SISTEMAS ---\n");
            sb.append(String.format("Eficiencia Solar: %.0f%%\n", probe.getSolarEfficiency() * 100));
        } else if (ship instanceof SpaceDebris) {
            SpaceDebris debris = (SpaceDebris) ship;
            sb.append("\n--- RIESGO ---\n");
            sb.append(String.format("Peligrosidad: %.1f/10\n", debris.getHazardLevel()));
        }
        
        txtMonitorNave.setText(sb.toString());
        txtMonitorNave.setCaretPosition(0);
    }

    // =========================================================================
    // COMPONENTES REUTILIZABLES DE UI
    // =========================================================================

    /** Crea una etiqueta de sección con formato de título */
    private JLabel crearEtiquetaSeccion(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Consolas", Font.BOLD, 13));
        lbl.setForeground(COLOR_TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        return lbl;
    }

    /** Crea una etiqueta descriptiva para un campo */
    private JLabel crearEtiquetaCampo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Consolas", Font.PLAIN, 11));
        lbl.setForeground(COLOR_TEXT_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    /** Crea un campo de texto estilizado */
    private JTextField crearCampoTexto(String valorInicial) {
        JTextField campo = new JTextField(valorInicial);
        campo.setBackground(new Color(20, 28, 20));
        campo.setForeground(COLOR_TEXT_PRIMARY);
        campo.setCaretColor(COLOR_TEXT_PRIMARY);
        campo.setFont(new Font("Consolas", Font.PLAIN, 13));
        campo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
        return campo;
    }

    /** Crea un botón estilizado con efecto hover */
    private JButton crearBoton(String texto, Color colorTexto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Consolas", Font.BOLD, 12));
        btn.setForeground(colorTexto);
        btn.setBackground(COLOR_BUTTON_BG);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        // Efectos de hover
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(COLOR_BUTTON_HOVER);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(COLOR_BUTTON_BG);
            }
        });

        return btn;
    }

    /** Crea un item de la leyenda del radar */
    private JPanel crearItemLeyenda(String simbolo, Color color, String descripcion) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 1));
        item.setBackground(COLOR_PANEL_BG);
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel lblSimbolo = new JLabel(simbolo);
        lblSimbolo.setFont(new Font("Consolas", Font.BOLD, 13));
        lblSimbolo.setForeground(color);
        item.add(lblSimbolo);

        JLabel lblDesc = new JLabel(descripcion);
        lblDesc.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblDesc.setForeground(COLOR_TEXT_DIM);
        item.add(lblDesc);

        return item;
    }

    // =========================================================================
    // CONFIGURACIÓN DE TIMERS
    // =========================================================================

    /**
     * Configura los dos timers del sistema:
     * 1. simulationTimer: Ejecuta un tick de simulación cada 1000ms
     * 2. radarSweepTimer: Anima el barrido del radar cada 30ms (~33 FPS)
     */
    private void configurarTimers() {
        // Timer principal de simulación: cada tick llama al engine
        simulationTimer = new javax.swing.Timer(1000, e -> ejecutarTickSimulacion());

        // Timer de animación del radar: rotación suave del barrido
        radarSweepTimer = new javax.swing.Timer(30, e -> {
            radarPanel.avanzarBarrido();
            radarPanel.repaint();
        });
    }

    // =========================================================================
    // REGISTRO DE EVENTOS (CONTROLADOR)
    // =========================================================================

    /**
     * Registra los ActionListeners de todos los botones de la interfaz.
     * Actúa como el "controlador" que conecta la vista con el backend.
     */
    private void registrarEventos() {
        
        // ---- BOTÓN: Iniciar Simulación ----
        btnIniciar.addActionListener(e -> {
            String ciudad = obtenerCiudadSeleccionada();
            String radioStr = txtRadio.getText().trim();
            String apiKey = txtApiKey.getText().trim();

            if (ciudad.isEmpty()) {
                logConsola("[ERROR]: Debes ingresar una ciudad para el radar.");
                return;
            }
            
            double radioParsed = 100.0;
            try {
                radioParsed = Double.parseDouble(radioStr);
            } catch (NumberFormatException ex) {
                logConsola("[Advertencia]: Radio inválido. Usando 100 km por defecto.");
            }
            final double radioF = radioParsed;

            // Deshabilitar el botón inmediatamente para evitar doble-clic
            btnIniciar.setEnabled(false);
            
            logConsola("[Sistema]: Conectando con API N2YO y geolocalizando \"" + ciudad + "\"...");
            logConsola("[Sistema]: Por favor espera, esto puede tomar unos segundos...");
            
            // Ejecutar la conexión al backend en un hilo separado para no congelar la UI
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    // Crear el SimulationEngine con la API key y el radio
                    engine = new SimulationEngine(apiKey, radioF);
                    
                    // Si el usuario eligió una ciudad diferente a Buenos Aires, reubicar
                    if (!ciudad.equalsIgnoreCase("Buenos Aires")) {
                        engine.setRadarLocationByCity(ciudad, radioF);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Verificar si hubo excepciones
                        
                        // Iniciar los timers de simulación y radar
                        simulationRunning = true;
                        tickCount = 0;
                        sincronizarDatosRadar();  // Pasar datos iniciales al radar
                        simulationTimer.start();
                        radarSweepTimer.start();

                        // Actualizar estado visual
                        lblEstado.setText("● En Línea");
                        lblEstado.setForeground(COLOR_TEXT_PRIMARY);
                        btnDetener.setEnabled(true);
                        btnCambiarCiudad.setEnabled(true);
                        habilitarAcciones(true);
                        actualizarComboNaves();

                        int navesCount = (engine.getTrackedObjects() != null) ? engine.getTrackedObjects().size() : 0;
                        lblNavesDetectadas.setText("Naves rastreadas: " + navesCount);

                        logConsola("[Sistema]: ¡Simulación iniciada! Radar posicionado en " + ciudad.toUpperCase() + " con radio " + radioF + " km.");
                        logConsola("[Sistema]: Rastreando " + navesCount + " objetos orbitales en tiempo real.");
                        logConsola("──────────────────────────────────────────────────────────────");
                    } catch (Exception ex) {
                        logConsola("[ERROR]: No se pudo conectar con la API: " + ex.getMessage());
                        btnIniciar.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        // ---- BOTÓN: Detener Simulación ----
        btnDetener.addActionListener(e -> {
            detenerSimulacion();
        });

        // ---- BOTÓN: Cambiar Ciudad ----
        btnCambiarCiudad.addActionListener(e -> {
            String ciudad = obtenerCiudadSeleccionada();
            if (ciudad.isEmpty() || engine == null) return;
            
            String radioStr = txtRadio.getText().trim();
            double radioParsed = 100.0;
            try {
                radioParsed = Double.parseDouble(radioStr);
            } catch (NumberFormatException ex) {
                // ignorar
            }
            final double radioF = radioParsed;

            logConsola("[Sistema]: Reubicando radar a \"" + ciudad + "\" con radio " + radioF + " km...");
            
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    engine.setRadarLocationByCity(ciudad, radioF);
                    return null;
                }
                @Override
                protected void done() {
                    sincronizarDatosRadar();  // Actualizar el radar con la nueva ubicación
                    actualizarComboNaves();
                    int navesCount = (engine.getTrackedObjects() != null) ? engine.getTrackedObjects().size() : 0;
                    lblNavesDetectadas.setText("Naves rastreadas: " + navesCount);
                    logConsola("[Sistema]: Radar reubicado. Detectadas " + navesCount + " naves sobre " + ciudad.toUpperCase() + ".");
                }
            };
            worker.execute();
        });

        // ---- BOTÓN: Maniobra de Evasión ----
        btnEvadeShip.addActionListener(e -> {
            int idx = cmbNaves.getSelectedIndex();
            if (idx >= 0 && engine != null) {
                engine.evadeShip(idx);
                logAccionNave(idx, "Maniobra de evasión ejecutada");
                actualizarMonitorNave();
            }
        });

        // ---- BOTÓN: Habilidad Especial ----
        btnSpecialAbility.addActionListener(e -> {
            int idx = cmbNaves.getSelectedIndex();
            if (idx >= 0 && engine != null) {
                engine.useSpecialAbility(idx);
                logAccionNave(idx, "Habilidad especial activada");
                actualizarMonitorNave();
            }
        });

        // ---- BOTÓN: Recargar Combustible ----
        btnRefuel.addActionListener(e -> {
            int idx = cmbNaves.getSelectedIndex();
            if (idx >= 0 && engine != null) {
                engine.refuelShipAtStation(idx);
                logAccionNave(idx, "Intento de acople y recarga");
                actualizarMonitorNave();
            }
        });

        // ---- BOTÓN: Toggle Escudo ----
        btnToggleShield.addActionListener(e -> {
            int idx = cmbNaves.getSelectedIndex();
            if (idx >= 0 && engine != null) {
                engine.toggleShield(idx);
                Spacecraft craft = engine.getTrackedObjects().get(idx);
                String estado = craft.isShieldActive() ? "ACTIVADO" : "DESACTIVADO";
                logConsola("[Acción]: Escudo de [" + craft.getName() + "] → " + estado);
                actualizarMonitorNave();
            }
        });

        // ---- BOTÓN: Generar Anomalía (Crisis) ----
        btnGenerarCrisis.addActionListener(e -> {
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

        // ---- COMBOBOX: Seleccionar Nave ----
        cmbNaves.addActionListener(e -> {
            if (!isUpdatingCombo) {
                actualizarMonitorNave();
            }
        });
    }

    /**
     * Método ejecutado cada 1000ms por el simulationTimer.
     * Replica la lógica del tick() del engine pero redirige la salida
     * a la consola gráfica y actualiza el radar visual.
     * 
     * IMPORTANTE: engine.tick() contiene llamadas HTTP bloqueantes (syncWithN2YO)
     * que demoran varios segundos. Por eso se ejecuta en un SwingWorker para
     * no congelar la interfaz. Un flag volátil (tickEnProceso) evita que se
     * acumulen ticks mientras la API aún responde.
     */
    private void ejecutarTickSimulacion() {
        if (engine == null || !simulationRunning) return;

        // Evitar solapamiento: si el tick anterior aún está en proceso (esperando HTTP),
        // no lanzar otro. Esto previene la acumulación de hilos y el lag.
        if (tickEnProceso) return;
        tickEnProceso = true;

        tickCount++;
        final int tickActual = tickCount;

        // Indicar visualmente que el tick está en proceso
        lblTickCount.setText("TICK: " + tickActual + " | SINCRONIZANDO... | " + obtenerHora());

        // Ejecutar engine.tick() en un hilo de fondo para no bloquear el EDT
        SwingWorker<Void, Void> tickWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                // Esto es lo que tarda: mueve naves + hace HTTP a N2YO para cada satélite
                engine.tick();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Verificar si hubo excepciones

                    // Todo lo de abajo se ejecuta en el EDT, seguro para Swing
                    sincronizarDatosRadar();

                    List<Spacecraft> naves = engine.getTrackedObjects();
                    String ciudad = engine.getRadar().getObserverCity();

                    // Redirigir la telemetría a la consola gráfica
                    logConsola("══════════════════════════════════════════════════════════════");
                    logConsola("  CENTRO DE CONTROL DE " + ciudad.toUpperCase() + " | TICK #" + tickActual);
                    logConsola("══════════════════════════════════════════════════════════════");

                    if (naves != null && !naves.isEmpty()) {
                        logConsola("--- FLOTA Y OBJETOS ORBITALES RASTREADOS ---");
                        for (int i = 0; i < naves.size(); i++) {
                            logConsola("[" + (i + 1) + "] " + naves.get(i).toString());
                        }
                    }

                    // Mostrar alertas de colisión
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

                    // Actualizar la barra superior
                    lblTickCount.setText("TICK: " + tickActual + " | ESTADO: EN LÍNEA | " + obtenerHora());

                    // Actualizar la cantidad de naves
                    int navesCount = (naves != null) ? naves.size() : 0;
                    lblNavesDetectadas.setText("Naves rastreadas: " + navesCount);

                    // Actualizar el combo de naves
                    actualizarComboNaves();

                    // Repintar el radar
                    radarPanel.repaint();

                    // Evaluar eventos de crisis y colisión
                    evaluarEventosCrisis();

                } catch (Exception ex) {
                    logConsola("[ERROR]: Fallo en tick #" + tickActual + ": " + ex.getMessage());
                } finally {
                    // Liberar el flag para permitir el siguiente tick
                    tickEnProceso = false;
                }
            }
        };
        tickWorker.execute();
    }

    /**
     * Revisa si hay una amenaza activa (RogueDebris) cerca de su objetivo y lanza el Pop-up interactivo.
     */
    private void evaluarEventosCrisis() {
        if (engine == null || engine.getTrackedObjects() == null) return;
        
        model.spacecraft.RogueDebris threat = null;
        for (Spacecraft s : engine.getTrackedObjects()) {
            if (s instanceof model.spacecraft.RogueDebris) {
                threat = (model.spacecraft.RogueDebris) s;
                break;
            }
        }
        
        if (threat != null && threat.getTarget() != null) {
            Spacecraft target = threat.getTarget();
            double dist = threat.getPosition().distanceTo(target.getPosition());
            
            // Si está a menos de 100km, se activa la crisis interactiva
            if (dist < 100.0) {
                // Pausar simulación y radar
                simulationTimer.stop();
                radarSweepTimer.stop();
                
                String mensaje = "¡ALERTA DE IMPACTO INMINENTE!\n\n"
                        + "La Basura Espacial Hostil [" + threat.getName() + "]\n"
                        + "se encuentra a " + String.format("%.1f", dist) + " km de la nave [" + target.getName() + "].\n\n"
                        + "¿Qué orden de emergencia desea ejecutar, Comandante?";
                
                Object[] opciones = {
                    "Forzar Evasión (-15L Combustible)",
                    "Resistir con Escudos (Requiere Escudo Activo)",
                    "Ignorar (Aceptar Impacto Crítico)"
                };
                
                int seleccion = JOptionPane.showOptionDialog(
                        this,
                        mensaje,
                        "CRISIS DE COLISIÓN DETECTADA",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null, // Icono custom null
                        opciones,
                        opciones[0]);
                
                // Procesar la decisión del jugador
                if (seleccion == 0) { // Evasión
                    boolean success = target.evade(0.5, 0.5);
                    if (success) {
                        logConsola("[CRISIS]: ¡Evasión exitosa! [" + target.getName() + "] maniobró a tiempo.");
                    } else {
                        logConsola("[CRISIS FATAL]: [" + target.getName() + "] no tuvo combustible para evadir. NAVE DESTRUIDA.");
                        engine.removeShip(target);
                    }
                } else if (seleccion == 1) { // Escudos
                    if (target.isShieldActive()) {
                        logConsola("[CRISIS]: [" + target.getName() + "] resistió el impacto. El escudo ha sido sobrecargado y destruido.");
                        target.deactivateShield();
                    } else {
                        logConsola("[CRISIS FATAL]: [" + target.getName() + "] intentó resistir sin escudos activados. NAVE DESTRUIDA.");
                        engine.removeShip(target);
                    }
                } else { // Ignorar
                    logConsola("[CRISIS FATAL]: [" + target.getName() + "] ha recibido un impacto directo. NAVE DESTRUIDA.");
                    engine.removeShip(target);
                }
                
                // La amenaza pasa de largo o explota, desaparece del radar
                engine.removeShip(threat);
                
                // Actualizar interfaz gráfica inmediatamente
                actualizarComboNaves();
                actualizarMonitorNave();
                radarPanel.repaint();
                
                // Reanudar la simulación
                if (simulationRunning) {
                    simulationTimer.start();
                    radarSweepTimer.start();
                }
            }
        }
    }


    /**
     * Detiene la simulación y limpia los timers.
     */
    private void detenerSimulacion() {
        simulationRunning = false;
        simulationTimer.stop();
        radarSweepTimer.stop();

        lblEstado.setText("● Detenido");
        lblEstado.setForeground(COLOR_ACCENT_YELLOW);
        lblTickCount.setText("TICK: " + tickCount + " | ESTADO: DETENIDO");
        btnIniciar.setEnabled(true);
        btnDetener.setEnabled(false);
        habilitarAcciones(false);

        logConsola("[Sistema]: Simulación detenida por el operador.");
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    /** Escribe un mensaje en la consola de telemetría con timestamp */
    private void logConsola(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            txtConsola.append(mensaje + "\n");
            // Auto-scroll hacia abajo
            txtConsola.setCaretPosition(txtConsola.getDocument().getLength());
        });
    }

    /** Log de acción sobre una nave específica */
    private void logAccionNave(int idx, String accion) {
        if (engine != null && engine.getTrackedObjects() != null && idx < engine.getTrackedObjects().size()) {
            Spacecraft craft = engine.getTrackedObjects().get(idx);
            logConsola("[Acción]: " + accion + " sobre [" + craft.getName() + "]");
        }
    }

    /** Actualiza el JComboBox con la lista actual de naves.
     *  Preserva la posición del divider del JSplitPane para evitar que el panel se encoja. */
    private void actualizarComboNaves() {
        if (engine == null || engine.getTrackedObjects() == null) return;
        
        isUpdatingCombo = true;
        // Guardar la posición actual del divider antes de modificar el combo
        int dividerPos = splitPane.getDividerLocation();
        
        int selectedIdx = cmbNaves.getSelectedIndex();
        cmbNaves.removeAllItems();
        List<Spacecraft> naves = engine.getTrackedObjects();
        for (int i = 0; i < naves.size(); i++) {
            Spacecraft s = naves.get(i);
            cmbNaves.addItem((i + 1) + ". " + s.getName() + " (" + s.getType() + ")");
        }
        if (selectedIdx >= 0 && selectedIdx < cmbNaves.getItemCount()) {
            cmbNaves.setSelectedIndex(selectedIdx);
        }
        
        // Restaurar la posición del divider para que no se mueva
        splitPane.setDividerLocation(dividerPos);
        isUpdatingCombo = false;
    }

    /** Habilita o deshabilita los botones de acciones sobre naves */
    private void habilitarAcciones(boolean enabled) {
        btnEvadeShip.setEnabled(enabled);
        btnSpecialAbility.setEnabled(enabled);
        btnRefuel.setEnabled(enabled);
        btnToggleShield.setEnabled(enabled);
        btnGenerarCrisis.setEnabled(enabled);
    }

    /** Obtiene la ciudad seleccionada o escrita en el combo de ciudades */
    private String obtenerCiudadSeleccionada() {
        Object selected = cmbCiudad.getSelectedItem();
        return (selected != null) ? selected.toString().trim() : "";
    }

    /** Devuelve la hora actual formateada */
    private String obtenerHora() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    // =========================================================================
    // CLASE INTERNA: RadarPanel - Pantalla del Radar Animada
    // =========================================================================

    /**
     * Panel personalizado que dibuja la pantalla del radar con:
     * - Fondo verde oscuro
     * - Círculos concéntricos (anillos de alcance)
     * - Cruz central (ubicación del observador)
     * - Línea de barrido rotativa con resplandor
     * - Blips de naves (puntos de colores según tipo)
     * - Etiquetas de identificación de cada nave
     * - Efecto de persistencia fosforescente en las estelas
     */
    static class RadarPanel extends JPanel {
        
        // Ángulo actual de la línea de barrido (en radianes)
        private double sweepAngle = 0.0;
        
        // Referencia a las naves (se establece desde el exterior)
        private List<Spacecraft> naves;
        
        // Centro del radar en coordenadas geográficas (para mapear lat/lng a píxeles)
        private double centroLat = -34.60;
        private double centroLng = -58.38;
        private double radioKm = 100.0;
        
        // Historial de posiciones para el efecto de estela
        private List<Point2D.Double> trail = new ArrayList<>();
        
        RadarPanel() {
            setBackground(COLOR_RADAR_BG);
            setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 2));
        }

        /** Avanza el ángulo de barrido para la animación */
        void avanzarBarrido() {
            sweepAngle += 0.03;  // ~1.7° por frame → ~6 segundos por rotación completa
            if (sweepAngle >= 2 * Math.PI) {
                sweepAngle -= 2 * Math.PI;
            }
        }

        /** Establece la lista de naves a dibujar */
        void setNaves(List<Spacecraft> naves) {
            this.naves = naves;
        }

        /** Actualiza el centro del radar basado en la posición del observador */
        void setCentro(double lat, double lng, double radioKm) {
            this.centroLat = lat;
            this.centroLng = lng;
            this.radioKm = radioKm;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            
            // Activar anti-aliasing para gráficos suaves
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;     // Centro X del radar
            int cy = h / 2;     // Centro Y del radar
            int radio = Math.min(cx, cy) - 30;  // Radio máximo del radar en píxeles

            // ---- 1. FONDO CON GRADIENTE RADIAL ----
            RadialGradientPaint bgGrad = new RadialGradientPaint(
                cx, cy, radio + 50,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{
                    new Color(5, 18, 5),
                    new Color(3, 10, 3),
                    new Color(0, 5, 0)
                }
            );
            g2.setPaint(bgGrad);
            g2.fillRect(0, 0, w, h);

            // ---- 2. CÍRCULOS CONCÉNTRICOS (Anillos de alcance) ----
            g2.setStroke(new BasicStroke(1.0f));
            int numCirculos = 5;
            for (int i = 1; i <= numCirculos; i++) {
                int r = (radio * i) / numCirculos;
                
                // Gradiente de opacidad: más brillante en el exterior
                int alpha = 40 + (i * 15);
                g2.setColor(new Color(0, 80, 0, Math.min(alpha, 255)));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);

                // Etiqueta de distancia en km
                int distKm = (int) ((radioKm * i) / numCirculos);
                g2.setFont(new Font("Consolas", Font.PLAIN, 10));
                g2.setColor(new Color(0, 100, 0, 150));
                g2.drawString(distKm + " km", cx + r + 4, cy - 3);
            }

            // ---- 3. LÍNEAS CARDINALES (Cruz del radar) ----
            g2.setColor(new Color(0, 50, 0, 100));
            g2.setStroke(new BasicStroke(0.8f));
            g2.drawLine(cx, cy - radio, cx, cy + radio);  // Línea vertical
            g2.drawLine(cx - radio, cy, cx + radio, cy);    // Línea horizontal

            // Etiquetas N, S, E, O
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(COLOR_TEXT_DIM);
            g2.drawString("N", cx - 4, cy - radio - 5);
            g2.drawString("S", cx - 4, cy + radio + 15);
            g2.drawString("E", cx + radio + 8, cy + 4);
            g2.drawString("O", cx - radio - 18, cy + 4);

            // ---- 4. LÍNEA DE BARRIDO (efecto sweep) ----
            dibujarBarrido(g2, cx, cy, radio);

            // ---- 5. CENTRO DEL RADAR (ubicación del observador) ----
            // Punto central brillante
            g2.setColor(new Color(0, 255, 100, 200));
            g2.fillOval(cx - 5, cy - 5, 10, 10);
            // Anillo pulsante alrededor del centro
            int pulso = (int)(4 * Math.sin(sweepAngle * 2)) + 10;
            g2.setColor(new Color(0, 200, 100, 60));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(cx - pulso, cy - pulso, pulso * 2, pulso * 2);

            // ---- 6. BLIPS DE NAVES ----
            dibujarNaves(g2, cx, cy, radio);

            // ---- 7. INFORMACIÓN SUPERPUESTA ----
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.setColor(COLOR_TEXT_DIM);
            g2.drawString("RADAR v2.0 | Lat: " + String.format("%.2f", centroLat) 
                + "° | Lng: " + String.format("%.2f", centroLng) + "°", 10, h - 10);
            g2.drawString("Radio: " + (int)radioKm + " km | Ángulo: " 
                + String.format("%.0f", Math.toDegrees(sweepAngle)) + "°", 10, h - 25);

            g2.dispose();
        }

        /**
         * Dibuja la línea de barrido del radar con efecto de resplandor cónico.
         */
        private void dibujarBarrido(Graphics2D g2, int cx, int cy, int radio) {
            // Cono de resplandor (trail del sweep)
            int trailDegrees = 40;  // Amplitud del cono de resplandor
            for (int i = 0; i < trailDegrees; i++) {
                double angle = sweepAngle - Math.toRadians(i);
                int alpha = (int)(25.0 * (1.0 - (double)i / trailDegrees));
                g2.setColor(new Color(0, 255, 80, Math.max(alpha, 0)));
                g2.setStroke(new BasicStroke(1.0f));
                int endX = cx + (int)(radio * Math.cos(angle));
                int endY = cy + (int)(radio * Math.sin(angle));
                g2.drawLine(cx, cy, endX, endY);
            }

            // Línea principal del barrido (más brillante)
            g2.setColor(COLOR_RADAR_LINE);
            g2.setStroke(new BasicStroke(2.0f));
            int endX = cx + (int)(radio * Math.cos(sweepAngle));
            int endY = cy + (int)(radio * Math.sin(sweepAngle));
            g2.drawLine(cx, cy, endX, endY);
        }

        /**
         * Dibuja los blips (puntos) de cada nave en sus coordenadas mapeadas.
         * El color y la forma dependen del tipo de nave (polimorfismo visual).
         */
        private void dibujarNaves(Graphics2D g2, int cx, int cy, int radio) {
            if (naves == null || naves.isEmpty()) return;

            for (Spacecraft craft : naves) {
                GeoPosition pos = craft.getPosition();
                if (pos == null) continue;

                // Mapear coordenadas geográficas (lat/lng) a píxeles del radar
                // Usar una proyección simple centrada en el observador
                double deltaLat = pos.getLatitude() - centroLat;
                double deltaLng = pos.getLongitude() - centroLng;

                // Convertir diferencias de grados a proporción del radio
                // ~111 km por grado de latitud, ~85 km por grado de longitud a lat ~35°
                double kmPerDegLat = 111.0;
                double kmPerDegLng = 111.0 * Math.cos(Math.toRadians(centroLat));
                
                double distXKm = deltaLng * kmPerDegLng;
                double distYKm = -deltaLat * kmPerDegLat;  // Invertir Y (pantalla crece hacia abajo)
                
                double distTotalKm = Math.sqrt(distXKm * distXKm + distYKm * distYKm);

                // Escalar: si está fuera del radio del radar, colocar en el borde
                double escala = (double) radio / radioKm;
                int px = cx + (int)(distXKm * escala);
                int py = cy + (int)(distYKm * escala);

                // Limitar dentro del círculo del radar
                double distPixel = Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy));
                if (distPixel > radio) {
                    double factor = radio / distPixel;
                    px = cx + (int)((px - cx) * factor);
                    py = cy + (int)((py - cy) * factor);
                }

                // Determinar color y forma según el tipo de nave
                Color colorBlip;
                int tamano = 6;
                boolean esTriangulo = false;

                if (craft instanceof SpaceStation) {
                    colorBlip = COLOR_ACCENT_CYAN;
                    tamano = 9;
                } else if (craft instanceof SpaceDebris) {
                    colorBlip = COLOR_ACCENT_RED;
                    tamano = 5;
                    esTriangulo = true;
                } else if (craft instanceof CrewShuttle) {
                    colorBlip = COLOR_ACCENT_YELLOW;
                    tamano = 7;
                } else if (craft.getType().contains("Carga")) {
                    colorBlip = COLOR_ACCENT_ORANGE;
                    tamano = 7;
                } else {
                    colorBlip = COLOR_TEXT_PRIMARY;
                    tamano = 6;
                }

                // Efecto de brillo cuando el barrido pasa sobre la nave
                double anguloNave = Math.atan2(py - cy, px - cx);
                double diffAngulo = Math.abs(sweepAngle - anguloNave);
                if (diffAngulo > Math.PI) diffAngulo = 2 * Math.PI - diffAngulo;
                
                boolean iluminado = diffAngulo < 0.3; // ~17° de proximidad al barrido
                
                if (iluminado) {
                    // Halo de resplandor cuando el sweep pasa
                    g2.setColor(new Color(colorBlip.getRed(), colorBlip.getGreen(), colorBlip.getBlue(), 60));
                    g2.fillOval(px - tamano * 2, py - tamano * 2, tamano * 4, tamano * 4);
                }

                // Dibujar el blip
                if (esTriangulo) {
                    // Triángulo para basura espacial
                    int[] xPoints = {px, px - tamano, px + tamano};
                    int[] yPoints = {py - tamano, py + tamano, py + tamano};
                    g2.setColor(colorBlip);
                    g2.fillPolygon(xPoints, yPoints, 3);
                } else {
                    // Círculo para las demás naves
                    g2.setColor(colorBlip);
                    g2.fillOval(px - tamano / 2, py - tamano / 2, tamano, tamano);
                    
                    // Anillo exterior para estaciones
                    if (craft instanceof SpaceStation) {
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawOval(px - tamano, py - tamano, tamano * 2, tamano * 2);
                    }
                }

                // Indicador de escudo activo
                if (craft.isShieldActive()) {
                    g2.setColor(new Color(100, 200, 255, 100));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(px - tamano - 3, py - tamano - 3, (tamano + 3) * 2, (tamano + 3) * 2);
                }

                // Etiqueta del nombre de la nave
                g2.setFont(new Font("Consolas", Font.PLAIN, 9));
                g2.setColor(new Color(colorBlip.getRed(), colorBlip.getGreen(), colorBlip.getBlue(), 180));
                
                // Acortar el nombre si es muy largo
                String labelNombre = craft.getName();
                if (labelNombre.length() > 20) {
                    labelNombre = labelNombre.substring(0, 18) + "…";
                }
                g2.drawString(labelNombre, px + tamano + 3, py + 3);
            }
        }
    }

    // =========================================================================
    // PUNTO DE ENTRADA PRINCIPAL
    // =========================================================================

    /**
     * Método main para ejecutar la GUI directamente.
     * Utiliza SwingUtilities.invokeLater para lanzar en el EDT (Event Dispatch Thread).
     */
    public static void main(String[] args) {
        // Intentar aplicar el Look and Feel del sistema operativo
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Si falla, usar el LnF por defecto de Java
        }

        SwingUtilities.invokeLater(() -> {
            MainGUI gui = new MainGUI();
            gui.setVisible(true);
        });
    }

    // =========================================================================
    // INTEGRACIÓN CON EL RADAR PANEL (actualización de datos)
    // =========================================================================
    
    /**
     * Método auxiliar que se invoca después de cada tick para pasar
     * los datos actualizados al RadarPanel.
     * Este método es llamado dentro de ejecutarTickSimulacion().
     */
    private void sincronizarDatosRadar() {
        if (engine == null) return;
        
        radarPanel.setNaves(engine.getTrackedObjects());
        
        Radar radar = engine.getRadar();
        if (radar != null && radar.getObserverPosition() != null) {
            radarPanel.setCentro(
                radar.getObserverPosition().getLatitude(),
                radar.getObserverPosition().getLongitude(),
                radar.getCoverageRadiusKm()
            );
        }
    }
}
