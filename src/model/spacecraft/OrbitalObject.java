package model.spacecraft;

import model.geometry.GeoPosition;

/**
 * Superclase abstracta que modela cualquier objeto en órbita terrestre.
 * Contiene únicamente identidad espacial y cinemática común (posición, velocidad, imagen),
 * resolviendo el problema de Herencia Rechazada (Refused Bequest).
 */
public abstract class OrbitalObject {
    protected String id;
    protected String name;
    protected int noradId;
    protected GeoPosition position;
    protected double velocityKmH;
    protected String nombreImagen;

    public OrbitalObject(String id, String name, int noradId, GeoPosition position, double velocityKmH) {
        this.id = id;
        this.name = name;
        this.noradId = noradId;
        this.position = position;
        this.velocityKmH = velocityKmH;
        this.nombreImagen = "Spacecraft.jpg";
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

    public String getImg() {
        return nombreImagen;
    }

    public void setImg(String img) {
        this.nombreImagen = img;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) | NORAD: %d | Pos: %s", 
                name, getType(), noradId, (position != null ? position.toString() : "N/A"));
    }
}
