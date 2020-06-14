package compiler.Type;

import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.IRTypeTable;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Instr.TypeSystem.VoidType;

public class NullType extends Type {
    public NullType() {
        super("null", 0);
    }

    @Override
    public IRType getIRType(IRTypeTable irTypeTable) {
        return new PointerType(new VoidType());
    }

    @Override
    public Operand getDefaultValue() {
        return new ConstNull();
    }
}
