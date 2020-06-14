package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

import java.util.ArrayList;

public class VariableStmtNode extends StmtNode {
    private ArrayList<VarNode> varList;

    public VariableStmtNode(Position position, ArrayList<VarNode> varList) {
        super(position);
        this.varList = varList;
    }

    public ArrayList<VarNode> getVarList() {
        return varList;
    }

    public void addVar(VarNode var) {
        varList.add(var);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("<VariableStmtNode>\n");
        string.append("varList:\n");
        for (VarNode var : varList)
            string.append(var.toString());
        return string.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
