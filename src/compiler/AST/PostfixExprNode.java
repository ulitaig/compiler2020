package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class PostfixExprNode extends ExprNode {
    public enum Operator {
        postInc, postDec
    }

    private Operator op;
    private ExprNode expr;

    public PostfixExprNode(Position position, String text, Operator op, ExprNode expr) {
        super(position, text);
        this.op = op;
        this.expr = expr;
    }

    public Operator getOp() {
        return op;
    }

    public ExprNode getExpr() {
        return expr;
    }

    @Override
    public String toString() {
        return "<PostfixExprNode>\n" + "op = " + op + "\nexpr:\n" + expr.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
