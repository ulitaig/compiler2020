package compiler.Instr.TypeSystem;

import compiler.Instr.Operand.Operand;

public class ArrayType extends IRType {
    private int size;
    private IRType type;

    public ArrayType(int size, IRType type) {
        this.size = size;
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    public IRType getType() {
        return type;
    }

    @Override
    public Operand getDefaultValue() {
        // This method will never be called.
        throw new RuntimeException();
    }

    @Override
    public int getBytes() {
        return type.getBytes() * size;
    }

    @Override
    public String toString() {
        return "[" + size + " x " + type.toString() + "]";
    }
}
