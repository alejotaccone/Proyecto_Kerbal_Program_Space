package model.spacecraft;

import model.geometry.GeoPosition;

public class SpaceDebris extends OrbitalObject {
    private double hazardLevel;
    private double driftAngle;

    public SpaceDebris(SpacecraftInfo info, GeoPosition position, double hazardLevel) {
        // La basura espacial no tiene motor/propulsión: extiende OrbitalObject directamente
        super(info, position, 28000.0, "SpaceDebris.jpg");
        this.hazardLevel = hazardLevel;
        this.driftAngle = Math.random() * 2 * Math.PI;
    }

    @Override
    public void move() {
        // Deriva orbital pasiva alrededor del sector (~22 km por tick) hasta alejarse del radar (>200 km)
        if (getPosition() != null) {
            driftAngle += (Math.random() - 0.5) * 0.25;
            double driftStepDeg = 0.20; // ~22 km de deriva por tick
            getPosition().setLatitude(getPosition().getLatitude() + Math.cos(driftAngle) * driftStepDeg);
            getPosition().setLongitude(getPosition().getLongitude() + Math.sin(driftAngle) * driftStepDeg);
        }
    }

    @Override
    public String getType() {
        return "Basura Espacial / Chatarra";
    }

    @Override
    public String performSpecialAbility() {
        return "Basura Espacial [" + getName() + "] genera interferencia de radar y riesgo cinético (Nivel " + hazardLevel + "/10).";
    }

    @Override
    public java.awt.Color getColorRadar() {
        return new java.awt.Color(255, 60, 60); // Rojo
    }

    @Override
    public int getTamanoIconoRadar() {
        return 5;
    }

    @Override
    public boolean esIconoTriangular() {
        return true;
    }

    @Override
    public String getDetalleTelemetria() {
        return String.format(
            "\n--- RIESGO ---\nPeligrosidad: %.1f/10\n",
            hazardLevel
        );
    }

    public double getHazardLevel() {
        return hazardLevel;
    }
}
