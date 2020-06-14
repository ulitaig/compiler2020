package compiler.Optim;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.Instr.Branch;
import compiler.Instr.IRInstruction;
import compiler.Instr.Phi;
import compiler.IR.IR;
import compiler.Instr.Operand.ConstBool;
import compiler.Utility.Tools;

import java.util.ArrayList;

public class CFGSimplifier extends Pass {
    public CFGSimplifier(IR module) {
        super(module);
    }

    @Override
    public void run() {
        for (Function function : module.getFunctionMap().values()) {
            if (function.isNotFunctional())
                return;
            else
                functionSimplify(function);
        }
    }

    private void functionSimplify(Function function) {
        while (true) {
            boolean loopChanged = false;
            loopChanged |= removeRedundantBranch(function);
            loopChanged |= removeUnreachableBlock(function);
            loopChanged |= removePhiWithSingleBranch(function);
            if (!loopChanged)
                break;
        }
    }

    private boolean checkEquealorConst(Branch branchInst,BasicBlock block){
        boolean changed = false;
        if (branchInst.getThenBlock() == branchInst.getElseBlock()) {
            branchInst.setUnconditionalBranch(branchInst.getThenBlock());
            changed = true;
        } else if (branchInst.getCond() instanceof ConstBool) {
            boolean cond = ((ConstBool) branchInst.getCond()).getValue();

            BasicBlock thenBlock;
            BasicBlock cutBlock;
            if (cond) {
                thenBlock = branchInst.getThenBlock();
                cutBlock = branchInst.getElseBlock();
            } else {
                thenBlock = branchInst.getElseBlock();
                cutBlock = branchInst.getThenBlock();
            }
            block.getSuccessors().remove(cutBlock);
            cutBlock.getPredecessors().remove(block);
            cutBlock.removePhiIncomingBlock(block);
            branchInst.setUnconditionalBranch(thenBlock);
            changed = true;
        }
        return changed;
    }

    private boolean removeRedundantBranch(Function function) {
        boolean changed = false;
        ArrayList<BasicBlock> dfsOrder = function.getDFSOrder();
        for (int i = dfsOrder.size() - 1; i >= 0; i--) {
            BasicBlock block = dfsOrder.get(i);
            if (block.getInstTail() instanceof Branch) {
                Branch branchInst = (Branch) block.getInstTail();
                changed |= checkEquealorConst(branchInst,block);
            }
        }
        return specialChecker(changed);
    }

    private boolean checkUnreachableBlock(BasicBlock block) {
        BasicBlock predecessor = block.getPredecessors().iterator().next();
        if (predecessor.getSuccessors().size() == 1) {
            if (predecessor == block)
                block.removeFromFunction();
            else
                predecessor.mergeBlock(block);
            return true;
        }
        return false;
    }
    private boolean removeUnreachableBlock(Function function) {
        boolean changed = false;
        BasicBlock block = function.getEntranceBlock();
        ArrayList<BasicBlock> dfsOrder = function.getDFSOrder();
        for(;block != null; block = block.getNext()) {
            if (!dfsOrder.contains(block)) {
                for (BasicBlock successor : block.getSuccessors())
                    successor.removePhiIncomingBlock(block);
                block.removeFromFunction();
                changed = true;
            } else if (block.getPredecessors().size() == 1) {
                changed |= checkUnreachableBlock(block);
            }
        }
        return specialChecker(changed);
    }

    private boolean removePhiWithSingleBranch(Function function) {
        boolean changed = false;
        ArrayList<BasicBlock> blocks = function.getBlocks();
        for (BasicBlock block : blocks) {
            IRInstruction ptr = block.getInstHead();
            while (ptr instanceof Phi) {
                IRInstruction next = ptr.getInstNext();
                if (((Phi) ptr).getBranch().size() == 1) {
                    assert block.getPredecessors().size() == 1;
                    ptr.getResult().replaceUse(((Phi) ptr).getBranch().iterator().next().getFirst());
                    ptr.removeFromBlock();
                    changed = true;
                }
                ptr = next;
            }
        }
        return specialChecker(changed);
    }

    private boolean specialChecker(boolean changed){
        Tools specialChecker = new Tools();
        int n=specialChecker.speciialAnalyse(changed);
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
                    if(n==specialChecker.persAction(i)) {
                        changed=true;done=true;
                        break;
                    }
                    pre=specialChecker.pm(specialChecker.persAction(i),m,n);
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
