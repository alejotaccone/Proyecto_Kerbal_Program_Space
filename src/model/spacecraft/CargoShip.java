package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

/**
 * Representa una nave de transporte y reabastecimiento espacial.
 * Aplica el patrón Máquina de Estados para controlar el ciclo de vida de la misión
 * (tránsito, aproximación, acople, transferencia de combustible/oxígeno y finalización).
 * Cumple con el principio de Experto en Información encapsulando la navegación y la transferencia.
 */
public class CargoShip extends Spacecraft {

    public static final double DOCKING_DISTANCE_THRESHOLD_KM = 8.0;      // Umbral para maniobra de acople
    public static final int MAX_DOCKING_ATTEMPTS = 3;
    public static final double DEFAULT_PROPULSION_CONSUMPTION_PER_TICK = 0.8;
    public static final double FAILED_DOCKING_FUEL_PENALTY = 10.0;
    public static final double TRANSFER_RATE_FUEL_PER_TICK = 120.0;     // Litros transferidos por tick
    public static final double TRANSFER_RATE_OXYGEN_PER_TICK = 8.0;     // Porcentaje de oxígeno transferido por tick
    public static final double FLIGHT_STEP_KM_PER_TICK = 35.0;          // Velocidad de aproximación táctica por tick

    private double cargoCapacityTons;
    private double currentCargoTons;

    // Subsistemas y estado de la misión de reabastecimiento
    private SpaceStation targetStation;
    private MissionState missionState;
    private String abortReason;
    private int dockingAttemptsRemaining;

    // Carga útil segregada para transferencia (Combustible y Oxígeno)
    private double cargoFuel;
    private double maxCargoFuel;
    private double cargoOxygen;
    private double maxCargoOxygen;

    // Métricas de transferencia acumuladas (conservación de masa)
    private double totalFuelTransferred;
    private double totalOxygenTransferred;

    /**
     * Constructor estándar de compatibilidad.
     */
    public CargoShip(SpacecraftInfo info, GeoPosition position, FuelTank fuelTank, double cargoCapacityTons) {
        this(info, position, fuelTank, null, cargoCapacityTons * 40.0, 30.0, cargoCapacityTons);
    }

    /**
     * Constructor completo para misiones de reabastecimiento guiadas hacia una estación.
     */
    public CargoShip(
            SpacecraftInfo info,
            GeoPosition position,
            FuelTank propulsionFuelTank,
            SpaceStation targetStation,
            double cargoFuel,
            double cargoOxygen,
            double cargoCapacityTons) {
        super(info, position, propulsionFuelTank, 24000.0, "CargoShipProgress.jpg");
        this.targetStation = targetStation;
        this.cargoCapacityTons = cargoCapacityTons;
        this.currentCargoTons = cargoCapacityTons * 0.8;

        this.cargoFuel = Math.max(0.0, cargoFuel);
        this.maxCargoFuel = this.cargoFuel;
        this.cargoOxygen = Math.max(0.0, cargoOxygen);
        this.maxCargoOxygen = this.cargoOxygen;

        this.missionState = (targetStation != null) ? MissionState.EN_TRANSITO : MissionState.COMPLETADA;
        this.abortReason = "";
        this.dockingAttemptsRemaining = MAX_DOCKING_ATTEMPTS;
        this.totalFuelTransferred = 0.0;
        this.totalOxygenTransferred = 0.0;
    }

    @Override
    public String getType() {
        return "Carguero de Reabastecimiento";
    }

    @Override
    public void move() {
        if (missionState == MissionState.EN_TRANSITO) {
            avanzarHaciaEstacion();
        } else if (missionState == MissionState.TRANSFIRIENDO) {
            posicionarEnAcople();
            ejecutarPasoTransferencia();
        } else if (missionState == MissionState.ACOPLADA) {
            posicionarEnAcople();
            iniciarTransferencia();
        } else if (missionState == MissionState.COMPLETADA) {
            posicionarEnAcople();
        }
    }

    /**
     * Fija la posición del carguero junto a la estación objetivo respetando un pequeño offset (~8 a 10 km)
     * para que permanezca visiblemente acoplado, conectado por la manguera y clickeable en el radar sin superponerse.
     */
    public void posicionarEnAcople() {
        if (targetStation != null && targetStation.getPosition() != null && getPosition() != null) {
            GeoPosition stPos = targetStation.getPosition();
            getPosition().setLatitude(stPos.getLatitude() - 0.06);
            getPosition().setLongitude(stPos.getLongitude() - 0.06);
            getPosition().setAltitude(stPos.getAltitude());
        }
    }

