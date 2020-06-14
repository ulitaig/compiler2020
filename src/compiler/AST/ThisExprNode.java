package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class ThisExprNode extends ExprNode {
    public ThisExprNode(Position position, String text) {
        super(position, text);
    }

    @Override
    public String toString() {
        return "<ThisExprNode>\n";
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
