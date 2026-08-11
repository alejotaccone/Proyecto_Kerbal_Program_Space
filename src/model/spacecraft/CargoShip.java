package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

public class CargoShip extends Spacecraft {
    private double cargoCapacityTons;
    private double currentCargoTons;

    public CargoShip(String id, String name, int noradId, GeoPosition position, FuelTank fuelTank, double cargoCapacityTons) {
        super(id, name, noradId, position, fuelTank, 24000.0); // Velocidad estándar ~24,000 km/h
        this.cargoCapacityTons = cargoCapacityTons;
        this.currentCargoTons = cargoCapacityTons * 0.8; // 80% llena por defecto
    }

    @Override
    public void move() {
        if (fuelTank != null && fuelTank.consume()) {
            // Avanza 0.05 grados de longitud por tick (simulación orbital lineal)
            position.setLongitude(position.getLongitude() + 0.05);
        }
    }

    @Override
    public String getType() {
        return "Nave de Carga Pesada";
    }

    @Override
    public String performSpecialAbility() {
        if (currentCargoTons > 5.0) {
            currentCargoTons -= 5.0;
            return "Nave [" + name + "] eyectó 5 toneladas de carga pesada para ganar agilidad y ahorrar combustible.";
        }
        return "Nave [" + name + "] no tiene suficiente carga extra para eyectar.";
    }

    public double getCargoCapacityTons() {
        return cargoCapacityTons;
    }

    public double getCurrentCargoTons() {
        return currentCargoTons;
    }
}
