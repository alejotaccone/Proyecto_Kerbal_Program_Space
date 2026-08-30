package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

public class ExplorationProbe extends Spacecraft {
    private double solarEfficiency;

    public ExplorationProbe(String id, String name, int noradId, GeoPosition position, FuelTank fuelTank, double solarEfficiency) {
        super(id, name, noradId, position, fuelTank, 27500.0);
        this.solarEfficiency = solarEfficiency;
        this.nombreImagen = "Spacecraft.jpg";
    }

    @Override
    public String getType() {
        return "Sonda Exploradora Solar";
    }

    @Override
    public String performSpecialAbility() {
        if (fuelTank != null) {
            double rechargedAmount = 10.0 * solarEfficiency;
            fuelTank.refuel(rechargedAmount);
            return "Sonda [" + name + "] desplegó sus paneles solares y recargó " + String.format("%.1f", rechargedAmount) + " L de energía.";
        }
        return "Sonda [" + name + "] sin tanque de energía válido.";
    }

    public double getSolarEfficiency() {
        return solarEfficiency;
    }
}
