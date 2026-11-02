package engine;

import api.N2YOApiClient;
import java.util.ArrayList;
import java.util.List;
import model.geometry.GeoPosition;
import model.spacecraft.EvasiveCapable;
import model.spacecraft.OrbitalObject;
import model.spacecraft.SpaceStation;
import model.components.FuelTank;
import model.spacecraft.CargoShip;
import model.spacecraft.MissionState;
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
        this(new N2YOApiClient(apiKey), new OrbitalEventSpawner(), targetNoradId);
    }

    public SimulationEngine(N2YOApiClient apiClient, OrbitalEventSpawner eventSpawner, int targetNoradId) {
        this.currentTick = 0;
        this.trackedObjects = new ArrayList<>();
        this.apiClient = (apiClient != null) ? apiClient : new N2YOApiClient(null);
        this.eventSpawner = (eventSpawner != null) ? eventSpawner : new OrbitalEventSpawner();
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

            // Regla de diseño: De la API N2YO SOLO se sincronizan las estaciones espaciales
            if (!(craft instanceof SpaceStation)) continue;

            GeoPosition oldPos = new GeoPosition(craft.getPosition().getLatitude(), craft.getPosition().getLongitude(), craft.getPosition().getAltitude());

            GeoPosition updatedPos = apiClient.fetchRealSatellitePosition(
                    craft.getNoradId(), 
                    craft.getPosition()
            );
            if (updatedPos != null) {
                double dLat = updatedPos.getLatitude() - oldPos.getLatitude();
                double dLng = updatedPos.getLongitude() - oldPos.getLongitude();
                double dAlt = updatedPos.getAltitude() - oldPos.getAltitude();

                craft.getPosition().setLatitude(updatedPos.getLatitude());
                craft.getPosition().setLongitude(updatedPos.getLongitude());
                craft.getPosition().setAltitude(updatedPos.getAltitude());

                sincronizarCarguerosConEstacion(craft, dLat, dLng, dAlt);
                for (OrbitalObject obj : trackedObjects) {
                    if (obj instanceof model.spacecraft.RogueDebris) {
                        model.spacecraft.RogueDebris rogue = (model.spacecraft.RogueDebris) obj;
                        if (rogue.getTarget() == craft && rogue.getPosition() != null) {
                            rogue.getPosition().setLatitude(rogue.getPosition().getLatitude() + dLat);
                            rogue.getPosition().setLongitude(rogue.getPosition().getLongitude() + dLng);
                            rogue.getPosition().setAltitude(rogue.getPosition().getAltitude() + dAlt);
                        }
                    }
                }
                actualizarPosicionRadar(craft);
                limpiarBasuraPasivaLejana();
            }
        }
    }

    /**
     * Actualiza el centro del radar local para que siga la posición de la estación objetivo.
     */
    private void actualizarPosicionRadar(OrbitalObject craft) {
        if (craft != null && craft.getNoradId() == targetStationNoradId && radar != null) {
            radar.actualizarUbicacion(craft.getPosition());
        }
    }

    /**
     * Sincroniza la posición de todos los cargueros vinculados a la estación dada.
     * Mantiene a las naves acopladas o transfiriendo inmediatamente ancladas a la estación
     * y desplaza a las que están en tránsito en el mismo marco orbital de referencia.
     */
    public void sincronizarCarguerosConEstacion(OrbitalObject station, double dLat, double dLng, double dAlt) {
        if (station == null || trackedObjects == null) return;

        for (OrbitalObject obj : trackedObjects) {
            if (obj instanceof CargoShip) {
                CargoShip cargo = (CargoShip) obj;
                if (cargo.getTargetStation() == station) {
                    if (cargo.getMissionState() != null && (cargo.getMissionState().isAcopladaOTransfiriendo() || cargo.getMissionState() == MissionState.COMPLETADA)) {
                        cargo.posicionarEnAcople();
                    } else if (cargo.getMissionState() == MissionState.EN_TRANSITO && cargo.getPosition() != null) {
                        cargo.getPosition().setLatitude(cargo.getPosition().getLatitude() + dLat);
                        cargo.getPosition().setLongitude(cargo.getPosition().getLongitude() + dLng);
                        cargo.getPosition().setAltitude(cargo.getPosition().getAltitude() + dAlt);
                    }
                }
            }
        }
    }

    // ==================== CICLO DE SIMULACIÓN ====================

    public void tick() {
        avanzarReloj();
        moverObjetos();
        limpiarBasuraPasivaLejana();
        generarEventosAleatorios();
        sincronizarRedSiCorresponde();
        reportarTelemetriaYAlertas();
    }

    /**
     * Elimina del rastreo aquellos fragmentos de basura espacial pasiva que hayan superado
     * los 200 km de distancia respecto de la estación espacial objetivo.
     */
    public void limpiarBasuraPasivaLejana() {
        SpaceStation station = getTargetStation();
        if (station == null || trackedObjects == null) return;

        java.util.Iterator<OrbitalObject> it = trackedObjects.iterator();
        while (it.hasNext()) {
            OrbitalObject obj = it.next();
            if (obj instanceof model.spacecraft.SpaceDebris && !(obj instanceof model.spacecraft.RogueDebris)) {
                if (obj.distanceTo(station) > 200.0) {
                    it.remove();
                    TelemetryLogger.printMessage("[Radar]: Basura Espacial [" + obj.getName() + "] superó los 200 km de distancia y salió del rango de seguimiento.");
                }
            }
        }
        if (monitoredShipIndex >= trackedObjects.size()) {
            monitoredShipIndex = -1;
        }
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
        if (!(craft instanceof EvasiveCapable)) {
            notificarYActualizarMonitor(index, "MANIOBRA IMPOSIBLE! El objeto [" + craft.getName() + "] no es una nave con propulsión propia.");
            return false;
        }

        EvasiveCapable evasiveShip = (EvasiveCapable) craft;
        double consumo = evasiveShip.getConsumoEvasion();
        double dLat = 0.5;
        double dLng = 0.5;
        boolean success = evasiveShip.evade(dLat, dLng, consumo);
        if (success) {
            sincronizarCarguerosConEstacion(craft, dLat, dLng, 0.0);
            actualizarPosicionRadar(craft);
        }
        String msg = success 
                ? String.format(java.util.Locale.US, "MANIOBRA EXITOSA! La nave [%s] cambio su orbita para evitar impacto (-%.0fL combustible).", craft.getName(), consumo)
                : "MANIOBRA FALLIDA! La nave [" + craft.getName() + "] no tiene suficiente combustible.";
        notificarYActualizarMonitor(index, msg);
        return success;
    }

    /**
     * Ejecuta una evasión forzada de emergencia ante colisión inminente (evento de crisis).
     * Aplica el consumo de crisis correspondiente, sincroniza instantáneamente los cargueros
     * vinculados y actualiza la posición del radar para evitar desfasajes visuales.
     *
     * @param target el objeto orbital que esquiva la amenaza
     * @return true si la maniobra fue exitosa; false si no pudo evadir
     */
    public boolean forzarEvasionCrisis(OrbitalObject target) {
        if (!(target instanceof EvasiveCapable)) return false;

        EvasiveCapable evasive = (EvasiveCapable) target;
        double consumo = evasive.getConsumoEvasionCrisis();
        double dLat = 0.8;
        double dLng = 0.8;
        boolean success = evasive.evade(dLat, dLng, consumo);
        if (success) {
            sincronizarCarguerosConEstacion(target, dLat, dLng, 0.0);
            actualizarPosicionRadar(target);
        }
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

    public String useSpecialAbility(int shipIndex) {
        if (!isIndiceValido(shipIndex)) return "Índice de nave inválido.";

        OrbitalObject craft = trackedObjects.get(shipIndex);
        String result = craft.performSpecialAbility();
        notificarYActualizarMonitor(shipIndex, result);
        return result;
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

    private int cargoShipCounter = 0;

    public SpaceStation getTargetStation() {
        for (OrbitalObject craft : trackedObjects) {
            if (craft instanceof SpaceStation) {
                return (SpaceStation) craft;
            }
        }
        return null;
    }

    public List<CargoShip> getActiveCargoShips() {
        List<CargoShip> list = new ArrayList<>();
        for (OrbitalObject craft : trackedObjects) {
            if (craft instanceof CargoShip) {
                CargoShip cargo = (CargoShip) craft;
                if (!cargo.getMissionState().isFinalizada()) {
                    list.add(cargo);
                }
            }
        }
        return list;
    }

    public CargoShip spawnCargoShip(SpaceStation station) {
        if (station == null || station.getPosition() == null) return null;
        cargoShipCounter++;
        String idStr = String.format("%02d", cargoShipCounter);
        String shipName = "KPS-Cargo-" + idStr;
        SpacecraftInfo info = new SpacecraftInfo("CRG-" + idStr, shipName, 70000 + cargoShipCounter);

        GeoPosition posStation = station.getPosition();
        double angle = Math.random() * 2 * Math.PI;
        double distKm = 80.0 + Math.random() * 45.0;
        double degDist = distKm / 111.0;
        double spawnLat = posStation.getLatitude() + Math.cos(angle) * degDist;
        double spawnLng = posStation.getLongitude() + Math.sin(angle) * degDist;
        double spawnAlt = posStation.getAltitude() + (Math.random() * 10.0 - 5.0);
        GeoPosition spawnPos = new GeoPosition(spawnLat, spawnLng, spawnAlt);

        FuelTank propulsionTank = new FuelTank(150.0, 120.0, 1.0);
        double cargoFuel = 600.0;
        double cargoCapacityTons = 12.0;

        CargoShip cargo = new CargoShip(info, spawnPos, propulsionTank, station, cargoFuel, cargoCapacityTons);
        this.trackedObjects.add(cargo);
        TelemetryLogger.printMessage("[MISIÓN]: Carguero [" + shipName + "] desplegado en órbita a " + String.format("%.1f", distKm) + " km de " + station.getName() + ".");
        return cargo;
    }
}