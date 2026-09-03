package model.spacecraft;

import java.util.ArrayList;
import java.util.List;
import model.components.FuelTank;
import model.components.Kerbal;
import model.geometry.GeoPosition;

public class CrewShuttle extends Spacecraft {
    private List<Kerbal> crew;

    public CrewShuttle(SpacecraftInfo info, GeoPosition position, FuelTank fuelTank) {
        super(info, position, fuelTank, 25000.0, "Spacecraft.jpg");
        this.crew = new ArrayList<>();
    }

    public void addCrewMember(Kerbal kerbal) {
        if (kerbal != null) {
            crew.add(kerbal);
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
            return pilot.ejecutarManiobraPilotaje();
        }
        return "Transbordador sin tripulación a bordo para realizar la maniobra.";
    }

    public List<Kerbal> getCrew() {
        return crew;
    }
}
