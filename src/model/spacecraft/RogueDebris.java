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
            
            double distKm = distanceTo(target);
            if (distKm > 0.1) {
                // Avanza ~35 km por tick directamente hacia el objetivo en el espacio 3D
                double stepKm = 35.0;
                if (distKm <= stepKm) {
                    getPosition().setLatitude(targetLat);
                    getPosition().setLongitude(targetLng);
                    getPosition().setAltitude(targetAlt);
                } else {
                    double factor = stepKm / distKm;
                    getPosition().setLatitude(currentLat + deltaLat * factor);
                    getPosition().setLongitude(currentLng + deltaLng * factor);
                    getPosition().setAltitude(currentAlt + deltaAlt * factor);
                }
            }
        }
    }

    public OrbitalObject getTarget() {
        return target;
    }
}
