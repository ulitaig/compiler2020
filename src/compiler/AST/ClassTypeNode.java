package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class ClassTypeNode extends TypeNode {
    public ClassTypeNode(Position position, String identifier) {
        super(position, identifier);
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "<ClassTypeNode>\n" + "identifier = " + identifier + "\n";
    }
}
