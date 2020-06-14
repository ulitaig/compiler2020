package compiler.Instr.Operand;

import compiler.Instr.IRInstruction;
import compiler.Instr.TypeSystem.IRType;

import java.util.Queue;
import java.util.Set;

abstract public class Constant extends Operand {
    public Constant(IRType type) {
        super(type);
    }

    @Override
    public boolean isConstValue() {
        return true;
    }

    @Override
    public void markBaseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue) {
        // do nothing.
    }

    abstract public Constant castToType(IRType objectType);

    @Override
    abstract public boolean equals(Object obj);
}
