package gui;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import model.spacecraft.OrbitalObject;

/**
 * Panel de configuración y acciones rápidas (WEST) del Dashboard Táctico.
 * Encapsula la selección de estación objetivo, API key, controles de simulación,
 * selector de naves, botones de maniobras y la leyenda del radar.
 * Diseñado con Alta Cohesión como componente independiente de UI.
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
    private JTextField txtApiKey;
    private JButton btnIniciar;
    private JButton btnDetener;
    private JButton btnCambiarEstacion;

    // Estado del Sistema
    private JLabel lblEstado;
    private JLabel lblNavesDetectadas;

    // Acciones sobre Naves
    private JComboBox<String> cmbNaves;
    private JButton btnEvadeShip;
    private JButton btnSpecialAbility;
    private JButton btnRefuel;
    private JButton btnGenerarCrisis;

    private boolean isUpdatingCombo = false;

    public ControlPanel() {
        super(new BorderLayout());
        setBackground(COLOR_PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_PANEL_BG);

        // ---- SECCIÓN: CONFIGURACIÓN DEL RADAR ----
        panel.add(crearEtiquetaSeccion(">> OBJETIVO DEL CENTRO DE COMANDO"));
        panel.add(Box.createVerticalStrut(8));

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

        panel.add(crearEtiquetaCampo("API Key N2YO:"));
        txtApiKey = crearCampoTexto(api.N2YOApiClient.DEFAULT_KEY);
        txtApiKey.setFont(new Font("Consolas", Font.PLAIN, 10));
        panel.add(txtApiKey);
        panel.add(Box.createVerticalStrut(12));

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

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.setBackground(COLOR_PANEL_BG);
        scroll.getViewport().setBackground(COLOR_PANEL_BG);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

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
        btnEvadeShip.setEnabled(enabled);
        btnSpecialAbility.setEnabled(enabled);
        btnRefuel.setEnabled(enabled);
        btnGenerarCrisis.setEnabled(enabled);
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

    public String getApiKey() {
        return txtApiKey.getText().trim();
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
    public void addEvadeShipListener(ActionListener l) { btnEvadeShip.addActionListener(l); }
    public void addSpecialAbilityListener(ActionListener l) { btnSpecialAbility.addActionListener(l); }
    public void addRefuelListener(ActionListener l) { btnRefuel.addActionListener(l); }
    public void addGenerarCrisisListener(ActionListener l) { btnGenerarCrisis.addActionListener(l); }
    public void addSeleccionarNaveListener(ActionListener l) { cmbNaves.addActionListener(l); }

    public JButton getBtnIniciar() { return btnIniciar; }
    public JButton getBtnDetener() { return btnDetener; }
    public JButton getBtnCambiarEstacion() { return btnCambiarEstacion; }
}
