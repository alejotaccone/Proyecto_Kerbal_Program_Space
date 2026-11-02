package test;

import gui.ControlPanel;
import model.components.FuelTank;
import model.geometry.GeoPosition;
import model.spacecraft.CargoShip;
import model.spacecraft.ExplorationProbe;
import model.spacecraft.MissionState;
import model.spacecraft.SpaceStation;
import model.spacecraft.SpacecraftInfo;

/**
 * Suite de verificación automatizada para:
 * 1. Consumo proporcional de combustible en maniobras de evasión normal (-100L) y crítica (-200L).
 * 2. Comportamiento en naves de exploración (-10L) y cargueros (-15L + carga*0.5).
 * 3. Fallo de evasión ante combustible insuficiente.
 * 4. Cooldown de 15 ticks para el lanzamiento de cargueros de reabastecimiento en la GUI.
 * 5. Sincronización instantánea de cargueros acoplados cuando la estación evade.
 * 6. Cinemática de desorbitado y reingreso atmosférico con CargoShipProgress_Desorbitando.jpg.
 */
public class EvasionAndCooldownCheck {

    public static void main(String[] args) {
        System.out.println("=== EJECUTANDO PRUEBAS DE EVASIÓN PROPORCIONAL Y COOLDOWN ===");

        testConsumoEvasionEstacionNormal();
        testConsumoEvasionEstacionCrisis();
        testFalloEvasionCombustibleInsuficiente();
        testConsumoEvasionOtrasNaves();
        testCooldownLanzamientoCarguero();
        testSincronizacionCargueroEnEvasion();
        testDesorbitadoCinematico();
        testAnomaliaUnificadaUnTickParaCrisis();
        testLimpiezaBasuraPasivaMasDe200Km();
        testBitacoraMisionYExportacion();

        System.out.println("=============================================================");
        System.out.println("RESULTADO: TODAS LAS PRUEBAS DE EVASIÓN Y COOLDOWN PASARON CON ÉXITO.");
    }

    private static void testConsumoEvasionEstacionNormal() {
        SpaceStation station = new SpaceStation(
            new SpacecraftInfo("ISS", "ISS Test", 25544),
            new GeoPosition(-34.60, -58.38, 420.0)
        );
        // El tanque de la estación tiene capacidad 2500 L
        station.getFuelTank().refuel(2500.0);

        double fuelInicial = station.getFuelTank().getCurrentLevel();
        if (station.getConsumoEvasion() != 100.0) {
            throw new AssertionError("El consumo normal de evasión de la estación debe ser 100.0 L pero fue: " + station.getConsumoEvasion());
        }

        boolean success = station.evade(0.5, 0.5);
        if (!success) {
            throw new AssertionError("La maniobra de evasión normal debería ser exitosa con tanque lleno.");
        }

        double fuelEsperado = fuelInicial - 100.0;
        double fuelActual = station.getFuelTank().getCurrentLevel();
        if (Math.abs(fuelActual - fuelEsperado) > 0.001) {
            throw new AssertionError("Consumo erróneo: esperado " + fuelEsperado + ", obtenido " + fuelActual);
        }

        System.out.println("[OK] Caso 1 - Evasión normal de estación: consume exactamente 100 L (restante: " + fuelActual + " L).");
    }

    private static void testConsumoEvasionEstacionCrisis() {
        SpaceStation station = new SpaceStation(
            new SpacecraftInfo("ISS", "ISS Test", 25544),
            new GeoPosition(-34.60, -58.38, 420.0)
        );
        station.getFuelTank().refuel(2500.0);

        if (station.getConsumoEvasionCrisis() != 200.0) {
            throw new AssertionError("El consumo de crisis de la estación debe ser 200.0 L (el doble) pero fue: " + station.getConsumoEvasionCrisis());
        }

        double fuelInicial = station.getFuelTank().getCurrentLevel();
        boolean success = station.evade(0.8, 0.8, station.getConsumoEvasionCrisis());
        if (!success) {
            throw new AssertionError("La maniobra de evasión de crisis debería ser exitosa.");
        }

        double fuelEsperado = fuelInicial - 200.0;
        double fuelActual = station.getFuelTank().getCurrentLevel();
        if (Math.abs(fuelActual - fuelEsperado) > 0.001) {
            throw new AssertionError("Consumo de crisis erróneo: esperado " + fuelEsperado + ", obtenido " + fuelActual);
        }

        System.out.println("[OK] Caso 2 - Evasión crítica de estación ante colisión inminente: consume exactamente 200 L (restante: " + fuelActual + " L).");
    }

