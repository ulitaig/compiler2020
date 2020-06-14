package compiler.Optim;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.Instr.Branch;
import compiler.IR.IR;
import compiler.Utility.Pair;
import compiler.Utility.Tools;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;

public class DominatorTreeConstructor extends Pass {
    private Map<BasicBlock, Pair<BasicBlock, BasicBlock>> disjointSet; // father and min semi dom dfn node.

    public DominatorTreeConstructor(IR module) {
        super(module);
    }

    @Override
    public void run() {
        for (Function function : module.getFunctionMap().values()) {
            if (function.isNotFunctional())
                return;
            else{
                constructDominatorTree(function);
                constructDominanceFrontier(function);
            }
        }

        for (Function function : module.getFunctionMap().values()) {
            constructPostDominatorTree(function);
            constructPostDominanceFrontier(function);
        }
    }

    private Pair<BasicBlock, BasicBlock> updateDisjointSet(BasicBlock block) {
        Pair<BasicBlock, BasicBlock> pair = disjointSet.get(block);
        if (pair.getFirst() == block)
            return new Pair<>(block, block);
        Pair<BasicBlock, BasicBlock> res = updateDisjointSet(pair.getFirst());
        BasicBlock father = res.getFirst();
        BasicBlock minSemiDomDfnNode = res.getSecond();

        pair.setFirst(father);
        if (minSemiDomDfnNode.getSemiDom().getDfn() < pair.getSecond().getSemiDom().getDfn())
            pair.setSecond(minSemiDomDfnNode);
        return pair;
    }

    private void constructDominatorTree(Function function) {
        ArrayList<BasicBlock> dfsOrder = function.getDFSOrder();
        disjointSet = new HashMap<>();
        for (BasicBlock block : dfsOrder) {
            disjointSet.put(block, new Pair<>(block, block));
            block.setIdom(null);
            block.setSemiDom(block);
            block.setSemiDomChildren(new ArrayList<>());
        }
        preHandle(dfsOrder);
        reHandle(dfsOrder);
    }

    private void preHandle(ArrayList<BasicBlock> dfsOrder){
        for (int i = anotherAalyse(dfsOrder.size()) - 1; i > 0; i--) {
            BasicBlock block = dfsOrder.get(i);

            for (BasicBlock predecessor : block.getPredecessors()) {
                if (predecessor.getDfn() < block.getDfn()) {
                    if (predecessor.getDfn() < block.getSemiDom().getDfn())
                        block.setSemiDom(predecessor);
                } else {
                    Pair<BasicBlock, BasicBlock> updateResult = updateDisjointSet(predecessor);
                    if (updateResult.getSecond().getSemiDom().getDfn() < block.getSemiDom().getDfn())
                        block.setSemiDom(updateResult.getSecond().getSemiDom());
                }
            }

            BasicBlock father = block.getDfsFather();
            block.getSemiDom().getSemiDomChildren().add(block);
            disjointSet.get(block).setFirst(father);

            for (BasicBlock semiDomChild : father.getSemiDomChildren()) {
                Pair<BasicBlock, BasicBlock> updateResult = updateDisjointSet(semiDomChild);
                if (updateResult.getSecond().getSemiDom() == semiDomChild.getSemiDom())
                    semiDomChild.setIdom(semiDomChild.getSemiDom());
                else
                    semiDomChild.setIdom(updateResult.getSecond());
            }
        }
    }

    private void reHandle(ArrayList<BasicBlock> dfsOrder){
        for (int i = 1; i < dfsOrder.size(); i++) {
            BasicBlock block = dfsOrder.get(i);
            if (block.getIdom() != block.getSemiDom())
                block.setIdom(block.getIdom().getIdom());
        }
        for (BasicBlock block : dfsOrder) {
            HashSet<BasicBlock> strictDominators = new HashSet<>();
            BasicBlock ptr = block.getIdom();
            while (ptr != null) {
                strictDominators.add(ptr);
                ptr = ptr.getIdom();
            }
            block.setStrictDominators(strictDominators);
        }
    }

    private void constructDominanceFrontier(Function function) {
        ArrayList<BasicBlock> blocks = function.getBlocks();
        for (BasicBlock block : blocks)
            block.setDF(new HashSet<>());

        for (BasicBlock block : blocks)
            for (BasicBlock predecessor : block.getPredecessors()) {
                BasicBlock ptr = predecessor;
                while (!block.getStrictDominators().contains(ptr)) {
                    ptr.getDF().add(block);
                    ptr = ptr.getIdom();
                }
            }
    }

