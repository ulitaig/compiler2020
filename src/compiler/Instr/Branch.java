package compiler.Instr;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.IR.IRObject;
import compiler.IR.IRVisitor;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Parameter;
import compiler.Instr.Operand.Register;
import compiler.Instr.TypeSystem.IntegerType;
import compiler.Optim.*;

import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Branch extends IRInstruction {
    private Operand cond;
    private BasicBlock thenBlock;
    private BasicBlock elseBlock;

    public Branch(BasicBlock basicBlock, Operand cond, BasicBlock thenBlock, BasicBlock elseBlock) {
        super(basicBlock);
        this.cond = cond;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;

        assert cond == null || cond.getType().equals(new IntegerType(IntegerType.BitWidth.int1));
    }

    @Override
    public void successfullyAdd() {
        this.getBasicBlock().getSuccessors().add(thenBlock);
        thenBlock.getPredecessors().add(this.getBasicBlock());

        if (cond != null) {
            this.getBasicBlock().getSuccessors().add(elseBlock);
            elseBlock.getPredecessors().add(this.getBasicBlock());

            cond.addUse(this);
            elseBlock.addUse(this);
        }
        thenBlock.addUse(this);
    }

    public boolean isConditional() {
        return cond != null;
    }

    public Operand getCond() {
        return cond;
    }

    public BasicBlock getThenBlock() {
        return thenBlock;
    }

    public BasicBlock getElseBlock() {
        return elseBlock;
    }

    @Override
    public Register getResult() {
        throw new RuntimeException("Get result of branch instruction.");
    }

    public void setUnconditionalBranch(BasicBlock thenBlock) {
        if (cond != null) {
            this.cond.removeUse(this);
            this.elseBlock.removeUse(this);
            this.elseBlock.getPredecessors().remove(getBasicBlock());
            getBasicBlock().getSuccessors().remove(this.elseBlock);
        }
        this.thenBlock.removeUse(this);
        this.thenBlock.getPredecessors().remove(getBasicBlock());
        getBasicBlock().getSuccessors().remove(this.thenBlock);

        this.cond = null;
        this.thenBlock = thenBlock;
        this.elseBlock = null;
        this.thenBlock.addUse(this);
        this.thenBlock.getPredecessors().add(getBasicBlock());
        getBasicBlock().getSuccessors().add(this.thenBlock);
    }

    private void swapBlocks() {
        assert cond != null;
        BasicBlock tmp = thenBlock;
        thenBlock = elseBlock;
        elseBlock = tmp;
    }

    private void replaceCond(Operand newCond) {
        assert cond != null;
        cond.removeUse(this);
        cond = newCond;
        cond.addUse(this);
    }

    @Override
    public void replaceUse(IRObject oldUse, IRObject newUse) {
        if (cond == oldUse) {
            cond.removeUse(this);
            cond = (Operand) newUse;
            cond.addUse(this);
        } else {
            if (thenBlock == oldUse) {
                thenBlock.removeUse(this);
                thenBlock = (BasicBlock) newUse;
                thenBlock.addUse(this);
            }
            if (elseBlock == oldUse) {
                elseBlock.removeUse(this);
                elseBlock = (BasicBlock) newUse;
                elseBlock.addUse(this);
            }
        }
    }

    @Override
    public void removeFromBlock() {
        if (cond != null) {
            cond.removeUse(this);
            elseBlock.removeUse(this);
        }
        thenBlock.removeUse(this);
        super.removeFromBlock();
    }


    @Override
    public void markUseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue) {
        if (cond != null)
            cond.markBaseAsLive(live, queue);
    }

    @Override
    public void clonedUseReplace(Map<BasicBlock, BasicBlock> blockMap, Map<Operand, Operand> operandMap) {
        if (cond != null) {
            if (cond instanceof Parameter || cond instanceof Register) {
                assert operandMap.containsKey(cond);
                cond = operandMap.get(cond);
            }
            cond.addUse(this);

            assert blockMap.containsKey(elseBlock);
            elseBlock = blockMap.get(elseBlock);
            elseBlock.addUse(this);
        }
        assert blockMap.containsKey(thenBlock);
        thenBlock = blockMap.get(thenBlock);
        thenBlock.addUse(this);
    }


    @Override
    public boolean replaceResultWithConstant(SCCP sccp) {
        // Do nothing.
        return false;
    }

    @Override
    public CommonSubexpressionElimination.Expression convertToExpression() {
        throw new RuntimeException("Convert branch instruction to expression.");
    }


    @Override
    public void addConstraintsForAndersen(Map<Operand, Andersen.Node> nodeMap, Set<Andersen.Node> nodes) {
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
        if (cond == null || !cond.registerDefIsBinaryOpInst()
                || !(((BinaryOp) ((Register) cond).getDef()).isLogicalNot()))
            return false;
        Operand newCond = ((BinaryOp) ((Register) cond).getDef()).getLogicalNotOperand();
        this.replaceCond(newCond);
        this.swapBlocks();
        return true;
    }

    @Override
    public String toString() {
        if (cond != null)
            return "br i1 " + cond.toString() + ", label " + thenBlock.toString() + ", label " + elseBlock.toString();
        else
            return "br label " + thenBlock.toString();
    }

    @Override
    public Object clone() {
        Branch branchInst = (Branch) super.clone();
        branchInst.cond = this.cond;
        branchInst.thenBlock = this.thenBlock;
        branchInst.elseBlock = this.elseBlock;

        return branchInst;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
