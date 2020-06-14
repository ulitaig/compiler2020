package compiler.Type;

import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.IRTypeTable;
import compiler.Instr.TypeSystem.IntegerType;

public class IntType extends Type {
    public IntType() {
        super("int", 0);
    }

    @Override
    public IRType getIRType(IRTypeTable irTypeTable) {
        return irTypeTable.get(this);
    }

    @Override
    public Operand getDefaultValue() {
        return new ConstInt(IntegerType.BitWidth.int32, 0);
    }
}
