package model.spacecraft;

import model.geometry.GeoPosition;

public class SpaceDebris extends OrbitalObject {
    private double hazardLevel;

    public SpaceDebris(SpacecraftInfo info, GeoPosition position, double hazardLevel) {
        // La basura espacial no tiene motor/propulsión: extiende OrbitalObject directamente
        super(info, position, 28000.0);
        this.hazardLevel = hazardLevel;
        this.nombreImagen = "SpaceDebris.jpg";
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
