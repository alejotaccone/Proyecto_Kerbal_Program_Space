package radar;

import java.util.ArrayList;
import java.util.List;
import model.geometry.GeoPosition;
import model.spacecraft.Spacecraft;
import model.spacecraft.SpaceDebris;

public class Radar {
    private String observerCity;
    private GeoPosition observerPosition;
    private double coverageRadiusKm;

    public Radar(String observerCity, double latitude, double longitude, double altitude, double coverageRadiusKm) {
        this.observerCity = observerCity;
        this.observerPosition = new GeoPosition(latitude, longitude, altitude);
        this.coverageRadiusKm = coverageRadiusKm;
    }

    /**
     * Escanea la lista de objetos espaciales y retorna aquellos dentro del rango del radar.
     */
    public List<Spacecraft> scanCoverageArea(List<Spacecraft> allObjects) {
        List<Spacecraft> detected = new ArrayList<>();
        if (allObjects != null) {
            for (Spacecraft craft : allObjects) {
                double distance = observerPosition.distanceTo(craft.getPosition());
                if (distance <= coverageRadiusKm) {
                    detected.add(craft);
                }
            }
        }
        return detected;
    }

    /**
     * Evalúa potenciales riesgos de colisión o alertas entre objetos espaciales.
     */
    public List<String> detectCollisionRisks(List<Spacecraft> allObjects) {
        List<String> alerts = new ArrayList<>();
        if (allObjects == null || allObjects.size() < 2) return alerts;

        for (int i = 0; i < allObjects.size(); i++) {
            for (int j = i + 1; j < allObjects.size(); j++) {
                Spacecraft naveA = allObjects.get(i);
                Spacecraft naveB = allObjects.get(j);

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

    public double getCoverageRadiusKm() {
        return coverageRadiusKm;
    }
}
