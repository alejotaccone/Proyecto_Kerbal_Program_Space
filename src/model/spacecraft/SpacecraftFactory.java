package model.spacecraft;

import model.components.FuelTank;
import model.components.Kerbal;
import model.components.KerbalRole;
import model.geometry.GeoPosition;

/**
 * Fábrica pura (GRASP Creator / Factory Pattern) para la instanciación polimórfica
 * de objetos orbitales a partir de datos telemétricos obtenidos de la API N2YO.
 * Desacopla la capa de comunicación de red (N2YOApiClient) del modelo de dominio.
 */
public class SpacecraftFactory {

    /**
     * Mapea e instancia dinámicamente el tipo de objeto orbital apropiado según el nombre y NORAD ID.
     */
    public static OrbitalObject crearDesdeDatosN2YO(int noradId, String satName, GeoPosition pos) {
        String upper = (satName != null) ? satName.toUpperCase() : "";
        String craftId = "SAT-" + noradId;
        SpacecraftInfo info = new SpacecraftInfo(craftId, satName + " (N2YO API)", noradId);

        if (upper.contains("TIANHE") || upper.contains("TIANGONG") || upper.contains("CSS") || upper.contains("CHINA") || noradId == 48274) {
            return new SpaceStation(info, pos, "SpaceSation_China.jfif", "Estación Espacial China (Tiangong)");
        } else if (upper.contains("STATION") || upper.contains("ISS") || noradId == 25544) {
            return new SpaceStation(info, pos, "SpaceStation_Internacional.jpg", "Estación Espacial Internacional (ISS)");
        } else if (upper.contains("ONEWEB") || upper.contains("DEBRIS") || upper.contains("COSMOS") || upper.contains("DEB") || upper.contains("SL-")) {
            return new SpaceDebris(info, pos, 8.2);
        } else if (upper.contains("NOAA") || upper.contains("CARGO") || upper.contains("DRAGON") || upper.contains("DELTA") || upper.contains("ATLAS")) {
            return new CargoShip(info, pos, new FuelTank(200.0, 180.0, 3.0), 18.5);
        } else if (upper.contains("SOYUZ") || upper.contains("CREW") || upper.contains("STARLINER")) {
            CrewShuttle shuttle = new CrewShuttle(info, pos, new FuelTank(180.0, 150.0, 3.0));
            shuttle.agregarTripulante("Comandante Real", KerbalRole.PILOT, 90);
            return shuttle;
        } else {
            // Sonda de exploración por defecto para satélites científicos como HST (Hubble), TELSTAR, ESSA, etc.
            return new ExplorationProbe(info, pos, new FuelTank(120.0, 100.0, 1.5), 0.95);
        }
    }
}
