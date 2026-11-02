package model.spacecraft;

import model.components.FuelTank;
import model.components.PercentageGauge;
import model.geometry.GeoPosition;

public class SpaceStation extends OrbitalObject implements FuelCarrier, EvasiveCapable {
    private String stationType;
    private String imagenDestruida;
    private String textoDestruccion;
    
    // Subsistemas internos encapsulados con tipado de dominio
    private FuelTank fuelTank;                 // Reserva de combustible para acople y maniobras orbitales
    private PercentageGauge oxygenGauge;       // Porcentaje de soporte vital (0.0% a 100.0%)
    private PercentageGauge batteryGauge;      // Porcentaje de energía eléctrica (0.0% a 100.0%)
    private double temperature;                // Grados Celsius
    private boolean solarPanelsDeployed;       // Estado de los paneles solares

    public SpaceStation(SpacecraftInfo info, GeoPosition position) {
        this(
            info,
            position,
            "SpaceStation_Internacional.jpg",
            "Estación Espacial Internacional (ISS)",
            "SpaceStation_Internacional_Destruida.jfif",
            "La Estación Espacial Internacional quedó totalmente inoperativa"
        );
    }

    public SpaceStation(SpacecraftInfo info, GeoPosition position, String nombreImagen, String stationType) {
        this(
            info,
            position,
            nombreImagen,
            stationType,
            "SpaceStation_Internacional_Destruida.jfif",
            "La " + (stationType != null ? stationType : "Estación Espacial") + " quedó totalmente inoperativa"
        );
    }

    /**
     * Constructor parametrizado que aplica el Principio de Abierto/Cerrado (SOLID - OCP).
     * Permite extender el sistema con nuevas estaciones espaciales (con sus propias imágenes y
     * mensajes de destrucción) sin necesidad de abrir ni modificar el código de SpaceStation.
     */
    public SpaceStation(
            SpacecraftInfo info,
            GeoPosition position,
            String nombreImagen,
            String stationType,
            String imagenDestruida,
            String textoDestruccion) {
        // La estación espacial es un objeto orbital con velocidad orbital estándar (~27600 km/h)
        super(info, position, 27600.0, nombreImagen);
        this.stationType = stationType;
        this.imagenDestruida = (imagenDestruida != null && !imagenDestruida.isEmpty()) ? imagenDestruida : "SpaceStation_Internacional_Destruida.jfif";
        this.textoDestruccion = (textoDestruccion != null && !textoDestruccion.isEmpty())
                ? textoDestruccion
                : "La Estación Espacial quedó totalmente inoperativa";
        
        // Inicializar componentes de subsistemas y reserva de combustible
        this.fuelTank = new FuelTank(2500.0, 1800.0, 0.0);
        this.oxygenGauge = new PercentageGauge(80.0);
        this.batteryGauge = new PercentageGauge(100.0);
        this.temperature = 22.0;
        this.solarPanelsDeployed = true;
    }

    @Override
    public void move() {
        // Simulación de subsistemas internos de soporte vital por tick
        actualizarSoporteVital();
        actualizarBaterias();
        regularTemperatura();
    }

    /**
     * Sistema de Soporte Vital y Control Ambiental (ECLSS / OGS).
     * Si los paneles solares están desplegados y la batería supera el 30%, se ejecuta la
     * electrólisis para regenerar oxígeno consumiendo un excedente leve de energía eléctrica.
     * Si no hay energía o están replegados, el oxígeno decae pasivamente por consumo de la tripulación.
     */
    private void actualizarSoporteVital() {
        if (solarPanelsDeployed && batteryGauge.isAbove(30.0)) {
            if (oxygenGauge.getLevel() < 100.0) {
                batteryGauge.decrease(0.10); // Consumo eléctrico del generador de electrólisis
                oxygenGauge.increase(0.15);  // Regeneración gradual de oxígeno
            }
        } else {
            // Sin energía o paneles replegados: decae por respiración de la tripulación
            oxygenGauge.decrease(0.05);
        }
    }

    private void actualizarBaterias() {
        if (solarPanelsDeployed) {
            batteryGauge.increase(0.25); // Recarga solar activa de paneles
        } else {
            batteryGauge.decrease(0.8);  // Consumo pasivo sin recarga
        }
    }

