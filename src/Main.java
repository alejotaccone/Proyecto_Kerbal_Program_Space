import engine.SimulationEngine;
import engine.TelemetryLogger;
import java.util.List;
import java.util.Scanner;
import model.spacecraft.Spacecraft;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================================");
        System.out.println("   KERBAL PROGRAM SPACE - CENTRO DE CONTROL Y MONITOREO ORBITAL   ");
        System.out.println("   (Integracion de Datos N2YO NORAD & Radar Terrestre)            ");
        System.out.println("==================================================================");

        System.out.print("Ingresa tu API Key de N2YO (O presiona ENTER para usar tu clave guardada): ");
        String apiKeyInput = scanner.nextLine().trim();
        if (apiKeyInput.isEmpty()) {
            apiKeyInput = api.N2YOApiClient.DEFAULT_KEY;
        }

        SimulationEngine engine = new SimulationEngine(apiKeyInput, api.N2YOApiClient.NORAD_ISS); // ISS by default

        // Lanzar la segunda consola de monitoreo de nave
        TelemetryLogger.launchMonitorConsole();

        System.out.println("\n[Sistema]: Inicializacion completa. Satelites cargados desde la API N2YO en vivo.");
        System.out.println("[Sistema]: Se abrio una segunda ventana para monitorear naves individualmente.");

        boolean running = true;
        while (running) {
            System.out.println("\n------------------------------------------------------------------");
            System.out.println("                     MENU DEL CENTRO DE CONTROL                   ");
            System.out.println("------------------------------------------------------------------");
            System.out.println("1. Avanzar Simulacion (Ejecutar Tick)");
            System.out.println("2. Cambiar Ubicacion del Radar (Elegir Ciudad via API Geolocalizacion)");
            System.out.println("3. Ejecutar Maniobra de Evasion Orbital en una Nave");
            System.out.println("4. Activar Habilidad Especial de la Nave");
            System.out.println("5. Intentar Acople y Recarga de Combustible en la Estacion (ISS)");
            System.out.println("7. Configurar / Cambiar API Key de N2YO");
            System.out.println("8. Seleccionar Nave para Monitor (Segunda Consola)");
            System.out.println("0. Salir del Simulador");
            System.out.print("\nSelecciona una opcion (0-8): ");

            String choiceStr = scanner.nextLine().trim();

            switch (choiceStr) {
                case "1":
                    engine.tick();
                    break;

                case "2":
                    selectCityPrompt(scanner, engine);
                    break;

                case "3":
                    int shipEvadeIndex = selectShipPrompt(scanner, engine.getTrackedObjects(), "evacuar/evadir");
                    if (shipEvadeIndex != -1) {
                        engine.evadeShip(shipEvadeIndex);
                    }
                    break;

                case "4":
                    int shipAbilityIndex = selectShipPrompt(scanner, engine.getTrackedObjects(), "usar su habilidad especial");
                    if (shipAbilityIndex != -1) {
                        engine.useSpecialAbility(shipAbilityIndex);
                    }
                    break;

                case "5":
                    int shipRefuelIndex = selectShipPrompt(scanner, engine.getTrackedObjects(), "repostar en la Estacion");
                    if (shipRefuelIndex != -1) {
                        engine.refuelShipAtStation(shipRefuelIndex);
                    }
                    break;

                case "7":
                    System.out.print("Ingresa la nueva API Key de N2YO: ");
                    String newKey = scanner.nextLine().trim();
                    engine.getApiClient().setApiKey(newKey);
                    System.out.println("API Key actualizada. Sincronizando datos N2YO...");
                    engine.syncWithN2YO();
                    break;

                case "8":
                    int shipMonitorIndex = selectShipPrompt(scanner, engine.getTrackedObjects(), "monitorear en la segunda consola");
                    if (shipMonitorIndex != -1) {
                        engine.selectShipForMonitoring(shipMonitorIndex);
                        System.out.println(">> Nave seleccionada para monitoreo. Revisa la segunda ventana.");
                    }
                    break;

                case "0":
                    running = false;
                    // Limpiar archivo de monitor al salir
                    try { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("nave_monitor.txt")); } catch (Exception e) {}
                    try { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("monitor.ps1")); } catch (Exception e) {}
                    System.out.println("\n=== CERRANDO CENTRO DE CONTROL ESPACIAL. HASTA LUEGO! ===");
                    break;

                default:
                    System.out.println("Opcion invalida. Intenta nuevamente.");
                    break;
            }
        }

        scanner.close();
    }

    private static void selectCityPrompt(Scanner scanner, SimulationEngine engine) {
        String[] stations = {
            "Estación Espacial Internacional (ISS) [NORAD: " + api.N2YOApiClient.NORAD_ISS + "]",
            "Estación Espacial Tiangong (CSS) [NORAD: " + api.N2YOApiClient.NORAD_TIANGONG + "]"
        };

        System.out.println("\n--- SELECCION DE ESTACION OBJETIVO ---");
        for (int i = 0; i < stations.length; i++) {
            System.out.printf("  [%d] %s\n", (i + 1), stations[i]);
        }
        System.out.print("Selecciona una estacion (1-2): ");

        String input = scanner.nextLine().trim();
        try {
            int option = Integer.parseInt(input);
            if (option == 1) {
                engine.setTargetStation(api.N2YOApiClient.NORAD_ISS);
            } else if (option == 2) {
                engine.setTargetStation(api.N2YOApiClient.NORAD_TIANGONG);
            } else {
                System.out.println("Seleccion invalida.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Opcion invalida.");
        }
    }

    private static int selectShipPrompt(Scanner scanner, List<Spacecraft> naves, String actionName) {
        if (naves == null || naves.isEmpty()) {
            System.out.println("No hay naves disponibles.");
            return -1;
        }

        System.out.println("\nSelecciona la nave sobre la cual deseas " + actionName + ":");
        for (int i = 0; i < naves.size(); i++) {
            Spacecraft nave = naves.get(i);
            System.out.printf("  [%d] %s (%s) - Pos: %s\n", (i + 1), nave.getName(), nave.getType(), nave.getPosition().toString());
        }
        System.out.print("Numero de nave (1-" + naves.size() + ") o 0 para cancelar: ");

        try {
            int selection = Integer.parseInt(scanner.nextLine().trim());
            if (selection >= 1 && selection <= naves.size()) {
                return selection - 1;
            }
        } catch (NumberFormatException e) {
            // Entrada inválida
        }

        System.out.println("Operacion cancelada.");
        return -1;
    }
}
