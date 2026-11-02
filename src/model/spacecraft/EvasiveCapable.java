package model.spacecraft;

/**
 * Interfaz segregada para objetos orbitales dotados de propulsión activa capaz de ejecutar
 * maniobras evasivas de emergencia.
 * Aplica el Principio de Segregación de Interfaces (SOLID - ISP), separando la capacidad de
 * evasión propulsada de aquellos objetos orbitales pasivos o estacionarios.
 */
public interface EvasiveCapable {

    /**
     * Ejecuta una maniobra evasiva modificando la trayectoria orbital.
     * Utiliza el consumo predeterminado de getConsumoEvasion().
     *
     * @param deltaLat desplazamiento en latitud
     * @param deltaLng desplazamiento en longitud
     * @return true si la maniobra fue exitosa; false si no pudo completarse
     */
    boolean evade(double deltaLat, double deltaLng);

    /**
     * Ejecuta una maniobra evasiva especificando la cantidad de combustible requerida.
     * Permite diferenciar maniobras estándar de maniobras forzadas de emergencia crítica.
     *
     * @param deltaLat desplazamiento en latitud
     * @param deltaLng desplazamiento en longitud
     * @param consumo  litros de combustible a descontar
     * @return true si la maniobra fue exitosa; false si no pudo completarse
     */
    default boolean evade(double deltaLat, double deltaLng, double consumo) {
        return evade(deltaLat, deltaLng);
    }

    /**
     * Retorna la cantidad de combustible requerida para una maniobra evasiva normal
     * desde el panel de control.
     *
     * @return litros de combustible consumidos por maniobra normal
     */
    default double getConsumoEvasion() {
        return 15.0;
    }

    /**
     * Retorna la cantidad de combustible requerida para una maniobra evasiva forzada
     * ante un evento de impacto crítico inminente (el doble de una maniobra normal).
     *
     * @return litros de combustible consumidos por maniobra crítica
     */
    default double getConsumoEvasionCrisis() {
        return getConsumoEvasion() * 2.0;
    }
}
