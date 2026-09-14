package gui;

import engine.TelemetryLogger;
import java.awt.*;
import javax.swing.*;
import model.spacecraft.OrbitalObject;
import model.spacecraft.SpaceStation;

/**
 * Panel lateral especialista (EAST) para el monitoreo visual y telemétrico
 * de la nave seleccionada en el Dashboard Táctico.
 * Cumple con el patrón GRASP Alta Cohesión.
 */
public class ShipMonitorPanel extends JPanel {

    private static final Color COLOR_PANEL_BG = new Color(15, 20, 15);
    private static final Color COLOR_BORDER = new Color(0, 70, 30);
    private static final Color COLOR_TEXT_PRIMARY = new Color(0, 255, 65);
    private static final Color COLOR_TEXT_DIM = new Color(0, 100, 40);

    private JLabel lblImagenNave;
    private JTextArea txtMonitorNave;

    public ShipMonitorPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(320, 0));
        setBackground(COLOR_PANEL_BG);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 2, 0, 0, COLOR_BORDER),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        JLabel lblTitulo = new JLabel(">> CONSOLA DE NAVE SELECCIONADA");
        lblTitulo.setFont(new Font("Consolas", Font.BOLD, 13));
        lblTitulo.setForeground(COLOR_TEXT_PRIMARY);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        add(lblTitulo, BorderLayout.NORTH);

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

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setBackground(COLOR_PANEL_BG);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        centerPanel.add(lblImagenNave, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Actualiza la imagen y los datos de telemetría de la nave seleccionada.
     */
    public void actualizar(OrbitalObject nave) {
        if (nave != null) {
            ShipImageLoader.cargarImagenNave(lblImagenNave, nave.getNombreImagen());
            txtMonitorNave.setText(TelemetryLogger.generarResumenNave(nave));
        } else {
            ShipImageLoader.cargarImagenNave(lblImagenNave, null);
            txtMonitorNave.setText("\nNo hay nave seleccionada");
        }
        txtMonitorNave.setCaretPosition(0);
    }

    /**
     * Muestra el estado de emergencia por señal perdida.
     */
    public void mostrarSenalPerdida(SpaceStation station) {
        ShipImageLoader.cargarImagenNave(lblImagenNave, "Señal_Perdida.jpg");

        StringBuilder sb = new StringBuilder();
        sb.append("=== TELEMETRÍA INTERRUMPIDA ===\n\n");
        sb.append("⚠ SEÑAL PERDIDA CON LA ESTACIÓN ⚠\n\n");
        if (station != null && station.getTextoDestruccion() != null) {
            sb.append(station.getTextoDestruccion()).append("\n\n");
        } else {
            sb.append("La Estación Espacial quedó totalmente inoperativa.\n\n");
        }
        sb.append("-----------------------------------\n");
        sb.append("Razón:  Impacto crítico kinetico.\n");
        sb.append("Estado: TRANSMISIÓN INTERRUMPIDA\n");
        sb.append("Sector: ALERTA DE EMERGENCIA\n");

        txtMonitorNave.setText(sb.toString());
        txtMonitorNave.setCaretPosition(0);
    }

    public JLabel getLblImagenNave() {
        return lblImagenNave;
    }

    public JTextArea getTxtMonitorNave() {
        return txtMonitorNave;
    }
}