    private void regularTemperatura() {
        if (batteryGauge.isAbove(20.0)) {
            // Regulación térmica activa hacia temperatura óptima (22°C)
            if (temperature < 22.0) temperature += 0.1;
            if (temperature > 22.0) temperature -= 0.1;
        } else {
            // Falla de regulación térmica por baja energía
            temperature -= 0.5;
        }
    }

    @Override
    public String getType() {
        return (stationType != null && !stationType.isEmpty()) ? stationType : "Estación Espacial Orbital";
    }

    @Override
    public String performSpecialAbility() {
        return "Estación Espacial [" + getName() + "] emitió una señal de acople y recarga para naves cercanas.";
    }

    @Override
    public java.awt.Color getColorRadar() {
        return new java.awt.Color(0, 200, 220); // Cian
    }

    @Override
    public int getTamanoIconoRadar() {
        return 9;
    }

    @Override
    public boolean tieneAnilloRadar() {
        return true;
    }

    @Override
    public String getDetalleTelemetria() {
        boolean eclssActivo = solarPanelsDeployed && batteryGauge.isAbove(30.0);
        String estadoO2 = eclssActivo
                ? (oxygenGauge.getLevel() >= 99.9 ? "ÓPTIMO (100%)" : "ECLSS ELECTRÓLISIS (Cargando)")
                : "CONSUMO TRIPULACIÓN (Sin energía)";

        return String.format(
            "\n--- SISTEMAS VITALES ---\nOxígeno:   %.1f%% [%s]\nBatería:   %.1f%%\nTemp:      %.1f °C\nPaneles:   %s\n",
            getOxygenLevel(),
            estadoO2,
            getBatteryLevel(),
            getTemperature(),
            areSolarPanelsDeployed() ? "DESPLEGADOS (Cargando)" : "RETRAÍDOS (Consumo)"
        );
    }

    @Override
    public double getConsumoEvasion() {
        return 100.0; // Maniobra orbital normal de estación (-100L)
    }

    @Override
    public double getConsumoEvasionCrisis() {
        return 200.0; // Maniobra forzada ante impacto crítico de anomalía (el doble: -200L)
    }

    @Override
    public boolean evade(double deltaLat, double deltaLng) {
        return evade(deltaLat, deltaLng, getConsumoEvasion());
    }

    @Override
    public boolean evade(double deltaLat, double deltaLng, double consumo) {
        if (fuelTank != null && fuelTank.getCurrentLevel() >= consumo) {
            boolean success = fuelTank.consume(consumo);
            if (getPosition() != null) {
                getPosition().setLatitude(getPosition().getLatitude() + deltaLat);
                getPosition().setLongitude(getPosition().getLongitude() + deltaLng);
                return true;
            }
        }
        return false;
    }

    public boolean refuelShip(Spacecraft ship) {
        if (ship != null && this.distanceTo(ship) <= 500.0) { // Dentro del radio de acople (500 km)
            double cantidad = 50.0;
            if (fuelTank != null && fuelTank.getCurrentLevel() >= cantidad) {
                fuelTank.consume(cantidad);
                return ship.refuel(cantidad);
            }
        }
        return false;
    }
    
    @Override
    public FuelTank getFuelTank() {
        return fuelTank;
    }

    @Override
    public String getEtiquetaCombustible() {
        return String.format("%.1f%% (Reserva Acople)", fuelTank != null ? fuelTank.getPercentage() : 0.0);
    }
    
    // Getters y Setters de telemetría y subsistemas
    public double getOxygenLevel() { return oxygenGauge.getLevel(); }
    public double getBatteryLevel() { return batteryGauge.getLevel(); }
    public PercentageGauge getOxygenGauge() { return oxygenGauge; }
    public PercentageGauge getBatteryGauge() { return batteryGauge; }
    public double getTemperature() { return temperature; }

    public boolean areSolarPanelsDeployed() { return solarPanelsDeployed; }
    public void setSolarPanelsDeployed(boolean solarPanelsDeployed) { this.solarPanelsDeployed = solarPanelsDeployed; }

    public boolean toggleSolarPanels() {
        this.solarPanelsDeployed = !this.solarPanelsDeployed;
        return this.solarPanelsDeployed;
    }
    
    public String getImagenDestruida() { return imagenDestruida; }
    public void setImagenDestruida(String imagenDestruida) { this.imagenDestruida = imagenDestruida; }
    public String getTextoDestruccion() { return textoDestruccion; }
    public void setTextoDestruccion(String textoDestruccion) { this.textoDestruccion = textoDestruccion; }
}