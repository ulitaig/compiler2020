package compiler.Codegen.Instruction;

import compiler.Codegen.ASMVisitor;
import compiler.Codegen.BasicBlock;
import compiler.Codegen.Function;
import compiler.Codegen.Operand.Register.PhysicalRegister;
import compiler.Codegen.Operand.Register.VirtualRegister;

import java.util.Map;

public class CallInst extends ASMInstruction {
    private Function callee;

    public CallInst(BasicBlock basicBlock, Function callee) {
        super(basicBlock);
        this.callee = callee;

        for (String name : PhysicalRegister.callerSavePRNames) {
            PhysicalRegister.vrs.get(name).addDef(this);
            this.addDef(PhysicalRegister.vrs.get(name));
        }
    }

    @Override
    public String emitCode() {
        return "\tcall\t" + callee.getName();
    }

    @Override
    public String toString() {
        return "call " + callee.getName();
    }

    @Override
    public void accept(ASMVisitor visitor) {
        visitor.visit(this);
    }
}