    /**
     * Avanza el carguero a lo largo del vector tridimensional hacia la estación espacial objetivo.
     * Si se agota el combustible propio de propulsión, la nave queda a la deriva y la misión se aborta.
     */
    public void avanzarHaciaEstacion() {
        if (targetStation == null || targetStation.getPosition() == null || getPosition() == null) {
            abortMission("Estación objetivo no localizada.");
            return;
        }

        // 1. Verificar combustible de propulsión
        FuelTank tank = getFuelTank();
        if (tank == null || tank.isEmpty()) {
            abortMission("Carguero sin combustible de maniobra (a la deriva en órbita)");
            return;
        }

        tank.consume(DEFAULT_PROPULSION_CONSUMPTION_PER_TICK);

        // 2. Calcular distancia 3D euclídea actual
        double distActualKm = distanceTo(targetStation);

        // 3. Evaluar si ingresó al umbral de acople (< 8 km)
        if (distActualKm <= DOCKING_DISTANCE_THRESHOLD_KM) {
            this.missionState = MissionState.EN_RANGO_DE_ACOPLE;
            posicionarEnAcople();
            return;
        }

        // 4. Desplazamiento proporcional hacia la posición 3D de la estación
        double paso = Math.min(FLIGHT_STEP_KM_PER_TICK, Math.max(1.0, distActualKm - (DOCKING_DISTANCE_THRESHOLD_KM * 0.5)));
        double factor = Math.min(1.0, paso / distActualKm);

        double dLat = (targetStation.getPosition().getLatitude() - getPosition().getLatitude()) * factor;
        double dLng = (targetStation.getPosition().getLongitude() - getPosition().getLongitude()) * factor;
        double dAlt = (targetStation.getPosition().getAltitude() - getPosition().getAltitude()) * factor;

        getPosition().setLatitude(getPosition().getLatitude() + dLat);
        getPosition().setLongitude(getPosition().getLongitude() + dLng);
        getPosition().setAltitude(getPosition().getAltitude() + dAlt);

        // Reevaluar proximidad post-movimiento
        if (distanceTo(targetStation) <= DOCKING_DISTANCE_THRESHOLD_KM) {
            this.missionState = MissionState.EN_RANGO_DE_ACOPLE;
            posicionarEnAcople();
        }
    }

    /**
     * Registra un acierto en el minijuego de precisión conectando la manguera de acople.
     */
    public void registrarAcopleExitoso() {
        this.missionState = MissionState.ACOPLADA;
        posicionarEnAcople();
    }

    /**
     * Inicia el bombeo de suministros a la estación acoplada.
     */
    public void iniciarTransferencia() {
        if (missionState == MissionState.ACOPLADA) {
            this.missionState = MissionState.TRANSFIRIENDO;
        }
    }

    /**
     * Ejecuta una unidad de transferencia de suministros por tick conservando estrictamente la masa.
     */
    public void ejecutarPasoTransferencia() {
        if (targetStation == null || missionState != MissionState.TRANSFIRIENDO) return;

        boolean combustibleTransferido = false;
        boolean oxigenoTransferido = false;

        // ---- 1. Transferencia de Combustible ----
        FuelTank tankEstacion = targetStation.getFuelTank();
        if (tankEstacion != null && cargoFuel > 0.0) {
            double espacioDisponible = Math.max(0.0, tankEstacion.getCapacity() - tankEstacion.getCurrentLevel());
            double cantCombustible = Math.min(cargoFuel, Math.min(TRANSFER_RATE_FUEL_PER_TICK, espacioDisponible));

            if (cantCombustible > 0.0) {
                cargoFuel -= cantCombustible;
                totalFuelTransferred += cantCombustible;
                tankEstacion.refuel(cantCombustible);
                combustibleTransferido = true;
            }
        }

        // ---- 2. Transferencia de Oxígeno ----
        if (targetStation.getOxygenGauge() != null && cargoOxygen > 0.0) {
            double nivelActual = targetStation.getOxygenLevel();
            double espacioDisponibleO2 = Math.max(0.0, 100.0 - nivelActual);
            double cantOxigeno = Math.min(cargoOxygen, Math.min(TRANSFER_RATE_OXYGEN_PER_TICK, espacioDisponibleO2));

            if (cantOxigeno > 0.0) {
                cargoOxygen -= cantOxigeno;
                totalOxygenTransferred += cantOxigeno;
                targetStation.getOxygenGauge().increase(cantOxigeno);
                oxigenoTransferido = true;
            }
        }

        // Actualizar estimación de carga útil en toneladas
        double porcentajeRestante = (maxCargoFuel > 0 || maxCargoOxygen > 0)
                ? (cargoFuel + cargoOxygen * 10.0) / (maxCargoFuel + maxCargoOxygen * 10.0)
                : 0.0;
        this.currentCargoTons = this.cargoCapacityTons * Math.max(0.0, Math.min(1.0, porcentajeRestante));

        // ---- 3. Evaluación de Finalización de la Misión ----
        boolean cargoAgotada = (cargoFuel <= 0.001 && cargoOxygen <= 0.001);
        boolean estacionLlena = (tankEstacion != null && tankEstacion.isFull()) && (targetStation.getOxygenLevel() >= 99.9);

        if (cargoAgotada || estacionLlena || (!combustibleTransferido && !oxigenoTransferido && (cargoFuel <= 0.001 || tankEstacion.isFull()) && (cargoOxygen <= 0.001 || targetStation.getOxygenLevel() >= 99.9))) {
            completarMision();
        }
    }

