package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

/**
 * Superclase abstracta para vehículos espaciales activos con sistema de propulsión.
 * Aplica el Principio de Segregación de Interfaces (SOLID - ISP) implementando
 * únicamente las capacidades de portar combustible (FuelCarrier) y maniobra evasiva (EvasiveCapable).
 */
public abstract class Spacecraft extends OrbitalObject implements FuelCarrier, EvasiveCapable {
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

    @Override
    public double getConsumoEvasion() {
        return 15.0;
    }

    @Override
    public boolean evade(double deltaLat, double deltaLng) {
        return evade(deltaLat, deltaLng, getConsumoEvasion());
    }

    @Override
    public boolean evade(double deltaLat, double deltaLng, double consumo) {
        if (fuelTank != null && fuelTank.getCurrentLevel() >= consumo) {
            boolean success = fuelTank.consume(consumo); // Consumo proporcional de combustible por maniobra
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

    @Override
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
