package model.spacecraft;

import model.geometry.GeoPosition;

/**
 * Basura Espacial Hostil (Anomalía de colisión).
 * Esta clase hereda de SpaceDebris pero sobreescribe el movimiento para
 * interceptar activamente a su objetivo simulando una crisis orbital.
 * En la versión 3D, su vector de intercepción converge en latitud, longitud y altitud.
 */
public class RogueDebris extends SpaceDebris {
    
    private OrbitalObject target;

    public RogueDebris(SpacecraftInfo info, GeoPosition position, double hazardLevel, OrbitalObject target) {
        super(info, position, hazardLevel);
        this.target = target;
    }

    @Override
    public void move() {
        if (target != null && target.getPosition() != null && getPosition() != null) {
            double targetLat = target.getPosition().getLatitude();
            double targetLng = target.getPosition().getLongitude();
            double targetAlt = target.getPosition().getAltitude();
            
            double currentLat = getPosition().getLatitude();
            double currentLng = getPosition().getLongitude();
            double currentAlt = getPosition().getAltitude();
            
            double deltaLat = targetLat - currentLat;
            double deltaLng = targetLng - currentLng;
            double deltaAlt = targetAlt - currentAlt;
            
            double distHorizontal = Math.sqrt(deltaLat * deltaLat + deltaLng * deltaLng);
            
            if (distHorizontal > 0) {
                // Se mueve rápidamente hacia el objetivo (~0.3 grados por tick, aprox 30km)
                double step = 0.3;
                if (distHorizontal < step) {
                    // Si está muy cerca en horizontal, converge directamente al blanco
                    getPosition().setLatitude(targetLat);
                    getPosition().setLongitude(targetLng);
                    getPosition().setAltitude(targetAlt);
                } else {
                    double factor = step / distHorizontal;
                    getPosition().setLatitude(currentLat + deltaLat * factor);
                    getPosition().setLongitude(currentLng + deltaLng * factor);
                    // Convergencia de altitud proporcional al avance horizontal
                    getPosition().setAltitude(currentAlt + deltaAlt * Math.min(1.0, factor));
                }
            } else if (Math.abs(deltaAlt) > 0.1) {
                // Si ya está alineado en coordenadas horizontales pero difiere en altitud
                double altStep = 30.0; // km por tick
                if (Math.abs(deltaAlt) < altStep) {
                    getPosition().setAltitude(targetAlt);
                } else {
                    getPosition().setAltitude(currentAlt + Math.signum(deltaAlt) * altStep);
                }
            }
        }
    }

    public OrbitalObject getTarget() {
        return target;
    }
}
