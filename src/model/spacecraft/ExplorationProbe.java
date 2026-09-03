package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

public class ExplorationProbe extends Spacecraft {
    private double solarEfficiency;

    public ExplorationProbe(SpacecraftInfo info, GeoPosition position, FuelTank fuelTank, double solarEfficiency) {
        super(info, position, fuelTank, 27500.0, "Spacecraft.jpg");
        this.solarEfficiency = solarEfficiency;
    }

    @Override
    public String getType() {
        return "Sonda Exploradora Solar";
    }

    @Override
    public String performSpecialAbility() {
        if (getFuelTank() != null) {
            double rechargedAmount = 10.0 * solarEfficiency;
            getFuelTank().refuel(rechargedAmount);
            return "Sonda [" + getName() + "] desplegó sus paneles solares y recargó " + String.format("%.1f", rechargedAmount) + " L de energía.";
        }
        return "Sonda [" + getName() + "] sin tanque de energía válido.";
    }

    public double getSolarEfficiency() {
        return solarEfficiency;
    }
}
