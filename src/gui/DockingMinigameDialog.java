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
import java.util.function.Consumer;
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
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import model.minigame.PrecisionGauge;
import model.spacecraft.CargoShip;

/**
 * Diálogo modal interactivo para el minijuego de precisión de acople orbital.
 * Si el jugador falla un intento, presenta de inmediato el mensaje de error junto
 * con la imagen de acoplamiento fallido y transiciona fluidamente al siguiente intento.
 */
public class DockingMinigameDialog {

    public static boolean mostrarMinijuego(
            Component parent,
            CargoShip cargo,
            Consumer<String> logConsola) {

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = (owner instanceof Frame) ? new JDialog((Frame) owner, true) : new JDialog((Dialog) owner, true);
        dialog.setUndecorated(true);

        final String nombreCarguero = cargo.getName();
        final String nombreEstacion = (cargo.getTargetStation() != null) ? cargo.getTargetStation().getName() : "Estación Espacial";
        final int maxIntentos = CargoShip.MAX_DOCKING_ATTEMPTS;

        // Contador mutable de intento actual
        final int[] intentoActual = new int[]{CargoShip.MAX_DOCKING_ATTEMPTS - cargo.getDockingAttemptsRemaining() + 1};

        // Modelo desacoplado del minijuego con dificultad calibrada según el intento
        PrecisionGauge gauge = new PrecisionGauge();
        gauge.ajustarDificultadPorIntento(intentoActual[0]);

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
            nombreCarguero, nombreEstacion, intentoActual[0], maxIntentos
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

        // 1. Imagen de acoplamiento
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

        // ---- Control de estados reactivos ----
        final boolean[] resultado = new boolean[]{false};
        final boolean[] procesandoAccion = new boolean[]{false};

        // ---- Configuración de Key Bindings para la Barra Espaciadora ----
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("SPACE"), "frenarMarcador"
        );
        mainPanel.getActionMap().put("frenarMarcador", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gauge.isStopped() || procesandoAccion[0]) return;

                boolean acierto = gauge.frenar();
                animTimer.stop();
                barPanel.repaint();
                procesandoAccion[0] = true;

                if (acierto) {
                    resultado[0] = true;
                    lblFeedback.setText("¡MANGUERA CONECTADA CON ÉXITO!");
                    lblFeedback.setForeground(new Color(0, 255, 100));

                    Timer closeTimer = new Timer(1200, evt -> dialog.dispose());
                    closeTimer.setRepeats(false);
                    closeTimer.start();
                } else {
                    boolean puedeReintentar = cargo.procesarIntentoFallido();

                    ImageIcon imgFallo = ShipImageLoader.obtenerImageIconEscalado("CargoShipProgress_AcoplamientoFallido.jpg", 400, 200);
                    if (imgFallo != null) {
                        lblImagen.setIcon(imgFallo);
                    }

                    if (puedeReintentar) {
                        lblFeedback.setText(String.format("¡ERROR! Fallo en intento %d (-10L Combustible). Preparando reintento...", intentoActual[0]));
                        lblFeedback.setForeground(new Color(255, 80, 80));

                        if (logConsola != null) {
                            logConsola.accept(String.format(
                                "[MISIÓN]: ¡Error de alineación en intento %d para [%s]! Penalización -10L aplicada. Quedan %d intentos.",
                                intentoActual[0], nombreCarguero, cargo.getDockingAttemptsRemaining()
                            ));
                        }

                        // Pausa breve de 1.2 segundos para mostrar el error e iniciar el 2do intento de inmediato
                        Timer retryTimer = new Timer(1200, evt -> {
                            intentoActual[0] = CargoShip.MAX_DOCKING_ATTEMPTS - cargo.getDockingAttemptsRemaining() + 1;
                            lblSubtitle.setText(String.format(
                                "Carguero: %s  |  Estación: %s  |  Intento: %d de %d",
                                nombreCarguero, nombreEstacion, intentoActual[0], maxIntentos
                            ));
                            ImageIcon imgAcop = ShipImageLoader.obtenerImageIconEscalado("CargoShipProgress_Acoplamiento.jpg", 400, 200);
                            if (imgAcop != null) {
                                lblImagen.setIcon(imgAcop);
                            }
                            gauge.ajustarDificultadPorIntento(intentoActual[0]);
                            gauge.reiniciar();
                            lblFeedback.setText("Presiona [BARRA ESPACIADORA] para acoplar en la zona verde");
                            lblFeedback.setForeground(new Color(220, 255, 220));
                            barPanel.repaint();
                            procesandoAccion[0] = false;
                            animTimer.start();
                        });
                        retryTimer.setRepeats(false);
                        retryTimer.start();
                    } else {
                        resultado[0] = false;
                        lblFeedback.setText("¡ERROR FATAL! Intentos de acople agotados. Misión abortada.");
                        lblFeedback.setForeground(new Color(255, 50, 50));

                        if (logConsola != null) {
                            logConsola.accept(String.format(
                                "[MISIÓN FATAL]: Se agotaron los 3 intentos de acople para [%s]. ¡Misión abortada!",
                                nombreCarguero
                            ));
                        }

                        Timer closeTimer = new Timer(1500, evt -> dialog.dispose());
                        closeTimer.setRepeats(false);
                        closeTimer.start();
                    }
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
     * Sobrecarga de compatibilidad hacia atrás si no se pasa la instancia completa de CargoShip.
     */
    public static boolean mostrarMinijuego(
            Component parent, 
            String nombreCarguero, 
            String nombreEstacion, 
            int intentoActual, 
            int maxIntentos) {

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = (owner instanceof Frame) ? new JDialog((Frame) owner, true) : new JDialog((Dialog) owner, true);
        dialog.setUndecorated(true);

        PrecisionGauge gauge = new PrecisionGauge();
        gauge.ajustarDificultadPorIntento(intentoActual);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(new Color(8, 14, 8));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 220, 100), 2),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

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

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JLabel lblImagen = new JLabel();
        lblImagen.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblImagen.setBorder(BorderFactory.createLineBorder(new Color(0, 80, 40), 1));
        ImageIcon imgAcoplamiento = ShipImageLoader.obtenerImageIconEscalado("CargoShipProgress_Acoplamiento.jpg", 400, 200);
        if (imgAcoplamiento != null) {
            lblImagen.setIcon(imgAcoplamiento);
        }
        centerPanel.add(lblImagen);
        centerPanel.add(Box.createVerticalStrut(10));

        JLabel lblFeedback = new JLabel("Presiona [BARRA ESPACIADORA] para acoplar en la zona verde");
        lblFeedback.setFont(new Font("Consolas", Font.BOLD, 12));
        lblFeedback.setForeground(new Color(220, 255, 220));
        lblFeedback.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(lblFeedback);
        centerPanel.add(Box.createVerticalStrut(10));

        BarraPrecisionPanel barPanel = new BarraPrecisionPanel(gauge);
        barPanel.setPreferredSize(new Dimension(400, 36));
        barPanel.setMaximumSize(new Dimension(400, 36));
        barPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(barPanel);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        Timer animTimer = new Timer(16, e -> {
            gauge.update();
            barPanel.repaint();
        });

        final boolean[] resultado = new boolean[]{false};

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
                    lblFeedback.setText(String.format("¡ERROR! Fallo en intento %d de %d", intentoActual, maxIntentos));
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