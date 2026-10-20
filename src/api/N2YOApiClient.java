package api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import model.geometry.GeoPosition;
import model.spacecraft.OrbitalObject;
import model.spacecraft.SpacecraftFactory;

public class N2YOApiClient {
    private static final String BASE_URL = "https://api.n2yo.com/rest/v1/satellite/";
    public static final String DEFAULT_KEY = "";
    private String apiKey;
    private HttpClient httpClient;
    private N2YOJsonParser jsonParser;

    public static final int NORAD_ISS = 25544;       // Estación Espacial Internacional (ISS)
    public static final int NORAD_HUBBLE = 20580;    // Telescopio Espacial Hubble (HST)
    public static final int NORAD_TIANGONG = 48274;  // Estación Espacial China Tiangong (CSS)
    public static final int NORAD_NOAA19 = 33591;    // Satélite de observación NOAA 19
    public static final int NORAD_ONEWEB = 48212;    // Satélite / Restos en órbita polar ONEWEB-0179

    public N2YOApiClient(String apiKey) {
        this(apiKey, new N2YOJsonParser());
    }

    public N2YOApiClient(String apiKey, N2YOJsonParser jsonParser) {
        String keyToUse = ApiConfig.esClaveValida(apiKey) ? apiKey.trim() : ApiConfig.obtenerApiKey();
        this.apiKey = keyToUse != null ? keyToUse.trim() : "";
        this.jsonParser = (jsonParser != null) ? jsonParser : new N2YOJsonParser();
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
            // Manejo de errores de conexión o timeout sin exponer credenciales
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

        if (!ApiConfig.esClaveValida(apiKey)) {
            return fallbackPos;
        }

        String url = String.format(Locale.US, "%spositions/%d/%.4f/%.4f/%.1f/1/&apiKey=%s", 
                BASE_URL, noradId, fallbackPos.getLatitude(), fallbackPos.getLongitude(), fallbackPos.getAltitude(), apiKey);

        String responseBody = sendGet(url, Duration.ofSeconds(2));
        if (jsonParser.contienePosiciones(responseBody)) {
            GeoPosition pos = jsonParser.parsePositionFromJson(responseBody);
            if (pos != null) return pos;
        }

        return fallbackPos;
    }

    /**
     * Petición HTTP a N2YO para un satélite individual.
     * Si no hay API Key o falla la conexión, fabrica un objeto orbital con posición predeterminada de contingencia.
     */
    public OrbitalObject fetchRealSatellite(int noradId) {
        GeoPosition posDefecto = new GeoPosition(-34.60, -58.38, 420.0);
        String nombreFallback = (noradId == NORAD_TIANGONG) ? "Tiangong (CSS)" : "Estación Espacial Internacional (ISS)";

        if (ApiConfig.esClaveValida(apiKey)) {
            String url = String.format(Locale.US, "%spositions/%d/-34.60/-58.38/25/1/&apiKey=%s", BASE_URL, noradId, apiKey);
            String responseBody = sendGet(url, Duration.ofSeconds(4));
            if (jsonParser.contienePosiciones(responseBody)) {
                String satName = jsonParser.parseSatelliteName(responseBody, noradId);
                GeoPosition pos = jsonParser.parsePositionFromJson(responseBody);
                if (pos != null) {
                    return SpacecraftFactory.crearDesdeDatosN2YO(noradId, satName, pos);
                }
            }
        }

        // Si no hay key o la red falló, crear la estación con datos seguros de contingencia
        return SpacecraftFactory.crearDesdeDatosN2YO(noradId, nombreFallback + " [Modo Local]", posDefecto);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = (apiKey != null) ? apiKey.trim() : "";
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean tieneClaveConfigurada() {
        return ApiConfig.esClaveValida(apiKey);
    }
}
