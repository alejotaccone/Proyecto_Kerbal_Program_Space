package gui;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * Servicio utilitario para la carga, validación y escalado proporcional de imágenes
 * de naves espaciales y objetos orbitales.
 * Extrae la responsabilidad de I/O de archivos y renderizado de imágenes fuera de MainGUI (God Class).
 * Implementa un caché de imágenes en memoria RAM para optimizar la fluidez de cada tick.
 */
public class ShipImageLoader {

    private static final Map<String, ImageIcon> imageCache = new ConcurrentHashMap<>();

    public static void cargarImagenNave(JLabel lblImagen, String nombreImagen) {
        if (lblImagen == null) return;

        if (nombreImagen == null || nombreImagen.trim().isEmpty()) {
            lblImagen.setIcon(null);
            lblImagen.setText("[ SIN NAVE SELECCIONADA ]");
            return;
        }

        int targetW = 290;
        int targetH = 170;
        String cacheKey = nombreImagen + "_" + targetW + "_" + targetH;

        // Intentar obtener la imagen pre-escalada desde la memoria RAM (0ms latency)
        ImageIcon cachedIcon = imageCache.get(cacheKey);
        if (cachedIcon != null) {
            lblImagen.setIcon(cachedIcon);
            lblImagen.setText("");
            return;
        }

        // Si no está en caché, cargar desde disco, escalar y guardar en caché
        ImageIcon newIcon = cargarYEscalar(nombreImagen, targetW, targetH);
        if (newIcon != null) {
            imageCache.put(cacheKey, newIcon);
            lblImagen.setIcon(newIcon);
            lblImagen.setText("");
        } else {
            lblImagen.setIcon(null);
            lblImagen.setText("[ " + nombreImagen + " ]");
        }
    }

    public static ImageIcon obtenerImageIconEscalado(String nombreImagen, int targetW, int targetH) {
        if (nombreImagen == null || nombreImagen.trim().isEmpty()) return null;

        String cacheKey = nombreImagen + "_" + targetW + "_" + targetH;
        ImageIcon cachedIcon = imageCache.get(cacheKey);
        if (cachedIcon != null) {
            return cachedIcon;
        }

        ImageIcon newIcon = cargarYEscalar(nombreImagen, targetW, targetH);
        if (newIcon != null) {
            imageCache.put(cacheKey, newIcon);
        }
        return newIcon;
    }

    private static ImageIcon cargarYEscalar(String nombreImagen, int targetW, int targetH) {
        try {
            File file = new File("src/Img/" + nombreImagen);
            if (!file.exists()) {
                file = new File("Img/" + nombreImagen);
            }

            if (file.exists()) {
                BufferedImage imgOriginal = ImageIO.read(file);
                if (imgOriginal != null) {
                    double ratioW = (double) targetW / imgOriginal.getWidth();
                    double ratioH = (double) targetH / imgOriginal.getHeight();
                    double scale = Math.min(ratioW, ratioH);

                    int newW = (int) (imgOriginal.getWidth() * scale);
                    int newH = (int) (imgOriginal.getHeight() * scale);

                    Image scaled = imgOriginal.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }
            }
        } catch (Exception e) {
            // Ignorar errores de carga
        }
        return null;
    }

    public static void limpiarCache() {
        imageCache.clear();
    }
}
