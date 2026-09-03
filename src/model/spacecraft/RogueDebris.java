package model.spacecraft;

import model.geometry.GeoPosition;

/**
 * Basura Espacial Hostil (Anomalía de colisión).
 * Esta clase hereda de SpaceDebris pero sobreescribe el movimiento para
 * interceptar activamente a su objetivo simulando una crisis.
 * Utiliza getPosition() para respetar el encapsulamiento de su superclase.
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
            
            double currentLat = getPosition().getLatitude();
            double currentLng = getPosition().getLongitude();
            
            double deltaLat = targetLat - currentLat;
            double deltaLng = targetLng - currentLng;
            
            double dist = Math.sqrt(deltaLat * deltaLat + deltaLng * deltaLng);
            
            if (dist > 0) {
                // Se mueve rápidamente hacia el objetivo (~0.3 grados por tick, aprox 30km)
                double step = 0.3;
                if (dist < step) {
                    // Si está muy cerca, salta directamente a la posición
                    getPosition().setLatitude(targetLat);
                    getPosition().setLongitude(targetLng);
                } else {
                    getPosition().setLatitude(currentLat + (deltaLat / dist) * step);
                    getPosition().setLongitude(currentLng + (deltaLng / dist) * step);
                }
            }
        }
    }

    public OrbitalObject getTarget() {
        return target;
    }
}