    private static void testFalloEvasionCombustibleInsuficiente() {
        SpaceStation station = new SpaceStation(
            new SpacecraftInfo("ISS", "ISS Test", 25544),
            new GeoPosition(-34.60, -58.38, 420.0)
        );
        // Vaciar tanque consumiendo todo
        station.getFuelTank().consume(station.getFuelTank().getCurrentLevel());
        // Dejar con 150 L
        station.getFuelTank().refuel(150.0);

        // Con 150 L, la maniobra normal (100 L) debe tener éxito
        boolean normalOk = station.evade(0.5, 0.5);
        if (!normalOk) {
            throw new AssertionError("Con 150 L la estación debió poder evadir normalmente (100 L).");
        }
        // Ahora quedan 50 L
        if (Math.abs(station.getFuelTank().getCurrentLevel() - 50.0) > 0.001) {
            throw new AssertionError("Debieron quedar 50 L tras maniobra normal.");
        }

        // Intento de evasión normal con solo 50 L -> debe fallar
        boolean normalFalla = station.evade(0.5, 0.5);
        if (normalFalla) {
            throw new AssertionError("Con 50 L la estación NO debió poder evadir normalmente (requiere 100 L).");
        }

        // Intento de evasión crítica con solo 50 L -> debe fallar
        boolean crisisFalla = station.evade(0.8, 0.8, station.getConsumoEvasionCrisis());
        if (crisisFalla) {
            throw new AssertionError("Con 50 L la estación NO debió poder evadir forzadamente en crisis (requiere 200 L).");
        }

        System.out.println("[OK] Caso 3 - Rechazo y fallo consistente de evasión cuando el combustible es inferior a la maniobra requerida.");
    }

    private static void testConsumoEvasionOtrasNaves() {
        ExplorationProbe probe = new ExplorationProbe(
            new SpacecraftInfo("VOY", "Voyager-Test", 99991),
            new GeoPosition(10, 10, 500),
            new FuelTank(100.0, 100.0, 1.0),
            50.0
        );
        if (probe.getConsumoEvasion() != 10.0 || probe.getConsumoEvasionCrisis() != 20.0) {
            throw new AssertionError("Sonda: consumo normal debe ser 10 L y crisis 20 L.");
        }

        CargoShip cargoVacio = new CargoShip(
            new SpacecraftInfo("CRG-0", "Cargo-Vacio", 70010),
            new GeoPosition(0, 0, 400),
            new FuelTank(100.0, 100.0, 1.0),
            null,
            0.0,
            0.0
        );
        if (cargoVacio.getConsumoEvasion() != 15.0) {
            throw new AssertionError("Carguero vacío debe consumir 15 L pero dio: " + cargoVacio.getConsumoEvasion());
        }

        CargoShip cargoLleno = new CargoShip(
            new SpacecraftInfo("CRG-10", "Cargo-Lleno", 70011),
            new GeoPosition(0, 0, 400),
            new FuelTank(100.0, 100.0, 1.0),
            null,
            200.0,
            10.0
        );
        // Con 10t de capacidad, currentCargoTons = 8.0t -> 15 + 8*0.5 = 19.0 L
        if (cargoLleno.getConsumoEvasion() != 19.0) {
            throw new AssertionError("Carguero con 10t debe consumir 19 L pero dio: " + cargoLleno.getConsumoEvasion());
        }

        System.out.println("[OK] Caso 4 - Consumo proporcional segregado en sondas (10 L) y cargueros según peso de carga (15 L a 19 L).");
    }

