package model.components;

/**
 * Representa a un tripulante en la misión espacial.
 * Se le ha dotado de comportamiento propio y tipado fuerte (KerbalRole),
 * resolviendo los Bad Smells de Data Class y Primitive Obsession.
 */
public class Kerbal {
    private String name;
    private KerbalRole role;
    private int courage; // 0 - 100

    public Kerbal(String name, KerbalRole role, int courage) {
        this.name = (name != null && !name.trim().isEmpty()) ? name : "Tripulante Desconocido";
        this.role = (role != null) ? role : KerbalRole.PILOT;
        this.courage = Math.max(0, Math.min(100, courage));
    }

    public Kerbal(String name, String roleStr, int courage) {
        this(name, KerbalRole.fromString(roleStr), courage);
    }

    /**
     * Ejecuta una maniobra de pilotaje u operación según el rol y valentía del tripulante.
     */
    public String ejecutarManiobraPilotaje() {
        return role.ejecutarAccionEspecial(name, courage);
    }

    /**
     * Evalúa si el tripulante cuenta con las aptitudes y nivel de valentía para pilotar.
     */
    public boolean esAptoParaPilotar() {
        return role.esAptoParaPilotar(courage);
    }

    public String getName() {
        return name;
    }

    public KerbalRole getRoleEnum() {
        return role;
    }

    public String getRole() {
        return role.getDescripcion();
    }

    public int getCourage() {
        return courage;
    }

    @Override
    public String toString() {
        return String.format("Kerbal [%s - Rol: %s, Valentía: %d%%]", name, role.getDescripcion(), courage);
    }
}
