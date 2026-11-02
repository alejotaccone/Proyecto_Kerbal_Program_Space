package gui;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import model.spacecraft.OrbitalObject;

/**
 * Panel de configuración y operaciones tácticas (WEST) del Dashboard.
 * Permite seleccionar la estación objetivo, controlar la simulación,
 * desplegar/replegar paneles solares para carga de batería y O2,
 * lanzar misiones de reabastecimiento, inyectar anomalías orbitales
 * y ejecutar maniobras evasivas sobre naves individuales.
 */
public class ControlPanel extends JPanel {

    private static final Color COLOR_PANEL_BG      = new Color(16, 20, 30);
    private static final Color COLOR_TEXT_PRIMARY   = new Color(0, 230, 80);
    private static final Color COLOR_TEXT_DIM       = new Color(0, 120, 40);
    private static final Color COLOR_ACCENT_CYAN    = new Color(0, 200, 220);
    private static final Color COLOR_ACCENT_RED     = new Color(255, 60, 60);
    private static final Color COLOR_ACCENT_YELLOW  = new Color(255, 200, 0);
    private static final Color COLOR_ACCENT_ORANGE  = new Color(255, 140, 0);
    private static final Color COLOR_BORDER         = new Color(0, 80, 30);
    private static final Color COLOR_BUTTON_BG      = new Color(0, 60, 25);
    private static final Color COLOR_BUTTON_HOVER   = new Color(0, 90, 40);

    // Controles de Configuración
    private JComboBox<String> cmbEstacion;
    private JButton btnIniciar;
    private JButton btnDetener;
    private JButton btnCambiarEstacion;

    // Estado del Sistema
    private JLabel lblEstado;
    private JLabel lblNavesDetectadas;

    // Subsistemas de Estación
    private JButton btnPanelesSolares;

    // Operaciones de Misión y Control de Flota
    private JButton btnLanzarCarguero;
    private JButton btnGenerarCrisis;
    private JComboBox<String> cmbNaves;
    private JButton btnEvadeShip;

    private int cargueroCooldownTicks = 0;
    private boolean isUpdatingCombo = false;

