package compiler.Codegen.Instruction;

import compiler.Codegen.ASMVisitor;
import compiler.Codegen.BasicBlock;
import compiler.Codegen.Operand.Register.VirtualRegister;

import java.util.Set;

public class UnaryInst extends ASMInstruction {
    public enum OpName {
        seqz, snez, sltz, sgtz
    }

    private OpName op;
    private VirtualRegister rs;
    private VirtualRegister rd;

    public UnaryInst(BasicBlock basicBlock, OpName op, VirtualRegister rs, VirtualRegister rd) {
        super(basicBlock);
        this.op = op;
        this.rs = rs;
        this.rd = rd;

        this.rs.addUse(this);
        this.rd.addDef(this);
        this.addUse(this.rs);
        this.addDef(this.rd);
    }

    @Override
    public void addToUEVarAndVarKill(Set<VirtualRegister> UEVar, Set<VirtualRegister> varKill) {
        if (!varKill.contains(rs))
            UEVar.add(rs);
        varKill.add(rd);
    }

    @Override
    public void replaceDef(VirtualRegister oldVR, VirtualRegister newVR) {
        assert rd == oldVR;
        rd = newVR;
        super.replaceDef(oldVR, newVR);
    }

    @Override
    public void replaceUse(VirtualRegister oldVR, VirtualRegister newVR) {
        assert rs == oldVR;
        rs = newVR;
        super.replaceUse(oldVR, newVR);
    }

    @Override
    public String emitCode() {
        return "\t" + op.name() + "\t" + rd.emitCode() + ", " + rs.emitCode();
    }

    @Override
    public String toString() {
        return op + " " + rd + ", " + rs;
    }

    @Override
    public void accept(ASMVisitor visitor) {
        visitor.visit(this);
    }
}
