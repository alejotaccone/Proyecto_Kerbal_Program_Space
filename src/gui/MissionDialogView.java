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
            "<span style='color: #FFFFFF;'>Iniciando bombeo presurizado de combustible y oxígeno...</span>" +
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

    public static void mostrarMisionCompletada(Component parent, String carguero, String estacion, double fuelTransferido, double o2Transferido) {
        String html = String.format(
            "<html><div style='text-align: center; font-family: Consolas; font-size: 11px; color: #00FF88; width: 360px;'>" +
            "La transferencia de suministros hacia <b style='color: #00FFFF;'>[%s]</b> ha concluido satisfactoriamente.<br><br>" +
            "<table style='margin: 0 auto; color: #FFFFFF; font-family: Consolas; font-size: 11px;'>" +
            "<tr><td align='right'>Combustible suministrado:</td><td align='left'><b style='color: #FFB400;'>+%.1f L</b></td></tr>" +
            "<tr><td align='right'>Soporte vital (O2):</td><td align='left'><b style='color: #00FF66;'>+%.1f %%</b></td></tr>" +
            "</table><br>" +
            "El carguero <b style='color: #FFB400;'>[%s]</b> ha desacoplado de manera segura y completó su ciclo de misión." +
            "</div></html>",
            estacion, fuelTransferido, o2Transferido, carguero
        );
        mostrarAlerta(parent, "═══ [★] REABASTECIMIENTO COMPLETADO ═══", html, new Color(0, 240, 120), "[ CERRAR INFORME DE MISIÓN ]");
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

        JLabel lblBody = new JLabel(cuerpoHtml);
        lblBody.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(lblBody, BorderLayout.CENTER);

        JButton btnOk = crearBotonTactico(textoBoton, colorTema);
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

    private static JButton crearBotonTactico(String texto, Color colorTexto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Consolas", Font.BOLD, 12));
        btn.setForeground(colorTexto);
        btn.setBackground(new Color(15, 25, 15));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorTexto, 1),
            BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(30, 50, 30));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(15, 25, 15));
            }
        });
        return btn;
    }
}
