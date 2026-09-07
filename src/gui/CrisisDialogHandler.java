package gui;

import engine.SimulationEngine;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import model.spacecraft.OrbitalObject;
import model.spacecraft.RogueDebris;
import model.spacecraft.SpaceStation;
import model.spacecraft.Spacecraft;

/**
 * Controlador de diálogos interactivos y lógica de decisión ante eventos de crisis y colisiones.
 * Desacopla la lógica de juego y manejo de alertas de impacto fuera de MainGUI (God Class).
 * Renderiza ventanas modales personalizadas sin marco nativo con la misma paleta táctica oscura de la UI.
 */
public class CrisisDialogHandler {

    /**
     * Revisa si hay una amenaza activa (RogueDebris) cerca de su objetivo y lanza el diálogo interactivo.
     */
    public static boolean evaluarEventosCrisis(
            Component parent, 
            SimulationEngine engine, 
            Runnable onPausarSimulacion, 
            Runnable onReanudarSimulacion, 
            Consumer<String> logConsola) {
        return evaluarEventosCrisis(parent, engine, onPausarSimulacion, onReanudarSimulacion, logConsola, null);
    }

    /**
     * Sobrecarga con callback para notificación de destrucción de estación espacial.
     */
    public static boolean evaluarEventosCrisis(
            Component parent, 
            SimulationEngine engine, 
            Runnable onPausarSimulacion, 
            Runnable onReanudarSimulacion, 
            Consumer<String> logConsola,
            Consumer<SpaceStation> onEstacionDestruida) {

        if (engine == null || engine.getTrackedObjects() == null) return false;

        RogueDebris threat = buscarAmenazaRogue(engine);
        if (threat == null || threat.getTarget() == null) return false;

        OrbitalObject target = threat.getTarget();
        double dist = threat.getPosition().distanceTo(target.getPosition());

        // Si está a menos de 100km, se activa la crisis interactiva
        if (dist < 100.0) {
            if (onPausarSimulacion != null) onPausarSimulacion.run();

            int seleccion = mostrarDialogoCrisis(parent, threat, target, dist);
            procesarDecisionCrisis(parent, seleccion, target, engine, logConsola, onEstacionDestruida);
            finalizarCrisis(threat, engine, onReanudarSimulacion);
            return true;
        }

        return false;
    }

    private static RogueDebris buscarAmenazaRogue(SimulationEngine engine) {
        for (OrbitalObject nave : engine.getTrackedObjects()) {
            if (nave instanceof RogueDebris) {
                return (RogueDebris) nave;
            }
        }
        return null;
    }

