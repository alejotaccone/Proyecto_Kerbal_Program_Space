package model.components;

public class FuelTank {
    private double capacity;
    private double currentLevel;
    private double consumptionRate;

    public FuelTank(double capacity, double initialLevel, double consumptionRate) {
        this.capacity = capacity;
        this.currentLevel = Math.min(initialLevel, capacity);
        this.consumptionRate = consumptionRate;
    }

    public boolean consume() {
        return consume(consumptionRate);
    }

    public boolean consume(double amount) {
        if (currentLevel >= amount) {
            currentLevel -= amount;
            return true;
        } else {
            currentLevel = 0;
            return false;
        }
    }

    public void refuel(double amount) {
        currentLevel += amount;
        if (currentLevel > capacity) {
            currentLevel = capacity;
        }
    }

    public boolean isEmpty() {
        return currentLevel <= 0;
    }

    public double getCapacity() {
        return capacity;
    }

    public double getCurrentLevel() {
        return currentLevel;
    }

    public double getConsumptionRate() {
        return consumptionRate;
    }

    public void setConsumptionRate(double consumptionRate) {
        this.consumptionRate = consumptionRate;
    }

    public double getPercentage() {
        return (currentLevel / capacity) * 100.0;
    }

    @Override
    public String toString() {
        return String.format("%.1f/%.1f L (%.1f%%)", currentLevel, capacity, getPercentage());
    }
}
