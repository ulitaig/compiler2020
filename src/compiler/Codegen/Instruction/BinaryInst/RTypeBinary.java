package compiler.Codegen.Instruction.BinaryInst;

import compiler.Codegen.ASMVisitor;
import compiler.Codegen.BasicBlock;
import compiler.Codegen.Operand.Register.VirtualRegister;

import java.util.Set;

public class RTypeBinary extends BinaryInst {
    public enum OpName {
        add, sub, mul, div, rem, sll, sra, and, or, xor, slt
    }

    private OpName op;
    private VirtualRegister rs2;

    public RTypeBinary(BasicBlock basicBlock, OpName op,
                       VirtualRegister rs1, VirtualRegister rs2, VirtualRegister rd) {
        super(basicBlock, rd, rs1);
        this.op = op;
        this.rs2 = rs2;

        this.rs2.addUse(this);
        this.addUse(this.rs2);
    }

    @Override
    public void addToUEVarAndVarKill(Set<VirtualRegister> UEVar, Set<VirtualRegister> varKill) {
        if (!varKill.contains(rs2))
            UEVar.add(rs2);
        super.addToUEVarAndVarKill(UEVar, varKill);
    }

    @Override
    public void replaceUse(VirtualRegister oldVR, VirtualRegister newVR) {
        if (rs2 == oldVR)
            rs2 = newVR;
        super.replaceUse(oldVR, newVR);
    }

    @Override
    public String emitCode() {
        return "\t" + op.name() + "\t" + getRd().emitCode() + ", " + getRs1().emitCode() + ", " + rs2.emitCode();
    }

    @Override
    public String toString() {
        return op + " " + getRd() + ", " + getRs1() + ", " + rs2;
    }

    @Override
    public void accept(ASMVisitor visitor) {
        visitor.visit(this);
    }
}
