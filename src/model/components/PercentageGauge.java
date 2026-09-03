package model.components;

/**
 * Representa un indicador o medidor de porcentaje acotado estrictamente entre 0.0% y 100.0%.
 * Encapsula la lógica de incremento, decremento y control de umbrales,
 * resolviendo el Bad Smell de Primitive Obsession sobre variables de telemetría porcentuales.
 */
public class PercentageGauge {
    private double level;

    public PercentageGauge() {
        this(100.0);
    }

    public PercentageGauge(double initialLevel) {
        this.level = Math.max(0.0, Math.min(100.0, initialLevel));
    }

    public void increase(double amount) {
        this.level = Math.min(100.0, this.level + amount);
    }

    public void decrease(double amount) {
        this.level = Math.max(0.0, this.level - amount);
    }

    public double getLevel() {
        return level;
    }

    public boolean isAbove(double threshold) {
        return this.level > threshold;
    }

    public boolean isDepleted() {
        return this.level <= 0.0;
    }

    @Override
    public String toString() {
        return String.format("%.1f%%", level);
    }
}
