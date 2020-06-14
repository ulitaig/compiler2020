package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

import java.util.ArrayList;

public class BlockNode extends StmtNode {
    private ArrayList<StmtNode> statements;

    public BlockNode(Position position, ArrayList<StmtNode> statements) {
        super(position);
        this.statements = statements;
    }

    public ArrayList<StmtNode> getStatements() {
        return statements;
    }

    public void addStatement(StmtNode stmt) {
        statements.add(stmt);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("<BlockNode>\n");
        string.append("statements:\n");
        for (StmtNode statement : statements)
            string.append(statement.toString());
        return string.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}