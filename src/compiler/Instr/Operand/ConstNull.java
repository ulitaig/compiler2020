package compiler.Instr.Operand;

import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Instr.TypeSystem.VoidType;

public class ConstNull extends Constant {
    public ConstNull() {
        super(new PointerType(new VoidType()));
    }

    @Override
    public Constant castToType(IRType objectType) {
        if (objectType instanceof PointerType)
            return new ConstNull();
        throw new RuntimeException("ConstNull cast to " + objectType.toString());
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConstNull;
    }
}
