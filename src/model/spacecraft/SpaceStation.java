package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

public class SpaceStation extends Spacecraft {
    private boolean dockingAvailable;

    public SpaceStation(String id, String name, int noradId, GeoPosition position) {
        this(id, name, noradId, position, "SpaceStation_Internacional.jpg");
    }

    public SpaceStation(String id, String name, int noradId, GeoPosition position, String img) {
        // La estación espacial tiene un tanque de reserva ilimitado/grande y orbita suavemente
        super(id, name, noradId, position, new FuelTank(10000.0, 10000.0, 0.0), 27600.0);
        this.dockingAvailable = true;
        this.img = img;
    }

    @Override
    public void move() {
        // Órbita estable constante
        position.setLongitude(position.getLongitude() + 0.07);
    }

    @Override
    public String getType() {
        return "Estación Espacial Orbital";
    }

    @Override
    public String performSpecialAbility() {
        return "Estación Espacial [" + name + "] emitió una señal de acople y recarga para naves cercanas.";
    }

    public boolean refuelShip(Spacecraft ship) {
        if (ship != null && ship.getFuelTank() != null) {
            double distance = this.position.distanceTo(ship.getPosition());
            if (distance <= 500.0) { // Dentro del radio de acople (500 km)
                ship.getFuelTank().refuel(50.0);
                return true;
            }
        }
        return false;
    }

    public boolean isDockingAvailable() {
        return dockingAvailable;
    }
}
