package gui;

import engine.SimulationEngine;
import java.awt.Component;
import java.util.function.Consumer;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import model.spacecraft.OrbitalObject;
import model.spacecraft.RogueDebris;
import model.spacecraft.SpaceStation;
import model.spacecraft.Spacecraft;

/**
 * Controlador de diálogos interactivos y lógica de decisión ante eventos de crisis y colisiones.
 * Desacopla la lógica de juego y manejo de alertas de impacto fuera de MainGUI (God Class).
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

    private static int mostrarDialogoCrisis(Component parent, RogueDebris threat, OrbitalObject target, double dist) {
        String mensaje = "¡ALERTA DE IMPACTO INMINENTE!\n\n"
                + "La Basura Espacial Hostil [" + threat.getName() + "]\n"
                + "se encuentra a " + String.format("%.1f", dist) + " km de la nave [" + target.getName() + "].\n\n"
                + "¿Qué orden de emergencia desea ejecutar, Comandante?";

        Object[] opciones = {
            "Forzar Evasión (-15L Combustible)",
            "Ignorar (Aceptar Impacto Crítico)"
        };

        return JOptionPane.showOptionDialog(
                parent,
                mensaje,
                "CRISIS DE COLISIÓN DETECTADA",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                opciones,
                opciones[0]);
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

    private static void manejarDestruccionObjeto(
            Component parent, 
            OrbitalObject target, 
            SimulationEngine engine, 
            Consumer<SpaceStation> onEstacionDestruida) {

        if (target instanceof SpaceStation) {
            SpaceStation station = (SpaceStation) target;

            // 1. Mostrar ventana emergente (Pop-up) con la imagen de la estación destruida e informar el evento
            String imgDestruida = station.getImagenDestruida();
            String textoDestruccion = station.getTextoDestruccion();

            ImageIcon iconDestruida = ShipImageLoader.obtenerImageIconEscalado(imgDestruida, 360, 220);

            javax.swing.JPanel panelDialog = new javax.swing.JPanel(new java.awt.BorderLayout(0, 10));
            panelDialog.setBackground(new java.awt.Color(20, 10, 10));
            panelDialog.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

            if (iconDestruida != null) {
                javax.swing.JLabel lblImg = new javax.swing.JLabel(iconDestruida);
                lblImg.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                panelDialog.add(lblImg, java.awt.BorderLayout.CENTER);
            }

            javax.swing.JLabel lblTexto = new javax.swing.JLabel(
                "<html><div style='text-align: center; color: #FF4444; font-weight: bold; font-family: Consolas; font-size: 13px;'>" 
                + textoDestruccion + "</div></html>"
            );
            lblTexto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            panelDialog.add(lblTexto, java.awt.BorderLayout.SOUTH);

            JOptionPane.showMessageDialog(
                parent,
                panelDialog,
                "¡ESTACIÓN ESPACIAL DESTRUIDA!",
                JOptionPane.ERROR_MESSAGE
            );

            // 2. Notificar callback a la GUI para cambiar la imagen lateral derecha a Señal_Perdida.jpg
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
}
