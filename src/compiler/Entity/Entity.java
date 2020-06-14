package compiler.Entity;

import compiler.Utility.Position;

abstract public class Entity {
    private String name;
    private boolean referred;
    private Position position;

    public Entity(String name, Position position) {
        this.name = name;
        this.referred = false;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setReferred() {
        referred = true;
    }

    public boolean isReferred() { // used for warning
        return referred;
    }

    public Position getPosition() {
        return position;
    }
}
