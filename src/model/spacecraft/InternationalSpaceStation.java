package model.spacecraft;

import model.geometry.GeoPosition;

public class InternationalSpaceStation extends SpaceStation {

    public InternationalSpaceStation(String id, String name, int noradId, GeoPosition position) {
        super(id, name, noradId, position, "SpaceStation_Internacional.jpg");
    }

    @Override
    public String getType() {
        return "Estación Espacial Internacional (ISS)";
    }
}
