package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class IdExprNode extends ExprNode {
    private String identifier;

    public IdExprNode(Position position, String text, String identifier) {
        super(position, text);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return "<IdExprNode>\n" + "identifier = " + identifier + "\n";
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
