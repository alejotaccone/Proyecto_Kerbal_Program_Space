package model.geometry;

public class GeoPosition {
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
     * Calcula la distancia aproximada en kilómetros entre dos coordenadas geográficas y altitudes.
     */
    public double distanceTo(GeoPosition other) {
        if (other == null) return Double.MAX_VALUE;

        final int R = 6371; // Radio medio de la Tierra en km

        double latDistance = Math.toRadians(other.latitude - this.latitude);
        double lonDistance = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude))
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double surfaceDistance = R * c;

        // Incorporar diferencia de altitud
        double altDifference = other.altitude - this.altitude;
        return Math.sqrt(surfaceDistance * surfaceDistance + altDifference * altDifference);
    }

    @Override
    public String toString() {
        return String.format("Lat: %.2f°, Lng: %.2f°, Alt: %.1f km", latitude, longitude, altitude);
    }
}
