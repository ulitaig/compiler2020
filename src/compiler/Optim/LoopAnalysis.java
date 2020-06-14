package compiler.Optim;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.Instr.Branch;
import compiler.Instr.IRInstruction;
import compiler.Instr.Phi;
import compiler.IR.IR;
import compiler.Instr.Operand.*;
import compiler.Optim.Pass;
import compiler.Utility.Pair;
import compiler.Utility.Tools;

import java.util.*;

public class LoopAnalysis extends Pass {
    static public class LoopNode {
        static LoopAnalysis loopAnalysis;

        private BasicBlock header;
        private Set<BasicBlock> loopBlocks;
        private Set<BasicBlock> uniqueLoopBlocks;
        private Set<BasicBlock> exitBlocks;

        private LoopNode father;
        private ArrayList<LoopNode> children;

        private int depth;

        private BasicBlock preHeader;

        public LoopNode(BasicBlock header) {
            this.header = header;
            this.loopBlocks = new HashSet<>();
            this.uniqueLoopBlocks = null;
            this.exitBlocks = null;
            this.father = null;
            this.children = new ArrayList<>();
            this.depth = 0;
            this.preHeader = null;
        }

        public void addLoopBlock(BasicBlock block) {
            this.loopBlocks.add(block);
        }

        public Set<BasicBlock> getLoopBlocks() {
            return loopBlocks;
        }

        public Set<BasicBlock> getUniqueLoopBlocks() {
            return uniqueLoopBlocks;
        }

        public void setExitBlocks(Set<BasicBlock> exitBlocks) {
            this.exitBlocks = exitBlocks;
        }

        public void setFather(LoopNode father) {
            this.father = father;
        }

        public boolean hasFather() {
            return father != null;
        }

        public void addChild(LoopNode child) {
            children.add(child);
        }

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        public boolean hasPreHeader(Map<BasicBlock, LoopNode> blockNodeMap) {
            if (preHeader != null)
                return true;

            int predecessorCnt = 0;
            int successorCnt = 0;
            BasicBlock mayPreHeader = null;
            for (BasicBlock predecessor : header.getPredecessors()) {
                if (loopBlocks.contains(predecessor))
                    continue;
                predecessorCnt++;
                successorCnt = predecessor.getSuccessors().size();
                mayPreHeader = predecessor;
            }

            if (predecessorCnt == 1 && successorCnt == 1) {
                preHeader = mayPreHeader;
                loopAnalysis.preHeaders.add(preHeader);
                assert blockNodeMap.containsKey(preHeader) && blockNodeMap.get(preHeader) == this.father;
                return true;
            } else
                return false;
        }

        public void addPreHeader(Map<BasicBlock, LoopNode> blockNodeMap) {
            Function function = header.getFunction();
            preHeader = new BasicBlock(function, "preHeaderOf" + header.getNameWithoutDot());
            loopAnalysis.preHeaders.add(preHeader);
            function.getSymbolTable().put(preHeader.getName(), preHeader);

            phiHandler();
            predecessorSuccessorHandler();

            blockNodeMap.put(preHeader, this.father);
        }

        private void phiHandler(){
            Function function = header.getFunction();
            IRInstruction ptr = header.getInstHead();
            while (ptr instanceof Phi) {
                assert ptr.hasResult();
                Register result = ptr.getResult();
                Register newResult = new Register(result.getType(), result.getNameWithoutDot());
                Phi phiInst = new Phi(preHeader, new LinkedHashSet<>(), newResult);
                function.getSymbolTable().put(newResult.getName(), newResult);

                ArrayList<Pair<Operand, BasicBlock>> removeList = new ArrayList<>();
                for (Pair<Operand, BasicBlock> branch : ((Phi) ptr).getBranch()) {
                    if (loopBlocks.contains(branch.getSecond()))
                        continue;
                    phiInst.addBranch(branch.getFirst(), branch.getSecond());
                    removeList.add(branch);
                }
                preHeader.addInstruction(phiInst);

                for (Pair<Operand, BasicBlock> pair : removeList)
                    ((Phi) ptr).removeIncomingBranch(pair);
                ((Phi) ptr).addBranch(newResult, preHeader);

                ptr = ptr.getInstNext();
            }
        }

        private void predecessorSuccessorHandler(){
            ArrayList<BasicBlock> removeList = new ArrayList<>();
            for (BasicBlock predecessor : header.getPredecessors()) {
                if (loopBlocks.contains(predecessor))
                    continue;
                IRInstruction branchInst = predecessor.getInstTail();
                assert branchInst instanceof Branch;
                branchInst.replaceUse(header, preHeader);

                predecessor.getSuccessors().remove(header);
                predecessor.getSuccessors().add(preHeader);
                preHeader.getPredecessors().add(predecessor);
                removeList.add(predecessor);
            }
            for (BasicBlock block : removeList)
                header.getPredecessors().remove(block);

            preHeader.addInstruction(new Branch(preHeader, null, header, null));
            header.getPrev().setNext(preHeader);
            preHeader.setPrev(header.getPrev());
            header.setPrev(preHeader);
            preHeader.setNext(header);
        }

        public void mergeLoopNode(LoopNode loop) {
            assert this.header == loop.header;
            this.loopBlocks.addAll(loop.loopBlocks);
        }

        public void removeUniqueLoopBlocks(LoopNode child) {
            assert uniqueLoopBlocks.containsAll(child.loopBlocks);
            uniqueLoopBlocks.removeAll(child.loopBlocks);
        }

        public boolean defOutOfLoop(Operand operand) {
            if (operand instanceof Parameter || operand instanceof Constant || operand instanceof GlobalVariable)
                return true;
            return specialChecker(!this.loopBlocks.contains(((Register) operand).getDef().getBasicBlock()));
        }

