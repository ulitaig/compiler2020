package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;

public class IntConstantNode extends ConstExprNode {
    private long value;

    public IntConstantNode(Position position, String text, long value) {
        super(position, text);
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "<IntConstantNode>\n" + "value = " + value + "\n";
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}