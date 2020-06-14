package compiler.Codegen.Operand;

abstract public class ASMOperand {
    abstract public String emitCode();

    @Override
    abstract public String toString();
}
