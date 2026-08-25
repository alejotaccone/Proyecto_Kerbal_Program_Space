package model.spacecraft;

import java.util.ArrayList;
import java.util.List;
import model.components.FuelTank;
import model.components.Kerbal;
import model.geometry.GeoPosition;

public class CrewShuttle extends Spacecraft {
    private List<Kerbal> crew;

    public CrewShuttle(String id, String name, int noradId, GeoPosition position, FuelTank fuelTank) {
        super(id, name, noradId, position, fuelTank, 25000.0);
        this.crew = new ArrayList<>();
        this.img = "Spacecraft.jpg";
    }

    public void addCrewMember(Kerbal kerbal) {
        if (kerbal != null) {
            crew.add(kerbal);
        }
    }

    @Override
    public void move() {
        if (fuelTank != null) {
            fuelTank.consume();
        }
    }

    @Override
    public String getType() {
        return "Transbordador Tripulado";
    }

    @Override
    public String performSpecialAbility() {
        if (!crew.isEmpty()) {
            Kerbal pilot = crew.get(0);
            return "El tripulante " + pilot.getName() + " (" + pilot.getRole() + ") realizó un ajuste fino de inclinación orbital con valentía del " + pilot.getCourage() + "%.";
        }
        return "Transbordador sin tripulación a bordo para realizar la maniobra.";
    }

    public List<Kerbal> getCrew() {
        return crew;
    }
}
