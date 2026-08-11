package engine;

import java.util.List;
import model.components.Kerbal;
import model.spacecraft.CrewShuttle;
import model.spacecraft.Spacecraft;

public class TelemetryLogger {

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
            System.out.println(" ⚠️ ALERTAS CRÍTICAS DEL RADAR N2YO DETECTADAS:");
            for (String alert : alerts) {
                System.out.println("   -> " + alert);
            }
            System.out.println("------------------------------------------------------------------");
        } else {
            System.out.println("\n[Radar]: Órbita despejada sin alertas de colisión inminente.");
        }
    }

    public static void printMessage(String message) {
        System.out.println("\n>> [CENTRO DE CONTROL]: " + message);
    }
}
