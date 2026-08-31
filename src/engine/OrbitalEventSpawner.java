package engine;

import java.util.List;
import model.geometry.GeoPosition;
import model.spacecraft.OrbitalObject;
import model.spacecraft.RogueDebris;
import model.spacecraft.SpaceDebris;
import model.spacecraft.SpaceStation;
import model.spacecraft.SpacecraftInfo;

/**
 * Clase responsable de la generación estocástica de eventos orbitales,
 * spawning de fragmentos de basura espacial pasiva y activación de anomalías cinéticas (Rogue Debris).
 * Extrae la lógica de eventos fuera de SimulationEngine (God Class).
 */
public class OrbitalEventSpawner {

    /**
     * Evalúa probabilidades aleatorias por tick para generar eventos en el sector.
     */
    public void evaluarEventosAleatoriosPorTick(List<OrbitalObject> trackedObjects) {
        double rand = Math.random();
        if (rand < 0.15) {
            spawnSpaceDebris(trackedObjects);   // 15% probabilidad por tick (~1 basura pasiva cada minuto)
        } else if (rand < 0.20) {
            triggerCrisisEvent(trackedObjects); // 5% probabilidad por tick (~1 amenaza hostil cada 3-4 minutos)
        }
    }

    /**
     * Busca la estación espacial principal o devuelve el primer objeto rastreado como fallback.
     */
    public OrbitalObject buscarEstacionObjetivo(List<OrbitalObject> trackedObjects) {
        if (trackedObjects == null || trackedObjects.isEmpty()) return null;
        for (OrbitalObject nave : trackedObjects) {
            if (nave instanceof SpaceStation) {
                return nave;
            }
        }
        return trackedObjects.get(0);
    }

    /**
     * Genera una posición geográfica polar aleatoria alrededor de una coordenada central.
     */
    public GeoPosition generarPosicionCercana(GeoPosition centro, double minDistDeg, double maxDistDeg) {
        if (centro == null) return null;
        double angle = Math.random() * 2 * Math.PI;
        double distDeg = minDistDeg + Math.random() * (maxDistDeg - minDistDeg);
        double spawnLat = centro.getLatitude() + Math.cos(angle) * distDeg;
        double spawnLng = centro.getLongitude() + Math.sin(angle) * distDeg;
        return new GeoPosition(spawnLat, spawnLng, centro.getAltitude());
    }

    /**
     * Genera un fragmento de basura espacial pasiva cerca de la estación objetivo.
     */
    public boolean spawnSpaceDebris(List<OrbitalObject> trackedObjects) {
        if (trackedObjects == null || trackedObjects.isEmpty()) return false;

        long countDebris = trackedObjects.stream()
                .filter(nave -> (nave instanceof SpaceDebris && !(nave instanceof RogueDebris)))
                .count();

        if (countDebris >= 3) return false; // Límite máximo de 3 basuras pasivas en pantalla

        OrbitalObject station = buscarEstacionObjetivo(trackedObjects);
        if (station != null && station.getPosition() != null) {
            GeoPosition spawnPos = generarPosicionCercana(station.getPosition(), 0.25, 0.80);

            int idNum = (int)(Math.random() * 9000 + 1000);
            SpacecraftInfo info = new SpacecraftInfo("DEB-" + idNum, "Restos NORAD-" + idNum, 90000 + idNum);
            SpaceDebris debris = new SpaceDebris(
                    info,
                    spawnPos,
                    6.5 + Math.random() * 3.0
            );

            trackedObjects.add(debris);
            TelemetryLogger.printMessage("Radar detectó nuevo fragmento de Basura Espacial [" + debris.getName() + "].");
            return true;
        }
        return false;
    }

    /**
     * Genera una anomalía cinética (Rogue Debris) en trayectoria de colisión con la estación.
     */
    public boolean triggerCrisisEvent(List<OrbitalObject> trackedObjects) {
        if (trackedObjects == null || trackedObjects.isEmpty()) return false;

        boolean hasRogue = trackedObjects.stream().anyMatch(nave -> nave instanceof RogueDebris);
        if (!hasRogue) {
            OrbitalObject target = buscarEstacionObjetivo(trackedObjects);
            if (target != null && target.getPosition() != null) {
                GeoPosition spawnPos = generarPosicionCercana(target.getPosition(), 0.9, 0.9);

                int idNum = (int)(Math.random() * 900 + 100);
                SpacecraftInfo info = new SpacecraftInfo("RD-" + idNum, "ANOMALÍA CINÉTICA RD-" + idNum, 99999);
                RogueDebris rogue = new RogueDebris(
                        info, 
                        spawnPos, 
                        9.5, 
                        target);

                trackedObjects.add(rogue);
                TelemetryLogger.printMessage("¡ALERTA CRÍTICA DEL RADAR! Anomalía Cinética (Rogue Debris) ingresó al sector en rumbo de impacto.");
                return true;
            }
        }
        return false;
    }
}
