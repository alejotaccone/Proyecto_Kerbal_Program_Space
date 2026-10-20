package model.geometry;

/**
 * Representa una coordenada tridimensional en el espacio geocéntrico (latitud, longitud y altitud).
 * Aplica el patrón GRASP Experto en Información encapsulando los cálculos de distancia
 * tanto euclídea tridimensional (ECEF) como de arco sobre la superficie terrestre (Haversine).
 */
public class GeoPosition {
    public static final double EARTH_RADIUS_KM = 6371.0;

    private double latitude;
    private double longitude;
    private double altitude; // en kilómetros

    public GeoPosition(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    /**
     * Calcula la distancia euclidiana tridimensional en kilómetros entre dos posiciones orbitales.
     * Convierte las coordenadas esféricas geocéntricas (latitud, longitud, altitud)
     * al sistema cartesiano ECEF (Earth-Centered, Earth-Fixed):
     *   r = 6371.0 + altitud_km
     *   x = r * cos(lat) * cos(lon)
     *   y = r * cos(lat) * sin(lon)
     *   z = r * sin(lat)
     * y obtiene la distancia euclídea: sqrt((x2 - x1)^2 + (y2 - y1)^2 + (z2 - z1)^2).
     */
    public double distance3DTo(GeoPosition other) {
        if (other == null) return Double.MAX_VALUE;

        double r1 = EARTH_RADIUS_KM + this.altitude;
        double r2 = EARTH_RADIUS_KM + other.altitude;

        double phi1 = Math.toRadians(this.latitude);
        double lambda1 = Math.toRadians(this.longitude);
        double phi2 = Math.toRadians(other.latitude);
        double lambda2 = Math.toRadians(other.longitude);

        double x1 = r1 * Math.cos(phi1) * Math.cos(lambda1);
        double y1 = r1 * Math.cos(phi1) * Math.sin(lambda1);
        double z1 = r1 * Math.sin(phi1);

        double x2 = r2 * Math.cos(phi2) * Math.cos(lambda2);
        double y2 = r2 * Math.cos(phi2) * Math.sin(lambda2);
        double z2 = r2 * Math.sin(phi2);

        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calcula la distancia de arco sobre la superficie terrestre (ground-track al nivel del mar)
     * utilizando la fórmula clásica de Haversine con radio medio R = 6371.0 km.
     * Ignora la altitud de los objetos, midiendo únicamente la distancia entre sus proyecciones sobre la Tierra.
     */
    public double surfaceDistanceKm(GeoPosition other) {
        if (other == null) return Double.MAX_VALUE;

        double latDistance = Math.toRadians(other.latitude - this.latitude);
        double lonDistance = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude))
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calcula la distancia física real entre dos objetos en el espacio tridimensional.
     * Por defecto utiliza la distancia euclidiana 3D (ECEF) precisa.
     * Aplica el patrón GRASP Experto en Información.
     */
    public double distanceTo(GeoPosition other) {
        return distance3DTo(other);
    }

    @Override
    public String toString() {
        return String.format("Lat: %.2f°, Lng: %.2f°, Alt: %.1f km", latitude, longitude, altitude);
    }
}
