package model.spacecraft;

/**
 * Parameter Object que encapsula los datos de identidad básica de un objeto orbital.
 * Resuelve el Code Smell de "Parámetros Largos" (Long Parameter List)
 * en los constructores de la jerarquía de OrbitalObject y Spacecraft.
 */
public class SpacecraftInfo {
    private final String id;
    private final String name;
    private final int noradId;

    public SpacecraftInfo(String id, String name, int noradId) {
        this.id = id;
        this.name = name;
        this.noradId = noradId;
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

    @Override
    public String toString() {
        return String.format("%s (ID: %s, NORAD: %d)", name, id, noradId);
    }
}
