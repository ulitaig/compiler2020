package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;

public class BoolConstantNode extends ConstExprNode {
    private boolean value;

    public BoolConstantNode(Position position, String text, boolean value) {
        super(position, text);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "<BoolConstantNode>\n" + "value = " + value + "\n";
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
