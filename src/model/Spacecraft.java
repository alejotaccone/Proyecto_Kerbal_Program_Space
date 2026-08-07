package model;

public class Spacecraft {
    private String id;
    private double x;
    private double y;
    private double fuel;
    private double speedX;
    private double speedY;

    public Spacecraft(String id, double initialX, double initialY, double initialFuel, double speedX, double speedY) {
        this.id = id;
        this.x = initialX;
        this.y = initialY;
        this.fuel = initialFuel;
        this.speedX = speedX;
        this.speedY = speedY;
    }

    public void move() {
        if (fuel > 0) {
            x += speedX;
            y += speedY;
            fuel -= 5.0; // Consumo lineal de combustible por cada movimiento
            if (fuel < 0) {
                fuel = 0;
            }
            System.out.println("Nave [" + id + "] se movió a la posición (" + x + ", " + y + "). Combustible restante: " + fuel);
        } else {
            System.out.println("Nave [" + id + "] sin combustible. No puede moverse.");
        }
    }

    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getFuel() {
        return fuel;
    }
}
