package test;

import controller.AlertQueue;
import java.util.ArrayList;
import java.util.List;
import controller.MissionController;
import engine.SimulationEngine;
import model.components.FuelTank;
import model.geometry.GeoPosition;
import model.spacecraft.CargoShip;
import model.spacecraft.MissionState;
import model.spacecraft.SpaceStation;
import model.spacecraft.SpacecraftInfo;

/**
 * Suite de pruebas unitarias sin dependencias externas para misiones de reabastecimiento:
 * Conservación estricta de masa, límites de capacidad, fallos de combustible y validaciones de spawn.
 */
public class ResupplyMissionCheck {

    public static void main(String[] args) {
        System.out.println("=== EJECUTANDO PRUEBAS DE MISIÓN DE REABASTECIMIENTO ===");
        int fallos = 0;

        // Caso 1: Conservación estricta de masa durante la transferencia
        SpaceStation station1 = new SpaceStation(
            new SpacecraftInfo("ISS", "ISS Test", 25544),
            new GeoPosition(-34.60, -58.38, 420.0)
        );
        // Estación con 1800 L de combustible (capacidad 2500 L) y 80% de oxígeno
        station1.getFuelTank().consume(700.0);
        station1.getOxygenGauge().decrease(20.0);

        double stationFuelInicial = station1.getFuelTank().getCurrentLevel();
        double stationO2Inicial = station1.getOxygenLevel();

        CargoShip cargo1 = new CargoShip(
            new SpacecraftInfo("CRG-01", "KPS-Cargo-01", 70001),
            new GeoPosition(-34.60, -58.38, 420.0),
            new FuelTank(100.0, 100.0, 1.0),
            station1,
            200.0, // 200 L de fuel para entregar
            15.0,  // 15% de O2 para entregar
            10.0
        );
        cargo1.registrarAcopleExitoso();
        cargo1.iniciarTransferencia();

        double cargoFuelInicial = cargo1.getCargoFuel();
        double cargoO2Inicial = cargo1.getCargoOxygen();

        // Ejecutar pasos de transferencia hasta completar
        int safetyTicks = 0;
        while (cargo1.getMissionState() == MissionState.TRANSFIRIENDO && safetyTicks < 50) {
            cargo1.ejecutarPasoTransferencia();
            safetyTicks++;
        }

        double deltaFuelEstacion = station1.getFuelTank().getCurrentLevel() - stationFuelInicial;
        double deltaFuelCargo = cargoFuelInicial - cargo1.getCargoFuel();
        double deltaO2Estacion = station1.getOxygenLevel() - stationO2Inicial;
        double deltaO2Cargo = cargoO2Inicial - cargo1.getCargoOxygen();

        boolean fuelConservaMasa = Math.abs(deltaFuelEstacion - deltaFuelCargo) < 0.001 && deltaFuelCargo > 199.0;
        boolean o2ConservaMasa = Math.abs(deltaO2Estacion - deltaO2Cargo) < 0.001 && deltaO2Cargo > 14.9;
        boolean misionCompletada = (cargo1.getMissionState() == MissionState.COMPLETADA);

        if (fuelConservaMasa && o2ConservaMasa && misionCompletada) {
            System.out.println("[OK] Caso 1 - Conservación de masa: Fuel (" + deltaFuelCargo + " L) y O2 (" + deltaO2Cargo + " %) transferidos exactamente sin pérdida.");
        } else {
            System.err.println("[ERROR] Caso 1 - Error en conservación de masa: FuelDelta=" + deltaFuelEstacion + " vs " + deltaFuelCargo + ", O2Delta=" + deltaO2Estacion + " vs " + deltaO2Cargo);
            fallos++;
        }

        // Caso 2: Estación casi llena (no sobrepasar capacidad máxima ni 100%)
        SpaceStation station2 = new SpaceStation(
            new SpacecraftInfo("ISS", "ISS Casi Llena", 25544),
            new GeoPosition(-34.60, -58.38, 420.0)
        );
        // Llenar al tope y dejar solo 10 L de espacio de fuel (2490 / 2500) y 1.5% de O2 (98.5 / 100)
        station2.getFuelTank().refuel(2500.0);
        station2.getOxygenGauge().increase(100.0);
        station2.getFuelTank().consume(10.0);
        station2.getOxygenGauge().decrease(1.5);

        CargoShip cargo2 = new CargoShip(
            new SpacecraftInfo("CRG-02", "KPS-Cargo-02", 70002),
            new GeoPosition(-34.60, -58.38, 420.0),
            new FuelTank(100.0, 100.0, 1.0),
            station2,
            300.0, // El carguero tiene 300 L (mucho más de los 10 L que entran)
            25.0,  // El carguero tiene 25% de O2 (mucho más del 1.5% que entra)
            10.0
        );
        cargo2.registrarAcopleExitoso();
        cargo2.iniciarTransferencia();

        safetyTicks = 0;
        while (cargo2.getMissionState() == MissionState.TRANSFIRIENDO && safetyTicks < 50) {
            cargo2.ejecutarPasoTransferencia();
            safetyTicks++;
        }

        boolean fuelTopeExacto = (station2.getFuelTank().getCurrentLevel() == 2500.0) && station2.getFuelTank().isFull();
        boolean o2TopeExacto = (station2.getOxygenLevel() == 100.0);
        boolean fuelTransferidoExacto = (Math.abs(cargo2.getTotalFuelTransferred() - 10.0) < 0.001);
        boolean o2TransferidoExacto = (Math.abs(cargo2.getTotalOxygenTransferred() - 1.5) < 0.001);
        boolean completoPorEstacionLlena = (cargo2.getMissionState() == MissionState.COMPLETADA);

        if (fuelTopeExacto && o2TopeExacto && fuelTransferidoExacto && o2TransferidoExacto && completoPorEstacionLlena) {
            System.out.println("[OK] Caso 2 - Estación casi llena: Tanque topeado a 2500 L y O2 al 100.0% sin desbordes. Transferidos exactamente 10 L y 1.5%.");
        } else {
            System.err.println("[ERROR] Caso 2 - Desborde detectado: Fuel=" + station2.getFuelTank().getCurrentLevel() + ", O2=" + station2.getOxygenLevel());
            fallos++;
        }

        // Caso 3: Carguero sin combustible en tránsito
        SpaceStation station3 = new SpaceStation(
            new SpacecraftInfo("ISS", "ISS Destino", 25544),
            new GeoPosition(-34.60, -58.38, 420.0)
        );
        // Posicionar carguero a 100 km de la estación con solo 0.2 L de combustible de maniobra
        CargoShip cargo3 = new CargoShip(
            new SpacecraftInfo("CRG-03", "KPS-Cargo-03", 70003),
            new GeoPosition(-33.70, -58.38, 420.0), // ~100 km
            new FuelTank(50.0, 0.2, 0.0),           // Combustible casi nulo
            station3,
            200.0,
            20.0,
            10.0
        );

        cargo3.avanzarHaciaEstacion(); // Primer paso consume el remanente
        cargo3.avanzarHaciaEstacion(); // Segundo paso detecta tanque vacío

        boolean abortoSinFuel = (cargo3.getMissionState() == MissionState.ABORTADA);
        boolean motivoCorrecto = cargo3.getAbortReason().toLowerCase().contains("sin combustible");

        if (abortoSinFuel && motivoCorrecto) {
            System.out.println("[OK] Caso 3 - Carguero sin combustible en tránsito: Misión abortada correctamente (\"" + cargo3.getAbortReason() + "\").");
        } else {
            System.err.println("[ERROR] Caso 3 - No se abortó por falta de combustible: Estado=" + cargo3.getMissionState());
            fallos++;
        }

        // Caso 4: 3 intentos fallidos de acople llevan a ABORTADA
        CargoShip cargo4 = new CargoShip(
            new SpacecraftInfo("CRG-04", "KPS-Cargo-04", 70004),
            new GeoPosition(-34.60, -58.38, 420.0),
            new FuelTank(100.0, 100.0, 0.0),
            station3,
            200.0,
            20.0,
            10.0
        );
        cargo4.setMissionState(MissionState.EN_RANGO_DE_ACOPLE);

        boolean puede1 = cargo4.procesarIntentoFallido();
        int intentos1 = cargo4.getDockingAttemptsRemaining();

        boolean puede2 = cargo4.procesarIntentoFallido();
        int intentos2 = cargo4.getDockingAttemptsRemaining();

        boolean puede3 = cargo4.procesarIntentoFallido(); // 3er intento fallido
        int intentos3 = cargo4.getDockingAttemptsRemaining();

        boolean abortoPor3Fallos = (cargo4.getMissionState() == MissionState.ABORTADA);

        if (puede1 && intentos1 == 2 && puede2 && intentos2 == 1 && !puede3 && intentos3 == 0 && abortoPor3Fallos) {
            System.out.println("[OK] Caso 4 - Tres fallos consecutivos en minijuego: Descuenta intentos (2, 1, 0) y transiciona a ABORTADA.");
        } else {
            System.err.println("[ERROR] Caso 4 - Fallo en ciclo de 3 intentos: " + intentos1 + ", " + intentos2 + ", " + intentos3);
            fallos++;
        }

        // Caso 5: Validaciones de Spawn en MissionController
        SimulationEngine engine = new SimulationEngine("", 25544);
        AlertQueue alertQueue = new AlertQueue(() -> {}, () -> {});
        StringBuilder logMensajes = new StringBuilder();
        MissionController controller = new MissionController(engine, alertQueue, logMensajes::append);

        // A. Lanzar con estación llena (rellenar al 100% para verificar la validación)
        SpaceStation targetStation = engine.getTargetStation();
        targetStation.getFuelTank().refuel(2500.0);
        targetStation.getOxygenGauge().increase(100.0);
        boolean lanzadoConEstacionLlena = controller.lanzarCarguero(null, 25544);
        boolean rechazoEstacionLlena = !lanzadoConEstacionLlena && logMensajes.toString().contains("100%");

        // B. Consumir suministros de la estación para permitir lanzamientos
        targetStation.getFuelTank().consume(500.0);
        targetStation.getOxygenGauge().decrease(20.0);

        // C. Lanzar 3 cargueros (debe permitir hasta 3)
        boolean l1 = controller.lanzarCarguero(null, 25544);
        boolean l2 = controller.lanzarCarguero(null, 25544);
        boolean l3 = controller.lanzarCarguero(null, 25544);

        // D. Lanzar 4to carguero (debe ser rechazado por límite de 3)
        logMensajes.setLength(0);
        boolean l4 = controller.lanzarCarguero(null, 25544);
        boolean rechazoLimite3 = !l4 && logMensajes.toString().contains("Límite");

        if (rechazoEstacionLlena && l1 && l2 && l3 && rechazoLimite3) {
            System.out.println("[OK] Caso 5 - Validaciones de spawn: Rechazo si estación llena y límite estricto de máximo 3 cargueros activos simultáneos.");
        } else {
            System.err.println("[ERROR] Caso 5 - Error en validaciones de spawn: Llena=" + rechazoEstacionLlena + ", l1=" + l1 + ", l2=" + l2 + ", l3=" + l3 + ", Limite=" + rechazoLimite3);
            fallos++;
        }

        // Caso 6: Radar no genera alerta de colisión para CargoShip
        radar.Radar testRadar = new radar.Radar("TestCity", station3.getPosition(), 100.0);
        List<model.spacecraft.OrbitalObject> listaConCarguero = new ArrayList<>();
        listaConCarguero.add(station3);
        listaConCarguero.add(cargo1);
        List<String> alertasRadar = testRadar.detectCollisionRisks(listaConCarguero);
        if (alertasRadar.isEmpty()) {
            System.out.println("[OK] Caso 6 - Radar: CargoShip excluido exitosamente de alertas de colision.");
        } else {
            System.err.println("[ERROR] Caso 6 - Radar genero falsas alertas de colision para CargoShip: " + alertasRadar);
            fallos++;
        }

        // Caso 7: Posicionamiento tactico en acople y tasa rapida de combustible
        cargo1.posicionarEnAcople();
        double dAcople = cargo1.distanceTo(station3);
        boolean distanciaAcopleOk = (dAcople >= 5.0 && dAcople <= 15.0); // Cerca pero no superpuesto
        if (distanciaAcopleOk && CargoShip.TRANSFER_RATE_FUEL_PER_TICK >= 100.0) {
            System.out.println("[OK] Caso 7 - Posicionamiento de acople: Distancia tactica=" + String.format("%.2f", dAcople) + " km (no superpuesto) y tasa rapida (" + CargoShip.TRANSFER_RATE_FUEL_PER_TICK + " L/tick).");
        } else {
            System.err.println("[ERROR] Caso 7 - Distancia o tasa invalida: Distancia=" + dAcople + " km, Tasa=" + CargoShip.TRANSFER_RATE_FUEL_PER_TICK);
            fallos++;
        }

        System.out.println("========================================================");
        if (fallos == 0) {
            System.out.println("RESULTADO: TODAS LAS PRUEBAS DE MISIÓN PASARON EXITOSAMENTE.");
        } else {
            System.err.println("RESULTADO: " + fallos + " PRUEBAS FALLARON.");
            System.exit(1);
        }
    }
}
