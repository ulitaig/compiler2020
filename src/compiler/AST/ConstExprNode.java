package compiler.AST;

import compiler.Utility.Position;

abstract public class ConstExprNode extends ExprNode {
    public ConstExprNode(Position position, String text) {
        super(position, text);
    }
}