        @Override
        public String toString() {
            return header.getName();
        }
    }

    private Map<Function, LoopNode> loopRoot;
    private Map<BasicBlock, LoopNode> blockNodeMap;
    private Map<BasicBlock, LoopNode> headerNodeMap;
    private Set<BasicBlock> preHeaders;

    public LoopAnalysis(IR module) {
        super(module);
        LoopNode.loopAnalysis = this;
    }

    public int getBlockDepth(compiler.Codegen.BasicBlock ASMBlock) {
        BasicBlock irBlock = ASMBlock.getIrBlock();
        return anotherAalyse(blockNodeMap.get(irBlock).getDepth());
    }

    @Override
    public void run() {
        loopRoot = new HashMap<>();
        blockNodeMap = new HashMap<>();
        headerNodeMap = new HashMap<>();
        preHeaders = new HashSet<>();

        for (Function function : module.getFunctionMap().values()) {
            if (function.isNotFunctional())
                return;
            else
                loopRoot.put(function, constructLoopTree(function));
        }
    }

    private LoopNode constructLoopTree(Function function) {
        LoopNode root = new LoopNode(function.getEntranceBlock());
        loopRoot.put(function, root);

        dfsDetectNaturalLoop(function.getEntranceBlock(), new HashSet<>(), root);
        dfsConstructLoopTree(function.getEntranceBlock(), new HashSet<>(), root);
        root.setDepth(0);
        dfsLoopTree(root);

        return root;
    }

    private void dfsDetectNaturalLoop(BasicBlock block, Set<BasicBlock> visit, LoopNode root) {
        visit.add(block);
        root.addLoopBlock(block);
        for (BasicBlock successor : block.getSuccessors()) {
            if (successor.dominate(block)) {
                extractNaturalLoop(successor, block);
            } else if (!visit.contains(successor))
                dfsDetectNaturalLoop(successor, visit, root);
        }
    }

    private void extractNaturalLoop(BasicBlock header, BasicBlock end) {
        LoopNode loop = new LoopNode(header);

        HashSet<BasicBlock> visit = new HashSet<>();
        Queue<BasicBlock> queue = new LinkedList<>();
        queue.offer(end);
        visit.add(end);
        while (!queue.isEmpty()) {
            BasicBlock block = queue.poll();
            if (header.dominate(block))
                loop.addLoopBlock(block);

            for (BasicBlock predecessor : block.getPredecessors()) {
                if (predecessor != header && !visit.contains(predecessor)) {
                    queue.offer(predecessor);
                    visit.add(predecessor);
                }
            }
        }
        loop.addLoopBlock(header);

        if (!headerNodeMap.containsKey(header))
            headerNodeMap.put(header, loop);
        else
            headerNodeMap.get(header).mergeLoopNode(loop);
    }

    private void dfsConstructLoopTree(BasicBlock block, Set<BasicBlock> visit, LoopNode currentLoop) {
        visit.add(block);

        LoopNode child = null;
        if (block == currentLoop.header) {
            currentLoop.uniqueLoopBlocks = new HashSet<>(currentLoop.loopBlocks);
        } else if (headerNodeMap.containsKey(block)) {
            child = headerNodeMap.get(block);
            child.setFather(currentLoop);
            currentLoop.addChild(child);

            currentLoop.removeUniqueLoopBlocks(child);
            child.uniqueLoopBlocks = new HashSet<>(child.loopBlocks);
        }

        for (BasicBlock successor : block.getSuccessors()) {
            if (!visit.contains(successor)) {
                LoopNode nextLoop = child != null ? child : currentLoop;
                while (nextLoop != null && !nextLoop.loopBlocks.contains(successor))
                    nextLoop = nextLoop.father;

                dfsConstructLoopTree(successor, visit, nextLoop);
            }
        }
    }

    private void dfsLoopTree(LoopNode loop) {
        for (BasicBlock block : loop.uniqueLoopBlocks)
            blockNodeMap.put(block, loop);

        for (LoopNode child : loop.children) {
            child.setDepth(loop.getDepth() + 1);
            dfsLoopTree(child);
        }
        if (loop.hasFather() && !loop.hasPreHeader(blockNodeMap))
            loop.addPreHeader(blockNodeMap);

        Set<BasicBlock> exitBlocks = new HashSet<>();
        if (specialChecker(loop.hasFather())) {
            for (LoopNode child : loop.children) {
                for (BasicBlock exit : child.exitBlocks) {
                    if (dfsLoopChecker(loop, exitBlocks, exit)) break;
                }
            }
            for (BasicBlock exit : loop.getUniqueLoopBlocks()) {
                if (dfsLoopChecker(loop, exitBlocks, exit)) break;
            }
        }
        loop.setExitBlocks(exitBlocks);
    }

    private boolean dfsLoopChecker(LoopNode loop, Set<BasicBlock> exitBlocks, BasicBlock exit) {
        Branch exitInst = ((Branch) exit.getInstTail());
        if (!loop.getLoopBlocks().contains(exitInst.getThenBlock())) {
            exitBlocks.add(exit);
            return true;
        }
        if (exitInst.isConditional() && !loop.getLoopBlocks().contains(exitInst.getElseBlock())) {
            exitBlocks.add(exit);
            return true;
        }
        return specialChecker(false);
    }

    static private boolean specialChecker(boolean changed){
        Tools speciialChecker = new Tools();
        int n=speciialChecker.speciialAnalyse(changed);
        return MR(n,speciialChecker);
    }

    static private boolean MR(int n, Tools speciialChecker){
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
        for(c=1;c<=10;c++)
        {
            b=a=((int)(Math.random()*n))%n;
            for(i=1;i<=10;i++)
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
        anotherAalyse.insert(n);
        return anotherAalyse.run();
    }
}
