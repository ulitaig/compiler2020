package compiler.Instr;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.IR.IRObject;
import compiler.IR.IRVisitor;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Register;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Instr.TypeSystem.IRType;
import compiler.Optim.*;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Allocate extends IRInstruction {
    private Register result;
    private IRType type;

    public Allocate(BasicBlock basicBlock, Register result, IRType type) {
        super(basicBlock);
        this.result = result;
        this.type = type;

        assert (new PointerType(type)).equals(result.getType());
    }

    @Override
    public void successfullyAdd() {
        result.setDef(this);
    }

    @Override
    public Register getResult() {
        return result;
    }

    public IRType getType() {
        return type;
    }

    @Override
    public void replaceUse(IRObject oldUse, IRObject newUse) {
    }

    @Override
    public void markUseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue) {
    }


    @Override
    public void clonedUseReplace(Map<BasicBlock, BasicBlock> blockMap, Map<Operand, Operand> operandMap) {
    }

    @Override
    public boolean replaceResultWithConstant(SCCP sccp) {
        SCCP.Status status = sccp.getStatus(result);
        if (status.getOperandStatus() == SCCP.Status.OperandStatus.constant) {
            result.replaceUse(status.getOperand());
            this.removeFromBlock();
            return true;
        } else
            return false;
    }

    @Override
    public CommonSubexpressionElimination.Expression convertToExpression() {
        throw new RuntimeException("Convert alloca to expression.");
    }


    @Override
    public void addConstraintsForAndersen(Map<Operand, Andersen.Node> nodeMap, Set<Andersen.Node> nodes) {
        assert nodeMap.containsKey(result);
        Andersen.Node pointer = nodeMap.get(result);
        Andersen.Node pointTo = new Andersen.Node(pointer.getName() + ".alloca");
        pointer.getPointsTo().add(pointTo);
        nodes.add(pointTo);
    }

    @Override
    public boolean updateResultScope(Map<Operand, SideEffectChecker.Scope> scopeMap,
                                     Map<Function, SideEffectChecker.Scope> returnValueScope) {
        if (scopeMap.get(result) != SideEffectChecker.Scope.local) {
            scopeMap.replace(result, SideEffectChecker.Scope.local);
            return true;
        } else
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
        return result.toString() + " = alloca " + type.toString();
    }

    @Override
    public Object clone() {
        Allocate allocateInst = (Allocate) super.clone();
        allocateInst.result = (Register) this.result.clone();
        allocateInst.type = this.type;

        allocateInst.result.setDef(allocateInst);
        return allocateInst;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
