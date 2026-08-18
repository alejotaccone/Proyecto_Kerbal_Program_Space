package engine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import model.components.Kerbal;
import model.spacecraft.CargoShip;
import model.spacecraft.CrewShuttle;
import model.spacecraft.ExplorationProbe;
import model.spacecraft.SpaceDebris;
import model.spacecraft.Spacecraft;

public class TelemetryLogger {

    private static final String MONITOR_FILE = "nave_monitor.txt";

    public static void printHeader(int tick, String observerCity) {
        System.out.println("\n==================================================================");
        System.out.printf("  CENTRO DE CONTROL ESPACIAL DE %s | TICK #%d\n", observerCity.toUpperCase(), tick);
        System.out.println("==================================================================");
    }

    public static void printSpacecraftStatus(List<Spacecraft> ships) {
        System.out.println("\n--- FLOTA Y OBJETOS ORBITALES RASTREADOS ---");
        if (ships == null || ships.isEmpty()) {
            System.out.println("No hay objetos rastreados en órbita.");
            return;
        }
        for (int i = 0; i < ships.size(); i++) {
            Spacecraft craft = ships.get(i);
            System.out.printf("[%d] %s\n", (i + 1), craft.toString());
            
            // Si es un transbordador tripulado, mostrar miembros
            if (craft instanceof CrewShuttle) {
                CrewShuttle shuttle = (CrewShuttle) craft;
                if (!shuttle.getCrew().isEmpty()) {
                    System.out.print("    Tripulación a bordo: ");
                    for (Kerbal k : shuttle.getCrew()) {
                        System.out.print("[" + k.getName() + " - " + k.getRole() + "] ");
                    }
                    System.out.println();
                }
            }
        }
    }

    public static void printAlerts(List<String> alerts) {
        if (alerts != null && !alerts.isEmpty()) {
            System.out.println("\n------------------------------------------------------------------");
            System.out.println(" ALERTAS CRITICAS DEL RADAR N2YO DETECTADAS:");
            for (String alert : alerts) {
                System.out.println("   -> " + alert);
            }
            System.out.println("------------------------------------------------------------------");
        } else {
            System.out.println("\n[Radar]: Orbita despejada sin alertas de colision inminente.");
        }
    }

    public static void printMessage(String message) {
        System.out.println("\n>> [CENTRO DE CONTROL]: " + message);
    }

    // ==================== MONITOR DE NAVE (Segunda Consola) ====================

    /**
     * Lanza una segunda ventana de consola (PowerShell) que muestra el archivo nave_monitor.txt
     * de forma continua, refrescándolo cada 800ms.
     */
    public static void launchMonitorConsole() {
        try {
            String script = "$host.UI.RawUI.WindowTitle = 'MONITOR DE NAVE - Kerbal Program Space'\r\n"
                    + "while($true) {\r\n"
                    + "    Clear-Host\r\n"
                    + "    if (Test-Path '" + MONITOR_FILE + "') {\r\n"
                    + "        Get-Content '" + MONITOR_FILE + "'\r\n"
                    + "    } else {\r\n"
                    + "        Write-Host ''\r\n"
                    + "        Write-Host '  =================================================================='\r\n"
                    + "        Write-Host '       MONITOR DE NAVE - KERBAL PROGRAM SPACE'\r\n"
                    + "        Write-Host '  =================================================================='\r\n"
                    + "        Write-Host ''\r\n"
                    + "        Write-Host '  Esperando seleccion de nave...'\r\n"
                    + "        Write-Host '  Use la opcion [8] del menu principal para seleccionar una nave.'\r\n"
                    + "        Write-Host ''\r\n"
                    + "    }\r\n"
                    + "    Start-Sleep -Milliseconds 800\r\n"
                    + "}\r\n";

            Files.writeString(Path.of("monitor.ps1"), script);

            new ProcessBuilder("cmd", "/c", "start", "powershell", "-ExecutionPolicy", "Bypass", "-File", "monitor.ps1")
                    .directory(new File(System.getProperty("user.dir")))
                    .start();

        } catch (Exception e) {
            System.out.println("[Sistema]: No se pudo abrir la ventana de monitoreo de nave. " + e.getMessage());
        }
    }

