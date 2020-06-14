package compiler.Codegen.Operand.Immediate;

import compiler.Codegen.Operand.ASMOperand;

abstract public class Immediate extends ASMOperand {
    @Override
    abstract public boolean equals(Object obj);
}
