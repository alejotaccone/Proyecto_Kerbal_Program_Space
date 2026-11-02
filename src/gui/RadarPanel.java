package gui;

import java.awt.*;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import model.geometry.GeoPosition;
import model.spacecraft.OrbitalObject;
import model.spacecraft.RadarRenderable;

/**
 * Panel personalizado y autónomo que dibuja la pantalla del radar animada.
 * Desacopla la lógica de renderizado 2D y proyección trigonométrica fuera de MainGUI (God Class).
 */
public class RadarPanel extends JPanel {

    private static final Color COLOR_RADAR_BG       = new Color(5, 10, 5);
    private static final Color COLOR_BORDER         = new Color(0, 80, 30);
    private static final Color COLOR_RADAR_LINE     = new Color(0, 255, 80, 200);
    private static final Color COLOR_TEXT_PRIMARY   = new Color(0, 230, 80);
    private static final Color COLOR_TEXT_DIM       = new Color(0, 120, 40);
    private static final Color COLOR_ACCENT_CYAN    = new Color(0, 200, 220);
    private static final Color COLOR_ACCENT_RED     = new Color(255, 60, 60);
    private static final Color COLOR_ACCENT_YELLOW  = new Color(255, 200, 0);
    private static final Color COLOR_ACCENT_ORANGE  = new Color(255, 140, 0);

    // Ángulo actual de la línea de barrido (en radianes)
    private double sweepAngle = 0.0;

    // Referencia a las naves (se establece desde el exterior)
    private List<OrbitalObject> naves;

    // Centro del radar en coordenadas geográficas (para mapear lat/lng a píxeles)
    private GeoPosition centroPos;
    private double centroLat = -34.60;
    private double centroLng = -58.38;
    private double radioKm = 100.0;

