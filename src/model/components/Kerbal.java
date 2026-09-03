package model.components;

/**
 * Representa a un tripulante en la misión espacial.
 * Se le ha dotado de comportamiento propio (ejecutar maniobras y validaciones de aptitud),
 * eliminando el Bad Smell de Data Class (Entidad Anémica).
 */
public class Kerbal {
    private String name;
    private String role; // PILOT, ENGINEER, SCIENTIST
    private int courage; // 0 - 100

    public Kerbal(String name, String role, int courage) {
        this.name = name;
        this.role = role;
        this.courage = courage;
    }

    /**
     * Ejecuta una maniobra de pilotaje u operación según el rol y valentía del tripulante.
     */
    public String ejecutarManiobraPilotaje() {
        if ("PILOT".equalsIgnoreCase(this.role)) {
            return String.format("El piloto %s realizó un ajuste fino de inclinación orbital con valentía del %d%%.", name, courage);
        }
        return String.format("El tripulante %s (%s) asistió en la maniobra orbital con valentía del %d%%.", name, role, courage);
    }

    /**
     * Evalúa si el tripulante cuenta con las aptitudes y nivel de valentía para pilotar.
     */
    public boolean esAptoParaPilotar() {
        return "PILOT".equalsIgnoreCase(this.role) && this.courage >= 50;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public int getCourage() {
        return courage;
    }

    @Override
    public String toString() {
        return String.format("Kerbal [%s - Rol: %s, Valentía: %d%%]", name, role, courage);
    }
}
