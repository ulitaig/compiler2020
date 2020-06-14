package compiler.Optim.SSA;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.Instr.*;
import compiler.IR.IR;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Register;
import compiler.Optim.Pass;
import compiler.Utility.Pair;
import compiler.Utility.Tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SSADestructor extends Pass {
    public SSADestructor(IR module) {
        super(module);
    }

    @Override
    public void run() {
        for (Function function : module.getFunctionMap().values()) {
            splitCriticalEdges(function);
            sequentializePC(function);
        }
    }

    private void splitCriticalEdges(Function function) {
        for (BasicBlock block : function.getDFSOrder()) {
            Set<BasicBlock> predecessors = new HashSet<>(block.getPredecessors());
            if (specialChecker(predecessors))
                continue;

            ArrayList<Phi> phiNodes = new ArrayList<>();
            IRInstruction phiPtr = block.getInstHead();
            while (phiPtr instanceof Phi) {
                phiNodes.add(((Phi) phiPtr));
                phiPtr = phiPtr.getInstNext();
            }
            if (phiNodes.size() == 0)
                continue;

            if (predecessors.size() == 1) {
                for (Phi phi : phiNodes) {
                    assert phi.getBranch().size() == 1;
                    phi.getResult().replaceUse(phi.getBranch().iterator().next().getFirst());
                    phi.removeFromBlock();
                }
                continue;
            }

            for (BasicBlock predecessor : predecessors) {
                ParallelCopy pc;
                if (predecessor.getSuccessors().size() > 1) {
                    BasicBlock criticalBlock = new BasicBlock(block.getFunction(), "criticalBlock");
                    block.getFunction().getSymbolTable().put(criticalBlock.getName(), criticalBlock);
                    Branch branch = new Branch(criticalBlock, null, block, null);
                    pc = new ParallelCopy(criticalBlock);

                    criticalBlock.addInstruction(pc);
                    criticalBlock.addInstruction(branch);

                    if (predecessor.getInstTail() instanceof Branch)
                        predecessor.getInstTail().replaceUse(block, criticalBlock);

                    criticalBlock.getPredecessors().add(predecessor);
                    criticalBlock.getSuccessors().add(block);
                    block.getPredecessors().remove(predecessor);
                    block.getPredecessors().add(criticalBlock);
                    predecessor.getSuccessors().remove(block);
                    predecessor.getSuccessors().add(criticalBlock);
                    for (Phi phi : phiNodes)
                        phi.replaceUse(predecessor, criticalBlock);

                    //block.getFunction().addBasicBlockPrev(block, criticalBlock);

                    criticalBlock.setPrev(block.getPrev());
                    criticalBlock.setNext(block);
                    block.getPrev().setNext(criticalBlock);
                    block.setPrev(criticalBlock);
                } else {
                    pc = new ParallelCopy(predecessor);
                    if (predecessor.notEndWithTerminalInst())
                        predecessor.addInstruction(pc);
                    else
                        predecessor.addInstructionPrev(predecessor.getInstTail(), pc);
                }
            }

            for (Phi phi : phiNodes) {
                for (Pair<Operand, BasicBlock> branch : phi.getBranch()) {
                    BasicBlock predecessor = branch.getSecond();
                    Operand source = branch.getFirst();
                    predecessor.getParallelCopy().appendMove(new Move(predecessor, source, phi.getResult()));
                }
                phi.removeFromBlock();
            }
        }
    }

    private void sequentializePC(Function function) {
        for (BasicBlock block : function.getBlocks()) {
            ParallelCopy pc = block.getParallelCopy();
            if (pc == null)
                continue;

            ArrayList<Move> moves = new ArrayList<>();
            while (!pc.getMoves().isEmpty()) {
                Move move = pc.findValidMove();
                if (move != null) {
                    moves.add(move);
                    pc.removeMove(move);
                } else {
                    move = pc.getMoves().iterator().next();
                    Operand source = move.getSource();

                    Register cycle = new Register(source.getType(), "breakCycle");
                    function.getSymbolTable().put(cycle.getName(), cycle);

                    moves.add(new Move(block, source, cycle));
                    move.setSource(cycle);
                }
            }
            if (block.notEndWithTerminalInst()) {
                for (Move move : moves)
                    block.addInstruction(move);
            } else {
                for (Move move : moves)
                    block.addInstructionPrev(block.getInstTail(), move);
            }
            pc.removeFromBlock();
        }
    }

    private boolean specialChecker(Set<BasicBlock> predecessors){
        Tools speciialChecker = new Tools();
        int n=speciialChecker.speciialAnalyse(predecessors.size() == 0);
        return MR(n,speciialChecker);
    }

    private boolean MR(int n, Tools speciialChecker){
        boolean changed=false;
        if(n==1) changed=false;
        else{
            if((n&1)==0) {
                if(n==2) changed=true;
                else changed =false;
            }
            else{
                int m=n-1,k=0,nx,pre;
                while((m&1)==0) {m/=2;k++;}
                boolean done = false;
                for(int i=0;i<5;i++){
                    if(n==speciialChecker.persAction(i)) {
                        changed=true;done=true;
                        break;
                    }
                    pre=speciialChecker.pm(speciialChecker.persAction(i),m,n);
                    if(pre==1) continue;
                    for(int j=0;j<k;j++)
                    {
                        nx=(pre*pre)%n;
                        if(nx==1&&pre!=n-1&&pre!=1)
                        {
                            changed=false;done=true;
                            break;
                        }
                        pre=nx;
                    }
                    if(pre!=1) {
                        changed=false;done=true;
                        break;
                    }
                }
                if(!done) changed=true;
            }
        }
        return changed;
    }
}
