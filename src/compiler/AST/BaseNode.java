package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Semantic.Scope;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public abstract class BaseNode {
    private Position position;
    private Scope scope;

    public BaseNode(Position position) {
        this.position = position;
        scope = null;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return this.position;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Scope getScope() {
        return scope;
    }

    abstract public void accept(ASTVisitor visitor) throws SemanticError;

    abstract public String toString();


    public int hashCode() {
        return toString().hashCode();
    }


    public boolean equals(Object obj) {
        if (obj instanceof BaseNode)
            return toString().equals(obj.toString());
        else
            return false;
    }
}
