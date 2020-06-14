package compiler.Instr;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.IR.IRObject;
import compiler.IR.IRVisitor;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Register;
import compiler.Optim.*;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

abstract public class IRInstruction implements Cloneable {
    private BasicBlock basicBlock;

    private IRInstruction instPrev;
    private IRInstruction instNext;

    public IRInstruction(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    public void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    abstract public void successfullyAdd();

    public void setInstPrev(IRInstruction instPrev) {
        this.instPrev = instPrev;
    }

    public void setInstNext(IRInstruction instNext) {
        this.instNext = instNext;
    }

    public IRInstruction getInstPrev() {
        return instPrev;
    }

    public IRInstruction getInstNext() {
        return instNext;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public boolean hasResult() {
        if (this instanceof Call)
            return !((Call) this).isVoidCall();
        return this instanceof Allocate
                || this instanceof BinaryOp
                || this instanceof BitCastTo
                || this instanceof GetElemPtr
                || this instanceof Icmp
                || this instanceof Load
                || this instanceof Phi
                || this instanceof Move;
    }

    abstract public boolean replaceResultWithConstant(SCCP sccp);

    abstract public Register getResult();

    public boolean isNotTerminalInst() {
        return !(this instanceof Branch) && !(this instanceof Return);
    }

    abstract public void replaceUse(IRObject oldUse, IRObject newUse);

    public void removeFromBlock() {
        if (instPrev == null)
            basicBlock.setInstHead(instNext);
        else
            instPrev.setInstNext(instNext);

        if (instNext == null)
            basicBlock.setInstTail(instPrev);
        else
            instNext.setInstPrev(instPrev);
    }

    public void removeFromBlockWithoutRemoveUse() {
        if (instPrev == null)
            basicBlock.setInstHead(instNext);
        else
            instPrev.setInstNext(instNext);

        if (instNext == null)
            basicBlock.setInstTail(instPrev);
        else
            instNext.setInstPrev(instPrev);
    }

    abstract public void markUseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue);

    public boolean canConvertToExpression() {
        assert !(this instanceof Allocate);
        return this instanceof BinaryOp
                || this instanceof BitCastTo
                || this instanceof GetElemPtr
                || this instanceof Icmp
                || this instanceof Load;
    }


    abstract public void clonedUseReplace(Map<BasicBlock, BasicBlock> blockMap, Map<Operand, Operand> operandMap);

    abstract public boolean combineInst(Queue<IRInstruction> queue, Set<IRInstruction> inQueue);



    abstract public CommonSubexpressionElimination.Expression convertToExpression();

    abstract public void addConstraintsForAndersen(Map<Operand, Andersen.Node> nodeMap, Set<Andersen.Node> nodes);

    abstract public boolean updateResultScope(Map<Operand, SideEffectChecker.Scope> scopeMap,
                                              Map<Function, SideEffectChecker.Scope> returnValueScope);

    //abstract public boolean checkLoopInvariant(LoopAnalysis.LoopNode loop, LICM licm);

    abstract public boolean canBeHoisted(LoopAnalysis.LoopNode loop);


    @Override
    abstract public String toString();

    @Override
    public Object clone() {
        IRInstruction instruction;
        try {
            instruction = (IRInstruction) super.clone();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

        instruction.basicBlock = this.basicBlock;
        instruction.instPrev = this.instPrev;
        instruction.instNext = this.instNext;
        return instruction;
    }

    abstract public void accept(IRVisitor visitor);
}
