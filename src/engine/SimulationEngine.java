package engine;

public class SimulationEngine {
    private int currentTick;

    public SimulationEngine() {
        this.currentTick = 0;
    }

    public void tick() {
        currentTick++;
        System.out.println("\n--- SIMULACIÓN - TICK #" + currentTick + " ---");
    }

    public int getCurrentTick() {
        return currentTick;
    }
}
