package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

public abstract class Spacecraft {
    protected String id;
    protected String name;
    protected int noradId;
    protected GeoPosition position;
    protected FuelTank fuelTank;
    protected double velocityKmH;
    protected String img;

    public Spacecraft(String id, String name, int noradId, GeoPosition position, FuelTank fuelTank, double velocityKmH) {
        this.id = id;
        this.name = name;
        this.noradId = noradId;
        this.position = position;
        this.fuelTank = fuelTank;
        this.velocityKmH = velocityKmH;
        this.img = "Spacecraft.jpg";
    }

    public void move() {
        if (fuelTank != null) {
            fuelTank.consume();
        }
    }

    public abstract String getType();
    public abstract String performSpecialAbility();

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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getNoradId() {
        return noradId;
    }

    public GeoPosition getPosition() {
        return position;
    }

    public FuelTank getFuelTank() {
        return fuelTank;
    }

    public double getVelocityKmH() {
        return velocityKmH;
    }

    public void setVelocityKmH(double velocityKmH) {
        this.velocityKmH = velocityKmH;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    @Override
    public String toString() {
        String fuelStr = (fuelTank != null) ? fuelTank.toString() : "N/A (Sin motor)";
        return String.format("%s (%s) | NORAD: %d | Pos: %s | Combustible: %s", 
                name, getType(), noradId, position.toString(), fuelStr);
    }
}
