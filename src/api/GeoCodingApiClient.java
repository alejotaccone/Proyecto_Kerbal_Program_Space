package api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import model.geometry.GeoPosition;

public class GeoCodingApiClient {
    private static final String BASE_URL = "https://geocoding-api.open-meteo.com/v1/search?name=";
    private HttpClient httpClient;
    private Map<String, GeoPosition> fallbackCities;

    public GeoCodingApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        initializeFallbackCities();
    }

    private void initializeFallbackCities() {
        fallbackCities = new HashMap<>();
        fallbackCities.put("buenos aires", new GeoPosition(-34.6037, -58.3816, 25.0));
        fallbackCities.put("nueva york", new GeoPosition(40.7128, -74.0060, 10.0));
        fallbackCities.put("tokio", new GeoPosition(35.6895, 139.6917, 40.0));
        fallbackCities.put("londres", new GeoPosition(51.5074, -0.1278, 15.0));
        fallbackCities.put("madrid", new GeoPosition(40.4168, -3.7038, 650.0));
        fallbackCities.put("parís", new GeoPosition(48.8566, 2.3522, 35.0));
        fallbackCities.put("sídney", new GeoPosition(-33.8688, 151.2093, 19.0));
        fallbackCities.put("el cairo", new GeoPosition(30.0444, 31.2357, 23.0));
        fallbackCities.put("río de janeiro", new GeoPosition(-22.9068, -43.1729, 5.0));
        fallbackCities.put("ciudad de méxico", new GeoPosition(19.4326, -99.1332, 2240.0));
    }

    /**
     * Consulta la API pública de Geolocalización Open-Meteo para obtener latitud y longitud.
     */
    public GeoPosition fetchCityCoordinates(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return fallbackCities.get("buenos aires");
        }

        String cleanName = cityName.trim().toLowerCase();

        try {
            String encodedName = URLEncoder.encode(cityName.trim(), StandardCharsets.UTF_8);
            String url = BASE_URL + encodedName + "&count=1&language=es&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(4))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("\"results\":")) {
                String json = response.body();
                double lat = extractJsonDouble(json, "\"latitude\":");
                double lng = extractJsonDouble(json, "\"longitude\":");
                double alt = json.contains("\"elevation\":") ? extractJsonDouble(json, "\"elevation\":") : 50.0;
                return new GeoPosition(lat, lng, alt);
            }
        } catch (Exception e) {
            // Error de conexión a la API de geolocalización
        }

        // Fallback a diccionario de respaldo si falla la red
        if (fallbackCities.containsKey(cleanName)) {
            return fallbackCities.get(cleanName);
        }

        return fallbackCities.get("buenos aires");
    }

    private double extractJsonDouble(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return 0.0;
        int start = index + key.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        String valStr = json.substring(start, end).trim();
        return Double.parseDouble(valStr);
    }
}
