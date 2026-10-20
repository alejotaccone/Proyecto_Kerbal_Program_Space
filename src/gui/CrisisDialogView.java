package gui;

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
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * ============================================================================
 * VISTA: CrisisDialogView (Capa GUI / Vista en MVC)
 * ============================================================================
 * Esta clase es exclusivamente visual (Vista).
 * Su única responsabilidad es mostrar ventanas modales en pantalla con el
 * estilo cibernético/táctico del sistema y devolver la respuesta del usuario.
 * NO decide qué le pasa a la nave ni borra objetos del motor (eso lo hace el Controlador).
 * ============================================================================
 */
public class CrisisDialogView {

    /**
     * Muestra la ventana modal de alerta de impacto inminente.
     * Retorna 0 si el usuario eligió "Forzar Evasión", o 1 si eligió "Ignorar".
     */
    public static int mostrarDialogoDecisionCrisis(Component parent, String nombreAmenaza, double distanciaKm, String nombreObjetivo) {
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

        // Mensaje del cuerpo con formato táctico
        String bodyHtml = String.format(
            "<html><div style='text-align: center; font-family: Consolas; font-size: 11px; color: #00FF66; width: 340px;'>" +
            "La Basura Espacial Hostil <b style='color: #FF5555;'>[%s]</b><br>" +
            "se encuentra a <b style='color: #FFFF55;'>%.1f km</b> de la nave <b style='color: #00FFFF;'>[%s]</b>.<br><br>" +
            "<span style='color: #FFFFFF;'>¿Qué orden de emergencia desea ejecutar, Comandante?</span>" +
            "</div></html>",
            nombreAmenaza, distanciaKm, nombreObjetivo
        );
        JLabel lblBody = new JLabel(bodyHtml);
        lblBody.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblBody, BorderLayout.CENTER);

        // Opciones Tácticas (Botones de acción)
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        btnPanel.setOpaque(false);

        JButton btnEvade = crearBotonTactico("[1] FORZAR EVASIÓN (-15L Combustible)", new Color(255, 180, 0));
        JButton btnIgnore = crearBotonTactico("[2] IGNORAR (Aceptar Impacto Crítico)", new Color(255, 60, 60));

        final int[] result = new int[]{1}; // Por defecto ignorar si se cierra la ventana

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

    /**
     * Despliega la ventana emergente táctica cuando una estación espacial es destruida.
     */
    public static void mostrarDialogoEstacionDestruida(Component parent, String imagenDestruida, String textoDestruccion) {
        ImageIcon iconDestruida = ShipImageLoader.obtenerImageIconEscalado(imagenDestruida, 360, 210);

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

        // Panel Central (Imagen + Mensaje explicativo)
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
    }

    /**
     * Crea un botón estilizado según la estética cibernética oscura de la aplicación.
     */
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

