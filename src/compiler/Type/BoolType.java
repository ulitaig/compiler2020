package compiler.Type;

import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.IRTypeTable;

public class BoolType extends Type {
    public BoolType() {
        super("bool", 0);
    }

    @Override
    public IRType getIRType(IRTypeTable irTypeTable) {
        return irTypeTable.get(this);
    }

    @Override
    public Operand getDefaultValue() {
        return new ConstBool(false);
    }
}
