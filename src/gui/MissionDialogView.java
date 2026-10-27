package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Vista de alertas modales para misiones de reabastecimiento.
 * Mantiene la misma estética táctica cibernética que CrisisDialogView.
 */
public class MissionDialogView {

    public static void mostrarAcopleExitoso(Component parent, String carguero, String estacion) {
        String html = String.format(
            "<html><div style='text-align: center; font-family: Consolas; font-size: 11px; color: #00FF66; width: 340px;'>" +
            "¡Maniobra de aproximación completada con éxito!<br><br>" +
            "La manguera de acople de <b style='color: #FFB400;'>[%s]</b> ha sido asegurada al puerto de <b style='color: #00FFFF;'>[%s]</b>.<br><br>" +
            "<span style='color: #FFFFFF;'>Iniciando bombeo presurizado de combustible...</span>" +
            "</div></html>",
            carguero, estacion
        );
        mostrarAlerta(parent, "═══ [✓] ACOPLE EXITOSO: MANGUERA CONECTADA ═══", html, new Color(0, 220, 100), "[ INICIAR FLUJO DE CARGA ]");
    }

    public static void mostrarAcopleFallido(Component parent, String carguero, String estacion) {
        String html = String.format(
            "<html><div style='text-align: center; font-family: Consolas; font-size: 11px; color: #FF6666; width: 340px;'>" +
            "Se han agotado los 3 intentos reglamentarios de aproximación.<br><br>" +
            "La nave <b style='color: #FFB400;'>[%s]</b> no logró alinear los vectores de captura con <b style='color: #00FFFF;'>[%s]</b>.<br><br>" +
            "<span style='color: #FFFFFF;'>La misión de reabastecimiento ha sido <b style='color: #FF4444;'>ABORTADA</b> por seguridad orbital.</span>" +
            "</div></html>",
            carguero, estacion
        );
        mostrarAlerta(parent, "═══ [✗] ALERTA: MISIÓN DE ACOPLE ABORTADA ═══", html, new Color(255, 60, 60), "[ CONFIRMAR Y RETIRAR NAVE ]");
    }

    public static void mostrarSinCombustible(Component parent, String carguero) {
        String html = String.format(
            "<html><div style='text-align: center; font-family: Consolas; font-size: 11px; color: #FFAA00; width: 340px;'>" +
            "El carguero <b style='color: #FFB400;'>[%s]</b> ha consumido la totalidad de sus reservas de propulsión de navegación.<br><br>" +
            "<span style='color: #FFFFFF;'>La nave ha quedado inerte a la deriva en órbita. Misión cancelada.</span>" +
            "</div></html>",
            carguero
        );
        mostrarAlerta(parent, "═══ [!] CARGUERO SIN COMBUSTIBLE: MISIÓN ABORTADA ═══", html, new Color(255, 140, 0), "[ REGISTRAR INCIDENCIA ]");
    }

    public static void mostrarMisionCompletada(Component parent, String carguero, String estacion, double fuelTransferido) {
        String html = String.format(
            "<html><div style='text-align: center; font-family: Consolas; font-size: 11px; color: #00FF88; width: 360px;'>" +
            "La transferencia de combustible hacia <b style='color: #00FFFF;'>[%s]</b> ha concluido satisfactoriamente.<br><br>" +
            "<table style='margin: 0 auto; color: #FFFFFF; font-family: Consolas; font-size: 11px;'>" +
            "<tr><td align='right'>Combustible suministrado:</td><td align='left'><b style='color: #FFB400;'>+%.1f L</b></td></tr>" +
            "</table><br>" +
            "El carguero <b style='color: #FFB400;'>[%s]</b> se ha desacoplado exitosamente de la estación y procedió a su desorbitado controlado (retirado del radar)." +
            "</div></html>",
            estacion, fuelTransferido, carguero
        );
        mostrarAlerta(parent, "═══ [★] REABASTECIMIENTO COMPLETADO ═══", html, new Color(0, 240, 120), "[ CERRAR INFORME DE MISIÓN ]");
    }

    public static void mostrarMisionCompletada(Component parent, String carguero, String estacion, double fuelTransferido, double o2Ignorado) {
        mostrarMisionCompletada(parent, carguero, estacion, fuelTransferido);
    }

    private static void mostrarAlerta(Component parent, String titulo, String cuerpoHtml, Color colorTema, String textoBoton) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = (owner instanceof Frame) ? new JDialog((Frame) owner, true) : new JDialog((Dialog) owner, true);
        dialog.setUndecorated(true);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 14));
        mainPanel.setBackground(new Color(8, 14, 8));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorTema, 2),
            BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));

        JLabel lblHeader = new JLabel(titulo);
        lblHeader.setFont(new Font("Consolas", Font.BOLD, 13));
        lblHeader.setForeground(colorTema);
        lblHeader.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblHeader, BorderLayout.NORTH);

        JLabel lblCuerpo = new JLabel(cuerpoHtml);
        lblCuerpo.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblCuerpo, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);

        JButton btnCerrar = new JButton(textoBoton);
        btnCerrar.setFont(new Font("Consolas", Font.BOLD, 12));
        btnCerrar.setForeground(colorTema);
        btnCerrar.setBackground(new Color(15, 30, 15));
        btnCerrar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorTema, 1),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        btnCerrar.setFocusPainted(false);
        btnCerrar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnCerrar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnCerrar.setBackground(new Color(25, 55, 25));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnCerrar.setBackground(new Color(15, 30, 15));
            }
        });

        btnCerrar.addActionListener(e -> dialog.dispose());
        buttonPanel.add(btnCerrar);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}