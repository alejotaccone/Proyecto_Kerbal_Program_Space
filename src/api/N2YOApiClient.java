package api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import model.components.FuelTank;
import model.components.Kerbal;
import model.geometry.GeoPosition;
import model.spacecraft.CargoShip;
import model.spacecraft.CrewShuttle;
import model.spacecraft.ExplorationProbe;
import model.spacecraft.OrbitalObject;
import model.spacecraft.SpaceDebris;
import model.spacecraft.SpaceStation;
import model.spacecraft.SpacecraftInfo;

public class N2YOApiClient {
    private static final String BASE_URL = "https://api.n2yo.com/rest/v1/satellite/";
    public static final String DEFAULT_KEY = "DPP6C4-6KSLTW-K5Z8BU-5THO";
    private String apiKey;
    private HttpClient httpClient;

    public static final int NORAD_ISS = 25544;       // Estación Espacial Internacional (ISS)
    public static final int NORAD_HUBBLE = 20580;    // Telescopio Espacial Hubble (HST)
    public static final int NORAD_TIANGONG = 48274;  // Estación Espacial China Tiangong (CSS)
    public static final int NORAD_NOAA19 = 33591;    // Satélite de observación NOAA 19
    public static final int NORAD_ONEWEB = 48212;    // Satélite / Restos en órbita polar ONEWEB-0179

    public N2YOApiClient(String apiKey) {
        this.apiKey = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : DEFAULT_KEY;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    /**
     * Realiza una petición HTTP GET de forma reutilizable y segura.
     */
    private String sendGet(String url, Duration timeout) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(timeout)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception e) {
            // Manejo de errores de conexión o timeout
        }
        return null;
    }

    /**
     * Petición HTTP a N2YO para actualizar la posición en tiempo real de un satélite individual.
     */
    public GeoPosition fetchRealSatellitePosition(int noradId, GeoPosition fallbackPos) {
        if (fallbackPos == null) {
            fallbackPos = new GeoPosition(0.0, 0.0, 400.0);
        }
        String url = String.format(Locale.US, "%spositions/%d/%.4f/%.4f/%.1f/1/&apiKey=%s", 
                BASE_URL, noradId, fallbackPos.getLatitude(), fallbackPos.getLongitude(), fallbackPos.getAltitude(), apiKey);

        String responseBody = sendGet(url, Duration.ofSeconds(2));
        if (responseBody != null && responseBody.contains("\"positions\":")) {
            GeoPosition pos = parsePositionFromJson(responseBody);
            if (pos != null) return pos;
        }

        return fallbackPos;
    }

    /**
     * Petición HTTP a N2YO para un satélite individual.
     */
    public OrbitalObject fetchRealSatellite(int noradId) {
        String url = String.format(Locale.US, "%spositions/%d/-34.60/-58.38/25/1/&apiKey=%s", BASE_URL, noradId, apiKey);

        String responseBody = sendGet(url, Duration.ofSeconds(4));
        if (responseBody != null && responseBody.contains("\"positions\":")) {
            String satName = extractJsonString(responseBody, "\"satname\":");
            if (satName == null || satName.trim().isEmpty()) {
                satName = "NORAD-" + noradId;
            }

            GeoPosition pos = parsePositionFromJson(responseBody);
            if (pos != null) {
                return instantiateFromN2YOData(noradId, satName.trim(), pos);
            }
        }

        return null;
    }

    /**
     * Mapeo dinámico polimórfico basado estrictamente en el nombre y NORAD ID real de la API N2YO.
     */
    private OrbitalObject instantiateFromN2YOData(int noradId, String satName, GeoPosition pos) {
        String upper = satName.toUpperCase();
        String craftId = "SAT-" + noradId;
        SpacecraftInfo info = new SpacecraftInfo(craftId, satName + " (N2YO API)", noradId);

        if (upper.contains("TIANHE") || upper.contains("TIANGONG") || upper.contains("CSS") || upper.contains("CHINA") || noradId == NORAD_TIANGONG) {
            return new SpaceStation(info, pos, "SpaceSation_China.jfif", "Estación Espacial China (Tiangong)");
        } else if (upper.contains("STATION") || upper.contains("ISS") || noradId == NORAD_ISS) {
            return new SpaceStation(info, pos, "SpaceStation_Internacional.jpg", "Estación Espacial Internacional (ISS)");
        } else if (upper.contains("ONEWEB") || upper.contains("DEBRIS") || upper.contains("COSMOS") || upper.contains("DEB") || upper.contains("SL-")) {
            return new SpaceDebris(info, pos, 8.2);
        } else if (upper.contains("NOAA") || upper.contains("CARGO") || upper.contains("DRAGON") || upper.contains("DELTA") || upper.contains("ATLAS")) {
            return new CargoShip(info, pos, new FuelTank(200.0, 180.0, 3.0), 18.5);
        } else if (upper.contains("SOYUZ") || upper.contains("CREW") || upper.contains("STARLINER")) {
            CrewShuttle shuttle = new CrewShuttle(info, pos, new FuelTank(180.0, 150.0, 3.0));
            shuttle.addCrewMember(new Kerbal("Comandante Real", "PILOT", 90));
            return shuttle;
        } else {
            // Sonda de exploración por defecto para satélites científicos como HST (Hubble), TELSTAR, ESSA, etc.
            return new ExplorationProbe(info, pos, new FuelTank(120.0, 100.0, 1.5), 0.95);
        }
    }

    private GeoPosition parsePositionFromJson(String json) {
        try {
            if (json.contains("\"satlatitude\":") && json.contains("\"satlongitude\":")) {
                double lat = extractJsonDouble(json, "\"satlatitude\":");
                double lng = extractJsonDouble(json, "\"satlongitude\":");
                double alt = json.contains("\"sataltitude\":") ? extractJsonDouble(json, "\"sataltitude\":") : 400.0;
                return new GeoPosition(lat, lng, alt);
            }
        } catch (Exception e) {
            // Error de parseo
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

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
