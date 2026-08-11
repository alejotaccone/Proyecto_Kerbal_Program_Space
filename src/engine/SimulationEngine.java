package engine;

import api.GeoCodingApiClient;
import api.N2YOApiClient;
import java.util.ArrayList;
import java.util.List;
import model.components.FuelTank;
import model.components.Kerbal;
import model.geometry.GeoPosition;
import model.spacecraft.CargoShip;
import model.spacecraft.CrewShuttle;
import model.spacecraft.ExplorationProbe;
import model.spacecraft.SpaceStation;
import model.spacecraft.Spacecraft;
import radar.Radar;

public class SimulationEngine {
    private int currentTick;
    private List<Spacecraft> trackedObjects;
    private Radar radar;
    private N2YOApiClient apiClient;
    private GeoCodingApiClient geoCodingClient;

    public SimulationEngine(String apiKey) {
        this.currentTick = 0;
        this.trackedObjects = new ArrayList<>();
        this.geoCodingClient = new GeoCodingApiClient();
        this.radar = new Radar("Buenos Aires", -34.60, -58.38, 25.0, 100.0);
        this.apiClient = new N2YOApiClient(apiKey);

        setRadarLocationByCity("Buenos Aires");
    }

    /**
     * Cambia la ubicación de la estación terrena / radar consultando la API de Geolocalización,
     * y obtiene de la API N2YO los satélites que están sobrevolando esa ciudad en un radio de 100 km.
     */
    public void setRadarLocationByCity(String cityName) {
        GeoPosition cityCoords = geoCodingClient.fetchCityCoordinates(cityName);
        this.radar = new Radar(cityName, cityCoords.getLatitude(), cityCoords.getLongitude(), cityCoords.getAltitude(), 100.0);
        
        List<Spacecraft> satellitesAboveCity = apiClient.fetchSatellitesAbove(cityCoords, 100.0);
        if (satellitesAboveCity != null && !satellitesAboveCity.isEmpty()) {
            this.trackedObjects = satellitesAboveCity;
            TelemetryLogger.printMessage("Radar reubicado exitosamente en " + cityName.toUpperCase() 
                    + " (" + cityCoords.toString() + "). Se detectaron " + trackedObjects.size() + " satélites reales sobrevolando el área.");
        }
    }

    public void syncWithN2YO() {
        if (trackedObjects == null || trackedObjects.isEmpty()) return;
        for (Spacecraft craft : trackedObjects) {
            GeoPosition updatedPos = apiClient.fetchRealSatellitePosition(
                    craft.getNoradId(), 
                    craft.getPosition().getLatitude(), 
                    craft.getPosition().getLongitude(), 
                    craft.getPosition().getAltitude()
            );
            if (updatedPos != null) {
                craft.getPosition().setLatitude(updatedPos.getLatitude());
                craft.getPosition().setLongitude(updatedPos.getLongitude());
                craft.getPosition().setAltitude(updatedPos.getAltitude());
            }
        }
    }

    public void tick() {
        currentTick++;
        TelemetryLogger.printHeader(currentTick, radar.getObserverCity());

        // Mover cada nave en órbita según sus reglas de polimorfismo
        for (Spacecraft craft : trackedObjects) {
            craft.move();
        }

        // Sincronizar actualización de posiciones N2YO
        syncWithN2YO();

        // Mostrar estado actual y telemetría por consola
        TelemetryLogger.printSpacecraftStatus(trackedObjects);

        // Escanear alertas de colisión
        List<String> alerts = radar.detectCollisionRisks(trackedObjects);
        TelemetryLogger.printAlerts(alerts);
    }

    public boolean evadeShip(int index) {
        if (index >= 0 && index < trackedObjects.size()) {
            Spacecraft craft = trackedObjects.get(index);
            boolean success = craft.evade(0.5, 0.5); // Desplazamiento de maniobra de evasión
            if (success) {
                TelemetryLogger.printMessage("¡MANIOBRA EXITOSA! La nave [" + craft.getName() + "] cambió su órbita para evitar impacto.");
                return true;
            } else {
                TelemetryLogger.printMessage("¡MANIOBRA FALLIDA! La nave [" + craft.getName() + "] no tiene suficiente combustible.");
            }
        }
        return false;
    }

    public boolean refuelShipAtStation(int shipIndex) {
        if (shipIndex >= 0 && shipIndex < trackedObjects.size()) {
            Spacecraft ship = trackedObjects.get(shipIndex);
            for (Spacecraft craft : trackedObjects) {
                if (craft instanceof SpaceStation) {
                    SpaceStation station = (SpaceStation) craft;
                    if (station.refuelShip(ship)) {
                        TelemetryLogger.printMessage("Acople exitoso: La nave [" + ship.getName() + "] repostó en la estación [" + station.getName() + "].");
                        return true;
                    }
                }
            }
            TelemetryLogger.printMessage("No hay ninguna Estación Espacial dentro del radio de acople (500 km) para la nave [" + ship.getName() + "].");
        }
        return false;
    }

    public void useSpecialAbility(int shipIndex) {
        if (shipIndex >= 0 && shipIndex < trackedObjects.size()) {
            Spacecraft craft = trackedObjects.get(shipIndex);
            String result = craft.performSpecialAbility();
            TelemetryLogger.printMessage(result);
        }
    }

    public void toggleShield(int shipIndex) {
        if (shipIndex >= 0 && shipIndex < trackedObjects.size()) {
            Spacecraft craft = trackedObjects.get(shipIndex);
            if (craft.isShieldActive()) {
                craft.deactivateShield();
                TelemetryLogger.printMessage("Escudo de la nave [" + craft.getName() + "] DESACTIVADO.");
            } else {
                craft.activateShield();
                TelemetryLogger.printMessage("Escudo de la nave [" + craft.getName() + "] ACTIVADO.");
            }
        }
    }

    public List<Spacecraft> getTrackedObjects() {
        return trackedObjects;
    }

    public Radar getRadar() {
        return radar;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public N2YOApiClient getApiClient() {
        return apiClient;
    }
}
