package model.components;

public class Kerbal {
    private String name;
    private String role; // PILOT, ENGINEER, SCIENTIST
    private int courage; // 0 - 100

    public Kerbal(String name, String role, int courage) {
        this.name = name;
        this.role = role;
        this.courage = courage;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public int getCourage() {
        return courage;
    }

    @Override
    public String toString() {
        return String.format("Kerbal [%s - Rol: %s, Valentía: %d%%]", name, role, courage);
    }
}