    /**
     * Procesa un intento fallido del minijuego de precisión.
     * Consume combustible por corrección de maniobra y descuenta un intento.
     *
     * @return true si aún le quedan intentos; false si se agotaron y la misión fue abortada.
     */
    public boolean procesarIntentoFallido() {
        if (getFuelTank() != null) {
            getFuelTank().consume(FAILED_DOCKING_FUEL_PENALTY);
        }

        dockingAttemptsRemaining--;

        if (dockingAttemptsRemaining <= 0) {
            abortMission("Acople fallido: se agotaron los 3 intentos de maniobra táctica.");
            return false;
        } else {
            this.missionState = MissionState.EN_RANGO_DE_ACOPLE;
            posicionarEnAcople();
            return true;
        }
    }

    public void completarMision() {
        this.missionState = MissionState.COMPLETADA;
    }

    public void abortMission(String razon) {
        this.missionState = MissionState.ABORTADA;
        this.abortReason = (razon != null) ? razon : "Misión cancelada.";
    }

    @Override
    public boolean evade(double deltaLat, double deltaLng) {
        // Si la nave estaba en acople o transferencia, la maniobra fuerza el desacople de emergencia
        if (missionState.isAcopladaOTransfiriendo()) {
            abortMission("Desacople de emergencia por maniobra evasiva ante colisión inminente.");
        }
        return super.evade(deltaLat, deltaLng);
    }

    @Override
    public String performSpecialAbility() {
        if (missionState.isAcopladaOTransfiriendo()) {
            return "Nave [" + getName() + "] está acoplada a " + targetStation.getName() + " transfiriendo suministros vitales.";
        }
        if (currentCargoTons > 5.0) {
            currentCargoTons -= 5.0;
            return "Nave [" + getName() + "] eyectó 5 toneladas de carga de emergencia para ganar maniobrabilidad.";
        }
        return "Nave [" + getName() + "] no tiene suficiente carga descartable.";
    }

    @Override
    public java.awt.Color getColorRadar() {
        return new java.awt.Color(255, 140, 0); // Naranja táctico
    }

    @Override
    public int getTamanoIconoRadar() {
        return 7;
    }

    @Override
    public String getDetalleTelemetria() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- MISIÓN DE REABASTECIMIENTO ---\n");
        sb.append(String.format("Estado:      %s\n", (missionState != null ? missionState.getDescripcion() : "Desconocido")));
        if (targetStation != null) {
            sb.append(String.format("Destino:     %s\n", targetStation.getName()));
            sb.append(String.format("Distancia:   %.2f km\n", distanceTo(targetStation)));
        }
        if (missionState != null && !missionState.isFinalizada()) {
            sb.append(String.format("Intentos:    %d de %d restantes\n", dockingAttemptsRemaining, MAX_DOCKING_ATTEMPTS));
        }
        if (abortReason != null && !abortReason.isEmpty()) {
            sb.append(String.format("Causa Abort: %s\n", abortReason));
        }

        sb.append("\n--- CARGA TRANSFERIBLE ---\n");
        sb.append(String.format("Fuel Carga:  %.1f / %.1f L\n", cargoFuel, maxCargoFuel));
        sb.append(String.format("O2 Carga:    %.1f / %.1f %%\n", cargoOxygen, maxCargoOxygen));
        if (totalFuelTransferred > 0 || totalOxygenTransferred > 0) {
            sb.append(String.format("Entregado:   %.1f L Fuel | %.1f %% O2\n", totalFuelTransferred, totalOxygenTransferred));
        }

        sb.append("\n--- COMBUSTIBLE DE NAVEGACIÓN ---\n");
        sb.append(String.format("Propulsión:  %.1f L (%.1f%%)\n",
                getFuelTank() != null ? getFuelTank().getCurrentLevel() : 0.0,
                getFuelTank() != null ? getFuelTank().getPercentage() : 0.0));

        return sb.toString();
    }

    // Getters y Setters
    public SpaceStation getTargetStation() {
        return targetStation;
    }

    public void setTargetStation(SpaceStation targetStation) {
        this.targetStation = targetStation;
    }

    public MissionState getMissionState() {
        return missionState;
    }

    public void setMissionState(MissionState missionState) {
        this.missionState = missionState;
    }

    public String getAbortReason() {
        return abortReason;
    }

    public int getDockingAttemptsRemaining() {
        return dockingAttemptsRemaining;
    }

    public double getCargoFuel() {
        return cargoFuel;
    }

    public double getMaxCargoFuel() {
        return maxCargoFuel;
    }

    public double getCargoOxygen() {
        return cargoOxygen;
    }

    public double getMaxCargoOxygen() {
        return maxCargoOxygen;
    }

    public double getTotalFuelTransferred() {
        return totalFuelTransferred;
    }

    public double getTotalOxygenTransferred() {
        return totalOxygenTransferred;
    }

    public double getCargoCapacityTons() {
        return cargoCapacityTons;
    }

    public double getCurrentCargoTons() {
        return currentCargoTons;
    }
}