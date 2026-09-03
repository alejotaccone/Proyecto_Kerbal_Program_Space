package model.spacecraft;

import model.components.PercentageGauge;
import model.geometry.GeoPosition;

public class SpaceStation extends OrbitalObject {
    private String stationType;
    
    // Subsistemas internos encapsulados con tipado de dominio (PercentageGauge)
    private PercentageGauge oxygenGauge;       // Porcentaje de soporte vital (0.0% a 100.0%)
    private PercentageGauge batteryGauge;      // Porcentaje de energía eléctrica (0.0% a 100.0%)
    private double temperature;                // Grados Celsius
    private boolean solarPanelsDeployed;       // Estado de los paneles solares

    public SpaceStation(SpacecraftInfo info, GeoPosition position) {
        this(info, position, "SpaceStation_Internacional.jpg", "Estación Espacial Internacional (ISS)");
    }

    public SpaceStation(SpacecraftInfo info, GeoPosition position, String nombreImagen, String stationType) {
        // La estación espacial es un objeto orbital con velocidad orbital estándar (~27600 km/h)
        super(info, position, 27600.0, nombreImagen);
        this.stationType = stationType;
        
        // Inicializar componentes de subsistemas
        this.oxygenGauge = new PercentageGauge(100.0);
        this.batteryGauge = new PercentageGauge(100.0);
        this.temperature = 22.0;
        this.solarPanelsDeployed = true;
    }

    @Override
    public void move() {
        // Simulación de subsistemas internos de soporte vital por tick
        consumirOxigeno();
        actualizarBaterias();
        regularTemperatura();
    }

    private void consumirOxigeno() {
        oxygenGauge.decrease(0.05);
    }

    private void actualizarBaterias() {
        if (solarPanelsDeployed) {
            batteryGauge.increase(0.2); // Recarga solar
        } else {
            batteryGauge.decrease(0.8); // Consumo
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

    public boolean refuelShip(Spacecraft ship) {
        if (ship != null && getPosition() != null && ship.getPosition() != null) {
            double distance = getPosition().distanceTo(ship.getPosition());
            if (distance <= 500.0) { // Dentro del radio de acople (500 km)
                return ship.refuel(50.0);
            }
        }
        return false;
    }
    
    // Getters de telemetría y subsistemas
    public double getOxygenLevel() { return oxygenGauge.getLevel(); }
    public double getBatteryLevel() { return batteryGauge.getLevel(); }
    public PercentageGauge getOxygenGauge() { return oxygenGauge; }
    public PercentageGauge getBatteryGauge() { return batteryGauge; }
    public double getTemperature() { return temperature; }
    public boolean areSolarPanelsDeployed() { return solarPanelsDeployed; }
}
