import engine.SimulationEngine;
import model.Spacecraft;
import radar.Radar;

public class Main {
    public static void main(String[] args) {
        // Instanciación directa y manual del motor, nave y radar
        SimulationEngine engine = new SimulationEngine();
        Spacecraft ship = new Spacecraft("Apollo-11", 85.0, 90.0, 5.0, 5.0, 4.0);
        Radar radar = new Radar(100.0, 100.0);

        System.out.println("=== INICIANDO SIMULADOR DE TRÁFICO Y NAVEGACIÓN ORBITAL ===");
        System.out.println("Nave registrada: " + ship.getId() + " | Posición inicial: (" + ship.getX() + ", " + ship.getY() + ") | Combustible: " + ship.getFuel());
        System.out.println("Límites del Radar Terrestre: X=" + radar.getLimitX() + ", Y=" + radar.getLimitY());

        // Bucle de 5 ticks ejecutado y coordinado manualmente desde Main
        for (int i = 0; i < 5; i++) {
            if (ship.getFuel() <= 0) {
                System.out.println("La nave " + ship.getId() + " se ha quedado sin combustible. Finalizando simulación.");
                break;
                
            } else {
                           engine.tick();

            // Movimiento lineal manual de la nave
            ship.move();

            // Chequeo manual de la posición mediante el radar
            radar.checkPosition(ship); 
            }

        }

        System.out.println("\n=== SIMULACIÓN FINALIZADA CON ÉXITO ===");
    }
}
