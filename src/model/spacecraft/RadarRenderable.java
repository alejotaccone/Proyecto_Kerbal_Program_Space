package model.spacecraft;

import java.awt.Color;

/**
 * Interfaz segregada exclusivamente para las propiedades visuales de renderizado en el radar 2D.
 * Aplica el Principio de Segregación de Interfaces (SOLID - ISP), separando el contrato gráfico
 * de radar respecto de la lógica física y cinemática del dominio orbital.
 */
public interface RadarRenderable {

    /**
     * Color asignado para la representación del blip en la pantalla del radar.
     */
    default Color getColorRadar() {
        return new Color(0, 230, 80);
    }

    /**
     * Tamaño del ícono en píxeles sobre la pantalla de radar.
     */
    default int getTamanoIconoRadar() {
        return 6;
    }

    /**
     * Indica si el ícono debe dibujarse como triángulo de advertencia (ej. basura/amenazas).
     */
    default boolean esIconoTriangular() {
        return false;
    }

    /**
     * Indica si el ícono en el radar debe representarse con un anillo exterior (ej. estaciones nodriza).
     */
    default boolean tieneAnilloRadar() {
        return false;
    }
}
