package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

import java.util.ArrayList;

public class ProgramNode extends BaseNode {
    private ArrayList<UnitNode> programUnits;

    public ProgramNode(Position position, ArrayList<UnitNode> programUnits) {
        super(position);
        this.programUnits = programUnits;
    }

    public ArrayList<UnitNode> getProgramUnits() {
        return programUnits;
    }

    public void addProgramUnit(UnitNode programUnit) {
        programUnits.add(programUnit);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("<ProgramNode>\n");
        string.append("programUnits:\n");
        for (UnitNode unit : programUnits)
            string.append(unit.toString());
        return string.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
