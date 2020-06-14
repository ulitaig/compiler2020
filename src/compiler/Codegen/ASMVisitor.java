package compiler.Codegen;

import compiler.Codegen.Instruction.*;
import compiler.Codegen.Instruction.BinaryInst.ITypeBinary;
import compiler.Codegen.Instruction.BinaryInst.RTypeBinary;
import compiler.Codegen.Instruction.Branch.BinaryBranch;
import compiler.Codegen.Instruction.Branch.UnaryBranch;
import compiler.Codegen.Operand.GlobalVariable;

public interface ASMVisitor {
    void visit(Module module);
    void visit(Function function);
    void visit(BasicBlock block);

    void visit(GlobalVariable gv);

    void visit(MoveInst inst);
    void visit(UnaryInst inst);
    void visit(ITypeBinary inst);
    void visit(RTypeBinary inst);

    void visit(LoadAddressInst inst);
    void visit(LoadImmediate inst);
    void visit(LoadUpperImmediate inst);

    void visit(LoadInst inst);
    void visit(StoreInst inst);

    void visit(JumpInst inst);
    void visit(BinaryBranch inst);
    void visit(UnaryBranch inst);
    void visit(CallInst inst);
    void visit(ReturnInst inst);
}
