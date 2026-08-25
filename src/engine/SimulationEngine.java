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
import model.spacecraft.SpaceStation;
import model.spacecraft.Spacecraft;
import radar.Radar;

public class SimulationEngine {
    private int currentTick;
    private List<Spacecraft> trackedObjects;
    private Radar radar;
    private N2YOApiClient apiClient;
    private int monitoredShipIndex;
    private String lastMonitorAction;
    private int targetStationNoradId;

    public SimulationEngine(String apiKey, int targetNoradId) {
        this.currentTick = 0;
        this.trackedObjects = new ArrayList<>();
        this.apiClient = new N2YOApiClient(apiKey);
        this.monitoredShipIndex = -1;
        this.lastMonitorAction = "";
        // Inicializar el radar con coordenadas 0,0 por defecto
        this.radar = new Radar("Estación", 0.0, 0.0, 400.0, 100.0);

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
            Spacecraft ship = trackedObjects.get(monitoredShipIndex);
            TelemetryLogger.writeShipMonitorFile(ship, lastMonitorAction);
        }
    }

    // ==================== RADAR Y N2YO ====================

    /**
     * Establece la Estación Espacial objetivo y la obtiene de la API.
     */
    public void setTargetStation(int noradId) {
        this.targetStationNoradId = noradId;
        
        Spacecraft station = apiClient.fetchRealSatellite(noradId);
        
        if (station != null) {
            this.trackedObjects.clear();
            this.trackedObjects.add(station);
            
            // Actualizar el "radar" local para que se centre en la estación
            GeoPosition pos = station.getPosition();
            this.radar = new Radar(station.getName(), pos.getLatitude(), pos.getLongitude(), pos.getAltitude(), 100.0);
            
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
                
                // Si esta es la estación central, actualizar el centro del radar local
                if (craft.getNoradId() == targetStationNoradId && radar != null) {
                    radar.getObserverPosition().setLatitude(updatedPos.getLatitude());
                    radar.getObserverPosition().setLongitude(updatedPos.getLongitude());
                    radar.getObserverPosition().setAltitude(updatedPos.getAltitude());
                }
            }
        }
    }

    // ==================== CICLO DE SIMULACIÓN ====================

    public void tick() {
        currentTick++;
        TelemetryLogger.printHeader(currentTick, radar.getObserverCity());

        // Mover cada nave en órbita según sus reglas de polimorfismo
        for (Spacecraft craft : trackedObjects) {
            craft.move();
            
            // Actualizar el centro del radar para que siga a la estación objetivo CADA TICK de forma fluida
            if (craft.getNoradId() == targetStationNoradId && radar != null) {
                radar.getObserverPosition().setLatitude(craft.getPosition().getLatitude());
                radar.getObserverPosition().setLongitude(craft.getPosition().getLongitude());
                radar.getObserverPosition().setAltitude(craft.getPosition().getAltitude());
            }
        }

        // Lógica de generación probabilística de objetos por tick (duración tick: 10 segundos)
        double rand = Math.random();
        if (rand < 0.15) {
            spawnSpaceDebris();   // 15% probabilidad por tick (~1 basura pasiva cada minuto)
        } else if (rand < 0.20) {
            triggerCrisisEvent(); // 5% probabilidad por tick (~1 amenaza hostil cada 3-4 minutos)
        }

        // Sincronizar actualización de posiciones N2YO en segundo plano (asíncrono)
        // Esto evita que la simulación y la UI se traven esperando peticiones HTTP.
        if (currentTick == 1 || currentTick % 10 == 0) {
            syncWithN2YOAsync();
        }

        // Mostrar estado actual y telemetría por consola
        TelemetryLogger.printSpacecraftStatus(trackedObjects);

        // Escanear alertas de colisión
        List<String> alerts = radar.detectCollisionRisks(trackedObjects);
        TelemetryLogger.printAlerts(alerts);
    }
    
    // ==================== EVENTOS DE CRISIS Y BASURA ESPACIAL ====================
    
    public boolean spawnSpaceDebris() {
        if (trackedObjects == null || trackedObjects.isEmpty()) return false;
        
        long countDebris = trackedObjects.stream()
                .filter(s -> (s instanceof model.spacecraft.SpaceDebris && !(s instanceof model.spacecraft.RogueDebris)))
                .count();
                
        if (countDebris >= 3) return false; // Límite máximo de 3 basuras pasivas en pantalla
        
        Spacecraft station = null;
        for (Spacecraft s : trackedObjects) {
            if (s instanceof SpaceStation) {
                station = s; break;
            }
        }
        if (station == null && !trackedObjects.isEmpty()) station = trackedObjects.get(0);
        
        if (station != null) {
            double angle = Math.random() * 2 * Math.PI;
            double distDeg = 0.25 + Math.random() * 0.55; // Entre 25 km y 80 km
            double spawnLat = station.getPosition().getLatitude() + Math.cos(angle) * distDeg;
            double spawnLng = station.getPosition().getLongitude() + Math.sin(angle) * distDeg;
            
            int idNum = (int)(Math.random() * 9000 + 1000);
            model.spacecraft.SpaceDebris debris = new model.spacecraft.SpaceDebris(
                    "DEB-" + idNum,
                    "Restos NORAD-" + idNum,
                    90000 + idNum,
                    new GeoPosition(spawnLat, spawnLng, station.getPosition().getAltitude()),
                    6.5 + Math.random() * 3.0
            );
            
            trackedObjects.add(debris);
            TelemetryLogger.printMessage("Radar detectó nuevo fragmento de Basura Espacial [" + debris.getName() + "].");
            return true;
        }
        return false;
    }

    public boolean triggerCrisisEvent() {
        if (trackedObjects == null || trackedObjects.isEmpty()) return false;
        
        boolean hasRogue = false;
        for (Spacecraft s : trackedObjects) {
            if (s instanceof model.spacecraft.RogueDebris) {
                hasRogue = true; break;
            }
        }
        
        if (!hasRogue) {
            Spacecraft target = null;
            for (Spacecraft s : trackedObjects) {
                if (s instanceof SpaceStation) {
                    target = s; break;
                }
            }
            if (target == null && !trackedObjects.isEmpty()) target = trackedObjects.get(0);
            
            if (target != null) {
                double angle = Math.random() * 2 * Math.PI;
                double distDeg = 0.9; // ~90km de distancia en el sector
                double spawnLat = target.getPosition().getLatitude() + Math.cos(angle) * distDeg;
                double spawnLng = target.getPosition().getLongitude() + Math.sin(angle) * distDeg;
                
                int idNum = (int)(Math.random() * 900 + 100);
                model.spacecraft.RogueDebris rogue = new model.spacecraft.RogueDebris(
                        "RD-" + idNum, 
                        "ANOMALÍA CINÉTICA RD-" + idNum, 
                        new GeoPosition(spawnLat, spawnLng, target.getPosition().getAltitude()), 
                        9.5, 
                        target);
                
                trackedObjects.add(rogue);
                TelemetryLogger.printMessage("¡ALERTA CRÍTICA DEL RADAR! Anomalía Cinética (Rogue Debris) ingresó al sector en rumbo de impacto.");
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
