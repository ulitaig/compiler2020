package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class WhileStmtNode extends StmtNode {
    private ExprNode cond;
    private StmtNode body;

    public WhileStmtNode(Position position, ExprNode cond, StmtNode body) {
        super(position);
        this.cond = cond;
        this.body = body;
    }

    public ExprNode getCond() {
        return cond;
    }

    public boolean hasBody() {
        return body != null;
    }

    public StmtNode getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "<WhileStmtNode>\n" + "cond:\n" + cond.toString() + "body:\n" + body.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