    private static void testCooldownLanzamientoCarguero() {
        ControlPanel panel = new ControlPanel();
        panel.setEstadoOnline(true);

        if (!panel.getBtnLanzarCarguero().isEnabled()) {
            throw new AssertionError("El botón de lanzar carguero debe estar habilitado cuando el sistema está online y no hay cooldown.");
        }
        if (!panel.getBtnLanzarCarguero().getText().equals("[>] Lanzar Carguero")) {
            throw new AssertionError("Texto inicial del botón incorrecto: " + panel.getBtnLanzarCarguero().getText());
        }

        // Simular inicio de cooldown de 15 ticks
        panel.setCargueroCooldown(15);
        if (panel.getBtnLanzarCarguero().isEnabled()) {
            throw new AssertionError("El botón debe estar deshabilitado durante el cooldown.");
        }
        if (!panel.getBtnLanzarCarguero().getText().equals("[>] Carguero (Espera: 15t)")) {
            throw new AssertionError("Texto de cooldown incorrecto: " + panel.getBtnLanzarCarguero().getText());
        }

        // Simular avance de ticks
        for (int t = 14; t > 0; t--) {
            panel.setCargueroCooldown(t);
            if (panel.getBtnLanzarCarguero().isEnabled()) {
                throw new AssertionError("El botón debe continuar deshabilitado en tick " + t);
            }
            if (!panel.getBtnLanzarCarguero().getText().equals("[>] Carguero (Espera: " + t + "t)")) {
                throw new AssertionError("Texto de espera incorrecto en tick " + t);
            }
        }

        // Tick 0 -> Se reactiva
        panel.setCargueroCooldown(0);
        if (!panel.getBtnLanzarCarguero().isEnabled()) {
            throw new AssertionError("El botón debe reactivarse automáticamente al terminar los 15 ticks de cooldown.");
        }
        if (!panel.getBtnLanzarCarguero().getText().equals("[>] Lanzar Carguero")) {
            throw new AssertionError("El texto debe restaurarse a '[>] Lanzar Carguero'.");
        }

        System.out.println("[OK] Caso 5 - Cooldown de 15 ticks: deshabilita el botón, muestra cuenta regresiva y se rehabilita en 0 ticks.");
    }

    private static void testSincronizacionCargueroEnEvasion() {
        engine.SimulationEngine engine = new engine.SimulationEngine("", 25544);
        SpaceStation station = engine.getTargetStation();
        station.getFuelTank().refuel(2500.0);

        CargoShip cargo = engine.spawnCargoShip(station);
        cargo.registrarAcopleExitoso();
        cargo.iniciarTransferencia(); // missionState == TRANSFIRIENDO

        // Distancia inicial de acople
        double distInicial = cargo.distanceTo(station);
        if (distInicial > 12.0) {
            throw new AssertionError("El carguero debe estar en radio de acople (~9 km) pero está a: " + distInicial);
        }

        // 1. Maniobra normal de estación vía engine
        int stationIdx = engine.getTrackedObjects().indexOf(station);
        boolean normalOk = engine.evadeShip(stationIdx);
        if (!normalOk) throw new AssertionError("Evasión normal de estación falló.");

        double distPostNormal = cargo.distanceTo(station);
        if (Math.abs(distPostNormal - distInicial) > 0.1) {
            throw new AssertionError("El carguero perdió el acople tras evasión normal: distancia=" + distPostNormal);
        }
        if (cargo.getMissionState() != MissionState.TRANSFIRIENDO) {
            throw new AssertionError("El carguero debió seguir transfiriendo.");
        }

        // 2. Maniobra forzada de crisis vía engine
        boolean crisisOk = engine.forzarEvasionCrisis(station);
        if (!crisisOk) throw new AssertionError("Evasión de crisis falló.");

        double distPostCrisis = cargo.distanceTo(station);
        if (Math.abs(distPostCrisis - distInicial) > 0.1) {
            throw new AssertionError("El carguero se desfasó tras evasión de crisis: distancia=" + distPostCrisis);
        }
        if (cargo.getMissionState() != MissionState.TRANSFIRIENDO) {
            throw new AssertionError("El carguero debió seguir transfiriendo tras evasión de crisis sincronizada.");
        }

        System.out.println("[OK] Caso 6 - Sincronización orbital: El carguero acoplado acompaña instantáneamente a la estación en evasión normal y de crisis sin desfase.");
    }

