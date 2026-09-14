package engine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Clase especialista con alta cohesión para interactuar con el sistema operativo
 * y abrir ventanas de consola externas (PowerShell) para el monitoreo en tiempo real.
 */
public class ExternalMonitorLauncher {

    public static void lanzarConsolaPowerShell(String archivoMonitor) {
        try {
            String script = "$host.UI.RawUI.WindowTitle = 'MONITOR DE NAVE - Kerbal Program Space'\r\n"
                    + "while($true) {\r\n"
                    + "    Clear-Host\r\n"
                    + "    if (Test-Path '" + archivoMonitor + "') {\r\n"
                    + "        Get-Content '" + archivoMonitor + "'\r\n"
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
            System.err.println("[Sistema]: No se pudo abrir la ventana de monitoreo de nave: " + e.getMessage());
        }
    }
}
