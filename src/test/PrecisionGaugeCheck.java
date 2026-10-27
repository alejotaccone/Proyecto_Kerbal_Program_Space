package test;

import model.minigame.PrecisionGauge;

/**
 * Suite de pruebas unitarias sin dependencias externas para PrecisionGauge.
 */
public class PrecisionGaugeCheck {

    public static void main(String[] args) {
        System.out.println("=== EJECUTANDO PRUEBAS DE PRECISION GAUGE ===");
        int fallos = 0;

        // Caso 1: Frenar dentro de la zona verde
        PrecisionGauge gauge = new PrecisionGauge(1.0, 20.0); // verde de 40.0 a 60.0
        gauge.setCurrentPosition(50.0);
        boolean aciertoCentro = gauge.frenar();
        if (aciertoCentro && gauge.isStopped()) {
            System.out.println("[OK] Caso 1 - Frenado dentro de zona verde (50.0): ACIERTO");
        } else {
            System.err.println("[ERROR] Caso 1 - Esperaba acierto en centro");
            fallos++;
        }

        // Caso 2: Frenar en los bordes exactos
        gauge.reiniciar();
        gauge.configurarZonaVerdeCentrada(20.0); // 40.0 a 60.0
        gauge.setCurrentPosition(40.0);
        boolean aciertoBordeIzq = gauge.frenar();

        gauge.reiniciar();
        gauge.configurarZonaVerdeCentrada(20.0);
        gauge.setCurrentPosition(60.0);
        boolean aciertoBordeDer = gauge.frenar();

        if (aciertoBordeIzq && aciertoBordeDer) {
            System.out.println("[OK] Caso 2 - Frenado en bordes inclusivos (40.0 y 60.0): ACIERTO");
        } else {
            System.err.println("[ERROR] Caso 2 - Borde izquierdo: " + aciertoBordeIzq + ", Borde derecho: " + aciertoBordeDer);
            fallos++;
        }

        // Caso 3: Frenar fuera de la zona verde
        gauge.reiniciar();
        gauge.configurarZonaVerdeCentrada(20.0);
        gauge.setCurrentPosition(25.0);
        boolean falloFuera = gauge.frenar();

        gauge.reiniciar();
        gauge.configurarZonaVerdeCentrada(20.0);
        gauge.setCurrentPosition(75.0);
        boolean falloFuera2 = gauge.frenar();

        if (!falloFuera && !falloFuera2) {
            System.out.println("[OK] Caso 3 - Frenado fuera de zona verde (25.0 y 75.0): FALLO");
        } else {
            System.err.println("[ERROR] Caso 3 - Debería ser fallo");
            fallos++;
        }

        // Caso 4: Rebote correcto en los extremos (ping-pong)
        gauge.reiniciar();
        gauge.setCurrentPosition(99.0);
        gauge.setDirection(1.0);
        gauge.setSpeed(2.0);
        gauge.update(); // Debería llegar a 100.0 y cambiar dirección a -1.0
        boolean rebotoDerecha = (gauge.getCurrentPosition() == 100.0 && gauge.getDirection() == -1.0);

        gauge.setCurrentPosition(1.0);
        gauge.setDirection(-1.0);
        gauge.setSpeed(2.0);
        gauge.update(); // Debería llegar a 0.0 y cambiar dirección a 1.0
        boolean rebotoIzquierda = (gauge.getCurrentPosition() == 0.0 && gauge.getDirection() == 1.0);

        if (rebotoDerecha && rebotoIzquierda) {
            System.out.println("[OK] Caso 4 - Rebote elástico ping-pong en extremos (0.0 y 100.0): CORRECTO");
        } else {
            System.err.println("[ERROR] Caso 4 - Rebote derecho: " + rebotoDerecha + ", Rebote izquierdo: " + rebotoIzquierda);
            fallos++;
        }

        // Caso 5: Dificultad incremental por intento
        gauge.ajustarDificultadPorIntento(1);
        double vel1 = gauge.getSpeed();
        double ancho1 = gauge.getGreenZoneEnd() - gauge.getGreenZoneStart();

        gauge.ajustarDificultadPorIntento(2);
        double vel2 = gauge.getSpeed();
        double ancho2 = gauge.getGreenZoneEnd() - gauge.getGreenZoneStart();

        gauge.ajustarDificultadPorIntento(3);
        double vel3 = gauge.getSpeed();
        double ancho3 = gauge.getGreenZoneEnd() - gauge.getGreenZoneStart();

        if (vel1 < vel2 && vel2 < vel3 && ancho1 > ancho2 && ancho2 > ancho3) {
            System.out.println("[OK] Caso 5 - Dificultad por intentos: Velocidad crece (1.3 < 1.7 < 2.1) y ancho decrece (24 > 20 > 16)");
        } else {
            System.err.println("[ERROR] Caso 5 - Fallo en progresión de dificultad");
            fallos++;
        }

        System.out.println("=============================================");
        if (fallos == 0) {
            System.out.println("RESULTADO: TODAS LAS PRUEBAS DE PRECISION GAUGE PASARON EXITOSAMENTE.");
        } else {
            System.err.println("RESULTADO: " + fallos + " PRUEBAS FALLARON.");
            System.exit(1);
        }
    }
}
