package model.spacecraft;

/**
 * Estados del ciclo de vida de una misión de reabastecimiento orbital.
 * Aplicado al patrón Máquina de Estados para CargoShip.
 */
public enum MissionState {
    EN_TRANSITO("En Tránsito a Estación"),
    EN_RANGO_DE_ACOPLE("En Rango de Acople"),
    ACOPLE_EN_CURSO("Acople en Curso (Minijuego)"),
    ACOPLADA("Acoplada (Manguera Conectada)"),
    TRANSFIRIENDO("Transfiriendo Suministros"),
    DESORBITANDO("Desorbitando (Reingreso Atmosférico)"),
    REINGRESO_CONCLUIDO("Reingreso Atmosférico Concluido"),
    COMPLETADA("Misión Completada"),
    ABORTADA("Misión Abortada");

    private final String descripcion;

    MissionState(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isFinalizada() {
        return this == COMPLETADA || this == ABORTADA || this == DESORBITANDO || this == REINGRESO_CONCLUIDO;
    }

    public boolean isAcopladaOTransfiriendo() {
        return this == ACOPLADA || this == TRANSFIRIENDO;
    }

    public boolean isDesorbitando() {
        return this == DESORBITANDO;
    }
}