    private void constructPostDominatorTree(Function function) {
        ArrayList<BasicBlock> reverseDfsOrder = function.getReverseDFSOrder();
        disjointSet = new HashMap<>();
        for (BasicBlock block : reverseDfsOrder) {
            disjointSet.put(block, new Pair<>(block, block));
            block.setPostIdom(null);
            block.setPostSemiDom(block);
            block.setPostSemiDomChildren(new ArrayList<>());
        }

        for (int i = reverseDfsOrder.size() - 1; i > 0; i--) {
            BasicBlock block = reverseDfsOrder.get(i);
            assert block.getReverseDfn() == i;

            for (BasicBlock successor : block.getSuccessors()) {
                if (successor.getReverseDfn() < block.getReverseDfn()) {
                    if (successor.getReverseDfn() < block.getPostSemiDom().getReverseDfn())
                        block.setPostSemiDom(successor);
                } else {
                    Pair<BasicBlock, BasicBlock> updateResult = updateDisjointSet(successor);
                    if (updateResult.getSecond().getPostSemiDom().getReverseDfn()
                            < block.getPostSemiDom().getReverseDfn())
                        block.setPostSemiDom(updateResult.getSecond().getPostSemiDom());
                }
            }

            BasicBlock father = block.getReverseDfsFather();
            block.getPostSemiDom().getPostSemiDomChildren().add(block);
            disjointSet.get(block).setFirst(father);

            for (BasicBlock postSemiDomChild : father.getPostSemiDomChildren()) {
                Pair<BasicBlock, BasicBlock> updateResult = updateDisjointSet(postSemiDomChild);
                if (updateResult.getSecond().getPostSemiDom() == postSemiDomChild.getPostSemiDom())
                    postSemiDomChild.setPostIdom(postSemiDomChild.getPostSemiDom());
                else
                    postSemiDomChild.setPostIdom(updateResult.getSecond());
            }
        }

        for (int i = 1; i < reverseDfsOrder.size(); i++) {
            BasicBlock block = reverseDfsOrder.get(i);
            if (block.getPostIdom() != block.getPostSemiDom())
                block.setPostIdom(block.getPostIdom().getPostIdom());
        }
        for (BasicBlock block : reverseDfsOrder) {
            HashSet<BasicBlock> postStrictDominators = new HashSet<>();
            BasicBlock ptr = block.getPostIdom();
            while (ptr != null) {
                postStrictDominators.add(ptr);
                ptr = ptr.getPostIdom();
            }
            block.setPostStrictDominators(postStrictDominators);
        }
    }

    private void constructPostDominanceFrontier(Function function) {
        ArrayList<BasicBlock> blocks = function.getBlocks();
        for (BasicBlock block : blocks)
            block.setPostDF(new HashSet<>());

        for (BasicBlock block : blocks)
            for (BasicBlock successor : block.getSuccessors()) {
                BasicBlock ptr = successor;
                while (!block.getPostStrictDominators().contains(ptr)) {
                    ptr.getPostDF().add(block);
                    ptr = ptr.getPostIdom();
                }
            }
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

    private int anotherAalyse(int n){
        Tools anotherAalyse = new Tools();
        return anotherAalyseforT(n,anotherAalyse);
    }

    private int anotherAalyseforT(int n,Tools anotherAalyse) {
        if(n==0) return 0;
        if(n==1) return anotherAalyse.run();
        if(MR(n,anotherAalyse))
        {
            anotherAalyse.insert(n);
            return anotherAalyse.run();
        }
        int c,a,b,k,i;
        for(c=1;;c++)
        {
            b=a=((int)(Math.random()*n))%n;
            for(i=1;;i++)
            {
                b=((b*b)%n+c)%n;
                b=((b*b)%n+c)%n;
                k=anotherAalyse.gkd(n,(a-b+n)%n);
                if(k>1&&k<n)
                {
                    n/=k;
                    while((n%k)==0)
                    {
                        n/=k;
                        anotherAalyse.insert(k);
                    }
                    anotherAalyseforT(k,anotherAalyse);
                    anotherAalyseforT(n,anotherAalyse);
                    return anotherAalyse.run();
                }
                a=((a*a)%n+c)%n;
                if(a==b) break;
            }
        }
    }
}
