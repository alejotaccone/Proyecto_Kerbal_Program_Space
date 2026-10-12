package model.spacecraft;

import model.components.FuelTank;

/**
 * Interfaz segregada para objetos orbitales que cuentan con almacenamiento de combustible.
 * Aplica el Principio de Segregación de Interfaces (SOLID - ISP), permitiendo que solo las
 * entidades con tanque (como Spacecraft o SpaceStation) expongan esta capacidad sin forzar
 * a objetos inertes (como SpaceDebris) a depender de métodos de combustible.
 */
public interface FuelCarrier {

    /**
     * Devuelve el tanque de combustible asociado al objeto orbital.
     */
    FuelTank getFuelTank();

    /**
     * Etiqueta descriptiva del tipo de almacenamiento de combustible para telemetría.
     */
    default String getEtiquetaCombustible() {
        return String.format("%.1f%%", getFuelTank() != null ? getFuelTank().getPercentage() : 0.0);
    }
}
