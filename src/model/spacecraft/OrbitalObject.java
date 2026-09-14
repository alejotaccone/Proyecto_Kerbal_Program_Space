package model.spacecraft;

import model.geometry.GeoPosition;

/**
 * Superclase abstracta que modela cualquier objeto en órbita terrestre.
 * Aplica el patrón GRASP Experto en Información (Information Expert)
 * centralizando el cálculo de distancias y la interfaz de telemetría polimórfica.
 */
public abstract class OrbitalObject {
    private String id;
    private String name;
    private int noradId;
    private GeoPosition position;
    private double velocityKmH;
    private String nombreImagen;

    public OrbitalObject(SpacecraftInfo info, GeoPosition position, double velocityKmH) {
        this(info, position, velocityKmH, "Spacecraft.jpg");
    }

    public OrbitalObject(SpacecraftInfo info, GeoPosition position, double velocityKmH, String nombreImagen) {
        this.id = (info != null) ? info.getId() : "";
        this.name = (info != null) ? info.getName() : "Desconocido";
        this.noradId = (info != null) ? info.getNoradId() : 0;
        this.position = position;
        this.velocityKmH = velocityKmH;
        this.nombreImagen = (nombreImagen != null && !nombreImagen.isEmpty()) ? nombreImagen : "Spacecraft.jpg";
    }

    public abstract void move();
    public abstract String getType();
    public abstract String performSpecialAbility();

    /**
     * Retorna el bloque de telemetría y estado de los subsistemas propios del objeto.
     * Aplica el patrón GRASP Experto en Información + Polimorfismo.
     */
    public abstract String getDetalleTelemetria();

    /**
     * Calcula la distancia en kilómetros hacia otro objeto orbital.
     * Aplica el patrón GRASP Experto en Información delegando en GeoPosition.
     */
    public double distanceTo(OrbitalObject other) {
        if (other == null || other.position == null || this.position == null) {
            return Double.MAX_VALUE;
        }
        return this.position.distanceTo(other.position);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getNoradId() {
        return noradId;
    }

    public GeoPosition getPosition() {
        return position;
    }

    public double getVelocityKmH() {
        return velocityKmH;
    }

    public void setVelocityKmH(double velocityKmH) {
        this.velocityKmH = velocityKmH;
    }

    public String getNombreImagen() {
        return nombreImagen;
    }

    public void setNombreImagen(String nombreImagen) {
        this.nombreImagen = nombreImagen;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) | NORAD: %d | Pos: %s", 
                name, getType(), noradId, (position != null ? position.toString() : "N/A"));
    }
}
