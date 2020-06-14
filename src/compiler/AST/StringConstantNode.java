package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;

public class StringConstantNode extends ConstExprNode {
    private String value;

    public StringConstantNode(Position position, String text, String value) {
        super(position, text);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "<StringConstantNode>\n" + "value = " + value + "\n";
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