    /**
     * Muestra la ventana modal táctica de crisis de colisión con estilo oscuro desacoplado de Windows.
     */
    private static int mostrarDialogoCrisis(Component parent, RogueDebris threat, OrbitalObject target, double dist) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = (owner instanceof Frame) ? new JDialog((Frame) owner, true) : new JDialog((Dialog) owner, true);
        dialog.setUndecorated(true);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(new Color(8, 14, 8));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 60, 60), 2),
            BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));

        // Título de Alerta
        JLabel lblHeader = new JLabel("═══ ⚠ ALERTA DE IMPACTO INMINENTE ═══");
        lblHeader.setFont(new Font("Consolas", Font.BOLD, 14));
        lblHeader.setForeground(new Color(255, 60, 60));
        lblHeader.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblHeader, BorderLayout.NORTH);

        // Mensaje de Cuerpo
        String bodyHtml = String.format(
            "<html><div style='text-align: center; font-family: Consolas; font-size: 11px; color: #00FF66; width: 340px;'>" +
            "La Basura Espacial Hostil <b style='color: #FF5555;'>[%s]</b><br>" +
            "se encuentra a <b style='color: #FFFF55;'>%.1f km</b> de la nave <b style='color: #00FFFF;'>[%s]</b>.<br><br>" +
            "<span style='color: #FFFFFF;'>¿Qué orden de emergencia desea ejecutar, Comandante?</span>" +
            "</div></html>",
            threat.getName(), dist, target.getName()
        );
        JLabel lblBody = new JLabel(bodyHtml);
        lblBody.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblBody, BorderLayout.CENTER);

        // Opciones Tácticas (Botones)
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        btnPanel.setOpaque(false);

        JButton btnEvade = crearBotonTactico("[1] FORZAR EVASIÓN (-15L Combustible)", new Color(255, 180, 0));
        JButton btnIgnore = crearBotonTactico("[2] IGNORAR (Aceptar Impacto Crítico)", new Color(255, 60, 60));

        final int[] result = new int[]{1}; // Por defecto ignorar si se fuerza el cierre

        btnEvade.addActionListener(e -> {
            result[0] = 0;
            dialog.dispose();
        });

        btnIgnore.addActionListener(e -> {
            result[0] = 1;
            dialog.dispose();
        });

        btnPanel.add(btnEvade);
        btnPanel.add(btnIgnore);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return result[0];
    }

    private static void procesarDecisionCrisis(
            Component parent,
            int seleccion, 
            OrbitalObject target, 
            SimulationEngine engine, 
            Consumer<String> logConsola,
            Consumer<SpaceStation> onEstacionDestruida) {

        if (seleccion == 0) { // Evasión
            boolean success = false;
            if (target instanceof Spacecraft) {
                success = ((Spacecraft) target).evade(0.5, 0.5);
            }
            if (success) {
                if (logConsola != null) logConsola.accept("[CRISIS]: ¡Evasión exitosa! [" + target.getName() + "] maniobró a tiempo.");
            } else {
                if (logConsola != null) logConsola.accept("[CRISIS FATAL]: [" + target.getName() + "] no pudo evadir el impacto (sin propulsión/combustible). OBJETO DESTRUIDO.");
                manejarDestruccionObjeto(parent, target, engine, onEstacionDestruida);
            }
        } else { // Ignorar
            if (logConsola != null) logConsola.accept("[CRISIS FATAL]: [" + target.getName() + "] ha recibido un impacto directo. OBJETO DESTRUIDO.");
            manejarDestruccionObjeto(parent, target, engine, onEstacionDestruida);
        }
    }

    /**
     * Despliega la ventana emergente táctica personalizada cuando una estación es destruida.
     */
    private static void manejarDestruccionObjeto(
            Component parent, 
            OrbitalObject target, 
            SimulationEngine engine, 
            Consumer<SpaceStation> onEstacionDestruida) {

        if (target instanceof SpaceStation) {
            SpaceStation station = (SpaceStation) target;

            String imgDestruida = station.getImagenDestruida();
            String textoDestruccion = station.getTextoDestruccion();

            ImageIcon iconDestruida = ShipImageLoader.obtenerImageIconEscalado(imgDestruida, 360, 210);

            Window owner = SwingUtilities.getWindowAncestor(parent);
            JDialog dialog = (owner instanceof Frame) ? new JDialog((Frame) owner, true) : new JDialog((Dialog) owner, true);
            dialog.setUndecorated(true);

            JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
            mainPanel.setBackground(new Color(8, 14, 8));
            mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 50, 50), 2),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
            ));

            // Encabezado
            JLabel lblHeader = new JLabel("═══ [!] ALERTA CRÍTICA: ESTACIÓN DESTRUIDA ═══");
            lblHeader.setFont(new Font("Consolas", Font.BOLD, 13));
            lblHeader.setForeground(new Color(255, 60, 60));
            lblHeader.setHorizontalAlignment(SwingConstants.CENTER);
            mainPanel.add(lblHeader, BorderLayout.NORTH);

            // Panel Central (Imagen + Mensaje)
            JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
            centerPanel.setOpaque(false);

            if (iconDestruida != null) {
                JLabel lblImg = new JLabel(iconDestruida);
                lblImg.setHorizontalAlignment(SwingConstants.CENTER);
                lblImg.setBorder(BorderFactory.createLineBorder(new Color(100, 30, 30), 1));
                centerPanel.add(lblImg, BorderLayout.CENTER);
            }

            JLabel lblTexto = new JLabel(
                "<html><div style='text-align: center; color: #FF5555; font-weight: bold; font-family: Consolas; font-size: 12px; width: 360px;'>" 
                + textoDestruccion + "</div></html>"
            );
            lblTexto.setHorizontalAlignment(SwingConstants.CENTER);
            centerPanel.add(lblTexto, BorderLayout.SOUTH);

            mainPanel.add(centerPanel, BorderLayout.CENTER);

            // Botón Táctico de Cierre
            JButton btnOk = crearBotonTactico("[ ENTENDIDO / CORTAR TRANSMISIÓN ]", new Color(255, 80, 80));
            btnOk.addActionListener(e -> dialog.dispose());

            JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            southPanel.setOpaque(false);
            southPanel.add(btnOk);

            mainPanel.add(southPanel, BorderLayout.SOUTH);

            dialog.setContentPane(mainPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);

            // Notificar callback a la GUI para cambiar la imagen lateral derecha a Señal_Perdida.jpg
            if (onEstacionDestruida != null) {
                onEstacionDestruida.accept(station);
            }
        }

        engine.removeShip(target);
    }

    private static void finalizarCrisis(
            RogueDebris threat, 
            SimulationEngine engine, 
            Runnable onReanudarSimulacion) {

        // La amenaza desaparece tras el encuentro
        engine.removeShip(threat);

        if (onReanudarSimulacion != null) {
            onReanudarSimulacion.run();
        }
    }

    /** Crea botones estilizados tácticos acordes a la paleta cibernética de la UI */
    private static JButton crearBotonTactico(String texto, Color colorTexto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Consolas", Font.BOLD, 12));
        btn.setForeground(colorTexto);
        btn.setBackground(new Color(15, 25, 15));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorTexto, 1),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(35, 55, 35));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(15, 25, 15));
            }
        });
        return btn;
    }
}
