package gui;

import java.awt.*;
import javax.swing.*;

/**
 * Barra superior (NORTH) del Dashboard Táctico.
 * Muestra el título del sistema, estado de conexión y contador de ticks.
 * Diseñado con Alta Cohesión como componente independiente de UI.
 */
public class HeaderBarPanel extends JPanel {

    private static final Color COLOR_TEXT_PRIMARY = new Color(0, 230, 80);
    private static final Color COLOR_TEXT_SECONDARY = new Color(0, 180, 60);

    private JLabel lblTitulo;
    private JLabel lblTickCount;

    public HeaderBarPanel() {
        super(new BorderLayout());
        setBackground(new Color(0, 40, 15));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_TEXT_PRIMARY),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        lblTitulo = new JLabel("◆ KERBAL PROGRAM SPACE — DASHBOARD TÁCTICO");
        lblTitulo.setFont(new Font("Consolas", Font.BOLD, 16));
        lblTitulo.setForeground(COLOR_TEXT_PRIMARY);
        add(lblTitulo, BorderLayout.WEST);

        lblTickCount = new JLabel("TICK: 0 | ESTADO: INACTIVO");
        lblTickCount.setFont(new Font("Consolas", Font.PLAIN, 13));
        lblTickCount.setForeground(COLOR_TEXT_SECONDARY);
        add(lblTickCount, BorderLayout.EAST);
    }

    public void actualizarEstadoTick(int tick, String estado, String hora) {
        lblTickCount.setText(String.format("TICK: %d | ESTADO: %s | %s", tick, estado, hora));
    }

    public void setTextoEstado(String texto) {
        lblTickCount.setText(texto);
    }
}
