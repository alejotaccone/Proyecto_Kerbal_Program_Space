package model.spacecraft;

import model.geometry.GeoPosition;

/**
 * Superclase abstracta que modela cualquier objeto en órbita terrestre.
 * Todos sus atributos están estrictamente encapsulados como private,
 * resolviendo el Bad Smell de Intimidad Inapropiada (Inappropriate Intimacy).
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
