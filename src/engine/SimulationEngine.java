package engine;

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

    public SimulationEngine(String apiKey) {
        this.currentTick = 0;
        this.trackedObjects = new ArrayList<>();
        this.radar = new Radar("Estacion Terrena Buenos Aires", -34.60, -58.38, 25.0, 1000.0);
        this.apiClient = new N2YOApiClient(apiKey);

        initializeDefaultShips();
        syncWithN2YO();
    }

    private void initializeDefaultShips() {
        // 1. Nave de Carga de la misión Kerbal
        CargoShip cargo = new CargoShip("KSP-CARGO-1", "Kerbal Heavy Cargo", 99001,
                new GeoPosition(-34.50, -58.30, 415.0),
                new FuelTank(200.0, 150.0, 4.0), 20.0);
        trackedObjects.add(cargo);

        // 2. Sonda Exploradora
        ExplorationProbe probe = new ExplorationProbe("KSP-PROBE-1", "Sonda Voyager-K", 99002,
                new GeoPosition(-34.55, -58.25, 410.0), // Cercana para generar posible alerta inicial
                new FuelTank(100.0, 80.0, 2.0), 0.9);
        trackedObjects.add(probe);

        // 3. Transbordador con tripulantes Kerbal
        CrewShuttle shuttle = new CrewShuttle("KSP-CREW-1", "Kerbal Express-1", 99003,
                new GeoPosition(-35.10, -59.00, 430.0),
                new FuelTank(150.0, 120.0, 3.0));
        shuttle.addCrewMember(new Kerbal("Jebediah Kerman", "PILOT", 95));
        shuttle.addCrewMember(new Kerbal("Bill Kerman", "ENGINEER", 70));
        shuttle.addCrewMember(new Kerbal("Bob Kerman", "SCIENTIST", 40));
        trackedObjects.add(shuttle);
    }

    public void syncWithN2YO() {
        List<Spacecraft> liveObjects = apiClient.fetchLiveSpaceObjects();
        for (Spacecraft liveObj : liveObjects) {
            // Evitar duplicados por NORAD ID
            boolean exists = false;
            for (Spacecraft existing : trackedObjects) {
                if (existing.getNoradId() == liveObj.getNoradId()) {
                    existing.getPosition().setLatitude(liveObj.getPosition().getLatitude());
                    existing.getPosition().setLongitude(liveObj.getPosition().getLongitude());
                    existing.getPosition().setAltitude(liveObj.getPosition().getAltitude());
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                trackedObjects.add(liveObj);
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

        // Mostrar estado actual por consola
        List<Spacecraft> inRadarRange = radar.scanCoverageArea(trackedObjects);
        TelemetryLogger.printSpacecraftStatus(inRadarRange);

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
