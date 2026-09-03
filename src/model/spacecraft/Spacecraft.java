package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

/**
 * Superclase abstracta para vehículos espaciales activos con sistema de propulsión.
 * Encapsula estrictamente fuelTank como private.
 */
public abstract class Spacecraft extends OrbitalObject {
    private FuelTank fuelTank;

    public Spacecraft(SpacecraftInfo info, GeoPosition position, FuelTank fuelTank, double velocityKmH) {
        super(info, position, velocityKmH);
        this.fuelTank = fuelTank;
    }

    public Spacecraft(SpacecraftInfo info, GeoPosition position, FuelTank fuelTank, double velocityKmH, String nombreImagen) {
        super(info, position, velocityKmH, nombreImagen);
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
            if (success && getPosition() != null) {
                getPosition().setLatitude(getPosition().getLatitude() + deltaLat);
                getPosition().setLongitude(getPosition().getLongitude() + deltaLng);
                return true;
            }
        }
        return false;
    }

    /**
     * Recarga el tanque de combustible de la nave con la cantidad especificada.
     * Respeta el encapsulamiento protegiendo la manipulación interna de FuelTank.
     */
    public boolean refuel(double amount) {
        if (fuelTank != null) {
            fuelTank.refuel(amount);
            return true;
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
                getName(), getType(), getNoradId(), (getPosition() != null ? getPosition().toString() : "N/A"), fuelStr);
    }
}
