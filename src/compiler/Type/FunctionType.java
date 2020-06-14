package compiler.Type;

import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.IRTypeTable;

public class FunctionType extends Type {
    private Type type;

    public FunctionType(String name, Type type) {
        super(name, 0);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "#MethodType#";
    }

    @Override
    public IRType getIRType(IRTypeTable irTypeTable) {
        return null;
    }

    @Override
    public Operand getDefaultValue() {
        return null;
    }
}
