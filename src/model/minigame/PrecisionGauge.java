package model.minigame;

/**
 * Modelo desacoplado del minijuego de precisión de acople orbital.
 * No depende de Swing ni de AWT, permitiendo pruebas unitarias puras.
 * Representa una barra de 0.0 a 100.0 con una zona verde objetivo
 * y un marcador que oscila continuamente (ping-pong).
 */
public class PrecisionGauge {

    public static final double DEFAULT_MIN = 0.0;
    public static final double DEFAULT_MAX = 100.0;
    public static final double DEFAULT_BASE_SPEED = 1.3;
    public static final double DEFAULT_GREEN_WIDTH = 24.0;

    private double currentPosition;
    private double direction; // +1.0 hacia la derecha, -1.0 hacia la izquierda
    private double speed;
    private double greenZoneStart;
    private double greenZoneEnd;
    private boolean stopped;
    private boolean lastHitResult;

    public PrecisionGauge() {
        this(DEFAULT_BASE_SPEED, DEFAULT_GREEN_WIDTH);
    }

    public PrecisionGauge(double speed, double greenZoneWidth) {
        this.currentPosition = 0.0;
        this.direction = 1.0;
        this.speed = Math.max(0.1, speed);
        this.stopped = false;
        configurarZonaVerdeCentrada(greenZoneWidth);
    }

    /**
     * Centra la zona verde alrededor del valor medio (50.0).
     */
    public void configurarZonaVerdeCentrada(double width) {
        double clampedWidth = Math.max(5.0, Math.min(width, 80.0));
        double center = 50.0;
        this.greenZoneStart = center - (clampedWidth / 2.0);
        this.greenZoneEnd = center + (clampedWidth / 2.0);
    }

    /**
     * Ajusta la dificultad según el número de intento (1 a 3).
     * Cada reintento aumenta la velocidad y reduce levemente la zona verde.
     */
    public void ajustarDificultadPorIntento(int attempt) {
        switch (attempt) {
            case 1:
                this.speed = 1.3;
                configurarZonaVerdeCentrada(24.0);
                break;
            case 2:
                this.speed = 1.7;
                configurarZonaVerdeCentrada(20.0);
                break;
            case 3:
            default:
                this.speed = 2.1;
                configurarZonaVerdeCentrada(16.0);
                break;
        }
    }

    /**
     * Avanza un paso en la simulación del movimiento del marcador.
     * Rebota elásticamente en los extremos (0.0 y 100.0).
     */
    public void update() {
        if (stopped) return;

        currentPosition += direction * speed;

        if (currentPosition >= DEFAULT_MAX) {
            currentPosition = DEFAULT_MAX;
            direction = -1.0;
        } else if (currentPosition <= DEFAULT_MIN) {
            currentPosition = DEFAULT_MIN;
            direction = 1.0;
        }
    }

    /**
     * Detiene el marcador en la posición actual y evalúa si cayó dentro de la zona verde.
     *
     * @return true si el frenado ocurrió dentro de la zona verde; false en caso contrario.
     */
    public boolean frenar() {
        this.stopped = true;
        this.lastHitResult = evaluarAcierto();
        return this.lastHitResult;
    }

    /**
     * Evalúa si la posición actual está comprendida entre los límites de la zona verde (inclusive).
     */
    public boolean evaluarAcierto() {
        return currentPosition >= greenZoneStart && currentPosition <= greenZoneEnd;
    }

    public void reiniciar() {
        this.currentPosition = 0.0;
        this.direction = 1.0;
        this.stopped = false;
        this.lastHitResult = false;
    }

    // Getters y Setters para inspección y pruebas
    public double getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(double currentPosition) {
        this.currentPosition = Math.max(DEFAULT_MIN, Math.min(currentPosition, DEFAULT_MAX));
    }

    public double getDirection() {
        return direction;
    }

    public void setDirection(double direction) {
        this.direction = direction >= 0 ? 1.0 : -1.0;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = Math.max(0.01, speed);
    }

    public double getGreenZoneStart() {
        return greenZoneStart;
    }

    public double getGreenZoneEnd() {
        return greenZoneEnd;
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isLastHitResult() {
        return lastHitResult;
    }
}
