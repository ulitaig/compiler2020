package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class ContinueStmtNode extends StmtNode {
    public ContinueStmtNode(Position position) {
        super(position);
    }

    @Override
    public String toString() {
        return "<ContinueStmtNode>\n";
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
