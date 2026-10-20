package test;

import model.geometry.GeoPosition;

/**
 * Verificación automatizada sin dependencias externas para GeoPosition (Fórmula 3D ECEF).
 */
public class GeoPositionCheck {

    public static void main(String[] args) {
        System.out.println("=== EJECUTANDO PRUEBAS DE GEOPOSITION (3D ECEF vs SUPERFICIE) ===");
        int fallos = 0;

        // Caso 1: Mismo punto exacto (lat, lon, alt idénticos) -> debe dar 0.0 km
        GeoPosition p1 = new GeoPosition(-34.6037, -58.3816, 400.0);
        GeoPosition p2 = new GeoPosition(-34.6037, -58.3816, 400.0);
        double distMismoPunto = p1.distance3DTo(p2);
        if (Math.abs(distMismoPunto - 0.0) < 1e-6) {
            System.out.println("[OK] Caso 1 - Mismo punto exacto: " + distMismoPunto + " km (esperado 0.0 km)");
        } else {
            System.err.println("[FALLO] Caso 1 - Mismo punto exacto dio: " + distMismoPunto);
            fallos++;
        }

        // Caso 2: Mismo lat/lon y distinta altitud (ej. 400 km vs 35786 km) -> debe dar exactamente la diferencia de altitud
        GeoPosition pBase = new GeoPosition(0.0, 0.0, 400.0);
        GeoPosition pGeo = new GeoPosition(0.0, 0.0, 35786.0);
        double distAltPura = pBase.distance3DTo(pGeo);
        double esperadoAlt = 35786.0 - 400.0; // 35386.0 km
        if (Math.abs(distAltPura - esperadoAlt) < 1e-4) {
            System.out.println("[OK] Caso 2 - Misma vertical, distinta altitud: " + distAltPura + " km (esperado: " + esperadoAlt + " km)");
        } else {
            System.err.println("[FALLO] Caso 2 - Esperado: " + esperadoAlt + ", obtenido: " + distAltPura);
            fallos++;
        }

        // Caso 3: Dos puntos a la misma altitud orbital (400 km) separados un arco
        // La distancia 3D euclídea (cuerda) debe ser levemente menor que el arco orbital real:
        // Arco orbital = r * c = (6371 + 400) * c
        // Cuerda 3D = 2 * r * sin(c/2)
        GeoPosition pA = new GeoPosition(0.0, 0.0, 400.0);
        GeoPosition pB = new GeoPosition(0.0, 5.0, 400.0); // 5 grados de longitud en el ecuador
        double dist3D = pA.distance3DTo(pB);
        double r = 6371.0 + 400.0;
        double cRad = Math.toRadians(5.0);
        double arcoOrbitalEsperado = r * cRad; // ~ 590.89 km
        double cuerdaEsperada = 2.0 * r * Math.sin(cRad / 2.0); // ~ 590.52 km

        if (Math.abs(dist3D - cuerdaEsperada) < 1e-3) {
            System.out.println("[OK] Caso 3 - Misma altitud, cuerda 3D: " + dist3D + " km (cuerda teórica: " + cuerdaEsperada + " km, arco orbital: " + arcoOrbitalEsperado + " km)");
        } else {
            System.err.println("[FALLO] Caso 3 - Esperado: " + cuerdaEsperada + ", obtenido: " + dist3D);
            fallos++;
        }

        // Caso 4: Comprobación de método de superficie pura (Haversine)
        double distSuperficie = pA.surfaceDistanceKm(pB);
        double arcoNivelMar = 6371.0 * cRad; // ~ 555.97 km
        if (Math.abs(distSuperficie - arcoNivelMar) < 1e-3) {
            System.out.println("[OK] Caso 4 - Distancia de superficie pura (Haversine R=6371): " + distSuperficie + " km (esperado: " + arcoNivelMar + " km)");
        } else {
            System.err.println("[FALLO] Caso 4 - Esperado: " + arcoNivelMar + ", obtenido: " + distSuperficie);
            fallos++;
        }

        // Caso 5: distanceTo() debe coincidir con distance3DTo()
        double distDefault = pA.distanceTo(pB);
        if (Math.abs(distDefault - dist3D) < 1e-6) {
            System.out.println("[OK] Caso 5 - distanceTo() delega correctamente en distance3DTo()");
        } else {
            System.err.println("[FALLO] Caso 5 - distanceTo() no coincide con distance3DTo()");
            fallos++;
        }

        System.out.println("==========================================================");
        if (fallos == 0) {
            System.out.println("RESULTADO: TODAS LAS PRUEBAS PASARON EXITOSAMENTE.");
        } else {
            System.err.println("RESULTADO: HUBO " + fallos + " FALLOS.");
            System.exit(1);
        }
    }
}
