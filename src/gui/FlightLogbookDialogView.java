package gui;

import engine.FlightLogbook;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Ventana modal táctica que presenta la Bitácora de Misión y Estadísticas de Vuelo
 * del operador en tiempo real, con capacidad de exportación directa a archivos .txt y .json.
 */
public class FlightLogbookDialogView {

    private static final Color COLOR_BG_MODAL    = new Color(8, 14, 10);
    private static final Color COLOR_BORDER      = new Color(0, 200, 120);
    private static final Color COLOR_ACCENT_CYAN = new Color(0, 210, 230);
    private static final Color COLOR_ACCENT_GOLD = new Color(255, 195, 0);
    private static final Color COLOR_TEXT_GREEN  = new Color(0, 240, 100);

    public static void mostrarBitacora(Component parent, FlightLogbook logbook, Consumer<String> logConsola) {
        if (logbook == null) {
            logbook = new FlightLogbook();
        }
        final FlightLogbook datos = logbook;

        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = (owner instanceof Frame) ? new JDialog((Frame) owner, true) : new JDialog((Dialog) owner, true);
        dialog.setUndecorated(true);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 14));
        mainPanel.setBackground(COLOR_BG_MODAL);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 2),
            BorderFactory.createEmptyBorder(18, 22, 18, 22)
        ));

        // ---- ENCABEZADO ----
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("═══ [📋] BITÁCORA DE MISIÓN Y ESTADÍSTICAS DE VUELO ═══");
        lblTitle.setFont(new Font("Consolas", Font.BOLD, 14));
        lblTitle.setForeground(COLOR_TEXT_GREEN);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel(String.format(
            "Estación: %s  |  Ticks Operados: %d",
            datos.getEstacionActiva(), datos.getTicksOperados()
        ));
        lblSub.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblSub.setForeground(COLOR_ACCENT_CYAN);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(lblSub);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ---- CUERPO CON TABLA DE MÉTRICAS TÁCTICAS ----
        String htmlResumen = construirHtmlEstadisticas(datos);
        JLabel lblCuerpo = new JLabel(htmlResumen);
        lblCuerpo.setHorizontalAlignment(SwingConstants.CENTER);
        lblCuerpo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 80, 40), 1),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        mainPanel.add(lblCuerpo, BorderLayout.CENTER);

        // ---- PANEL INFERIOR: FEEDBACK + BOTONES DE EXPORTACIÓN Y CIERRE ----
        JPanel southContainer = new JPanel();
        southContainer.setLayout(new BoxLayout(southContainer, BoxLayout.Y_AXIS));
        southContainer.setOpaque(false);

        JLabel lblFeedbackExport = new JLabel("Selecciona un formato para exportar el reporte táctico a disco:");
        lblFeedbackExport.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblFeedbackExport.setForeground(new Color(140, 200, 150));
        lblFeedbackExport.setAlignmentX(Component.CENTER_ALIGNMENT);
        southContainer.add(lblFeedbackExport);
        southContainer.add(Box.createVerticalStrut(8));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnExportTxt = crearBotonNaval("[⬇] EXPORTAR .TXT", COLOR_ACCENT_CYAN);
        JButton btnExportJson = crearBotonNaval("[⬇] EXPORTAR .JSON", COLOR_ACCENT_GOLD);
        JButton btnCerrar = crearBotonNaval("[ CERRAR BITÁCORA ]", COLOR_TEXT_GREEN);

        btnExportTxt.addActionListener(e -> exportarArchivo(
            dialog,
            datos.generarReporteTxt(),
            "reporte_mision_kps.txt",
            "Archivos de Texto (*.txt)",
            "txt",
            lblFeedbackExport,
            logConsola
        ));

        btnExportJson.addActionListener(e -> exportarArchivo(
            dialog,
            datos.generarReporteJson(),
            "reporte_mision_kps.json",
            "Archivos JSON (*.json)",
            "json",
            lblFeedbackExport,
            logConsola
        ));

        btnCerrar.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnExportTxt);
        btnPanel.add(btnExportJson);
        btnPanel.add(btnCerrar);
        southContainer.add(btnPanel);

        mainPanel.add(southContainer, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static String construirHtmlEstadisticas(FlightLogbook d) {
        String colorPrecision = (d.getPorcentajePrecisionAcople() >= 70.0) ? "#00FF88"
                : (d.getPorcentajePrecisionAcople() >= 40.0 ? "#FFCC00" : "#FF5555");
        String colorImpactos = (d.getImpactosRecibidos() == 0) ? "#00FF88" : "#FF4444";

        return String.format(Locale.US,
            "<html><div style='font-family: Consolas; font-size: 11px; color: #D0FFD8; width: 440px;'>" +
            "<div style='text-align: center; margin-bottom: 8px;'>" +
            "Rango Operativo: <b style='color: #00FFFF;'>%s</b></div>" +

            "<div style='color: #00E650; border-bottom: 1px solid #005522; margin-top: 6px; margin-bottom: 4px;'>" +
            "<b>&gt;&gt; 1. LOGÍSTICA Y REABASTECIMIENTO ORBITAL</b></div>" +
            "<table width='100%%' cellspacing='2' cellpadding='1' style='font-family: Consolas; font-size: 11px; color: #CCCCCC;'>" +
            "<tr><td>• Total Combustible Reabastecido:</td><td align='right'><b style='color: #FFB400;'>+%.1f L</b></td></tr>" +
            "<tr><td>• Cargueros Lanzados / Completados:</td><td align='right'><b style='color: #00FF88;'>%d lanzados / %d exitosos</b></td></tr>" +
            "<tr><td>• Misiones Abortadas:</td><td align='right'><b style='color: #FF7755;'>%d</b></td></tr>" +
            "</table>" +

            "<div style='color: #00E650; border-bottom: 1px solid #005522; margin-top: 10px; margin-bottom: 4px;'>" +
            "<b>&gt;&gt; 2. PRECISIÓN EN MINIJUEGO DE ACOPLE</b></div>" +
            "<table width='100%%' cellspacing='2' cellpadding='1' style='font-family: Consolas; font-size: 11px; color: #CCCCCC;'>" +
            "<tr><td>• Porcentaje de Precisión de Acople:</td><td align='right'><b style='color: %s;'>%.1f%%</b></td></tr>" +
            "<tr><td>• Acoples Exitosos vs Intentos Fallidos:</td><td align='right'><b style='color: #FFFFFF;'>%d aciertos / %d fallos (%d tot.)</b></td></tr>" +
            "</table>" +

            "<div style='color: #00E650; border-bottom: 1px solid #005522; margin-top: 10px; margin-bottom: 4px;'>" +
            "<b>&gt;&gt; 3. DEFENSA Y EVASIÓN DE IMPACTOS</b></div>" +
            "<table width='100%%' cellspacing='2' cellpadding='1' style='font-family: Consolas; font-size: 11px; color: #CCCCCC;'>" +
            "<tr><td>• Evasiones Exitosas Totales:</td><td align='right'><b style='color: #00FFFF;'>%d maniobras</b></td></tr>" +
            "<tr><td>• Impactos Críticos Evitados (Crisis):</td><td align='right'><b style='color: #00FF88;'>%d evitados</b> (Normales: %d)</td></tr>" +
            "<tr><td>• Impactos Cinéticos Recibidos:</td><td align='right'><b style='color: %s;'>%d impactos</b></td></tr>" +
            "<tr><td>• Combustible Gastado en Evasiones:</td><td align='right'><b style='color: #FFCC00;'>%.1f L</b></td></tr>" +
            "</table>" +
            "</div></html>",
            d.getCalificacionOperador(),
            d.getTotalCombustibleReabastecido(),
            d.getCarguerosLanzados(), d.getMisionesCompletadas(),
            d.getMisionesAbortadas(),
            colorPrecision, d.getPorcentajePrecisionAcople(),
            d.getAcoplesExitosos(), d.getIntentosAcopleFallidos(), d.getIntentosAcopleTotales(),
            d.getTotalEvasionesExitosas(),
            d.getEvasionesCrisisExitosas(), d.getEvasionesNormalesExitosas(),
            colorImpactos, d.getImpactosRecibidos(),
            d.getCombustibleConsumidoEvasiones()
        );
    }

    private static void exportarArchivo(
            Component parent,
            String contenido,
            String nombreSugerido,
            String descripcionFiltro,
            String extension,
            JLabel lblFeedback,
            Consumer<String> logConsola) {

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String baseName = nombreSugerido.replace("." + extension, "_" + timestamp + "." + extension);

            JFileChooser chooser = new JFileChooser(new File("."));
            chooser.setDialogTitle("Exportar Reporte Táctico de Misión (." + extension.toUpperCase() + ")");
            chooser.setSelectedFile(new File(baseName));
            chooser.setFileFilter(new FileNameExtensionFilter(descripcionFiltro, extension));

            int userSelection = chooser.showSaveDialog(parent);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File archivoDestino = chooser.getSelectedFile();
                if (!archivoDestino.getName().toLowerCase().endsWith("." + extension)) {
                    archivoDestino = new File(archivoDestino.getAbsolutePath() + "." + extension);
                }

                Files.writeString(archivoDestino.toPath(), contenido, StandardCharsets.UTF_8);

                lblFeedback.setText("✔ Reporte exportado: " + archivoDestino.getName());
                lblFeedback.setForeground(COLOR_TEXT_GREEN);

                if (logConsola != null) {
                    logConsola.accept("[BITÁCORA]: Reporte táctico exportado exitosamente en: " + archivoDestino.getAbsolutePath());
                }
            }
        } catch (Exception ex) {
            lblFeedback.setText("✖ Error al exportar reporte: " + ex.getMessage());
            lblFeedback.setForeground(new Color(255, 80, 80));
        }
    }

    private static JButton crearBotonNaval(String texto, Color colorTexto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Consolas", Font.BOLD, 11));
        btn.setForeground(colorTexto);
        btn.setBackground(new Color(12, 28, 16));
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setPreferredSize(new Dimension(165, 34));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorTexto, 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(25, 60, 35));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(12, 28, 16));
            }
        });
        return btn;
    }
}
