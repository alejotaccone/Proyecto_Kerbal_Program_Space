package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

public abstract class Spacecraft extends OrbitalObject {
    protected FuelTank fuelTank;

    public Spacecraft(SpacecraftInfo info, GeoPosition position, FuelTank fuelTank, double velocityKmH) {
        super(info, position, velocityKmH);
        this.fuelTank = fuelTank;
    }

    @Override
    public void move() {
        if (fuelTank != null) {
            fuelTank.consume();
        }
    }

    public boolean evade(double deltaLat, double deltaLng) {
        if (fuelTank != null && !fuelTank.isEmpty()) {
            boolean success = fuelTank.consume(15.0); // Consumo de energía/combustible por maniobra
            if (success) {
                position.setLatitude(position.getLatitude() + deltaLat);
                position.setLongitude(position.getLongitude() + deltaLng);
                return true;
            }
        }
        return false;
    }

    public FuelTank getFuelTank() {
        return fuelTank;
    }

    @Override
    public String toString() {
        String fuelStr = (fuelTank != null) ? fuelTank.toString() : "N/A (Sin motor)";
        return String.format("%s (%s) | NORAD: %d | Pos: %s | Combustible: %s", 
                name, getType(), noradId, (position != null ? position.toString() : "N/A"), fuelStr);
    }
}
