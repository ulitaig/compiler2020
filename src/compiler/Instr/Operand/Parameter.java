package compiler.Instr.Operand;

import compiler.IR.Function;
import compiler.Instr.IRInstruction;
import compiler.Instr.TypeSystem.IRType;

import java.util.Queue;
import java.util.Set;

public class Parameter extends Operand {
    private String name;
    private Function function;

    public Parameter(IRType type, String name) {
        super(type);
        this.name = name;
    }

    public String getName() {
            return name;
    }

    public String getFullName() {
        return function.getName() + toString();
    }

    public String getNameWithoutDot() {
        if (name.contains("."))
            return name.split("\\.")[0];
        else
            throw new RuntimeException();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    @Override
    public boolean isConstValue() {
        return false;
    }

    @Override
    public void markBaseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue) {
        // do nothing.
    }

    @Override
    public String toString() {
        return "%" + name;
    }
}
