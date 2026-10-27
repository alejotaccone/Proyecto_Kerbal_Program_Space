package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import model.minigame.PrecisionGauge;

/**
 * Diálogo modal para el minijuego de precisión de acople orbital.
 * Sigue la estética cibernética oscura de CrisisDialogView.
 * Captura la barra espaciadora mediante InputMap/ActionMap para evitar problemas de foco.
 */
public class DockingMinigameDialog {

    public static boolean mostrarMinijuego(
            Component parent, 
            String nombreCarguero, 
            String nombreEstacion, 
            int intentoActual, 
            int maxIntentos) {

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = (owner instanceof Frame) ? new JDialog((Frame) owner, true) : new JDialog((Dialog) owner, true);
        dialog.setUndecorated(true);

        // Modelo desacoplado del minijuego con dificultad calibrada según el intento
        PrecisionGauge gauge = new PrecisionGauge();
        gauge.ajustarDificultadPorIntento(intentoActual);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(new Color(8, 14, 8));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 220, 100), 2),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        // ---- Encabezado Táctico ----
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("═══ [DOCK] MANIOBRA DE ACOPLE ORBITAL ═══");
        lblTitle.setFont(new Font("Consolas", Font.BOLD, 14));
        lblTitle.setForeground(new Color(0, 255, 100));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel(String.format(
            "Carguero: %s  |  Estación: %s  |  Intento: %d de %d",
            nombreCarguero, nombreEstacion, intentoActual, maxIntentos
        ));
        lblSubtitle.setFont(new Font("Consolas", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(255, 255, 100));
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblSubtitle);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ---- Contenedor Central: Imagen + Barra de Precisión + Instrucciones ----
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // 1. Imagen inicial de acoplamiento en curso
        JLabel lblImagen = new JLabel();
        lblImagen.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblImagen.setBorder(BorderFactory.createLineBorder(new Color(0, 80, 40), 1));
        ImageIcon imgAcoplamiento = ShipImageLoader.obtenerImageIconEscalado("CargoShipProgress_Acoplamiento.jpg", 400, 200);
        if (imgAcoplamiento != null) {
            lblImagen.setIcon(imgAcoplamiento);
        } else {
            lblImagen.setText("[ SIMULACIÓN DE APROXIMACIÓN ]");
            lblImagen.setForeground(Color.GRAY);
        }
        centerPanel.add(lblImagen);
        centerPanel.add(Box.createVerticalStrut(10));

        // 2. Mensaje de instrucciones y feedback en tiempo real
        JLabel lblFeedback = new JLabel("Presiona [BARRA ESPACIADORA] para acoplar en la zona verde");
        lblFeedback.setFont(new Font("Consolas", Font.BOLD, 12));
        lblFeedback.setForeground(new Color(220, 255, 220));
        lblFeedback.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(lblFeedback);
        centerPanel.add(Box.createVerticalStrut(10));

        // 3. Panel de dibujo de la barra de precisión
        BarraPrecisionPanel barPanel = new BarraPrecisionPanel(gauge);
        barPanel.setPreferredSize(new Dimension(400, 36));
        barPanel.setMaximumSize(new Dimension(400, 36));
        barPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(barPanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // ---- Animación (~60 FPS) ----
        Timer animTimer = new Timer(16, e -> {
            gauge.update();
            barPanel.repaint();
        });

        // ---- Resultado mutable ----
        final boolean[] resultado = new boolean[]{false};

        // ---- Configuración de Key Bindings para la Barra Espaciadora ----
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("SPACE"), "frenarMarcador"
        );
        mainPanel.getActionMap().put("frenarMarcador", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gauge.isStopped()) return;

                boolean acierto = gauge.frenar();
                animTimer.stop();
                barPanel.repaint();
                resultado[0] = acierto;

                if (acierto) {
                    lblFeedback.setText("¡MANGUERA CONECTADA CON ÉXITO!");
                    lblFeedback.setForeground(new Color(0, 255, 100));

                    Timer closeTimer = new Timer(1200, evt -> dialog.dispose());
                    closeTimer.setRepeats(false);
                    closeTimer.start();
                } else {
                    ImageIcon imgFallo = ShipImageLoader.obtenerImageIconEscalado("CargoShipProgress_AcoplamientoFallido.jpg", 400, 200);
                    if (imgFallo != null) {
                        lblImagen.setIcon(imgFallo);
                    }
                    lblFeedback.setText(String.format("¡FALLO DE ALINEACIÓN! (Intento %d de %d)", intentoActual, maxIntentos));
                    lblFeedback.setForeground(new Color(255, 80, 80));

                    Timer closeTimer = new Timer(1500, evt -> dialog.dispose());
                    closeTimer.setRepeats(false);
                    closeTimer.start();
                }
            }
        });

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        animTimer.start();
        dialog.setVisible(true);

        return resultado[0];
    }

    /**
     * Componente gráfico que renderiza la barra horizontal y el marcador oscilante.
     */
    private static class BarraPrecisionPanel extends JPanel {
        private final PrecisionGauge gauge;

        public BarraPrecisionPanel(PrecisionGauge gauge) {
            this.gauge = gauge;
            setBackground(new Color(10, 18, 10));
            setBorder(BorderFactory.createLineBorder(new Color(0, 100, 40), 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Fondo de la pista
            g2.setColor(new Color(12, 22, 12));
            g2.fillRect(0, 0, w, h);

            // 2. Zona Verde Objetivo (centrada)
            double startRatio = gauge.getGreenZoneStart() / 100.0;
            double endRatio = gauge.getGreenZoneEnd() / 100.0;
            int greenX = (int) (startRatio * w);
            int greenW = (int) ((endRatio - startRatio) * w);

            g2.setColor(new Color(0, 200, 80, 190));
            g2.fillRect(greenX, 2, greenW, h - 4);
            g2.setColor(new Color(0, 255, 120));
            g2.drawRect(greenX, 2, greenW, h - 4);

            // 3. Marcador Vertical Amarillo
            double markerRatio = gauge.getCurrentPosition() / 100.0;
            int markerX = (int) (markerRatio * w);

            g2.setColor(new Color(255, 255, 0));
            g2.fillRect(markerX - 2, 0, 4, h);

            // Triángulo indicador superior e inferior
            int[] xTop = {markerX - 4, markerX + 4, markerX};
            int[] yTop = {0, 0, 6};
            g2.fillPolygon(xTop, yTop, 3);

            int[] xBottom = {markerX - 4, markerX + 4, markerX};
            int[] yBottom = {h, h, h - 6};
            g2.fillPolygon(xBottom, yBottom, 3);

            g2.dispose();
        }
    }
}
