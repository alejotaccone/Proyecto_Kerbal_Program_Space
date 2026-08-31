package gui;

import engine.SimulationEngine;
import engine.TelemetryLogger;
import model.spacecraft.OrbitalObject;
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
    private JComboBox<String> cmbEstacion;
    private JTextField txtApiKey;
    private JButton btnIniciar;
    private JButton btnDetener;
    private JButton btnCambiarEstacion;
    private JLabel lblEstado;
    private JLabel lblTickCount;
    private JLabel lblNavesDetectadas;
    
    // Botones de acciones rápidas
    private JComboBox<String> cmbNaves;
    private JButton btnEvadeShip;
    private JButton btnSpecialAbility;
    private JButton btnRefuel;
    private JButton btnGenerarCrisis;

    // Panel central del radar (CENTER)
    private RadarPanel radarPanel;
    private JSplitPane splitPane;  // SplitPane para redimensionar panel izquierdo

    // Consola de telemetría (SOUTH)
    private JTextArea txtConsola;
    private JScrollPane scrollConsola;

    // Monitor de Nave Seleccionada (EAST)
    private JTextArea txtMonitorNave;
    private JLabel lblImagenNave;
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
        panel.add(crearEtiquetaSeccion(">> OBJETIVO DEL CENTRO DE COMANDO"));
        panel.add(Box.createVerticalStrut(8));

        // Campo: Estacion Espacial
        panel.add(crearEtiquetaCampo("Estación Espacial Objetivo:"));
        String[] estacionesPredefinidas = {
            "Estación Espacial Internacional (ISS)",
            "Estación Espacial Tiangong (CSS)"
        };
        cmbEstacion = new JComboBox<>(estacionesPredefinidas);
        cmbEstacion.setEditable(false);
        cmbEstacion.setSelectedIndex(0);
        cmbEstacion.setBackground(new Color(20, 30, 20));
        cmbEstacion.setForeground(COLOR_TEXT_PRIMARY);
        cmbEstacion.setFont(new Font("Consolas", Font.PLAIN, 12));
        cmbEstacion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cmbEstacion.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(cmbEstacion);
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

        btnCambiarEstacion = crearBoton("[R] CAMBIAR ESTACIÓN", COLOR_ACCENT_CYAN);
        btnCambiarEstacion.setEnabled(false);
        panel.add(btnCambiarEstacion);
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
        panel.add(Box.createVerticalStrut(6));
        
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
        
        // Agregar interactividad de clics sobre el radar
        radarPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickedIndex = radarPanel.getShipIndexAt(e.getX(), e.getY());
                if (clickedIndex >= 0) {
                    // +1 porque el índice 0 del combobox es "< Seleccione una nave >"
                    cmbNaves.setSelectedIndex(clickedIndex + 1);
                }
            }
        });
        
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

        lblImagenNave = new JLabel("[ SIN NAVE SELECCIONADA ]", SwingConstants.CENTER);
        lblImagenNave.setPreferredSize(new Dimension(290, 170));
        lblImagenNave.setMinimumSize(new Dimension(290, 170));
        lblImagenNave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        lblImagenNave.setBackground(new Color(8, 12, 8));
        lblImagenNave.setOpaque(true);
        lblImagenNave.setForeground(COLOR_TEXT_DIM);
        lblImagenNave.setFont(new Font("Consolas", Font.ITALIC, 11));
        lblImagenNave.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));

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

        // Panel central con la imagen arriba y los datos abajo
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setBackground(COLOR_PANEL_BG);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        centerPanel.add(lblImagenNave, BorderLayout.NORTH);
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
            ShipImageLoader.cargarImagenNave(lblImagenNave, null);
            txtMonitorNave.setText("\nNo hay nave seleccionada");
            return;
        }
        
        int idx = cmbNaves.getSelectedIndex() - 1;
        if (idx < 0 || idx >= engine.getTrackedObjects().size()) {
            ShipImageLoader.cargarImagenNave(lblImagenNave, null);
            txtMonitorNave.setText("\nNo hay nave seleccionada");
            return;
        }

        OrbitalObject ship = engine.getTrackedObjects().get(idx);
        ShipImageLoader.cargarImagenNave(lblImagenNave, ship.getNombreImagen());
        txtMonitorNave.setText(TelemetryLogger.generarResumenNave(ship));
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
        // Timer principal de simulación: cada tick llama al engine cada 10000ms (10 segundos)
        simulationTimer = new javax.swing.Timer(10000, e -> ejecutarTickSimulacion());

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
            int noradId = obtenerEstacionNoradId();
            String apiKey = txtApiKey.getText().trim();

            // Deshabilitar el botón inmediatamente para evitar doble-clic
            btnIniciar.setEnabled(false);
            
            logConsola("[Sistema]: Conectando con API N2YO y contactando estación...");
            logConsola("[Sistema]: Por favor espera, esto puede tomar unos segundos...");
            
            // Ejecutar la conexión al backend en un hilo separado para no congelar la UI
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    // Crear el SimulationEngine con la API key y el noradId
                    engine = new SimulationEngine(apiKey, noradId);
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
                        btnCambiarEstacion.setEnabled(true);
                        habilitarAcciones(true);
                        actualizarComboNaves();

                        logConsola("[Sistema]: ¡Conexión establecida con la estación!");
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

        // ---- BOTÓN: Cambiar Estación ----
        btnCambiarEstacion.addActionListener(e -> {
            int noradId = obtenerEstacionNoradId();
            if (engine == null) return;
            
            logConsola("[Sistema]: Reubicando foco a nueva estación...");
            
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    engine.setTargetStation(noradId);
                    return null;
                }
                @Override
                protected void done() {
                    sincronizarDatosRadar();  // Actualizar el radar con la nueva ubicación
                    actualizarComboNaves();
                    int navesCount = (engine.getTrackedObjects() != null) ? engine.getTrackedObjects().size() : 0;
                    lblNavesDetectadas.setText("Objetos en sector: " + navesCount);
                    logConsola("[Sistema]: Radar reubicado a la nueva estación.");
                }
            };
            worker.execute();
        });

        // ---- BOTÓN: Maniobra de Evasión ----
        btnEvadeShip.addActionListener(e -> {
            int idx = cmbNaves.getSelectedIndex() - 1;
            if (idx >= 0 && engine != null) {
                logConsola("[INFO]: Función 'Maniobra de evasión' en desarrollo. Próximamente.");
            }
        });

        // ---- BOTÓN: Habilidad Especial ----
        btnSpecialAbility.addActionListener(e -> {
            int idx = cmbNaves.getSelectedIndex() - 1;
            if (idx >= 0 && engine != null) {
                logConsola("[INFO]: Función 'Habilidad especial' en desarrollo. Próximamente.");
            }
        });

        // ---- BOTÓN: Recargar Combustible ----
        btnRefuel.addActionListener(e -> {
            int idx = cmbNaves.getSelectedIndex() - 1;
            if (idx >= 0 && engine != null) {
                logConsola("[INFO]: Función 'Acople y recarga' en desarrollo. Próximamente.");
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
                radarPanel.setSelectedIndex(cmbNaves.getSelectedIndex() - 1);
                radarPanel.repaint();
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

        // Indicar el tick actual y mantener el estado EN LÍNEA fluido
        lblTickCount.setText("TICK: " + tickActual + " | ESTADO: EN LÍNEA | " + obtenerHora());

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

                    List<OrbitalObject> naves = engine.getTrackedObjects();
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
                    actualizarMonitorNave(); // Actualización en tiempo real

                    // Repintar el radar
                    radarPanel.repaint();

                    // Evaluar eventos de crisis y colisión
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
                            MainGUI.this::logConsola
                    );

                    if (crisisOcurrio) {
                        actualizarComboNaves();
                        actualizarMonitorNave();
                        radarPanel.repaint();
                    }

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
            OrbitalObject craft = engine.getTrackedObjects().get(idx);
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
        cmbNaves.addItem("< Seleccione una nave >");
        List<OrbitalObject> naves = engine.getTrackedObjects();
        for (int i = 0; i < naves.size(); i++) {
            OrbitalObject nave = naves.get(i);
            cmbNaves.addItem((i + 1) + ". " + nave.getName() + " (" + nave.getType() + ")");
        }
        if (selectedIdx > 0 && selectedIdx < cmbNaves.getItemCount()) {
            cmbNaves.setSelectedIndex(selectedIdx);
        } else if (cmbNaves.getItemCount() > 1) {
            cmbNaves.setSelectedIndex(1); // Seleccionar la 1ra nave por defecto
        } else {
            cmbNaves.setSelectedIndex(0);
        }
        
        // Restaurar la posición del divider para que no se mueva
        splitPane.setDividerLocation(dividerPos);
        isUpdatingCombo = false;

        // Actualizar la información y la imagen en el panel lateral derecho automáticamente
        actualizarMonitorNave();
    }

    /** Habilita o deshabilita los botones de acciones sobre naves */
    private void habilitarAcciones(boolean enabled) {
        btnEvadeShip.setEnabled(enabled);
        btnSpecialAbility.setEnabled(enabled);
        btnRefuel.setEnabled(enabled);
        btnGenerarCrisis.setEnabled(enabled);
    }

    private int obtenerEstacionNoradId() {
        int index = cmbEstacion.getSelectedIndex();
        if (index == 0) return api.N2YOApiClient.NORAD_ISS;
        if (index == 1) return api.N2YOApiClient.NORAD_TIANGONG;
        return api.N2YOApiClient.NORAD_ISS;
    }

    /** Devuelve la hora actual formateada */
    private String obtenerHora() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
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
        radarPanel.setSelectedIndex(cmbNaves.getSelectedIndex() - 1);
        
        Radar radar = engine.getRadar();
        if (radar != null) {
            radarPanel.setCentro(
                radar.getObserverPosition(),
                radar.getCoverageRadiusKm()
            );
        }
    }
}
