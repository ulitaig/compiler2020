package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;

public class NullNode extends ConstExprNode {
    public NullNode(Position position, String text) {
        super(position, text);
    }

    @Override
    public String toString() {
        return "<NullNode>\n";
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
