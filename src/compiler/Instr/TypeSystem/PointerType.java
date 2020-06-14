package compiler.Instr.TypeSystem;

import compiler.Instr.Operand.ConstNull;
import compiler.Instr.Operand.Operand;

public class PointerType extends IRType {
    private IRType baseType;

    public PointerType(IRType baseType) {
        this.baseType = baseType;
    }

    public IRType getBaseType() {
        return baseType;
    }

    @Override
    public Operand getDefaultValue() {
        return new ConstNull();
    }

    @Override
    public int getBytes() {
        return 4;
    }

    @Override
    public String toString() {
        return baseType.toString() + "*";
    }
}
