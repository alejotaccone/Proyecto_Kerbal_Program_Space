package model.spacecraft;

import model.geometry.GeoPosition;

public class ChinaSpaceStation extends SpaceStation {

    public ChinaSpaceStation(String id, String name, int noradId, GeoPosition position) {
        super(id, name, noradId, position, "SpaceSation_China.jfif");
    }

    @Override
    public String getType() {
        return "Estación Espacial China (Tiangong)";
    }
}
