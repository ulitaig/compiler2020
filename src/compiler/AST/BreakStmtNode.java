package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class BreakStmtNode extends StmtNode {
    public BreakStmtNode(Position position) {
        super(position);
    }

    @Override
    public String toString() {
        return "<BreakStmtNode>\n";
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
