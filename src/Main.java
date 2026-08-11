import engine.SimulationEngine;
import java.util.List;
import java.util.Scanner;
import model.spacecraft.Spacecraft;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================================");
        System.out.println("   KERBAL PROGRAM SPACE - CENTRO DE CONTROL Y MONITOREO ORBITAL   ");
        System.out.println("   (Integración de Datos N2YO NORAD & Radar Terrestre)           ");
        System.out.println("==================================================================");

        System.out.print("Ingresa tu API Key de N2YO (O presiona ENTER para Modo Simulación/Demo): ");
        String apiKeyInput = scanner.nextLine().trim();

        SimulationEngine engine = new SimulationEngine(apiKeyInput);

        System.out.println("\n[Sistema]: Inicialización completa. Se han cargado las naves de prueba y objetos N2YO.");

        boolean running = true;
        while (running) {
            System.out.println("\n------------------------------------------------------------------");
            System.out.println("                     MENÚ DEL CENTRO DE CONTROL                   ");
            System.out.println("------------------------------------------------------------------");
            System.out.println("1. Avanzar Simulación (Ejecutar Tick)");
            System.out.println("2. Ejecutar Maniobra de Evasión Orbital en una Nave");
            System.out.println("3. Activar Habilidad Especial de la Nave");
            System.out.println("4. Intentar Acople y Recarga de Combustible en la Estación (ISS)");
            System.out.println("5. Activar / Desactivar Escudo de Protección");
            System.out.println("6. Configurar / Cambiar API Key de N2YO");
            System.out.println("0. Salir del Simulador");
            System.out.print("\nSelecciona una opción (0-6): ");

            String choiceStr = scanner.nextLine().trim();

            switch (choiceStr) {
                case "1":
                    engine.tick();
                    break;

                case "2":
                    int shipEvadeIndex = selectShipPrompt(scanner, engine.getTrackedObjects(), "evacuar/evadir");
                    if (shipEvadeIndex != -1) {
                        engine.evadeShip(shipEvadeIndex);
                    }
                    break;

                case "3":
                    int shipAbilityIndex = selectShipPrompt(scanner, engine.getTrackedObjects(), "usar su habilidad especial");
                    if (shipAbilityIndex != -1) {
                        engine.useSpecialAbility(shipAbilityIndex);
                    }
                    break;

                case "4":
                    int shipRefuelIndex = selectShipPrompt(scanner, engine.getTrackedObjects(), "repostar en la Estación");
                    if (shipRefuelIndex != -1) {
                        engine.refuelShipAtStation(shipRefuelIndex);
                    }
                    break;

                case "5":
                    int shipShieldIndex = selectShipPrompt(scanner, engine.getTrackedObjects(), "conmutar escudo de protección");
                    if (shipShieldIndex != -1) {
                        engine.toggleShield(shipShieldIndex);
                    }
                    break;

                case "6":
                    System.out.print("Ingresa la nueva API Key de N2YO: ");
                    String newKey = scanner.nextLine().trim();
                    engine.getApiClient().setApiKey(newKey);
                    System.out.println("API Key actualizada. Sincronizando datos N2YO...");
                    engine.syncWithN2YO();
                    break;

                case "0":
                    running = false;
                    System.out.println("\n=== CERRANDO CENTRO DE CONTROL ESPACIAL. ¡HASTA LUEGO! ===");
                    break;

                default:
                    System.out.println("Opción inválida. Intenta nuevamente.");
                    break;
            }
        }

        scanner.close();
    }

    private static int selectShipPrompt(Scanner scanner, List<Spacecraft> ships, String actionName) {
        if (ships == null || ships.isEmpty()) {
            System.out.println("No hay naves disponibles.");
            return -1;
        }

        System.out.println("\nSelecciona la nave sobre la cual deseas " + actionName + ":");
        for (int i = 0; i < ships.size(); i++) {
            Spacecraft s = ships.get(i);
            System.out.printf("  [%d] %s (%s) - Pos: %s\n", (i + 1), s.getName(), s.getType(), s.getPosition().toString());
        }
        System.out.print("Número de nave (1-" + ships.size() + ") o 0 para cancelar: ");

        try {
            int selection = Integer.parseInt(scanner.nextLine().trim());
            if (selection >= 1 && selection <= ships.size()) {
                return selection - 1;
            }
        } catch (NumberFormatException e) {
            // Entrada inválida
        }

        System.out.println("Operación cancelada.");
        return -1;
    }
}
