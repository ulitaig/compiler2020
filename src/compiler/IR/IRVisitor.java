package compiler.IR;

import compiler.Instr.*;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.*;

public interface IRVisitor {
    // ------ IR ------
    void visit(IR module);

    // ------ Function ------
    void visit(Function function);

    // ------ BasicBlock ------
    void visit(BasicBlock block);

    // ------ Instruction ------
    void visit(Return inst);
    void visit(Branch inst);
    void visit(BinaryOp inst);
    void visit(Allocate inst);
    void visit(Load inst);
    void visit(Store inst);
    void visit(GetElemPtr inst);
    void visit(BitCastTo inst);
    void visit(Icmp inst);
    void visit(Phi inst);
    void visit(Call inst);

    void visit(Move inst);
}
