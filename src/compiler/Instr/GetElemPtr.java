package compiler.Instr;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.IR.IRObject;
import compiler.IR.IRVisitor;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.ArrayType;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.IntegerType;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Optim.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class GetElemPtr extends IRInstruction {
    private Operand pointer;
    private ArrayList<Operand> index;
    private Register result;

    public GetElemPtr(BasicBlock basicBlock, Operand pointer, ArrayList<Operand> index, Register result) {
        super(basicBlock);
        this.pointer = pointer;
        this.index = index;
        this.result = result;

        assert pointer.getType() instanceof PointerType
                || (pointer instanceof GlobalVariable && pointer.getType() instanceof ArrayType);
        if (pointer.getType() instanceof PointerType)
            assert result.getType() instanceof PointerType;
        else
            assert result.getType().equals(new PointerType(new IntegerType(IntegerType.BitWidth.int8)));
    }

    @Override
    public void successfullyAdd() {
        result.setDef(this);

        pointer.addUse(this);
        for (Operand operand : index)
            operand.addUse(this);
    }

    public Operand getPointer() {
        return pointer;
    }

    public ArrayList<Operand> getIndex() {
        return index;
    }

    @Override
    public Register getResult() {
        return result;
    }

    @Override
    public void replaceUse(IRObject oldUse, IRObject newUse) {
        if (pointer == oldUse) {
            pointer.removeUse(this);
            pointer = (Operand) newUse;
            pointer.addUse(this);
        }
        for (int i = 0; i < index.size(); i++) {
            if (index.get(i) == oldUse) {
                index.get(i).removeUse(this);
                index.set(i, (Operand) newUse);
                index.get(i).addUse(this);
            }
        }
    }

    @Override
    public void removeFromBlock() {
        pointer.removeUse(this);
        for (Operand operand : index)
            operand.removeUse(this);
        super.removeFromBlock();
    }

    @Override
    public void markUseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue) {
        pointer.markBaseAsLive(live, queue);
        for (Operand operand : index)
            operand.markBaseAsLive(live, queue);
    }

    @Override
    public void clonedUseReplace(Map<BasicBlock, BasicBlock> blockMap, Map<Operand, Operand> operandMap) {
        if (pointer instanceof Parameter || pointer instanceof Register) {
            assert operandMap.containsKey(pointer);
            pointer = operandMap.get(pointer);
        }
        pointer.addUse(this);
        for (int i = 0; i < index.size(); i++) {
            Operand aIndex = index.get(i);
            if (aIndex instanceof Parameter || aIndex instanceof Register) {
                assert operandMap.containsKey(aIndex);
                index.set(i, operandMap.get(aIndex));
            }
            index.get(i).addUse(this);
        }
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
        String instructionName = "getelementptr";
        ArrayList<String> operands = new ArrayList<>();
        operands.add(pointer.toString());
        for (Operand operand : index)
            operands.add(operand.toString());
        return new CommonSubexpressionElimination.Expression(instructionName, operands);
    }


    @Override
    public void addConstraintsForAndersen(Map<Operand, Andersen.Node> nodeMap, Set<Andersen.Node> nodes) {
        assert result.getType() instanceof PointerType;
        assert pointer instanceof GlobalVariable || pointer.getType() instanceof PointerType;
        if (!(pointer instanceof ConstNull)) {
            assert nodeMap.containsKey(result);
            assert nodeMap.containsKey(pointer);
            nodeMap.get(pointer).getInclusiveEdge().add(nodeMap.get(result));
        }
    }

    @Override
    public boolean updateResultScope(Map<Operand, SideEffectChecker.Scope> scopeMap,
                                     Map<Function, SideEffectChecker.Scope> returnValueScope) {
        if (pointer instanceof ConstNull) {
            if (scopeMap.get(result) != SideEffectChecker.Scope.local) {
                scopeMap.replace(result, SideEffectChecker.Scope.local);
                return true;
            } else
                return false;
        }
        SideEffectChecker.Scope scope = scopeMap.get(pointer);
        assert scope != SideEffectChecker.Scope.undefined;
        if (scopeMap.get(result) != scope) {
            scopeMap.replace(result, scope);
            return true;
        } else
            return false;
    }

    /*@Override
    public boolean checkLoopInvariant(LoopAnalysis.LoopNode loop, LICM licm) {
        if (licm.isLoopInvariant(result, loop))
            return false;

        if (pointer instanceof GlobalVariable) {
            assert pointer.getType() instanceof ArrayType;
            licm.markLoopInvariant(result);
            return true;
        }

        if (!licm.isLoopInvariant(pointer, loop))
            return false;
        for (Operand index : this.index) {
            if (!licm.isLoopInvariant(index, loop))
                return false;
        }
        licm.markLoopInvariant(result);
        return true;
    }*/

    @Override
    public boolean canBeHoisted(LoopAnalysis.LoopNode loop) {
        if (!loop.defOutOfLoop(pointer))
            return false;
        for (Operand index : this.index) {
            if (!loop.defOutOfLoop(index))
                return false;
        }
        return true;
    }


    @Override
    public boolean combineInst(Queue<IRInstruction> queue, Set<IRInstruction> inQueue) {
        for (Operand operand : index) {
            if (!(operand instanceof ConstInt && ((ConstInt) operand).getValue() == 0))
                return false;
        }

        Operand replace = null;
        if (index.size() == 1) {
            // array gep
            replace = pointer;
        } else if (!(pointer.getType() instanceof ArrayType)){
            // class member gep
            Register newResult = new Register(result.getType(), result.getNameWithoutDot());
            this.getBasicBlock().getFunction().getSymbolTable().put(newResult.getName(), newResult);

            BitCastTo bitCastToInst = new BitCastTo(getBasicBlock(), pointer, result.getType(), newResult);
            getBasicBlock().addInstructionNext(this, bitCastToInst);
            queue.offer(bitCastToInst);
            inQueue.add(bitCastToInst);

            replace = newResult;
        }
        if (replace != null) {
            for (IRInstruction instruction : result.getUse().keySet()) {
                if (!inQueue.contains(instruction)) {
                    queue.offer(instruction);
                    inQueue.add(instruction);
                }
            }
            result.replaceUse(replace);
            return true;
        } else
            return false;
    }

    @Override
    public String toString() {
        IRType baseType;
        IRType pointerType;
        if (pointer.getType() instanceof PointerType) {
            baseType = ((PointerType) pointer.getType()).getBaseType();
            pointerType = pointer.getType();
        } else {
            baseType = pointer.getType();
            pointerType = new PointerType(baseType);
        }
        StringBuilder string = new StringBuilder();
        string.append(result.toString()).append(" = ");
        string.append("getelementptr ").append(baseType.toString()).append(", ");
        string.append(pointerType).append(" ").append(pointer.toString());
        for (Operand aIndex : index)
            string.append(", ").append(aIndex.getType().toString()).append(" ").append(aIndex.toString());
        return string.toString();
    }

    @Override
    public Object clone() {
        GetElemPtr getElementPtrInst = (GetElemPtr) super.clone();
        getElementPtrInst.pointer = this.pointer;
        getElementPtrInst.index = new ArrayList<>(index);
        getElementPtrInst.result = (Register) this.result.clone();

        getElementPtrInst.result.setDef(getElementPtrInst);
        return getElementPtrInst;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