    /**
     * Escribe la información detallada de la nave seleccionada en el archivo nave_monitor.txt.
     * La segunda consola lee este archivo y lo muestra automáticamente.
     */
    public static void writeShipMonitorFile(Spacecraft ship, String lastAction) {
        if (ship == null) return;

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("==================================================================\n");
            sb.append("       MONITOR DE NAVE - KERBAL PROGRAM SPACE\n");
            sb.append("==================================================================\n\n");
            sb.append("  Nave:     ").append(ship.getName()).append("\n");
            sb.append("  Tipo:     ").append(ship.getType()).append("\n");
            sb.append("  NORAD ID: ").append(ship.getNoradId()).append("\n");
            sb.append("  ID:       ").append(ship.getId()).append("\n");

            sb.append("\n------------------------------------------------------------------\n");
            sb.append("  POSICION ACTUAL\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append(String.format("  Latitud:  %.4f\n", ship.getPosition().getLatitude()));
            sb.append(String.format("  Longitud: %.4f\n", ship.getPosition().getLongitude()));
            sb.append(String.format("  Altitud:  %.1f km\n", ship.getPosition().getAltitude()));

            sb.append("\n------------------------------------------------------------------\n");
            sb.append("  COMBUSTIBLE\n");
            sb.append("------------------------------------------------------------------\n");
            if (ship.getFuelTank() != null) {
                double pct = ship.getFuelTank().getPercentage();
                int filled = (int) (pct / 5);
                int empty = 20 - filled;
                sb.append("  [");
                for (int i = 0; i < filled; i++) sb.append("#");
                for (int i = 0; i < empty; i++) sb.append("-");
                sb.append(String.format("] %.1f%%\n", pct));
                sb.append(String.format("  Nivel: %.1f / %.1f L\n", 
                        ship.getFuelTank().getCurrentLevel(), ship.getFuelTank().getCapacity()));
                sb.append(String.format("  Consumo por movimiento: %.1f L\n", ship.getFuelTank().getConsumptionRate()));
            } else {
                sb.append("  N/A (Sin motor - Objeto a la deriva)\n");
            }

            sb.append("\n------------------------------------------------------------------\n");
            sb.append("  ESTADO GENERAL\n");
            sb.append("------------------------------------------------------------------\n");
            sb.append("  Escudo:    ").append(ship.isShieldActive() ? ">> ACTIVADO <<" : "DESACTIVADO").append("\n");
            sb.append(String.format("  Velocidad: %.1f km/h\n", ship.getVelocityKmH()));

            // Información específica según el tipo de nave (polimorfismo)
            if (ship instanceof CrewShuttle) {
                CrewShuttle shuttle = (CrewShuttle) ship;
                sb.append("\n------------------------------------------------------------------\n");
                sb.append("  TRIPULACION A BORDO\n");
                sb.append("------------------------------------------------------------------\n");
                if (shuttle.getCrew().isEmpty()) {
                    sb.append("  (Sin tripulacion a bordo)\n");
                } else {
                    for (Kerbal k : shuttle.getCrew()) {
                        sb.append("  - ").append(k.toString()).append("\n");
                    }
                }
            } else if (ship instanceof CargoShip) {
                CargoShip cargo = (CargoShip) ship;
                sb.append("\n------------------------------------------------------------------\n");
                sb.append("  CARGA\n");
                sb.append("------------------------------------------------------------------\n");
                sb.append(String.format("  Capacidad: %.1f toneladas\n", cargo.getCargoCapacityTons()));
                sb.append(String.format("  Carga actual: %.1f toneladas\n", cargo.getCurrentCargoTons()));
            } else if (ship instanceof ExplorationProbe) {
                ExplorationProbe probe = (ExplorationProbe) ship;
                sb.append("\n------------------------------------------------------------------\n");
                sb.append("  PANELES SOLARES\n");
                sb.append("------------------------------------------------------------------\n");
                sb.append(String.format("  Eficiencia solar: %.0f%%\n", probe.getSolarEfficiency() * 100));
            } else if (ship instanceof SpaceDebris) {
                SpaceDebris debris = (SpaceDebris) ship;
                sb.append("\n------------------------------------------------------------------\n");
                sb.append("  PELIGROSIDAD\n");
                sb.append("------------------------------------------------------------------\n");
                sb.append(String.format("  Nivel de riesgo: %.1f / 10\n", debris.getHazardLevel()));
            }

            sb.append("\n==================================================================\n");
            sb.append("  ULTIMA ACCION REGISTRADA\n");
            sb.append("==================================================================\n");
            sb.append("  ").append(lastAction != null && !lastAction.isEmpty() ? lastAction : "Ninguna accion registrada aun.").append("\n");
            sb.append("==================================================================\n");

            Files.writeString(Path.of(MONITOR_FILE), sb.toString());
        } catch (Exception e) {
            // Error al escribir archivo de monitor
        }
    }
}