    private static void testDesorbitadoCinematico() {
        CargoShip cargo = new CargoShip(
            new SpacecraftInfo("CRG-TEST", "KPS-Cargo-Test", 70099),
            new GeoPosition(-34.60, -58.38, 420.0),
            new FuelTank(100.0, 100.0, 1.0),
            null,
            0.0,
            10.0
        );

        cargo.iniciarDesorbitado();
        if (cargo.getMissionState() != MissionState.DESORBITANDO) {
            throw new AssertionError("El carguero debió entrar en estado DESORBITANDO.");
        }
        if (!cargo.getNombreImagen().equals("CargoShipProgress_Desorbitando.jpg")) {
            throw new AssertionError("La imagen en desorbitado debe ser CargoShipProgress_Desorbitando.jpg pero fue: " + cargo.getNombreImagen());
        }
        if (cargo.getColorRadar().getRed() != 255 || cargo.getColorRadar().getGreen() != 60) {
            throw new AssertionError("Color radar en desorbitado erróneo: " + cargo.getColorRadar());
        }

        // Tick 1 de desorbitado
        cargo.move();
        if (cargo.getPosition().getAltitude() > 385.0) {
            throw new AssertionError("La altitud debió descender a ~380 km pero está en: " + cargo.getPosition().getAltitude());
        }
        if (cargo.getMissionState() != MissionState.DESORBITANDO) {
            throw new AssertionError("En tick 1 debe continuar desorbitando.");
        }

        // Tick 2 de desorbitado -> Debe concluir
        cargo.move();
        if (cargo.getMissionState() != MissionState.REINGRESO_CONCLUIDO) {
            throw new AssertionError("En tick 2 debe finalizar como REINGRESO_CONCLUIDO.");
        }
        if (!cargo.getMissionState().isFinalizada()) {
            throw new AssertionError("El estado de reingreso concluido debe ser finalizado.");
        }

        System.out.println("[OK] Caso 7 - Cinemática de desorbitado: Transición visual a CargoShipProgress_Desorbitando.jpg, descenso de altitud y conclusión de reingreso.");
    }

    private static void testAnomaliaUnificadaUnTickParaCrisis() {
        engine.SimulationEngine engine = new engine.SimulationEngine("", 25544);
        SpaceStation station = engine.getTargetStation();

        boolean spawned = engine.triggerCrisisEvent();
        if (!spawned) {
            throw new AssertionError("No se pudo generar la anomalía cinética.");
        }

        model.spacecraft.RogueDebris rogue = null;
        for (model.spacecraft.OrbitalObject obj : engine.getTrackedObjects()) {
            if (obj instanceof model.spacecraft.RogueDebris) {
                rogue = (model.spacecraft.RogueDebris) obj;
                break;
            }
        }
        if (rogue == null) {
            throw new AssertionError("No se encontró la instancia de RogueDebris en trackedObjects.");
        }

        double distInicial = rogue.distanceTo(station);
        if (distInicial < 100.0 || distInicial > 125.0) {
            throw new AssertionError("La anomalía debe aparecer justo fuera del umbral de 100 km (entre 100 y 125 km) para ser visible 1 tick en radar, pero apareció a: " + distInicial + " km");
        }

        // Al avanzar 1 tick, debe ingresar a la zona de crisis (< 100 km)
        rogue.move();
        double distPostUnTick = rogue.distanceTo(station);
        if (distPostUnTick >= 100.0) {
            throw new AssertionError("Tras 1 tick la anomalía debió ingresar a la zona crítica (< 100 km), pero quedó a: " + distPostUnTick + " km");
        }

        System.out.println(String.format(java.util.Locale.US,
                "[OK] Caso 8 - Anomalía unificada a 1 tick de impacto: nace a %.1f km (>100 km, visible en radar) y tras 1 tick avanza a %.1f km (<100 km, dispara dinámica de evasión/choque).",
                distInicial, distPostUnTick));
    }

