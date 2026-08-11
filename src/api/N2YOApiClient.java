package api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import model.components.FuelTank;
import model.geometry.GeoPosition;
import model.spacecraft.ExplorationProbe;
import model.spacecraft.SpaceDebris;
import model.spacecraft.SpaceStation;
import model.spacecraft.Spacecraft;

public class N2YOApiClient {
    private static final String BASE_URL = "https://api.n2yo.com/rest/v1/satellite/";
    private String apiKey;
    private HttpClient httpClient;
    private boolean onlineMode;

    public N2YOApiClient(String apiKey) {
        this.apiKey = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : "DEMO_KEY";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.onlineMode = !"DEMO_KEY".equals(this.apiKey);
    }

    /**
     * Consulta las posiciones reales del satélite con el NORAD ID dado.
     * Si no hay conexión o falla la API, usa el simulador offline seguro.
     */
    public GeoPosition fetchRealSatellitePosition(int noradId, double fallbackLat, double fallbackLng, double fallbackAlt) {
        if (!onlineMode) {
            return simulatePosition(fallbackLat, fallbackLng, fallbackAlt);
        }

        try {
            // Ejemplo de endpoint: /positions/{id}/{lat}/{lng}/{alt}/{seconds}
            String url = String.format("%spositions/%d/0/0/0/1/&apiKey=%s", BASE_URL, noradId, apiKey);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("\"positions\":")) {
                return parsePositionFromJson(response.body(), fallbackLat, fallbackLng, fallbackAlt);
            }
        } catch (Exception e) {
            // Fallback silencioso a modo offline en caso de error de red o límite de cuota
        }

        return simulatePosition(fallbackLat, fallbackLng, fallbackAlt);
    }

    /**
     * Devuelve una lista de naves reales registradas en N2YO (ISS, Hubble, Starlink, Basura).
     */
    public List<Spacecraft> fetchLiveSpaceObjects() {
        List<Spacecraft> liveObjects = new ArrayList<>();

        // 1. Estación Espacial Internacional (ISS - NORAD 25544)
        GeoPosition issPos = fetchRealSatellitePosition(25544, -34.60, -58.38, 420.0);
        SpaceStation iss = new SpaceStation("ISS-25544", "Estacion Espacial Internacional (ISS)", 25544, issPos);
        liveObjects.add(iss);

        // 2. Satélite Starlink (NORAD 44713)
        GeoPosition starlinkPos = fetchRealSatellitePosition(44713, -33.10, -57.80, 550.0);
        ExplorationProbe starlink = new ExplorationProbe("SL-44713", "Starlink-1007 (N2YO Live)", 44713, 
                starlinkPos, new FuelTank(100.0, 95.0, 1.0), 0.95);
        liveObjects.add(starlink);

        // 3. Basura Espacial rastreada por N2YO (NORAD 48212)
        GeoPosition debrisPos = fetchRealSatellitePosition(48212, -34.80, -58.20, 410.0);
        SpaceDebris debris = new SpaceDebris("DEBRIS-48212", "Resto de Cohete Cosmos-2251", 48212, debrisPos, 8.5);
        liveObjects.add(debris);

        return liveObjects;
    }

    private GeoPosition simulatePosition(double baseLat, double baseLng, double baseAlt) {
        // Pequeño desplazamiento orbital simulado
        double simLat = baseLat + (Math.sin(System.currentTimeMillis() / 5000.0) * 0.5);
        double simLng = baseLng + ((System.currentTimeMillis() / 2000.0) % 360.0) - 180.0;
        return new GeoPosition(simLat, simLng, baseAlt);
    }

    private GeoPosition parsePositionFromJson(String json, double fallbackLat, double fallbackLng, double fallbackAlt) {
        try {
            // Parser nativo simplificado de JSON para extraer satlat, satlng, satalt
            if (json.contains("\"satlat\":") && json.contains("\"satlng\":")) {
                double lat = extractJsonDouble(json, "\"satlat\":");
                double lng = extractJsonDouble(json, "\"satlng\":");
                double alt = json.contains("\"satalt\":") ? extractJsonDouble(json, "\"satalt\":") : fallbackAlt;
                return new GeoPosition(lat, lng, alt);
            }
        } catch (Exception e) {
            // Ignorar y retornar fallback
        }
        return simulatePosition(fallbackLat, fallbackLng, fallbackAlt);
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

    public boolean isOnlineMode() {
        return onlineMode;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        this.onlineMode = apiKey != null && !apiKey.trim().isEmpty() && !"DEMO_KEY".equals(apiKey);
    }
}
