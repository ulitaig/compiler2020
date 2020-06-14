package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;


public class BasicTypeNode extends TypeNode{

    public BasicTypeNode(Position location, String identifier) {
        super(location, identifier);
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "<BasicTypeNode>\nidentifier = " + identifier + "\n";
    }
}
