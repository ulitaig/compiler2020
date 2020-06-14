package compiler.AST;

import compiler.Utility.Position;

abstract public class StmtNode extends BaseNode {
    public StmtNode(Position position) {
        super(position);
    }
}
