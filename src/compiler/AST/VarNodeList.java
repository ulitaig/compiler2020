package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

import java.util.ArrayList;

public class VarNodeList extends UnitNode {
    private ArrayList<VarNode> varNodes;

    public VarNodeList(Position position, ArrayList<VarNode> varNodes) {
        super(position);
        this.varNodes = varNodes;
    }

    public ArrayList<VarNode> getVarNodes() {
        return varNodes;
    }

    public void addVarNode(VarNode varNode) {
        varNodes.add(varNode);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("<VarNodeList>\n");
        string.append("varNodes:\n");
        for (VarNode var : varNodes)
            string.append(var.toString());
        return string.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        throw new SemanticError(new Position(0,0),"visit VarNodeList");
    }
}
