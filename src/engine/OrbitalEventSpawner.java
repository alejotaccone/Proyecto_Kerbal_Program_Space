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
 * Aplica el patrón GRASP Creator / Factory para la instanciación de debris en el motor.
 */
public class OrbitalEventSpawner {

    /**
     * Evalúa probabilidades independientes por tick para generar basura espacial pasiva (decorativa)
     * y anomalías cinéticas hostiles (RogueDebris).
     * - Basura Espacial Pasiva: 50% de probabilidad por tick (aparece más rápido, ronda el espacio sin atacar).
     * - Anomalía Cinética Hostil: 30% de probabilidad por tick (activa la dinámica de evasión o choque).
     */
    public void evaluarEventosAleatoriosPorTick(List<OrbitalObject> trackedObjects) {
        if (Math.random() < 0.50) {
            spawnSpaceDebris(trackedObjects);   // 50% probabilidad por tick de basura pasiva decorativa
        }
        if (Math.random() < 0.30) {
            triggerCrisisEvent(trackedObjects); // 30% probabilidad por tick de generar Anomalía Cinética
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
     * Genera una posición geográfica polar aleatoria alrededor de una coordenada central (en grados).
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
     * Genera una posición geográfica a una distancia física exacta en kilómetros alrededor del centro,
     * compensando la curvatura orbital y el coseno de la latitud para que la distancia 3D sea consistente.
     */
    public GeoPosition generarPosicionPorDistanciaKm(GeoPosition centro, double minDistKm, double maxDistKm) {
        if (centro == null) return null;
        double angle = Math.random() * 2 * Math.PI;
        double distKm = minDistKm + Math.random() * (maxDistKm - minDistKm);

        double r = GeoPosition.EARTH_RADIUS_KM + centro.getAltitude();
        double kmPerDegLat = Math.toRadians(1.0) * r;
        double cosLat = Math.max(0.2, Math.abs(Math.cos(Math.toRadians(centro.getLatitude()))));
        double kmPerDegLng = kmPerDegLat * cosLat;

        double dLat = (distKm * Math.cos(angle)) / kmPerDegLat;
        double dLng = (distKm * Math.sin(angle)) / kmPerDegLng;

        return new GeoPosition(centro.getLatitude() + dLat, centro.getLongitude() + dLng, centro.getAltitude());
    }

    /**
     * Genera un fragmento de basura espacial pasiva cerca de la estación objetivo.
     * Es meramente decorativa: ronda por el espacio dentro del radio del radar sin atacar.
     */
    public boolean spawnSpaceDebris(List<OrbitalObject> trackedObjects) {
        if (trackedObjects == null || trackedObjects.isEmpty()) return false;

        long countDebris = trackedObjects.stream()
                .filter(nave -> (nave instanceof SpaceDebris && !(nave instanceof RogueDebris)))
                .count();

        if (countDebris >= 4) return false; // Límite máximo de 4 basuras pasivas decorativas en pantalla

        OrbitalObject station = buscarEstacionObjetivo(trackedObjects);
        if (station != null && station.getPosition() != null) {
            GeoPosition spawnPos = generarPosicionPorDistanciaKm(station.getPosition(), 30.0, 85.0);
            SpaceDebris debris = fabricarBasuraPasiva(spawnPos);

            trackedObjects.add(debris);
            TelemetryLogger.printMessage("Radar detectó nuevo fragmento de Basura Espacial [" + debris.getName() + "].");
            return true;
        }
        return false;
    }

    /**
     * Genera una anomalía cinética (Rogue Debris) en trayectoria de colisión con la estación.
     * Aparece entre 108 y 115 km para que permanezca visible durante 1 tick en el radar con su
     * vector predictivo antes de ingresar al umbral crítico (< 100 km) en el siguiente tick.
     */
    public boolean triggerCrisisEvent(List<OrbitalObject> trackedObjects) {
        if (trackedObjects == null || trackedObjects.isEmpty()) return false;

        boolean hasRogue = trackedObjects.stream().anyMatch(nave -> nave instanceof RogueDebris);
        if (!hasRogue) {
            OrbitalObject target = buscarEstacionObjetivo(trackedObjects);
            if (target != null && target.getPosition() != null) {
                GeoPosition spawnPos = generarPosicionPorDistanciaKm(target.getPosition(), 108.0, 115.0);
                RogueDebris rogue = fabricarAnomaliaHostil(spawnPos, target);

                trackedObjects.add(rogue);
                TelemetryLogger.printMessage("¡ALERTA CRÍTICA DEL RADAR! Anomalía Cinética (Rogue Debris) ingresó al sector en rumbo de impacto.");
                return true;
            }
        }
        return false;
    }

    /**
     * Fabrica una nueva instancia de SpaceDebris pasiva con identificadores únicos (GRASP Creator).
     */
    private SpaceDebris fabricarBasuraPasiva(GeoPosition spawnPos) {
        int idNum = (int)(Math.random() * 9000 + 1000);
        SpacecraftInfo info = new SpacecraftInfo("DEB-" + idNum, "Restos NORAD-" + idNum, 90000 + idNum);
        return new SpaceDebris(info, spawnPos, 6.5 + Math.random() * 3.0);
    }

    /**
     * Fabrica una nueva instancia de RogueDebris hostil apuntada a su objetivo (GRASP Creator).
     */
    private RogueDebris fabricarAnomaliaHostil(GeoPosition spawnPos, OrbitalObject target) {
        int idNum = (int)(Math.random() * 900 + 100);
        SpacecraftInfo info = new SpacecraftInfo("RD-" + idNum, "ANOMALÍA CINÉTICA RD-" + idNum, 99999);
        return new RogueDebris(info, spawnPos, 9.5, target);
    }
}
