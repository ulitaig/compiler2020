package compiler.Codegen.Instruction;

import compiler.Codegen.ASMVisitor;
import compiler.Codegen.BasicBlock;
import compiler.Codegen.Operand.Address.Address;
import compiler.Codegen.Operand.Register.VirtualRegister;

import java.util.Set;

public class LoadInst extends ASMInstruction {
    public enum ByteSize {
        lb, lw
    }

    private VirtualRegister rd;
    private ByteSize byteSize;
    private Address addr;

    public LoadInst(BasicBlock basicBlock, VirtualRegister rd, ByteSize byteSize, Address addr) {
        super(basicBlock);
        this.rd = rd;
        this.byteSize = byteSize;
        this.addr = addr;

        this.rd.addDef(this);
        this.addDef(this.rd);
        this.addr.addBaseUse(this);
    }

    public VirtualRegister getRd() {
        return rd;
    }

    public Address getAddr() {
        return addr;
    }

    public void removeFromBlock() {
        this.rd.removeDef(this);
        this.removeDef(this.rd);
        this.addr.removeBaseUse(this);

        rd = null;
        addr = null;
        if (getPrevInst() == null)
            getBasicBlock().setInstHead(getNextInst());
        else
            getPrevInst().setNextInst(getNextInst());
        if (getNextInst() == null)
            getBasicBlock().setInstTail(getPrevInst());
        else
            getNextInst().setPrevInst(getPrevInst());
    }

    @Override
    public void addToUEVarAndVarKill(Set<VirtualRegister> UEVar, Set<VirtualRegister> varKill) {
        addr.addToUEVarAndVarKill(UEVar, varKill);
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
        addr.replaceUse(oldVR, newVR);
        super.replaceUse(oldVR, newVR);
    }

    @Override
    public String emitCode() {
        return "\t" + byteSize.name() + "\t" + rd.emitCode() + ", " + addr.emitCode();
    }

    @Override
    public String toString() {
        return byteSize + " " + rd + ", " + addr;
    }

    @Override
    public void accept(ASMVisitor visitor) {
        visitor.visit(this);
    }
}
