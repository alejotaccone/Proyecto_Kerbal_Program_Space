package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

public class SpaceStation extends OrbitalObject {
    private boolean dockingAvailable;
    private String stationType;
    
    // Variables de sistemas internos (Telemetría Micro)
    private double oxygenLevel;        // Porcentaje (0.0 a 100.0)
    private double batteryLevel;       // Porcentaje (0.0 a 100.0)
    private double temperature;        // Grados Celsius
    private boolean solarPanelsDeployed; // Estado de los paneles

    public SpaceStation(SpacecraftInfo info, GeoPosition position) {
        this(info, position, "SpaceStation_Internacional.jpg", "Estación Espacial Internacional (ISS)");
    }

    public SpaceStation(SpacecraftInfo info, GeoPosition position, String nombreImagen) {
        this(info, position, nombreImagen, "Estación Espacial Orbital");
    }

    public SpaceStation(SpacecraftInfo info, GeoPosition position, String nombreImagen, String stationType) {
        // La estación espacial es un objeto orbital con velocidad orbital estándar (~27600 km/h)
        super(info, position, 27600.0);
        this.dockingAvailable = true;
        this.nombreImagen = nombreImagen;
        this.stationType = stationType;
        
        // Inicializar sistemas
        this.oxygenLevel = 100.0;
        this.batteryLevel = 100.0;
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
        oxygenLevel -= 0.05;
        if (oxygenLevel < 0.0) {
            oxygenLevel = 0.0;
        }
    }

    private void actualizarBaterias() {
        if (solarPanelsDeployed) {
            batteryLevel += 0.2; // Recarga solar
        } else {
            batteryLevel -= 0.8; // Consumo
        }
        if (batteryLevel > 100.0) batteryLevel = 100.0;
        if (batteryLevel < 0.0) batteryLevel = 0.0;
    }

    private void regularTemperatura() {
        if (batteryLevel > 20.0) {
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
        return "Estación Espacial [" + name + "] emitió una señal de acople y recarga para naves cercanas.";
    }

    public boolean refuelShip(Spacecraft ship) {
        if (ship != null && ship.getFuelTank() != null) {
            double distance = this.position.distanceTo(ship.getPosition());
            if (distance <= 500.0) { // Dentro del radio de acople (500 km)
                ship.getFuelTank().refuel(50.0);
                return true;
            }
        }
        return false;
    }

    public boolean isDockingAvailable() {
        return dockingAvailable;
    }
    
    // Getters y Setters de telemetría
    public double getOxygenLevel() { return oxygenLevel; }
    public void setOxygenLevel(double oxygenLevel) { this.oxygenLevel = oxygenLevel; }

    public double getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(double batteryLevel) { this.batteryLevel = batteryLevel; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public boolean areSolarPanelsDeployed() { return solarPanelsDeployed; }
    public void toggleSolarPanels() { this.solarPanelsDeployed = !this.solarPanelsDeployed; }
    
    // Métodos de control interactivo
    public void generateOxygen() {
        if (batteryLevel > 15.0) {
            batteryLevel -= 10.0;
            oxygenLevel += 25.0;
            if (oxygenLevel > 100.0) oxygenLevel = 100.0;
        }
    }
}
