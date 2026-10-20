package api;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Gestor centralizado de configuración de credenciales y claves de API.
 * Resuelve la API Key de N2YO con la siguiente prioridad:
 * 1. Variable de entorno del sistema operativo (N2YO_API_KEY)
 * 2. Archivo local config.properties en la raíz del proyecto (propiedad n2yo.api.key)
 * 3. Valor manual ingresado por el usuario en la GUI o CLI
 */
public class ApiConfig {

    public static final String ENV_VAR_NAME = "N2YO_API_KEY";
    public static final String CONFIG_FILE_NAME = "config.properties";
    public static final String PROPERTY_KEY = "n2yo.api.key";

    /**
     * Resuelve la clave configurada en el entorno o en el archivo config.properties.
     * Retorna una cadena vacía si no se encuentra configurada.
     */
    public static String obtenerApiKey() {
        // 1. Variable de entorno
        String envKey = System.getenv(ENV_VAR_NAME);
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }

        // 2. Archivo config.properties
        File configFile = new File(CONFIG_FILE_NAME);
        if (configFile.exists() && configFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);
                String propKey = props.getProperty(PROPERTY_KEY);
                if (propKey != null && !propKey.trim().isEmpty()) {
                    return propKey.trim();
                }
            } catch (Exception ignored) {
                // Silenciar error de lectura para no interrumpir el flujo
            }
        }

        return "";
    }

    /**
     * Evalúa si una clave dada es utilizable (no nula, no vacía y no un placeholder de ejemplo).
     */
    public static boolean esClaveValida(String key) {
        if (key == null) return false;
        String trimmed = key.trim();
        return !trimmed.isEmpty() 
            && !trimmed.equalsIgnoreCase("TU_API_KEY_AQUI")
            && !trimmed.equalsIgnoreCase("YOUR_API_KEY_HERE");
    }

    /**
     * Enmascara una clave para presentación segura en pantalla o logs (ej. "DPP6...5THO").
     */
    public static String enmascararKey(String key) {
        if (key == null || key.trim().isEmpty()) return "[NO CONFIGURADA]";
        String trimmed = key.trim();
        if (trimmed.length() <= 8) return "********";
        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }
}
