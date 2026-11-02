package engine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Modelo de dominio que centraliza la Bitácora de Misión y Estadísticas de Vuelo
 * del operador durante la sesión de simulación.
 * Aplica los principios SOLID (SRP) y GRASP (Experto en Información) encapsulando
 * el cálculo de métricas (precisión de acople, combustible transferido, evasiones)
 * y la generación de reportes exportables en formato .txt y .json.
 */
public class FlightLogbook {

    private final String fechaInicio;
    private String estacionActiva;
    private int ticksOperados;

    // Estadísticas de Reabastecimiento
    private int carguerosLanzados;
    private int misionesCompletadas;
    private int misionesAbortadas;
    private double totalCombustibleReabastecido;

    // Estadísticas del Minijuego de Acople
    private int intentosAcopleTotales;
    private int acoplesExitosos;
    private int intentosAcopleFallidos;

    // Estadísticas de Defensa y Evasión Orbital
    private int evasionesNormalesExitosas;
    private int evasionesCrisisExitosas;
    private int impactosRecibidos;
    private double combustibleConsumidoEvasiones;

    public FlightLogbook() {
        this.fechaInicio = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.estacionActiva = "Estación Espacial Internacional (ISS)";
    }

    // ==================== REGISTRO DE EVENTOS ====================

    public void setEstacionActiva(String nombreEstacion) {
        if (nombreEstacion != null && !nombreEstacion.isEmpty()) {
            this.estacionActiva = nombreEstacion;
        }
    }

    public void registrarTick() {
        this.ticksOperados++;
    }

    public void registrarLanzamientoCarguero() {
        this.carguerosLanzados++;
    }

    public void registrarIntentoAcople(boolean exitoso) {
        this.intentosAcopleTotales++;
        if (exitoso) {
            this.acoplesExitosos++;
        } else {
            this.intentosAcopleFallidos++;
        }
    }

    public void registrarMisionCompletada(double litrosTransferidos) {
        this.misionesCompletadas++;
        this.totalCombustibleReabastecido += Math.max(0.0, litrosTransferidos);
    }

    public void registrarMisionAbortada() {
        this.misionesAbortadas++;
    }

    public void registrarEvasionNormal(double combustibleConsumido) {
        this.evasionesNormalesExitosas++;
        this.combustibleConsumidoEvasiones += Math.max(0.0, combustibleConsumido);
    }

    public void registrarEvasionCrisis(double combustibleConsumido) {
        this.evasionesCrisisExitosas++;
        this.combustibleConsumidoEvasiones += Math.max(0.0, combustibleConsumido);
    }

    public void registrarImpactoRecibido() {
        this.impactosRecibidos++;
    }

    // ==================== CÁLCULO DE MÉTRICAS (EXPERTO EN INFORMACIÓN) ====================

    public double getPorcentajePrecisionAcople() {
        if (intentosAcopleTotales <= 0) {
            return 100.0;
        }
        return (acoplesExitosos * 100.0) / intentosAcopleTotales;
    }

    public int getTotalEvasionesExitosas() {
        return evasionesNormalesExitosas + evasionesCrisisExitosas;
    }

    public String getCalificacionOperador() {
        if (impactosRecibidos > 0) {
            return "CRÍTICO (Estación / Nave Impactada)";
        }
        if (misionesCompletadas >= 2 && getPorcentajePrecisionAcople() >= 75.0) {
            return "COMANDANTE SUPREMO (Excelencia Orbital)";
        }
        if (misionesCompletadas >= 1 || evasionesCrisisExitosas >= 1) {
            return "VETERANO TÁCTICO (Operación Sólida)";
        }
        return "OFICIAL EN SERVICIO (Operación Nominal)";
    }

    // ==================== EXPORTACIÓN DE REPORTES (.TXT / .JSON) ====================

