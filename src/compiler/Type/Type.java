package compiler.Type;

import compiler.Instr.Operand.Operand;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.IRTypeTable;

abstract public class Type {
    private String name;
    private long size;

    public Type(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public static boolean canNotAssign(Type left, Type right) {
        if (left instanceof ArrayType || left instanceof ClassType) {
            if (right instanceof NullType)
                return false;
            else
                return !left.equals(right);
        } else
            return !left.equals(right);
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Type)
            return toString().equals(obj.toString());
        else
            return false;
    }

    public String toString() {
        return name;
    }

    abstract public IRType getIRType(IRTypeTable irTypeTable);

    abstract public Operand getDefaultValue();
}
