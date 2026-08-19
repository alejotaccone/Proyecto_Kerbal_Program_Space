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
        // Basura espacial a la deriva incontrolada
        position.setLatitude(position.getLatitude() + (Math.random() * 0.04 - 0.02));
        position.setLongitude(position.getLongitude() + 0.1);
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
