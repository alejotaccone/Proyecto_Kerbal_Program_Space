package engine;


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
import model.spacecraft.OrbitalObject;
import model.spacecraft.SpaceStation;
import model.spacecraft.Spacecraft;
import model.spacecraft.SpacecraftInfo;
import radar.Radar;

public class SimulationEngine {
    private int currentTick;
    private List<OrbitalObject> trackedObjects;
    private Radar radar;
    private N2YOApiClient apiClient;
    private int monitoredShipIndex;
    private String lastMonitorAction;
    private int targetStationNoradId;
    private OrbitalEventSpawner eventSpawner;

    public SimulationEngine(String apiKey, int targetNoradId) {
        this.currentTick = 0;
        this.trackedObjects = new ArrayList<>();
        this.apiClient = new N2YOApiClient(apiKey);
        this.eventSpawner = new OrbitalEventSpawner();
        this.monitoredShipIndex = -1;
        this.lastMonitorAction = "";
        // Inicializar el radar con coordenadas 0,0 por defecto
        this.radar = new Radar("Estación", new GeoPosition(0.0, 0.0, 400.0), 100.0);

        setTargetStation(targetNoradId);
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
            OrbitalObject ship = trackedObjects.get(monitoredShipIndex);
            TelemetryLogger.writeShipMonitorFile(ship, lastMonitorAction);
        }
    }

    // ==================== RADAR Y N2YO ====================

    /**
     * Establece la Estación Espacial objetivo y la obtiene de la API.
     */
    public void setTargetStation(int noradId) {
        this.targetStationNoradId = noradId;
        
        OrbitalObject station = apiClient.fetchRealSatellite(noradId);
        
        if (station != null) {
            this.trackedObjects.clear();
            this.trackedObjects.add(station);
            
            // Actualizar el "radar" local para que se centre en la estación
            GeoPosition pos = station.getPosition();
            this.radar = new Radar(station.getName(), pos, 100.0);
            
            this.monitoredShipIndex = 0; // Seleccionar automáticamente la estación
            TelemetryLogger.printMessage("Centro de Comando conectado exitosamente con: " + station.getName() 
                    + " (" + pos.toString() + ").");
        } else {
            TelemetryLogger.printMessage("ERROR: No se pudo conectar con la estación NORAD ID: " + noradId);
        }
    }

    private volatile boolean isSyncingN2YO = false;

    public void syncWithN2YOAsync() {
        if (isSyncingN2YO || trackedObjects == null || trackedObjects.isEmpty()) return;
        isSyncingN2YO = true;
        
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                syncWithN2YO();
            } finally {
                isSyncingN2YO = false;
            }
        });
    }

    public void syncWithN2YO() {
        if (trackedObjects == null || trackedObjects.isEmpty()) return;
        for (OrbitalObject craft : trackedObjects) {
            GeoPosition updatedPos = apiClient.fetchRealSatellitePosition(
                    craft.getNoradId(), 
                    craft.getPosition()
            );
            if (updatedPos != null) {
                craft.getPosition().setLatitude(updatedPos.getLatitude());
                craft.getPosition().setLongitude(updatedPos.getLongitude());
                craft.getPosition().setAltitude(updatedPos.getAltitude());
                
                actualizarPosicionRadar(craft);
            }
        }
    }

    /**
     * Actualiza el centro del radar local para que siga la posición de la estación objetivo.
     */
    private void actualizarPosicionRadar(OrbitalObject craft) {
        if (craft != null && craft.getNoradId() == targetStationNoradId && radar != null && craft.getPosition() != null) {
            radar.getObserverPosition().setLatitude(craft.getPosition().getLatitude());
            radar.getObserverPosition().setLongitude(craft.getPosition().getLongitude());
            radar.getObserverPosition().setAltitude(craft.getPosition().getAltitude());
        }
    }

    // ==================== CICLO DE SIMULACIÓN ====================

    public void tick() {
        avanzarReloj();
        moverObjetos();
        generarEventosAleatorios();
        sincronizarRedSiCorresponde();
        reportarTelemetriaYAlertas();
    }

    private void avanzarReloj() {
        currentTick++;
        TelemetryLogger.printHeader(currentTick, radar.getObserverCity());
    }

    private void moverObjetos() {
        for (OrbitalObject craft : trackedObjects) {
            craft.move();
            actualizarPosicionRadar(craft);
        }
    }

    private void generarEventosAleatorios() {
        eventSpawner.evaluarEventosAleatoriosPorTick(trackedObjects);
    }

    private void sincronizarRedSiCorresponde() {
        if (currentTick == 1 || currentTick % 10 == 0) {
            syncWithN2YOAsync();
        }
    }

    private void reportarTelemetriaYAlertas() {
        TelemetryLogger.printSpacecraftStatus(trackedObjects);
        List<String> alerts = radar.detectCollisionRisks(trackedObjects);
        TelemetryLogger.printAlerts(alerts);
    }
    
    // ==================== EVENTOS DE CRISIS Y BASURA ESPACIAL ====================

    public boolean spawnSpaceDebris() {
        return eventSpawner.spawnSpaceDebris(trackedObjects);
    }

    public boolean triggerCrisisEvent() {
        return eventSpawner.triggerCrisisEvent(trackedObjects);
    }
    
    public void removeShip(OrbitalObject ship) {
        if (ship != null && trackedObjects.contains(ship)) {
            trackedObjects.remove(ship);
            if (monitoredShipIndex >= trackedObjects.size()) {
                monitoredShipIndex = -1; // Reset monitor if out of bounds
            }
        }
    }

    // ==================== ACCIONES SOBRE NAVES ====================

    public boolean evadeShip(int index) {
        if (!isIndiceValido(index)) return false;

        OrbitalObject craft = trackedObjects.get(index);
        if (!(craft instanceof Spacecraft)) {
            notificarYActualizarMonitor(index, "MANIOBRA IMPOSIBLE! El objeto [" + craft.getName() + "] no es una nave con propulsión propia.");
            return false;
        }

        Spacecraft ship = (Spacecraft) craft;
        boolean success = ship.evade(0.5, 0.5);
        String msg = success 
                ? "MANIOBRA EXITOSA! La nave [" + ship.getName() + "] cambio su orbita para evitar impacto."
                : "MANIOBRA FALLIDA! La nave [" + ship.getName() + "] no tiene suficiente combustible.";
        notificarYActualizarMonitor(index, msg);
        return success;
    }

    public boolean refuelShipAtStation(int shipIndex) {
        if (!isIndiceValido(shipIndex)) return false;

        OrbitalObject targetObj = trackedObjects.get(shipIndex);
        if (!(targetObj instanceof Spacecraft)) {
            notificarYActualizarMonitor(shipIndex, "El objeto orbital [" + targetObj.getName() + "] no posee tanque de combustible para repostar.");
            return false;
        }

        Spacecraft ship = (Spacecraft) targetObj;
        for (OrbitalObject craft : trackedObjects) {
            if (craft instanceof SpaceStation) {
                SpaceStation station = (SpaceStation) craft;
                if (station.refuelShip(ship)) {
                    notificarYActualizarMonitor(shipIndex, "Acople exitoso: La nave [" + ship.getName() + "] reposto en la estacion [" + station.getName() + "].");
                    return true;
                }
            }
        }

        notificarYActualizarMonitor(shipIndex, "No hay ninguna Estacion Espacial dentro del radio de acople (500 km) para la nave [" + ship.getName() + "].");
        return false;
    }

    public void useSpecialAbility(int shipIndex) {
        if (!isIndiceValido(shipIndex)) return;

        OrbitalObject craft = trackedObjects.get(shipIndex);
        String result = craft.performSpecialAbility();
        notificarYActualizarMonitor(shipIndex, result);
    }

    private boolean isIndiceValido(int index) {
        return index >= 0 && index < trackedObjects.size();
    }

    private void notificarYActualizarMonitor(int index, String mensaje) {
        TelemetryLogger.printMessage(mensaje);
        this.monitoredShipIndex = index;
        this.lastMonitorAction = mensaje;
        updateMonitor();
    }

    // ==================== GETTERS ====================

    public List<OrbitalObject> getTrackedObjects() {
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
