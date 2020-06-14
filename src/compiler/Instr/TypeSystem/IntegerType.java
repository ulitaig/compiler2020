package compiler.Instr.TypeSystem;

import compiler.Instr.Operand.ConstBool;
import compiler.Instr.Operand.ConstInt;
import compiler.Instr.Operand.Operand;

public class IntegerType extends IRType {
    public enum BitWidth {
        int1, int8, int32
    }

    private BitWidth bitWidth;

    public IntegerType(BitWidth bitWidth) {
        this.bitWidth = bitWidth;
    }

    public BitWidth getBitWidth() {
        return bitWidth;
    }

    @Override
    public Operand getDefaultValue() {
        if (bitWidth == BitWidth.int1)
            return new ConstBool(false);
        else
            return new ConstInt(bitWidth, 0);
    }

    @Override
    public int getBytes() {
        if (bitWidth == BitWidth.int1 || bitWidth == BitWidth.int8)
            return 1;
        else
            return 4;
    }

    @Override
    public String toString() {
        if (bitWidth == BitWidth.int1)
            return "i1";
        else if (bitWidth == BitWidth.int8)
            return "i8";
        else
            return "i32";
    }
}
