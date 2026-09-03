package model.components;

/**
 * Enumeración que modela los roles especializados de los tripulantes Kerbal.
 * Reemplaza el uso de Strings planos ("PILOT", "ENGINEER", "SCIENTIST")
 * eliminando el Bad Smell de Primitive Obsession.
 */
public enum KerbalRole {
    PILOT("Piloto") {
        @Override
        public String ejecutarAccionEspecial(String nombre, int valentia) {
            return String.format("El piloto %s realizó un ajuste fino de inclinación orbital con valentía del %d%%.", nombre, valentia);
        }

        @Override
        public boolean esAptoParaPilotar(int valentia) {
            return valentia >= 50;
        }
    },
    ENGINEER("Ingeniero") {
        @Override
        public String ejecutarAccionEspecial(String nombre, int valentia) {
            return String.format("El ingeniero %s optimizó la eficiencia de subsistemas con valentía del %d%%.", nombre, valentia);
        }

        @Override
        public boolean esAptoParaPilotar(int valentia) {
            return false;
        }
    },
    SCIENTIST("Científico") {
        @Override
        public String ejecutarAccionEspecial(String nombre, int valentia) {
            return String.format("El científico %s calibró los sensores telemétricos y solares con valentía del %d%%.", nombre, valentia);
        }

        @Override
        public boolean esAptoParaPilotar(int valentia) {
            return false;
        }
    };

    private final String descripcion;

    KerbalRole(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public abstract String ejecutarAccionEspecial(String nombre, int valentia);
    public abstract boolean esAptoParaPilotar(int valentia);

    /**
     * Mapea de forma segura un String a un KerbalRole, tolerando mayúsculas o minúsculas.
     */
    public static KerbalRole fromString(String roleStr) {
        if (roleStr != null) {
            String trimmed = roleStr.trim().toUpperCase();
            for (KerbalRole r : values()) {
                if (r.name().equalsIgnoreCase(trimmed) || r.getDescripcion().equalsIgnoreCase(trimmed)) {
                    return r;
                }
            }
        }
        return PILOT; // Valor por defecto
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
