package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class IfStmtNode extends StmtNode {
    private ExprNode cond;
    private StmtNode thenBody, elseBody;

    public IfStmtNode(Position position, ExprNode cond, StmtNode thenBody, StmtNode elseBody) {
        super(position);
        this.cond = cond;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    public ExprNode getCond() {
        return cond;
    }

    public boolean hasThenBody() {
        return thenBody != null;
    }

    public StmtNode getThenBody() {
        return thenBody;
    }

    public boolean hasElseBody() {
        return elseBody != null;
    }

    public StmtNode getElseBody() {
        return elseBody;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("<IfStmtNode>\n");
        string.append("cond:\n").append(cond.toString());
        string.append("thenBody:\n").append(thenBody.toString());
        if (hasElseBody())
            string.append("elseBody:\n").append(elseBody.toString());
        return string.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
