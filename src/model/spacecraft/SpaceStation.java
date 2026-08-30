package model.spacecraft;

import model.components.FuelTank;
import model.geometry.GeoPosition;

public class SpaceStation extends Spacecraft {
    private boolean dockingAvailable;
    
    // Variables de sistemas internos (Telemetría Micro)
    private double oxygenLevel;        // Porcentaje (0.0 a 100.0)
    private double batteryLevel;       // Porcentaje (0.0 a 100.0)
    private double temperature;        // Grados Celsius
    private boolean solarPanelsDeployed; // Estado de los paneles

    public SpaceStation(String id, String name, int noradId, GeoPosition position) {
        this(id, name, noradId, position, "SpaceStation_Internacional.jpg");
    }

    public SpaceStation(String id, String name, int noradId, GeoPosition position, String nombreImagen) {
        // La estación espacial tiene un tanque de reserva ilimitado/grande y orbita suavemente
        super(id, name, noradId, position, new FuelTank(10000.0, 10000.0, 0.0), 27600.0);
        this.dockingAvailable = true;
        this.nombreImagen = nombreImagen;
        
        // Inicializar sistemas
        this.oxygenLevel = 100.0;
        this.batteryLevel = 100.0;
        this.temperature = 22.0;
        this.solarPanelsDeployed = true;
    }

    @Override
    public void move() {
        // La posición en órbita se sincroniza en tiempo real desde la API N2YO.
        // Simulación de sistemas internos (se ejecuta 1 vez por segundo/tick)
        
        // 1. Oxígeno: Se consume lentamente por la tripulación
        oxygenLevel -= 0.05;
        if (oxygenLevel < 0) oxygenLevel = 0;
        
        // 2. Batería: Depende de los paneles solares
        if (solarPanelsDeployed) {
            batteryLevel += 0.2; // Recarga
        } else {
            batteryLevel -= 0.8; // Consumo
        }
        if (batteryLevel > 100.0) batteryLevel = 100.0;
        if (batteryLevel < 0) batteryLevel = 0;
        
        // 3. Temperatura: Tiende a bajar si no hay energía suficiente para la calefacción
        if (batteryLevel > 20.0) {
            // Regulación térmica activa
            if (temperature < 22.0) temperature += 0.1;
            if (temperature > 22.0) temperature -= 0.1;
        } else {
            // Falla de regulación térmica, el frío del espacio entra
            temperature -= 0.5;
        }
    }

    @Override
    public String getType() {
        return "Estación Espacial Orbital";
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
