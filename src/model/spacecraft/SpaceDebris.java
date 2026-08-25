package model.spacecraft;

import model.geometry.GeoPosition;

public class SpaceDebris extends Spacecraft {
    private double hazardLevel;

    public SpaceDebris(String id, String name, int noradId, GeoPosition position, double hazardLevel) {
        // La basura espacial no tiene motor/tanque de combustible
        super(id, name, noradId, position, null, 28000.0);
        this.hazardLevel = hazardLevel;
        this.img = "SpaceDebris.jpg";
    }

    @Override
    public void move() {
        // La posición se sincroniza en tiempo real desde la API N2YO.
    }

    @Override
    public String getType() {
        return "Basura Espacial / Chatarra";
    }

    @Override
    public String performSpecialAbility() {
        return "Basura Espacial [" + name + "] genera interferencia de radar y riesgo cinético (Nivel " + hazardLevel + "/10).";
    }

    public double getHazardLevel() {
        return hazardLevel;
    }
}
