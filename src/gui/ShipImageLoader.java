package gui;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * Servicio utilitario para la carga, validación y escalado proporcional de imágenes
 * de naves espaciales y objetos orbitales.
 * Extrae la responsabilidad de I/O de archivos y renderizado de imágenes fuera de MainGUI,
 * resolviendo el Code Smell de "God Class".
 */
public class ShipImageLoader {

    public static void cargarImagenNave(JLabel lblImagen, String nombreImagen) {
        if (lblImagen == null) return;

        if (nombreImagen == null || nombreImagen.trim().isEmpty()) {
            lblImagen.setIcon(null);
            lblImagen.setText("[ SIN NAVE SELECCIONADA ]");
            return;
        }

        try {
            File file = new File("src/Img/" + nombreImagen);
            if (!file.exists()) {
                file = new File("Img/" + nombreImagen);
            }

            if (file.exists()) {
                BufferedImage imgOriginal = ImageIO.read(file);
                if (imgOriginal != null) {
                    int targetW = 290;
                    int targetH = 170;

                    double ratioW = (double) targetW / imgOriginal.getWidth();
                    double ratioH = (double) targetH / imgOriginal.getHeight();
                    double scale = Math.min(ratioW, ratioH);

                    int newW = (int) (imgOriginal.getWidth() * scale);
                    int newH = (int) (imgOriginal.getHeight() * scale);

                    Image scaled = imgOriginal.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                    lblImagen.setIcon(new ImageIcon(scaled));
                    lblImagen.setText("");
                    return;
                }
            }
        } catch (Exception e) {
            // Fallback en caso de error de lectura
        }

        lblImagen.setIcon(null);
        lblImagen.setText("[ " + nombreImagen + " ]");
    }
}
