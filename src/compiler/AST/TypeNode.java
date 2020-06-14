package compiler.AST;

import compiler.Utility.Position;

abstract public class TypeNode extends BaseNode {
    protected String identifier;

    public TypeNode(Position position, String identifier) {
        super(position);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
