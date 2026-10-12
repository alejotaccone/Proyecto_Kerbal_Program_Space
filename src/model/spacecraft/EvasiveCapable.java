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
     *
     * @param deltaLat desplazamiento en latitud
     * @param deltaLng desplazamiento en longitud
     * @return true si la maniobra fue exitosa; false si no pudo completarse
     */
    boolean evade(double deltaLat, double deltaLng);
}
