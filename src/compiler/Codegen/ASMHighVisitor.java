package compiler.Codegen;

import compiler.Codegen.Instruction.*;
import compiler.Codegen.Instruction.BinaryInst.ITypeBinary;
import compiler.Codegen.Instruction.BinaryInst.RTypeBinary;
import compiler.Codegen.Instruction.Branch.BinaryBranch;
import compiler.Codegen.Instruction.Branch.UnaryBranch;
import compiler.Codegen.Operand.GlobalVariable;

public class ASMHighVisitor implements ASMVisitor {
    @Override
    public void visit(Module module){}
    public void visit(Function function){}
    public void visit(BasicBlock block){}

    public void visit(GlobalVariable gv){}

    public void visit(MoveInst inst){}
    public void visit(UnaryInst inst){}
    public void visit(ITypeBinary inst){}
    public void visit(RTypeBinary inst){}

    public void visit(LoadAddressInst inst){}
    public void visit(LoadImmediate inst){}
    public void visit(LoadUpperImmediate inst){}

    public void visit(LoadInst inst){}
    public void visit(StoreInst inst){}

    public void visit(JumpInst inst){}
    public void visit(BinaryBranch inst){}
    public void visit(UnaryBranch inst){}
    public void visit(CallInst inst){}
    public void visit(ReturnInst inst){}
}
