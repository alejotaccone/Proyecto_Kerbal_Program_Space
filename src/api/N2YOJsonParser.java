package api;

import model.geometry.GeoPosition;

/**
 * Especialista en el parseo y extracción de datos desde respuestas JSON de la API N2YO.
 * Aplica el Principio de Responsabilidad Única (SOLID - SRP), separando la interpretación
 * del formato de texto JSON de la capa de transporte y comunicación HTTP (N2YOApiClient).
 */
public class N2YOJsonParser {

    /**
     * Verifica si el cuerpo JSON recibido contiene un bloque válido de posiciones orbitales.
     */
    public boolean contienePosiciones(String json) {
        return json != null && json.contains("\"positions\":");
    }

    /**
     * Extrae el nombre del satélite desde el JSON o devuelve un identificador por defecto basado en NORAD.
     */
    public String parseSatelliteName(String json, int noradId) {
        if (json != null) {
            String satName = extractJsonString(json, "\"satname\":");
            if (satName != null && !satName.trim().isEmpty()) {
                return satName.trim();
            }
        }
        return "NORAD-" + noradId;
    }

    /**
     * Extrae las coordenadas geográficas (latitud, longitud y altitud) desde el JSON de N2YO.
     */
    public GeoPosition parsePositionFromJson(String json) {
        if (json == null) return null;
        try {
            if (json.contains("\"satlatitude\":") && json.contains("\"satlongitude\":")) {
                double lat = extractJsonDouble(json, "\"satlatitude\":");
                double lng = extractJsonDouble(json, "\"satlongitude\":");
                double alt = json.contains("\"sataltitude\":") ? extractJsonDouble(json, "\"sataltitude\":") : 400.0;
                return new GeoPosition(lat, lng, alt);
            }
        } catch (Exception e) {
            // Error de formato al parsear coordenadas numéricas
        }
        return null;
    }

    private double extractJsonDouble(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return 0.0;
        int start = index + key.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) end = json.indexOf("]", start);
        String valStr = json.substring(start, end).replace("}", "").replace("]", "").trim();
        return Double.parseDouble(valStr);
    }

    private String extractJsonString(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return "";
        int start = json.indexOf("\"", index + key.length());
        if (start == -1) return "";
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return "";
        return json.substring(start + 1, end);
    }
}
