package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class BinaryExprNode extends ExprNode {
    public enum Operator {
        mul, div, mod,
        add, sub,
        shiftLeft, shiftRight,
        less, greater, lessEqual, greaterEqual,
        equal, notEqual,
        bitwiseAnd, bitwiseXor, bitwiseOr,
        logicalAnd, logicalOr,
        assign
    }

    private Operator op;
    private ExprNode lhs, rhs;

    public BinaryExprNode(Position position, String text, Operator op, ExprNode lhs, ExprNode rhs) {
        super(position, text);
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public Operator getOp() {
        return op;
    }

    public ExprNode getLhs() {
        return lhs;
    }

    public ExprNode getRhs() {
        return rhs;
    }

    @Override
    public String toString() {
        String headString = "<BinaryExprNode>\n";
        String opString = "op = " + op.toString();
        String lhsString = "lhs = " + lhs.toString();
        String rhsString = "rhs = " + rhs.toString();
        return headString + opString + lhsString + rhsString;
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