    // Índice de la nave seleccionada (-1 si ninguna)
    private int selectedIndex = -1;

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }

    public RadarPanel() {
        setBackground(COLOR_RADAR_BG);
        setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 2));
    }

    /** Avanza el ángulo de barrido para la animación */
    public void avanzarBarrido() {
        sweepAngle += 0.03;  // ~1.7° por frame -> ~6 segundos por rotación completa
        if (sweepAngle >= 2 * Math.PI) {
            sweepAngle -= 2 * Math.PI;
        }
    }

    /** Establece la lista de naves a dibujar */
    public void setNaves(List<OrbitalObject> naves) {
        this.naves = naves;
    }

    /** Actualiza el centro del radar basado en la posición del observador */
    public void setCentro(GeoPosition pos, double radioKm) {
        if (pos != null) {
            this.centroPos = pos;
            this.centroLat = pos.getLatitude();
            this.centroLng = pos.getLongitude();
            this.radioKm = radioKm;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Activar anti-aliasing para gráficos suaves
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int ancho = getWidth();
        int alto = getHeight();
        int centroX = ancho / 2;     // Centro X del radar
        int centroY = alto / 2;     // Centro Y del radar
        int radio = Math.min(centroX, centroY) - 30;  // Radio máximo del radar en píxeles

        // ---- 1. FONDO CON GRADIENTE RADIAL ----
        RadialGradientPaint bgGrad = new RadialGradientPaint(
            centroX, centroY, radio + 50,
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{
                new Color(5, 18, 5),
                new Color(3, 10, 3),
                new Color(0, 5, 0)
            }
        );
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, ancho, alto);

        // ---- 2. CÍRCULOS CONCÉNTRICOS (Anillos de alcance) ----
        g2.setStroke(new BasicStroke(1.0f));
        int numCirculos = 5;
        for (int i = 1; i <= numCirculos; i++) {
            int r = (radio * i) / numCirculos;

            int alpha = 40 + (i * 15);
            g2.setColor(new Color(0, 80, 0, Math.min(alpha, 255)));
            g2.drawOval(centroX - r, centroY - r, r * 2, r * 2);

            // Etiqueta de distancia en km
            int distKm = (int) ((radioKm * i) / numCirculos);
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.setColor(new Color(0, 100, 0, 150));
            g2.drawString(distKm + " km", centroX + r + 4, centroY - 3);
        }

        // ---- 3. LÍNEAS CARDINALES (Cruz del radar) ----
        g2.setColor(new Color(0, 50, 0, 100));
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawLine(centroX, centroY - radio, centroX, centroY + radio);
        g2.drawLine(centroX - radio, centroY, centroX + radio, centroY);

        // Etiquetas N, S, E, O
        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(COLOR_TEXT_DIM);
        g2.drawString("N", centroX - 4, centroY - radio - 5);
        g2.drawString("S", centroX - 4, centroY + radio + 15);
        g2.drawString("E", centroX + radio + 8, centroY + 4);
        g2.drawString("O", centroX - radio - 18, centroY + 4);

        // ---- 4. LÍNEA DE BARRIDO (efecto sweep) ----
        dibujarBarrido(g2, centroX, centroY, radio);

        // ---- 5. CENTRO DEL RADAR (ubicación del observador) ----
        g2.setColor(new Color(0, 255, 100, 200));
        g2.fillOval(centroX - 5, centroY - 5, 10, 10);
        int pulso = (int)(4 * Math.sin(sweepAngle * 2)) + 10;
        g2.setColor(new Color(0, 200, 100, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(centroX - pulso, centroY - pulso, pulso * 2, pulso * 2);

        // ---- 6. BLIPS DE NAVES ----
        dibujarNaves(g2, centroX, centroY, radio);

        // ---- 7. INFORMACIÓN SUPERPUESTA ----
        double cLat = (centroPos != null) ? centroPos.getLatitude() : centroLat;
        double cLng = (centroPos != null) ? centroPos.getLongitude() : centroLng;
        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(COLOR_TEXT_DIM);
        g2.drawString("RADAR v2.0 | Lat: " + String.format("%.2f", cLat) 
            + "° | Lng: " + String.format("%.2f", cLng) + "°", 10, alto - 10);
        g2.drawString("Radio: " + (int)radioKm + " km | Ángulo: " 
            + String.format("%.0f", Math.toDegrees(sweepAngle)) + "°", 10, alto - 25);

        g2.dispose();
    }

    private void dibujarBarrido(Graphics2D g2, int centroX, int centroY, int radio) {
        int trailDegrees = 40;
        for (int i = 0; i < trailDegrees; i++) {
            double angle = sweepAngle - Math.toRadians(i);
            int alpha = (int)(25.0 * (1.0 - (double)i / trailDegrees));
            g2.setColor(new Color(0, 255, 80, Math.max(alpha, 0)));
            g2.setStroke(new BasicStroke(1.0f));
            int endX = centroX + (int)(radio * Math.cos(angle));
            int endY = centroY + (int)(radio * Math.sin(angle));
            g2.drawLine(centroX, centroY, endX, endY);
        }

        g2.setColor(COLOR_RADAR_LINE);
        g2.setStroke(new BasicStroke(2.0f));
        int endX = centroX + (int)(radio * Math.cos(sweepAngle));
        int endY = centroY + (int)(radio * Math.sin(sweepAngle));
        g2.drawLine(centroX, centroY, endX, endY);
    }

    private Point calcularPosicionPantalla(OrbitalObject craft, int centroX, int centroY, int radio) {
        if (craft == null || craft.getPosition() == null) return new Point(centroX, centroY);

        GeoPosition pos = craft.getPosition();
        // Si el objeto es la estación central o coincide con el centro del radar, se ancla exactamente en el centro
        if (craft.tieneAnilloRadar() || (centroPos != null && pos == centroPos)) {
            return new Point(centroX, centroY);
        }

        double cLat = (centroPos != null) ? centroPos.getLatitude() : centroLat;
        double cLng = (centroPos != null) ? centroPos.getLongitude() : centroLng;

        double deltaLat = pos.getLatitude() - cLat;
        double deltaLng = pos.getLongitude() - cLng;

        double kmPerDegLat = 111.0;
        double kmPerDegLng = 111.0 * Math.cos(Math.toRadians(cLat));

        double distXKm = deltaLng * kmPerDegLng;
        double distYKm = -deltaLat * kmPerDegLat;

        double escala = (double) radio / radioKm;
        int pixelX = centroX + (int)(distXKm * escala);
        int pixelY = centroY + (int)(distYKm * escala);

        double distPixel = Math.sqrt((pixelX - centroX) * (pixelX - centroX) + (pixelY - centroY) * (pixelY - centroY));
        if (distPixel > radio) {
            double factor = radio / distPixel;
            pixelX = centroX + (int)((pixelX - centroX) * factor);
            pixelY = centroY + (int)((pixelY - centroY) * factor);
        }

        return new Point(pixelX, pixelY);
    }

    private static class EstiloVisual {
        final Color color;
        final int tamano;
        final boolean esTriangulo;

        EstiloVisual(Color color, int tamano, boolean esTriangulo) {
            this.color = color;
            this.tamano = tamano;
            this.esTriangulo = esTriangulo;
        }
    }

    private EstiloVisual determinarEstiloVisual(RadarRenderable renderable) {
        if (renderable == null) {
            return new EstiloVisual(COLOR_TEXT_PRIMARY, 6, false);
        }
        return new EstiloVisual(renderable.getColorRadar(), renderable.getTamanoIconoRadar(), renderable.esIconoTriangular());
    }

    private void dibujarNaves(Graphics2D g2, int centroX, int centroY, int radio) {
        if (naves == null || naves.isEmpty()) return;

        for (OrbitalObject craft : naves) {
            if (craft.getPosition() == null) continue;

            Point screenPos = calcularPosicionPantalla(craft, centroX, centroY, radio);
            EstiloVisual estilo = determinarEstiloVisual(craft);

            if (craft instanceof model.spacecraft.CargoShip) {
                dibujarTrayectoriaCarguero(g2, (model.spacecraft.CargoShip) craft, screenPos, centroX, centroY, radio);
            } else if (craft instanceof model.spacecraft.RogueDebris) {
                dibujarVectorPredictivoAmenaza(g2, (model.spacecraft.RogueDebris) craft, screenPos, centroX, centroY, radio);
            } else if (craft instanceof model.spacecraft.SpaceDebris) {
                dibujarVectorProximidadBasura(g2, (model.spacecraft.SpaceDebris) craft, screenPos, centroX, centroY, radio);
            }

            dibujarIluminacionBarrido(g2, screenPos.x, screenPos.y, centroX, centroY, estilo);
            dibujarIconoNave(g2, craft, screenPos.x, screenPos.y, estilo);

            if (naves.indexOf(craft) == selectedIndex) {
                dibujarBordeSeleccion(g2, screenPos.x, screenPos.y, estilo);
            }

            dibujarEtiquetaNave(g2, craft, screenPos.x, screenPos.y, estilo);
        }
    }

    private void dibujarIluminacionBarrido(Graphics2D g2, int pixelX, int pixelY, int centroX, int centroY, EstiloVisual estilo) {
        double anguloNave = Math.atan2(pixelY - centroY, pixelX - centroX);
        double diffAngulo = Math.abs(sweepAngle - anguloNave);
        if (diffAngulo > Math.PI) diffAngulo = 2 * Math.PI - diffAngulo;

        if (diffAngulo < 0.3) {
            g2.setColor(new Color(estilo.color.getRed(), estilo.color.getGreen(), estilo.color.getBlue(), 60));
            g2.fillOval(pixelX - estilo.tamano * 2, pixelY - estilo.tamano * 2, estilo.tamano * 4, estilo.tamano * 4);
        }
    }

    private void dibujarTrayectoriaCarguero(Graphics2D g2, model.spacecraft.CargoShip cargo, Point cargoPos, int centroX, int centroY, int radio) {
        if (cargo.getTargetStation() == null || cargo.getTargetStation().getPosition() == null) return;

        if (cargo.getMissionState() == model.spacecraft.MissionState.DESORBITANDO) {
            Stroke strokeOrig = g2.getStroke();
            Color colorOrig = g2.getColor();
            int dx = (cargoPos.x - centroX);
            int dy = (cargoPos.y - centroY);
            double distC = Math.sqrt(dx * dx + dy * dy);
            if (distC > 0) {
                int tailLength = 26;
                int tx = cargoPos.x + (int)(dx / distC * tailLength);
                int ty = cargoPos.y + (int)(dy / distC * tailLength);

                g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(255, 60, 0, 160));
                g2.drawLine(cargoPos.x, cargoPos.y, tx, ty);

                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(255, 210, 40, 230));
                g2.drawLine(cargoPos.x, cargoPos.y, tx - (int)(dx / distC * 8), ty - (int)(dy / distC * 8));
            }
            g2.setStroke(strokeOrig);
            g2.setColor(colorOrig);
            return;
        }
        Point stationPos = calcularPosicionPantalla(cargo.getTargetStation(), centroX, centroY, radio);
        Stroke strokeOriginal = g2.getStroke();
        Color colorOriginal = g2.getColor();
        boolean acoplada = (cargo.getMissionState() != null && cargo.getMissionState().isAcopladaOTransfiriendo());
        if (acoplada) {
            g2.setColor(new Color(0, 255, 120, 220));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(cargoPos.x, cargoPos.y, stationPos.x, stationPos.y);
        } else if (cargo.getMissionState() != null && !cargo.getMissionState().isFinalizada()) {
            g2.setColor(new Color(255, 140, 0, 180));
            Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 6.0f}, 0.0f);
            g2.setStroke(dashed);
            g2.drawLine(cargoPos.x, cargoPos.y, stationPos.x, stationPos.y);
        }
        g2.setStroke(strokeOriginal);
        g2.setColor(colorOriginal);
    }

    private void dibujarVectorPredictivoAmenaza(Graphics2D g2, model.spacecraft.RogueDebris threat, Point threatPos, int centroX, int centroY, int radio) {
        if (threat.getTarget() == null || threat.getTarget().getPosition() == null) return;
        Point targetPos = calcularPosicionPantalla(threat.getTarget(), centroX, centroY, radio);

        double distKm = threat.distanceTo(threat.getTarget());
        int ticksEst = (int) Math.max(1, Math.ceil(distKm / 33.0));

        Stroke strokeOriginal = g2.getStroke();
        Color colorOriginal = g2.getColor();

        boolean zonaCritica = (distKm < 100.0);

        if (zonaCritica) {
            int alphaPulso = (int)(180 + 75 * Math.sin(sweepAngle * 6));
            alphaPulso = Math.max(80, Math.min(255, alphaPulso));
            g2.setColor(new Color(255, 40, 40, alphaPulso));
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.0f, 5.0f}, 0.0f));
        } else {
            g2.setColor(new Color(255, 120, 30, 200));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 6.0f}, 0.0f));
        }

        g2.drawLine(threatPos.x, threatPos.y, targetPos.x, targetPos.y);

        int rImpacto = zonaCritica ? (14 + (int)(4 * Math.sin(sweepAngle * 4))) : 10;
        g2.setColor(zonaCritica ? new Color(255, 40, 40, 220) : new Color(255, 140, 40, 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(targetPos.x - rImpacto, targetPos.y - rImpacto, rImpacto * 2, rImpacto * 2);
        g2.drawLine(targetPos.x - rImpacto - 3, targetPos.y, targetPos.x + rImpacto + 3, targetPos.y);
        g2.drawLine(targetPos.x, targetPos.y - rImpacto - 3, targetPos.x, targetPos.y + rImpacto + 3);

        int midX = (threatPos.x + targetPos.x) / 2;
        int midY = (threatPos.y + targetPos.y) / 2;
        double anguloVector = Math.atan2(targetPos.y - threatPos.y, targetPos.x - threatPos.x);
        int arrowSize = 6;
        int[] xArrow = {
            midX,
            (int) (midX - arrowSize * Math.cos(anguloVector - Math.PI / 6)),
            (int) (midX - arrowSize * Math.cos(anguloVector + Math.PI / 6))
        };
        int[] yArrow = {
            midY,
            (int) (midY - arrowSize * Math.sin(anguloVector - Math.PI / 6)),
            (int) (midY - arrowSize * Math.sin(anguloVector + Math.PI / 6))
        };
        g2.fillPolygon(xArrow, yArrow, 3);

        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        String textoHUD = zonaCritica 
            ? String.format(java.util.Locale.US, "⚠ IMPACTO INMINENTE (%.1fkm | ~%dt)", distKm, ticksEst)
            : String.format(java.util.Locale.US, "RUMBO COLISIÓN: %.1fkm (~%dt)", distKm, ticksEst);
        g2.setColor(zonaCritica ? new Color(255, 60, 60, 240) : new Color(255, 180, 50, 220));
        g2.drawString(textoHUD, midX + 8, midY - 4);

        g2.setStroke(strokeOriginal);
        g2.setColor(colorOriginal);
    }

    private void dibujarVectorProximidadBasura(Graphics2D g2, model.spacecraft.SpaceDebris debris, Point debrisPos, int centroX, int centroY, int radio) {
        if (centroPos == null) return;
        double distKm = (debris.getPosition() != null) ? debris.getPosition().distance3DTo(centroPos) : Double.MAX_VALUE;
        if (distKm <= 180.0) {
            Stroke strokeOrig = g2.getStroke();
            Color colorOrig = g2.getColor();

            g2.setColor(new Color(255, 200, 0, 90));
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f, 6.0f}, 0.0f));
            g2.drawLine(debrisPos.x, debrisPos.y, centroX, centroY);

            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.setColor(new Color(255, 200, 0, 150));
            g2.drawString(String.format(java.util.Locale.US, "Δ %.0fkm", distKm), (debrisPos.x + centroX) / 2 + 4, (debrisPos.y + centroY) / 2);

            g2.setStroke(strokeOrig);
            g2.setColor(colorOrig);
        }
    }

    private void dibujarIconoNave(Graphics2D g2, RadarRenderable renderable, int pixelX, int pixelY, EstiloVisual estilo) {
        if (renderable instanceof model.spacecraft.CargoShip) {
            int s = estilo.tamano + 1;
            int[] xPoints = {pixelX, pixelX + s, pixelX, pixelX - s};
            int[] yPoints = {pixelY - s, pixelY, pixelY + s, pixelY};
            g2.setColor(estilo.color);
            g2.fillPolygon(xPoints, yPoints, 4);
            g2.setColor(new Color(255, 230, 160));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawPolygon(xPoints, yPoints, 4);
            return;
        }
        g2.setColor(estilo.color);
        if (estilo.esTriangulo) {
            int[] xPoints = {pixelX, pixelX - estilo.tamano, pixelX + estilo.tamano};
            int[] yPoints = {pixelY - estilo.tamano, pixelY + estilo.tamano, pixelY + estilo.tamano};
            g2.fillPolygon(xPoints, yPoints, 3);
        } else {
            g2.fillOval(pixelX - estilo.tamano / 2, pixelY - estilo.tamano / 2, estilo.tamano, estilo.tamano);
            if (renderable.tieneAnilloRadar()) {
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(pixelX - estilo.tamano, pixelY - estilo.tamano, estilo.tamano * 2, estilo.tamano * 2);
            }
        }
    }

    private void dibujarBordeSeleccion(Graphics2D g2, int pixelX, int pixelY, EstiloVisual estilo) {
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        int selSize = estilo.tamano + 2;
        if (estilo.esTriangulo) {
            int[] xPointsSel = {pixelX, pixelX - selSize, pixelX + selSize};
            int[] yPointsSel = {pixelY - selSize, pixelY + selSize, pixelY + selSize};
            g2.drawPolygon(xPointsSel, yPointsSel, 3);
        } else {
            g2.drawOval(pixelX - selSize, pixelY - selSize, selSize * 2, selSize * 2);
        }
    }

    private void dibujarEtiquetaNave(Graphics2D g2, OrbitalObject craft, int pixelX, int pixelY, EstiloVisual estilo) {
        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.setColor(new Color(estilo.color.getRed(), estilo.color.getGreen(), estilo.color.getBlue(), 180));

        String labelNombre = craft.getName();
        if (craft instanceof model.spacecraft.CargoShip) {
            model.spacecraft.CargoShip cargo = (model.spacecraft.CargoShip) craft;
            if (cargo.getMissionState() == model.spacecraft.MissionState.DESORBITANDO) {
                labelNombre += String.format(java.util.Locale.US, " [REINGRESO: %.0fkm]", cargo.getPosition().getAltitude());
            }
        }
        if (labelNombre.length() > 24) {
            labelNombre = labelNombre.substring(0, 22) + "…";
        }
        g2.drawString(labelNombre, pixelX + estilo.tamano + 3, pixelY + 3);
    }

    /**
     * Calcula si las coordenadas del ratón coinciden con alguna de las naves dibujadas.
     * Retorna el índice de la nave, o -1 si no se clickeó ninguna.
     */
    public int getShipIndexAt(int mouseX, int mouseY) {
        if (naves == null || naves.isEmpty()) return -1;

        int ancho = getWidth();
        int alto = getHeight();
        int centroX = ancho / 2;
        int centroY = alto / 2;
        int radio = Math.min(centroX, centroY) - 30;

        for (int i = naves.size() - 1; i >= 0; i--) {
            OrbitalObject craft = naves.get(i);
            if (craft.getPosition() == null) continue;

            Point screenPos = calcularPosicionPantalla(craft, centroX, centroY, radio);

            double distToMouse = Math.sqrt((screenPos.x - mouseX) * (screenPos.x - mouseX) + (screenPos.y - mouseY) * (screenPos.y - mouseY));
            if (distToMouse <= 10.0) {
                return i;
            }
        }
        return -1;
    }
}
