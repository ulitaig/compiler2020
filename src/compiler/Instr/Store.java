package compiler.Instr;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.IR.IRObject;
import compiler.IR.IRVisitor;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Optim.*;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Store extends IRInstruction {
    private Operand value;
    private Operand pointer;

    public Store(BasicBlock basicBlock, Operand value, Operand pointer) {
        super(basicBlock);
        this.value = value;
        this.pointer = pointer;

        if (pointer instanceof GlobalVariable)
            assert pointer.getType().equals(value.getType());
        else {
            assert pointer.getType() instanceof PointerType;
            assert ((PointerType) pointer.getType()).getBaseType().equals(value.getType())
                    || value instanceof ConstNull;
        }
    }

    @Override
    public void successfullyAdd() {
        value.addUse(this);
        pointer.addUse(this);
    }

    public Operand getValue() {
        return value;
    }

    public Operand getPointer() {
        return pointer;
    }

    @Override
    public Register getResult() {
        throw new RuntimeException("Get result of store instruction");
    }

    @Override
    public void replaceUse(IRObject oldUse, IRObject newUse) {
        if (value == oldUse) {
            value.removeUse(this);
            value = (Operand) newUse;
            value.addUse(this);
        }
        if (pointer == oldUse) {
            pointer.removeUse(this);
            pointer = (Operand) newUse;
            pointer.addUse(this);
        }
    }

    @Override
    public void removeFromBlock() {
        value.removeUse(this);
        pointer.removeUse(this);
        super.removeFromBlock();
    }

    @Override
    public void markUseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue) {
        value.markBaseAsLive(live, queue);
        pointer.markBaseAsLive(live, queue);
    }

    @Override
    public void clonedUseReplace(Map<BasicBlock, BasicBlock> blockMap, Map<Operand, Operand> operandMap) {
        if (value instanceof Parameter || value instanceof Register) {
            assert operandMap.containsKey(value);
            value = operandMap.get(value);
        }
        if (pointer instanceof Parameter || pointer instanceof Register) {
            assert operandMap.containsKey(pointer);
            pointer = operandMap.get(pointer);
        }
        value.addUse(this);
        pointer.addUse(this);
    }

    @Override
    public boolean replaceResultWithConstant(SCCP sccp) {
        // Do nothing.
        return false;
    }

    @Override
    public CommonSubexpressionElimination.Expression convertToExpression() {
        throw new RuntimeException("Convert store instruction to expression.");
    }

    @Override
    public void addConstraintsForAndersen(Map<Operand, Andersen.Node> nodeMap, Set<Andersen.Node> nodes) {
        // *pointer = value -> value <= *pointer
        if (!(value.getType() instanceof PointerType))
            return;
        if (!(pointer instanceof ConstNull) && !(value instanceof ConstNull)) {
            assert nodeMap.containsKey(pointer);
            assert nodeMap.containsKey(value);
            nodeMap.get(pointer).getDereferenceRhs().add(nodeMap.get(value));
        }
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
        if (pointer instanceof GlobalVariable)
                return "store " + pointer.getType().toString() + " " + value.toString() +
                        ", " + (new PointerType(pointer.getType())).toString() + " " + pointer.toString();
        else
            return "store " + ((PointerType) pointer.getType()).getBaseType().toString() + " " + value.toString() +
                    ", " + pointer.getType().toString() + " " + pointer.toString();
    }

    @Override
    public Object clone() {
        Store storeInst = (Store) super.clone();
        storeInst.value = this.value;
        storeInst.pointer = this.pointer;

        return storeInst;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
