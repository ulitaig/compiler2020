package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class ReturnStmtNode extends StmtNode {
    private ExprNode returnValue;

    public ReturnStmtNode(Position position, ExprNode returnValue) {
        super(position);
        this.returnValue = returnValue;
    }

    public boolean hasReturnValue() {
        return returnValue != null;
    }

    public ExprNode getReturnValue() {
        return returnValue;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("<ReturnStmtNode>\n");
        if (hasReturnValue())
            string.append("returnValue:\n").append(returnValue.toString());
        return string.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