    public ControlPanel() {
        super(new BorderLayout());
        setBackground(COLOR_PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        JPanel panel = new ScrollableControlPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_PANEL_BG);

        // ---- SECCIÓN: CONFIGURACIÓN DEL RADAR / ESTACIÓN OBJETIVO ----
        panel.add(crearEtiquetaSeccion(">> CENTRO DE COMANDO ORBITAL"));
        panel.add(Box.createVerticalStrut(8));

        panel.add(crearEtiquetaCampo("Estación Espacial Objetivo:"));
        String[] estacionesPredefinidas = {
            "Estación Espacial Internacional (ISS)",
            "Estación Espacial Tiangong (CSS)"
        };
        cmbEstacion = new JComboBox<>(estacionesPredefinidas);
        cmbEstacion.setEditable(false);
        cmbEstacion.setSelectedIndex(0);
        cmbEstacion.setPrototypeDisplayValue("Estación Espacial Internacional");
        cmbEstacion.setBackground(new Color(20, 30, 20));
        cmbEstacion.setForeground(COLOR_TEXT_PRIMARY);
        cmbEstacion.setFont(new Font("Consolas", Font.PLAIN, 12));
        cmbEstacion.setPreferredSize(new Dimension(260, 32));
        cmbEstacion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cmbEstacion.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(cmbEstacion);
        panel.add(Box.createVerticalStrut(10));

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

        // ---- SECCIÓN: ESTADO DEL SISTEMA ----
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

        // ---- SECCIÓN: SUBSISTEMAS DE ESTACIÓN ----
        panel.add(crearEtiquetaSeccion(">> SUBSISTEMAS DE ESTACIÓN"));
        panel.add(Box.createVerticalStrut(8));

        btnPanelesSolares = crearBoton("[☼] Paneles: DESPLEGADOS", COLOR_TEXT_PRIMARY);
        btnPanelesSolares.setEnabled(false);
        btnPanelesSolares.setToolTipText("Despliega o repliega los paneles solares para cargar la batería e impulsar el generador de O2 por electrólisis.");
        panel.add(btnPanelesSolares);
        panel.add(Box.createVerticalStrut(15));

        // ---- SECCIÓN: OPERACIONES DE MISIÓN ----
        panel.add(crearEtiquetaSeccion(">> OPERACIONES DE MISIÓN"));
        panel.add(Box.createVerticalStrut(8));

        btnLanzarCarguero = crearBoton("[>] Lanzar Carguero", COLOR_ACCENT_ORANGE);
        btnLanzarCarguero.setEnabled(false);
        btnLanzarCarguero.setToolTipText("Despliega una nave de carga KPS para reabastecer de combustible a la estación.");
        panel.add(btnLanzarCarguero);
        panel.add(Box.createVerticalStrut(6));

        btnGenerarCrisis = crearBoton("[!] Generar Anomalía", COLOR_ACCENT_RED);
        btnGenerarCrisis.setEnabled(false);
        btnGenerarCrisis.setToolTipText("Inyecta una anomalía cinética (Rogue Debris) en rumbo de impacto para probar defensas.");
        panel.add(btnGenerarCrisis);
        panel.add(Box.createVerticalStrut(15));

        // ---- SECCIÓN: CONTROL DE FLOTA INDIVIDUAL ----
        panel.add(crearEtiquetaSeccion(">> CONTROL DE FLOTA"));
        panel.add(Box.createVerticalStrut(8));

        panel.add(crearEtiquetaCampo("Seleccionar Nave:"));
        cmbNaves = new JComboBox<>();
        cmbNaves.setPrototypeDisplayValue("1. Estación Espacial Internacional");
        cmbNaves.setBackground(new Color(20, 30, 20));
        cmbNaves.setForeground(COLOR_TEXT_PRIMARY);
        cmbNaves.setFont(new Font("Consolas", Font.PLAIN, 11));
        cmbNaves.setPreferredSize(new Dimension(260, 30));
        cmbNaves.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        cmbNaves.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(cmbNaves);
        panel.add(Box.createVerticalStrut(8));

        btnEvadeShip = crearBoton("[~] Maniobra Evasión", COLOR_ACCENT_YELLOW);
        btnEvadeShip.setEnabled(false);
        btnEvadeShip.setToolTipText("Ordena una maniobra de propulsores de emergencia a la nave seleccionada.");
        panel.add(btnEvadeShip);
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

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.setBackground(COLOR_PANEL_BG);
        scroll.getViewport().setBackground(COLOR_PANEL_BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Barra de desplazamiento compacta y estilizada con temática espacial
        JScrollBar vBar = scroll.getVerticalScrollBar();
        vBar.setPreferredSize(new Dimension(8, 0));
        vBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(0, 100, 60);
                this.thumbHighlightColor = new Color(0, 180, 100);
                this.trackColor = new Color(10, 18, 12);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return crearBotonInvisible();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return crearBotonInvisible();
            }
            private JButton crearBotonInvisible() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                b.setVisible(false);
                return b;
            }
        });

        add(scroll, BorderLayout.CENTER);
    }

    private JLabel crearEtiquetaSeccion(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Consolas", Font.BOLD, 13));
        lbl.setForeground(COLOR_TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        return lbl;
    }

    private JLabel crearEtiquetaCampo(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Consolas", Font.PLAIN, 11));
        lbl.setForeground(COLOR_TEXT_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JButton crearBoton(String texto, Color colorTexto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Consolas", Font.BOLD, 11));
        btn.setForeground(colorTexto);
        btn.setBackground(COLOR_BUTTON_BG);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1),
            BorderFactory.createEmptyBorder(7, 8, 7, 8)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setPreferredSize(new Dimension(260, 35));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

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

    // ==================== MÉTODOS DE ACTUALIZACIÓN DE ESTADO ====================

    public void actualizarComboNaves(List<OrbitalObject> naves, JSplitPane splitPane) {
        if (naves == null) return;

        isUpdatingCombo = true;
        int dividerPos = (splitPane != null) ? splitPane.getDividerLocation() : -1;

        int selectedIdx = cmbNaves.getSelectedIndex();
        cmbNaves.removeAllItems();
        cmbNaves.addItem("< Seleccione una nave >");

        for (int i = 0; i < naves.size(); i++) {
            OrbitalObject nave = naves.get(i);
            cmbNaves.addItem((i + 1) + ". " + nave.getName() + " (" + nave.getType() + ")");
        }

        if (selectedIdx > 0 && selectedIdx < cmbNaves.getItemCount()) {
            cmbNaves.setSelectedIndex(selectedIdx);
        } else if (cmbNaves.getItemCount() > 1) {
            cmbNaves.setSelectedIndex(1);
        } else {
            cmbNaves.setSelectedIndex(0);
        }

        if (splitPane != null && dividerPos > 0) {
            splitPane.setDividerLocation(dividerPos);
        }
        isUpdatingCombo = false;
    }

    public void setEstadoOnline(boolean online) {
        if (online) {
            lblEstado.setText("● En Línea");
            lblEstado.setForeground(COLOR_TEXT_PRIMARY);
            btnIniciar.setEnabled(false);
            btnDetener.setEnabled(true);
            btnCambiarEstacion.setEnabled(true);
            habilitarAccionesNaves(true);
        } else {
            lblEstado.setText("● Detenido");
            lblEstado.setForeground(COLOR_ACCENT_YELLOW);
            btnIniciar.setEnabled(true);
            btnDetener.setEnabled(false);
            habilitarAccionesNaves(false);
        }
    }

    public void habilitarAccionesNaves(boolean enabled) {
        btnPanelesSolares.setEnabled(enabled);
        btnLanzarCarguero.setEnabled(enabled && cargueroCooldownTicks == 0);
        btnGenerarCrisis.setEnabled(enabled);
        btnEvadeShip.setEnabled(enabled);
    }

    public void setCargueroCooldown(int ticksRestantes) {
        this.cargueroCooldownTicks = ticksRestantes;
        if (ticksRestantes > 0) {
            btnLanzarCarguero.setText(String.format("[>] Carguero (Espera: %dt)", ticksRestantes));
            btnLanzarCarguero.setEnabled(false);
        } else {
            btnLanzarCarguero.setText("[>] Lanzar Carguero");
            btnLanzarCarguero.setEnabled(lblEstado.getText().contains("Línea"));
        }
    }

    public int getCargueroCooldownTicks() {
        return cargueroCooldownTicks;
    }

    public void actualizarEstadoPaneles(boolean desplegados) {
        if (desplegados) {
            btnPanelesSolares.setText("[☼] Paneles: DESPLEGADOS");
            btnPanelesSolares.setForeground(COLOR_TEXT_PRIMARY);
        } else {
            btnPanelesSolares.setText("[○] Paneles: REPLEGADOS");
            btnPanelesSolares.setForeground(COLOR_ACCENT_YELLOW);
        }
    }

    public void setNavesRastreadasCount(int count) {
        lblNavesDetectadas.setText("Naves rastreadas: " + count);
    }

    public int getSelectedNoradId() {
        int index = cmbEstacion.getSelectedIndex();
        if (index == 0) return api.N2YOApiClient.NORAD_ISS;
        if (index == 1) return api.N2YOApiClient.NORAD_TIANGONG;
        return api.N2YOApiClient.NORAD_ISS;
    }

    /**
     * Resuelve la API Key de forma segura desde la configuración centralizada (ApiConfig).
     * No expone la clave en texto plano en la interfaz visual.
     */
    public String getApiKey() {
        return api.ApiConfig.obtenerApiKey();
    }

    public int getSelectedShipIndex() {
        return cmbNaves.getSelectedIndex() - 1;
    }

    public void setSelectedShipIndex(int index) {
        if (index >= -1 && index + 1 < cmbNaves.getItemCount()) {
            cmbNaves.setSelectedIndex(index + 1);
        }
    }

    public boolean isUpdatingCombo() {
        return isUpdatingCombo;
    }

    // ==================== REGISTRO DE EVENTOS (LISTENERS) ====================

    public void addIniciarListener(ActionListener l) { btnIniciar.addActionListener(l); }
    public void addDetenerListener(ActionListener l) { btnDetener.addActionListener(l); }
    public void addCambiarEstacionListener(ActionListener l) { btnCambiarEstacion.addActionListener(l); }
    public void addPanelesSolaresListener(ActionListener l) { btnPanelesSolares.addActionListener(l); }
    public void addEvadeShipListener(ActionListener l) { btnEvadeShip.addActionListener(l); }
    public void addLanzarCargueroListener(ActionListener l) { btnLanzarCarguero.addActionListener(l); }
    public void addGenerarCrisisListener(ActionListener l) { btnGenerarCrisis.addActionListener(l); }
    public void addSeleccionarNaveListener(ActionListener l) { cmbNaves.addActionListener(l); }

    public JButton getBtnIniciar() { return btnIniciar; }
    public JButton getBtnDetener() { return btnDetener; }
    public JButton getBtnCambiarEstacion() { return btnCambiarEstacion; }
    public JButton getBtnPanelesSolares() { return btnPanelesSolares; }
    public JButton getBtnLanzarCarguero() { return btnLanzarCarguero; }

    /**
     * Panel interno con Scrollable que obliga a que su ancho coincida siempre
     * con el ancho visible del JScrollPane, evitando que textos largos en JComboBox
     * ensanchen el BoxLayout y estiren los botones fuera de la vista.
     */
    private static class ScrollableControlPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 48;
        }
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}