    private static void testLimpiezaBasuraPasivaMasDe200Km() {
        engine.SimulationEngine engine = new engine.SimulationEngine("", 25544);
        SpaceStation station = engine.getTargetStation();

        model.spacecraft.SpaceDebris debrisCercana = new model.spacecraft.SpaceDebris(
                new SpacecraftInfo("DEB-1", "Basura Cercana", 90001),
                new GeoPosition(station.getPosition().getLatitude() + 0.3, station.getPosition().getLongitude(), station.getPosition().getAltitude()),
                5.0
        );
        model.spacecraft.SpaceDebris debrisLejana = new model.spacecraft.SpaceDebris(
                new SpacecraftInfo("DEB-2", "Basura Lejana", 90002),
                new GeoPosition(station.getPosition().getLatitude() + 2.5, station.getPosition().getLongitude(), station.getPosition().getAltitude()),
                5.0
        );

        engine.getTrackedObjects().add(debrisCercana);
        engine.getTrackedObjects().add(debrisLejana);

        engine.limpiarBasuraPasivaLejana();

        if (!engine.getTrackedObjects().contains(debrisCercana)) {
            throw new AssertionError("La basura pasiva a < 200 km NO debía eliminarse.");
        }
        if (engine.getTrackedObjects().contains(debrisLejana)) {
            throw new AssertionError("La basura pasiva a > 200 km debía desaparecer automáticamente.");
        }

        System.out.println("[OK] Caso 9 - Basura espacial pasiva desaparece automáticamente al superar los 200 km de distancia de la estación.");
    }

    private static void testBitacoraMisionYExportacion() {
        engine.FlightLogbook logbook = new engine.FlightLogbook();
        logbook.setEstacionActiva("ISS Test");
        logbook.registrarTick();
        logbook.registrarTick();
        logbook.registrarLanzamientoCarguero();
        logbook.registrarIntentoAcople(false);
        logbook.registrarIntentoAcople(true);
        logbook.registrarMisionCompletada(600.0);
        logbook.registrarEvasionNormal(100.0);
        logbook.registrarEvasionCrisis(200.0);

        if (Math.abs(logbook.getPorcentajePrecisionAcople() - 50.0) > 0.01) {
            throw new AssertionError("La precisión de acople con 1 fallo y 1 acierto debe ser 50.0%, fue: " + logbook.getPorcentajePrecisionAcople());
        }
        if (Math.abs(logbook.getTotalCombustibleReabastecido() - 600.0) > 0.01) {
            throw new AssertionError("El combustible reabastecido debe ser 600.0 L.");
        }
        if (logbook.getTotalEvasionesExitosas() != 2 || Math.abs(logbook.getCombustibleConsumidoEvasiones() - 300.0) > 0.01) {
            throw new AssertionError("Las evasiones totales deben ser 2 y el combustible gastado 300.0 L.");
        }

        String txt = logbook.generarReporteTxt();
        String json = logbook.generarReporteJson();
        if (!txt.contains("600.0 L") || !txt.contains("50.0%") || !json.contains("\"porcentajePrecision\": 50.0")) {
            throw new AssertionError("Los reportes .txt y .json no contienen las métricas esperadas.");
        }

        System.out.println("[OK] Caso 10 - Bitácora de Misión (FlightLogbook): Calcula precisión de acople (50.0%), evasiones (2), combustible (+600 L) y genera reportes .txt y .json.");
    }
}
