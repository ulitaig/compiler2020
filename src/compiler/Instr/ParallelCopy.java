package compiler.Instr;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.IR.IRObject;
import compiler.IR.IRVisitor;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Register;
import compiler.Optim.*;

import java.util.*;

public class ParallelCopy extends IRInstruction {
    private Set<Move> moves;

    public ParallelCopy(BasicBlock basicBlock) {
        super(basicBlock);
        moves = new HashSet<>();
    }

    public void appendMove(Move moveInst) {
        if (moveInst.getResult().equals(moveInst.getSource()))
            return;
        moves.add(moveInst);
    }

    public void removeMove(Move moveInst) {
        assert moves.contains(moveInst);
        moves.remove(moveInst);
    }

    public Move getMove() {
        if (moves.isEmpty())
            return null;
        return moves.iterator().next();
    }

    public Move findValidMove() {
        for (Move move1 : moves) {
            boolean flag = true;
            for (Move move2 : moves) {
                if (move2.getSource().equals(move1.getResult())) {
                    flag = false;
                    break;
                }
            }
            if (flag)
                return move1;
        }
        return null;
    }

    public Set<Move> getMoves() {
        return moves;
    }

    @Override
    public void successfullyAdd() {
        // Do nothing.
    }

    @Override
    public Register getResult() {
        return null;
    }

    @Override
    public void replaceUse(IRObject oldUse, IRObject newUse) {
        // Do nothing.
    }

    @Override
    public void markUseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue) {
        // Do nothing.
    }

    @Override
    public void clonedUseReplace(Map<BasicBlock, BasicBlock> blockMap, Map<Operand, Operand> operandMap) {
        // Do nothing.
    }

    @Override
    public boolean replaceResultWithConstant(SCCP sccp) {
        return false;
    }

    @Override
    public CommonSubexpressionElimination.Expression convertToExpression() {
        return null;
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
        StringBuilder string = new StringBuilder("parallelCopy ");
        Iterator<Move> it = moves.iterator();
        while (it.hasNext()) {
            Move move = it.next();
            string.append("[ ").append(move.getResult()).append(", ").append(move.getSource()).append(" ]");
            if (it.hasNext())
                string.append(", ");
        }
        return string.toString();
    }

    @Override
    public void accept(IRVisitor visitor) {
        // Do nothing.
    }
}
