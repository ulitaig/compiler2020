package compiler.Instr;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.IR.IRObject;
import compiler.IR.IRVisitor;
import compiler.Instr.Operand.ConstNull;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Parameter;
import compiler.Instr.Operand.Register;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Instr.TypeSystem.VoidType;
import compiler.Optim.*;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Return extends IRInstruction {
    private IRType type; // void or not
    private Operand returnValue;

    public Return(BasicBlock basicBlock, IRType type, Operand returnValue) {
        super(basicBlock);
        this.type = type;
        this.returnValue = returnValue;

        if (!(type instanceof VoidType))
            assert type.equals(returnValue.getType())
                    || (returnValue instanceof ConstNull && type instanceof PointerType);
        else
            assert returnValue == null;
    }

    @Override
    public void successfullyAdd() {
        if (returnValue != null)
            returnValue.addUse(this);
    }

    public IRType getType() {
        return type;
    }

    public Operand getReturnValue() {
        return returnValue;
    }

    @Override
    public Register getResult() {
        throw new RuntimeException("Get result of return instruction.");
    }

    @Override
    public void replaceUse(IRObject oldUse, IRObject newUse) {
        if (returnValue != null && returnValue == oldUse) {
            returnValue.removeUse(this);
            returnValue = (Operand) newUse;
            returnValue.addUse(this);
        }
    }

    @Override
    public void removeFromBlock() {
        if (returnValue != null)
            returnValue.removeUse(this);
        super.removeFromBlock();
    }

    @Override
    public void markUseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue) {
        if (returnValue != null)
            returnValue.markBaseAsLive(live, queue);
    }

    @Override
    public void clonedUseReplace(Map<BasicBlock, BasicBlock> blockMap, Map<Operand, Operand> operandMap) {
        if (!(type instanceof VoidType)) {
            if (returnValue instanceof Parameter || returnValue instanceof Register) {
                assert operandMap.containsKey(returnValue);
                returnValue = operandMap.get(returnValue);
            }
            returnValue.addUse(this);
        }
    }


    @Override
    public boolean replaceResultWithConstant(SCCP sccp) {
        // Do nothing.
        return false;
    }

    @Override
    public CommonSubexpressionElimination.Expression convertToExpression() {
        throw new RuntimeException("Convert return instruction to expression");
    }

    @Override
    public void addConstraintsForAndersen(Map<Operand, Andersen.Node> nodeMap, Set<Andersen.Node> nodes) {
        // Do nothing.
    }

    @Override
    public boolean updateResultScope(Map<Operand, SideEffectChecker.Scope> scopeMap,
                                     Map<Function, SideEffectChecker.Scope> returnValueScope) {
        return false;
    }

    /*@Override
    public boolean checkLoopInvariant(LoopAnalysis.LoopNode loop, LICM licm) {
        return false;
    }*/

    @Override
    public boolean canBeHoisted(LoopAnalysis.LoopNode loop) {
        return false;
    }

    @Override
    public boolean combineInst(Queue<IRInstruction> queue, Set<IRInstruction> inQueue) {
        return false;
    }

    @Override
    public String toString() {
        if (!(type instanceof VoidType))
            return "ret " + type.toString() + " " + returnValue.toString();
        else
            return "ret void";
    }

    @Override
    public Object clone() {
        Return returnInst = (Return) super.clone();
        returnInst.type = this.type;
        returnInst.returnValue = this.returnValue;

        return returnInst;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