    public String generarReporteTxt() {
        String fechaReporte = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================================\n");
        sb.append("   KERBAL PROGRAM SPACE - BITÁCORA DE MISIÓN Y REPORTE TÁCTICO      \n");
        sb.append("====================================================================\n");
        sb.append(String.format("Fecha de Inicio : %s\n", fechaInicio));
        sb.append(String.format("Fecha de Emisión: %s\n", fechaReporte));
        sb.append(String.format("Estación Activa : %s\n", estacionActiva));
        sb.append(String.format("Ticks Operados  : %d\n", ticksOperados));
        sb.append(String.format("Calificación    : %s\n", getCalificacionOperador()));
        sb.append("--------------------------------------------------------------------\n");
        sb.append(" [1] LOGÍSTICA Y REABASTECIMIENTO ORBITAL\n");
        sb.append("--------------------------------------------------------------------\n");
        sb.append(String.format(Locale.US, "  * Combustible Total Reabastecido : %.1f L\n", totalCombustibleReabastecido));
        sb.append(String.format("  * Cargueros Desplegados          : %d\n", carguerosLanzados));
        sb.append(String.format("  * Misiones Completadas (Reingreso): %d\n", misionesCompletadas));
        sb.append(String.format("  * Misiones Abortadas / Fallidas  : %d\n", misionesAbortadas));
        sb.append("--------------------------------------------------------------------\n");
        sb.append(" [2] DESEMPEÑO EN MINIJUEGO DE ACOPLE\n");
        sb.append("--------------------------------------------------------------------\n");
        sb.append(String.format(Locale.US, "  * Precisión de Acople            : %.1f%%\n", getPorcentajePrecisionAcople()));
        sb.append(String.format("  * Maniobras de Acople Exitosas   : %d\n", acoplesExitosos));
        sb.append(String.format("  * Intentos Fallidos de Alineación: %d\n", intentosAcopleFallidos));
        sb.append(String.format("  * Intentos Totales Registrados   : %d\n", intentosAcopleTotales));
        sb.append("--------------------------------------------------------------------\n");
        sb.append(" [3] DEFENSA ORBITAL Y MANIOBRAS DE EVASIÓN\n");
        sb.append("--------------------------------------------------------------------\n");
        sb.append(String.format("  * Total Evasiones Exitosas       : %d\n", getTotalEvasionesExitosas()));
        sb.append(String.format("  * Impactos Críticos Evitados     : %d\n", evasionesCrisisExitosas));
        sb.append(String.format("  * Maniobras Preventivas Normales : %d\n", evasionesNormalesExitosas));
        sb.append(String.format("  * Impactos Cinéticos Recibidos   : %d\n", impactosRecibidos));
        sb.append(String.format(Locale.US, "  * Combustible Gastado en Evasión : %.1f L\n", combustibleConsumidoEvasiones));
        sb.append("====================================================================\n");
        sb.append("        FIN DEL REPORTE OFICIAL - CENTRO DE CONTROL ORBITAL         \n");
        sb.append("====================================================================\n");
        return sb.toString();
    }

    public String generarReporteJson() {
        String fechaReporte = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(String.format("  \"proyecto\": \"Kerbal Program Space\",\n"));
        sb.append(String.format("  \"fechaInicio\": \"%s\",\n", escaparJson(fechaInicio)));
        sb.append(String.format("  \"fechaEmision\": \"%s\",\n", escaparJson(fechaReporte)));
        sb.append(String.format("  \"estacionActiva\": \"%s\",\n", escaparJson(estacionActiva)));
        sb.append(String.format("  \"ticksOperados\": %d,\n", ticksOperados));
        sb.append(String.format("  \"calificacionOperador\": \"%s\",\n", escaparJson(getCalificacionOperador())));
        sb.append("  \"reabastecimiento\": {\n");
        sb.append(String.format(Locale.US, "    \"combustibleTotalLitros\": %.1f,\n", totalCombustibleReabastecido));
        sb.append(String.format("    \"carguerosLanzados\": %d,\n", carguerosLanzados));
        sb.append(String.format("    \"misionesCompletadas\": %d,\n", misionesCompletadas));
        sb.append(String.format("    \"misionesAbortadas\": %d\n", misionesAbortadas));
        sb.append("  },\n");
        sb.append("  \"minijuegoAcople\": {\n");
        sb.append(String.format(Locale.US, "    \"porcentajePrecision\": %.1f,\n", getPorcentajePrecisionAcople()));
        sb.append(String.format("    \"acoplesExitosos\": %d,\n", acoplesExitosos));
        sb.append(String.format("    \"intentosFallidos\": %d,\n", intentosAcopleFallidos));
        sb.append(String.format("    \"intentosTotales\": %d\n", intentosAcopleTotales));
        sb.append("  },\n");
        sb.append("  \"defensaOrbital\": {\n");
        sb.append(String.format("    \"totalEvasionesExitosas\": %d,\n", getTotalEvasionesExitosas()));
        sb.append(String.format("    \"impactosCriticosEvitados\": %d,\n", evasionesCrisisExitosas));
        sb.append(String.format("    \"maniobrasPreventivasNormales\": %d,\n", evasionesNormalesExitosas));
        sb.append(String.format("    \"impactosRecibidos\": %d,\n", impactosRecibidos));
        sb.append(String.format(Locale.US, "    \"combustibleConsumidoEvasionesLitros\": %.1f\n", combustibleConsumidoEvasiones));
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String escaparJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ==================== GETTERS ====================

    public String getFechaInicio() { return fechaInicio; }
    public String getEstacionActiva() { return estacionActiva; }
    public int getTicksOperados() { return ticksOperados; }
    public int getCarguerosLanzados() { return carguerosLanzados; }
    public int getMisionesCompletadas() { return misionesCompletadas; }
    public int getMisionesAbortadas() { return misionesAbortadas; }
    public double getTotalCombustibleReabastecido() { return totalCombustibleReabastecido; }
    public int getIntentosAcopleTotales() { return intentosAcopleTotales; }
    public int getAcoplesExitosos() { return acoplesExitosos; }
    public int getIntentosAcopleFallidos() { return intentosAcopleFallidos; }
    public int getEvasionesNormalesExitosas() { return evasionesNormalesExitosas; }
    public int getEvasionesCrisisExitosas() { return evasionesCrisisExitosas; }
    public int getImpactosRecibidos() { return impactosRecibidos; }
    public double getCombustibleConsumidoEvasiones() { return combustibleConsumidoEvasiones; }
}
