package radar;

import model.Spacecraft;

public class Radar {
    private double limitX;
    private double limitY;

    public Radar(double limitX, double limitY) {
        this.limitX = limitX;
        this.limitY = limitY;
    }

    public void checkPosition(Spacecraft ship) {
        if (ship.getX() > limitX || ship.getY() > limitY) {
            System.out.println("¡ALERTA DE RADAR! La nave [" + ship.getId() + "] ha superado los límites seguros. Posición actual: (" + ship.getX() + ", " + ship.getY() + ")");
        } else {
            System.out.println("Radar: Nave [" + ship.getId() + "] dentro del rango seguro. Posición: (" + ship.getX() + ", " + ship.getY() + ")");
        }
    }

    public double getLimitX() {
        return limitX;
    }

    public double getLimitY() {
        return limitY;
    }
}
