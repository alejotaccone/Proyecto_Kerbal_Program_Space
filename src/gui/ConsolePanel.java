package gui;

import java.awt.*;
import javax.swing.*;

/**
 * Panel inferior (SOUTH) con la consola de telemetría en tiempo real.
 * Encapsula el formateo de logs tipo terminal y el scroll automático.
 * Diseñado con Alta Cohesión como componente independiente de UI.
 */
public class ConsolePanel extends JPanel {

    private static final Color COLOR_CONSOLE_BG = new Color(8, 12, 8);
    private static final Color COLOR_PANEL_BG = new Color(16, 20, 30);
    private static final Color COLOR_BORDER = new Color(0, 80, 30);
    private static final Color COLOR_TEXT_PRIMARY = new Color(0, 230, 80);
    private static final Color COLOR_TEXT_SECONDARY = new Color(0, 180, 60);

    private JTextArea txtConsola;
    private JScrollPane scrollConsola;

    public ConsolePanel() {
        super(new BorderLayout());
        setPreferredSize(new Dimension(0, 200));
        setBackground(COLOR_CONSOLE_BG);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, COLOR_BORDER),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        JLabel lblConsolaTitulo = new JLabel("  ◆ CONSOLA DE TELEMETRÍA — Registro de Actividad en Tiempo Real");
        lblConsolaTitulo.setFont(new Font("Consolas", Font.BOLD, 12));
        lblConsolaTitulo.setForeground(COLOR_TEXT_SECONDARY);
        lblConsolaTitulo.setBackground(new Color(0, 30, 10));
        lblConsolaTitulo.setOpaque(true);
        lblConsolaTitulo.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
        add(lblConsolaTitulo, BorderLayout.NORTH);

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
        scrollConsola.getVerticalScrollBar().setBackground(COLOR_PANEL_BG);

        add(scrollConsola, BorderLayout.CENTER);
    }

    /**
     * Escribe un mensaje en la consola de telemetría de forma thread-safe con auto-scroll.
     */
    public void log(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            txtConsola.append(mensaje + "\n");
            txtConsola.setCaretPosition(txtConsola.getDocument().getLength());
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> txtConsola.setText(""));
    }
}
