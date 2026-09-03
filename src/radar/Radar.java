package radar;

import java.util.ArrayList;
import java.util.List;
import model.geometry.GeoPosition;
import model.spacecraft.OrbitalObject;
import model.spacecraft.SpaceDebris;

public class Radar {
    private String observerCity;
    private GeoPosition observerPosition;
    private double coverageRadiusKm;

    public Radar(String observerCity, GeoPosition observerPosition, double coverageRadiusKm) {
        this.observerCity = observerCity;
        this.observerPosition = (observerPosition != null) ? observerPosition : new GeoPosition(0.0, 0.0, 400.0);
        this.coverageRadiusKm = coverageRadiusKm;
    }

    /**
     * Evalúa potenciales riesgos de colisión o alertas entre objetos espaciales.
     */
    public List<String> detectCollisionRisks(List<OrbitalObject> allObjects) {
        List<String> alerts = new ArrayList<>();
        if (allObjects == null || allObjects.size() < 2) return alerts;

        for (int i = 0; i < allObjects.size(); i++) {
            for (int j = i + 1; j < allObjects.size(); j++) {
                OrbitalObject naveA = allObjects.get(i);
                OrbitalObject naveB = allObjects.get(j);

                double distance = naveA.getPosition().distanceTo(naveB.getPosition());
                if (distance < 200.0) { // Menos de 200 km en órbita es alerta de colisión
                    String alert = String.format("¡ALERTA DE COLISIÓN! [%s] y [%s] a sólo %.1f km de distancia.", 
                            naveA.getName(), naveB.getName(), distance);
                    if (naveA instanceof SpaceDebris || naveB instanceof SpaceDebris) {
                        alert += " (Involucra Basura Espacial en trayectoria cinetica)";
                    }
                    alerts.add(alert);
                }
            }
        }
        return alerts;
    }

    public String getObserverCity() {
        return observerCity;
    }

    public GeoPosition getObserverPosition() {
        return observerPosition;
    }

    /**
     * Actualiza la posición geográfica del observador / centro del radar.
     * Respeta el principio de encapsulamiento (Tell, Don't Ask).
     */
    public void actualizarUbicacion(GeoPosition nuevaPosicion) {
        if (nuevaPosicion != null) {
            if (this.observerPosition == null) {
                this.observerPosition = new GeoPosition(nuevaPosicion.getLatitude(), nuevaPosicion.getLongitude(), nuevaPosicion.getAltitude());
            } else {
                this.observerPosition.setLatitude(nuevaPosicion.getLatitude());
                this.observerPosition.setLongitude(nuevaPosicion.getLongitude());
                this.observerPosition.setAltitude(nuevaPosicion.getAltitude());
            }
        }
    }

    public double getCoverageRadiusKm() {
        return coverageRadiusKm;
    }
}
