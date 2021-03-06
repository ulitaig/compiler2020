package compiler.Codegen.Instruction;

import compiler.Codegen.ASMVisitor;
import compiler.Codegen.BasicBlock;
import compiler.Codegen.Operand.Immediate.Immediate;
import compiler.Codegen.Operand.Register.VirtualRegister;

import java.util.Set;

public class LoadImmediate extends ASMInstruction {
    private VirtualRegister rd;
    private Immediate immediate;

    public LoadImmediate(BasicBlock basicBlock, VirtualRegister rd, Immediate immediate) {
        super(basicBlock);
        this.rd = rd;
        this.immediate = immediate;

        this.rd.addDef(this);
        this.addDef(this.rd);
    }

    @Override
    public void replaceDef(VirtualRegister oldVR, VirtualRegister newVR) {
        assert rd == oldVR;
        rd = newVR;
        super.replaceDef(oldVR, newVR);
    }

    @Override
    public void addToUEVarAndVarKill(Set<VirtualRegister> UEVar, Set<VirtualRegister> varKill) {
        varKill.add(rd);
    }

    @Override
    public String emitCode() {
        return "\tli\t" + rd.emitCode() + ", " + immediate.emitCode();
    }

    @Override
    public String toString() {
        return "li " + rd + ", " + immediate;
    }

    @Override
    public void accept(ASMVisitor visitor) {
        visitor.visit(this);
    }
}
