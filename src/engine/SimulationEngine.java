package engine;

import api.GeoCodingApiClient;
import api.N2YOApiClient;
import engine.TelemetryLogger;
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
    private int monitoredShipIndex;
    private String lastMonitorAction;

    public SimulationEngine(String apiKey, double radiusKm) {
        this.currentTick = 0;
        this.trackedObjects = new ArrayList<>();
        this.geoCodingClient = new GeoCodingApiClient();
        this.radar = new Radar("Buenos Aires", -34.60, -58.38, 25.0, radiusKm);
        this.apiClient = new N2YOApiClient(apiKey);
        this.monitoredShipIndex = -1;
        this.lastMonitorAction = "";

        setRadarLocationByCity("Buenos Aires", radiusKm);
    }

    // ==================== MONITOR DE NAVE ====================

    /**
     * Selecciona una nave para ser monitoreada en la segunda consola.
     */
    public void selectShipForMonitoring(int index) {
        if (index >= 0 && index < trackedObjects.size()) {
            this.monitoredShipIndex = index;
            this.lastMonitorAction = "Nave seleccionada para monitoreo.";
            updateMonitor();
        }
    }

    /**
     * Actualiza el archivo del monitor con la información actual de la nave seleccionada.
     */
    private void updateMonitor() {
        if (monitoredShipIndex >= 0 && monitoredShipIndex < trackedObjects.size()) {
            Spacecraft ship = trackedObjects.get(monitoredShipIndex);
            TelemetryLogger.writeShipMonitorFile(ship, lastMonitorAction);
        }
    }

    // ==================== RADAR Y N2YO ====================

    /**
     * Cambia la ubicación de la estación terrena / radar consultando la API de Geolocalización,
     * y obtiene de la API N2YO los satélites que están sobrevolando esa ciudad.
     */
    public void setRadarLocationByCity(String cityName, double radiusKm) {
        GeoPosition cityCoords = geoCodingClient.fetchCityCoordinates(cityName);
        this.radar = new Radar(cityName, cityCoords.getLatitude(), cityCoords.getLongitude(), cityCoords.getAltitude(), radiusKm);
        
        List<Spacecraft> satellitesAboveCity = apiClient.fetchSatellitesAbove(cityCoords, radiusKm);
        if (satellitesAboveCity != null && !satellitesAboveCity.isEmpty()) {
            this.trackedObjects = satellitesAboveCity;
            this.monitoredShipIndex = -1; // Resetear selección al cambiar de ciudad
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

    // ==================== CICLO DE SIMULACIÓN ====================

    public void tick() {
        currentTick++;
        TelemetryLogger.printHeader(currentTick, radar.getObserverCity());

        // Lógica de Evento Crítico: 8% de probabilidad de generar un RogueDebris apuntando a una nave
        // (Desactivado temporalmente por pedido del usuario para evitar generación automática)
        // if (trackedObjects.size() > 0 && Math.random() < 0.08) {
        //     triggerCrisisEvent();
        // }

        // Mover cada nave en órbita según sus reglas de polimorfismo
        for (Spacecraft craft : trackedObjects) {
            craft.move();
        }

        // Sincronizar actualización de posiciones N2YO solo cada 10 segundos para evitar Rate Limit
        if (currentTick == 1 || currentTick % 10 == 0) {
            syncWithN2YO();
        }

        // Mostrar estado actual y telemetría por consola
        TelemetryLogger.printSpacecraftStatus(trackedObjects);

        // Escanear alertas de colisión
        List<String> alerts = radar.detectCollisionRisks(trackedObjects);
        TelemetryLogger.printAlerts(alerts);
    }
    
    // ==================== EVENTOS DE CRISIS ====================
    
    public boolean triggerCrisisEvent() {
        if (trackedObjects == null || trackedObjects.isEmpty()) return false;
        
        boolean hasRogue = false;
        for (Spacecraft s : trackedObjects) {
            if (s instanceof model.spacecraft.RogueDebris) {
                hasRogue = true; break;
            }
        }
        
        if (!hasRogue) {
            // Elegir un objetivo al azar que no sea estación o basura
            Spacecraft target = null;
            List<Spacecraft> temp = new ArrayList<>(trackedObjects);
            java.util.Collections.shuffle(temp);
            for (Spacecraft s : temp) {
                if (!(s instanceof SpaceStation) && !(s instanceof model.spacecraft.SpaceDebris)) {
                    target = s;
                    break;
                }
            }
            if (target != null) {
                // Aparecer a ~150km de distancia en una dirección aleatoria
                double angle = Math.random() * 2 * Math.PI;
                double distDeg = 1.5; // aprox 150km
                double spawnLat = target.getPosition().getLatitude() + Math.cos(angle) * distDeg;
                double spawnLng = target.getPosition().getLongitude() + Math.sin(angle) * distDeg;
                
                model.spacecraft.RogueDebris rogue = new model.spacecraft.RogueDebris(
                        "RD-" + (int)(Math.random()*1000), 
                        "ANOMALIA CINETICA", 
                        new GeoPosition(spawnLat, spawnLng, target.getPosition().getAltitude()), 
                        9.5, 
                        target);
                
                trackedObjects.add(rogue);
                TelemetryLogger.printMessage("¡ALERTA DEL SISTEMA! Anomalía detectada entrando al sector del radar.");
                return true;
            }
        }
        return false;
    }
    
    public void removeShip(Spacecraft ship) {
        if (ship != null && trackedObjects.contains(ship)) {
            trackedObjects.remove(ship);
            if (monitoredShipIndex >= trackedObjects.size()) {
                monitoredShipIndex = -1; // Reset monitor if out of bounds
            }
        }
    }

    // ==================== ACCIONES SOBRE NAVES ====================

    public boolean evadeShip(int index) {
        if (index >= 0 && index < trackedObjects.size()) {
            Spacecraft craft = trackedObjects.get(index);
            boolean success = craft.evade(0.5, 0.5);
            if (success) {
                String msg = "MANIOBRA EXITOSA! La nave [" + craft.getName() + "] cambio su orbita para evitar impacto.";
                TelemetryLogger.printMessage(msg);
                this.monitoredShipIndex = index;
                this.lastMonitorAction = msg;
                updateMonitor();
                return true;
            } else {
                String msg = "MANIOBRA FALLIDA! La nave [" + craft.getName() + "] no tiene suficiente combustible.";
                TelemetryLogger.printMessage(msg);
                this.monitoredShipIndex = index;
                this.lastMonitorAction = msg;
                updateMonitor();
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
                        String msg = "Acople exitoso: La nave [" + ship.getName() + "] reposto en la estacion [" + station.getName() + "].";
                        TelemetryLogger.printMessage(msg);
                        this.monitoredShipIndex = shipIndex;
                        this.lastMonitorAction = msg;
                        updateMonitor();
                        return true;
                    }
                }
            }
            String msg = "No hay ninguna Estacion Espacial dentro del radio de acople (500 km) para la nave [" + ship.getName() + "].";
            TelemetryLogger.printMessage(msg);
            this.monitoredShipIndex = shipIndex;
            this.lastMonitorAction = msg;
            updateMonitor();
        }
        return false;
    }

    public void useSpecialAbility(int shipIndex) {
        if (shipIndex >= 0 && shipIndex < trackedObjects.size()) {
            Spacecraft craft = trackedObjects.get(shipIndex);
            String result = craft.performSpecialAbility();
            TelemetryLogger.printMessage(result);
            this.monitoredShipIndex = shipIndex;
            this.lastMonitorAction = result;
            updateMonitor();
        }
    }

    public void toggleShield(int shipIndex) {
        if (shipIndex >= 0 && shipIndex < trackedObjects.size()) {
            Spacecraft craft = trackedObjects.get(shipIndex);
            String msg;
            if (craft.isShieldActive()) {
                craft.deactivateShield();
                msg = "Escudo de la nave [" + craft.getName() + "] DESACTIVADO.";
            } else {
                craft.activateShield();
                msg = "Escudo de la nave [" + craft.getName() + "] ACTIVADO.";
            }
            TelemetryLogger.printMessage(msg);
            this.monitoredShipIndex = shipIndex;
            this.lastMonitorAction = msg;
            updateMonitor();
        }
    }

    // ==================== GETTERS ====================

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

    public int getMonitoredShipIndex() {
        return monitoredShipIndex;
    }
}
