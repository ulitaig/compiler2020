package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class MemberExprNode extends ExprNode {
    private ExprNode expr;
    private String identifier;

    public MemberExprNode(Position position, String text, ExprNode expr, String identifier) {
        super(position, text);
        this.expr = expr;
        this.identifier = identifier;
    }

    public ExprNode getExpr() {
        return expr;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return "<MemberExprNode>\n" + "expr:\n" + expr.toString() + "identifier = " + identifier + "\n";
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
