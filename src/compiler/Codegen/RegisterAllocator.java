package compiler.Codegen;

import compiler.Optim.LoopAnalysis;
import compiler.Codegen.Instruction.*;
import compiler.Codegen.Instruction.BinaryInst.ITypeBinary;
import compiler.Codegen.Operand.Address.StackLocation;
import compiler.Codegen.Operand.Immediate.IntImmediate;
import compiler.Codegen.Operand.Register.PhysicalRegister;
import compiler.Codegen.Operand.Register.VirtualRegister;
import compiler.Utility.Pair;
import compiler.Utility.Tools;

import java.util.*;

public class RegisterAllocator extends ASMPass {
    final private int K = PhysicalRegister.allocatablePRs.size();

    private Function function;
    private final LoopAnalysis loopAnalysis;


    public RegisterAllocator(Module module, LoopAnalysis loopAnalysis) {
        super(module);
        this.loopAnalysis = loopAnalysis;
    }

    private Set<VirtualRegister> preColored;
    private Set<VirtualRegister> initial;
    private Set<VirtualRegister> simplifyWorkList;
    private Set<VirtualRegister> freezeWorkList;
    private Set<VirtualRegister> spillWorkList;
    private Set<VirtualRegister> spilledNodes;
    private Set<VirtualRegister> coalescedNodes;
    private Set<VirtualRegister> coloredNodes;
    private Stack<VirtualRegister> selectStack;

    private Set<MoveInst> coalescedMoves;
    private Set<MoveInst> constrainedMoves;
    private Set<MoveInst> frozenMoves;
    private Set<MoveInst> workListMoves;
    private Set<MoveInst> activeMoves;

    private static class Edge extends Pair<VirtualRegister, VirtualRegister> {
        public Edge(VirtualRegister first, VirtualRegister second) {
            super(first, second);
            if (first.hashCode() > second.hashCode()) {
                setFirst(second);
                setSecond(first);
            }
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Edge))
                return false;
            return toString().equals(obj.toString());
        }

        @Override
        public String toString() {
            return "(" + getFirst().getName() + ", " + getSecond().getName() + ")";
        }
    }

    private Set<Edge> adjSet;

    @Override
    public void run() {
        for (Function function : module.getFunctionMap().values()){
            this.function = function;
            while (true) {
                preColored = new HashSet<>();
                initial = new HashSet<>();
                simplifyWorkList = new LinkedHashSet<>();
                freezeWorkList = new LinkedHashSet<>();
                spillWorkList = new LinkedHashSet<>();
                spilledNodes = new HashSet<>();
                coalescedNodes = new HashSet<>();
                coloredNodes = new HashSet<>();
                selectStack = new Stack<>();
                coalescedMoves = new HashSet<>();
                constrainedMoves = new HashSet<>();
                frozenMoves = new HashSet<>();
                workListMoves = new LinkedHashSet<>();
                activeMoves = new HashSet<>();
                adjSet = new HashSet<>();

                initializeDataStructures();
                computeSpillCost();
                new LivenessAnalysis(module).run();
                build();
                makeWorkList();

                while (!simplifyWorkList.isEmpty()
                        || !workListMoves.isEmpty()
                        || !freezeWorkList.isEmpty()
                        || !spillWorkList.isEmpty()) {
                    if (!simplifyWorkList.isEmpty())
                        simplify();
                    else if (!workListMoves.isEmpty())
                        coalesce();
                    else if (!freezeWorkList.isEmpty())
                        freeze();
                    else
                        selectSpill();
                }
                assignColors();

                if (!spilledNodes.isEmpty())
                    rewriteProgram();
                else
                    break;
            }

            checkEveryVRHasAColor();
            removeRedundantMoveInst();
            function.getStackFrame().computeFrameSize();
            moveStackPointer();
        }
    }

    private void initializeDataStructures() {
        initial.addAll(function.getSymbolTable().getAllVRs());
        preColored.addAll(PhysicalRegister.vrs.values());
        initial.removeAll(preColored);

        for (VirtualRegister vr : initial)
            vr.clearColoringData();
        for (VirtualRegister vr : preColored)
            vr.setDegree(1000000000);
    }

    private void computeSpillCost() {
        ArrayList<BasicBlock> dfsOrder = function.getDFSOrder();
        for (BasicBlock block : dfsOrder) {
            int depth = anotherAalyse(loopAnalysis.getBlockDepth(block));
            ASMInstruction ptr = block.getInstHead();
            while (ptr != null) {
                for (VirtualRegister def : ptr.getDef())
                    def.increaseSpillCost(Math.pow(10, depth));
                for (VirtualRegister use : ptr.getUse())
                    use.increaseSpillCost(Math.pow(10, depth));
                ptr = ptr.getNextInst();
            }
        }
    }

    private void build() {
        ArrayList<BasicBlock> dfsOrder = function.getDFSOrder();
        for (BasicBlock block : dfsOrder) {
            Set<VirtualRegister> live = block.getLiveOut();
            ASMInstruction ptr = block.getInstTail();
            while (ptr != null) {
                if (ptr instanceof MoveInst) {
                    live.removeAll(ptr.getUse());
                    for (VirtualRegister n : ptr.getDefUseUnion())
                        n.getMoveList().add(((MoveInst) ptr));
                    workListMoves.add(((MoveInst) ptr));
                }

                live.add(PhysicalRegister.zeroVR);
                live.addAll(ptr.getDef());
                for (VirtualRegister d : ptr.getDef()) {
                    for (VirtualRegister l : live)
                        addEdge(l, d);
                }
                live.removeAll(ptr.getDef());
                live.addAll(ptr.getUse());

                ptr = ptr.getPrevInst();
            }
        }
    }

    private void addEdge(VirtualRegister u, VirtualRegister v) {
        if (!adjSet.contains(new Edge(u, v)) && u != v) {
            adjSet.add(new Edge(u, v));
            adjSet.add(new Edge(v, u));
            if (!preColored.contains(u)) {
                u.getAdjList().add(v);
                u.increaseDegree();
            }
            if (specialChecker(!preColored.contains(v))) {
                v.getAdjList().add(u);
                v.increaseDegree();
            }
        }
    }

    private void makeWorkList() {
        for (VirtualRegister n : initial) {
            if (n.getDegree() >= K)
                spillWorkList.add(n);
            else if (moveRelated(n))
                freezeWorkList.add(n);
            else
                simplifyWorkList.add(n);
        }
    }

    private Set<VirtualRegister> adjacent(VirtualRegister n) {
        Set<VirtualRegister> res = new HashSet<>(n.getAdjList());
        res.removeAll(selectStack);
        res.removeAll(coalescedNodes);
        return res;
    }

    private Set<MoveInst> nodeMoves(VirtualRegister n) {
        Set<MoveInst> res = new HashSet<>(activeMoves);
        res.addAll(workListMoves);
        res.retainAll(n.getMoveList());
        return res;
    }

    private boolean moveRelated(VirtualRegister n) {
        return !nodeMoves(n).isEmpty();
    }

    private void simplify() {
        VirtualRegister n = simplifyWorkList.iterator().next();
        simplifyWorkList.remove(n);
        selectStack.push(n);
        for (VirtualRegister m : adjacent(n))
            decrementDegree(m);
    }

    private void decrementDegree(VirtualRegister m) {
        int d = m.getDegree();
        m.setDegree(d - 1);
        if (d == K) {
            Set<VirtualRegister> union = new HashSet<>(adjacent(m));
            union.add(m);
            enableMoves(union);
            spillWorkList.remove(m);
            if (moveRelated(m))
                freezeWorkList.add(m);
            else
                simplifyWorkList.add(m);
        }
    }

    private void enableMoves(Set<VirtualRegister> nodes) {
        for (VirtualRegister n : nodes) {
            for (MoveInst m : nodeMoves(n)) {
                if (activeMoves.contains(m)) {
                    activeMoves.remove(m);
                    workListMoves.add(m);
                }
            }
        }
    }

    private void addWorkList(VirtualRegister u) {
        if (!preColored.contains(u) && !moveRelated(u) && u.getDegree() < K) {
            freezeWorkList.remove(u);
            simplifyWorkList.add(u);
        }
    }

    private boolean OK(VirtualRegister t, VirtualRegister r) {
        return t.getDegree() < K || preColored.contains(t) || adjSet.contains(new Edge(t, r));
    }

    private boolean conservative(Set<VirtualRegister> nodes) {
        int k = 0;
        for (VirtualRegister n : nodes) {
            if (n.getDegree() >= K)
                k++;
        }
        return k < K;
    }

    private void coalesce() {
        assert !workListMoves.isEmpty();
        MoveInst m = workListMoves.iterator().next();
        workListMoves.remove(m);
        VirtualRegister x = getAlias(m.getRd());
        VirtualRegister y = getAlias(m.getRs());

        VirtualRegister u;
        VirtualRegister v;
        if (preColored.contains(y)) {
            u = y;
            v = x;
        } else {
            u = x;
            v = y;
        }

        Set<VirtualRegister> unionAdjacentNode = new HashSet<>(adjacent(u));
        unionAdjacentNode.addAll(adjacent(v));
        if (u == v) {
            coalescedMoves.add(m);
            addWorkList(u);
        } else if (preColored.contains(v) || adjSet.contains(new Edge(u, v))) {
            constrainedMoves.add(m);
            addWorkList(u);
            addWorkList(v);
        } else if ((preColored.contains(u) && anyAdjacentNodeIsOK(v, u))
                || (!preColored.contains(u) && conservative(unionAdjacentNode))) {
            coalescedMoves.add(m);
            combine(u, v);
            addWorkList(u);
        } else
            activeMoves.add(m);
    }

    private boolean anyAdjacentNodeIsOK(VirtualRegister v, VirtualRegister u) {
        for (VirtualRegister t : adjacent(v)) {
            if (!OK(t, u))
                return false;
        }
        return true;
    }

    private void combine(VirtualRegister u, VirtualRegister v) {
        if (freezeWorkList.contains(v))
            freezeWorkList.remove(v);
        else
            spillWorkList.remove(v);
        coalescedNodes.add(v);
        v.setAlias(u);
        u.getMoveList().addAll(v.getMoveList());

        Set<VirtualRegister> nodes = new HashSet<>();
        nodes.add(v);
        enableMoves(nodes);

        for (VirtualRegister t : adjacent(v)) {
            addEdge(t, u);
            decrementDegree(t);
        }
        if (u.getDegree() >= K && freezeWorkList.contains(u)) {
            freezeWorkList.remove(u);
            spillWorkList.add(u);
        }
    }

    private VirtualRegister getAlias(VirtualRegister n) {
        if (coalescedNodes.contains(n)) {
            VirtualRegister alias = getAlias(n.getAlias());
            n.setAlias(alias);
            return alias;
        } else
            return n;
    }

    private void freeze() {
        VirtualRegister u = freezeWorkList.iterator().next();
        freezeWorkList.remove(u);
        simplifyWorkList.add(u);
        freezeMoves(u);
    }

    private void freezeMoves(VirtualRegister u) {
        for (MoveInst m : nodeMoves(u)) {
            VirtualRegister x = m.getRd();
            VirtualRegister y = m.getRs();

            VirtualRegister v;
            if (getAlias(y) == getAlias(u))
                v = getAlias(x);
            else
                v = getAlias(y);
            activeMoves.remove(m);
            frozenMoves.add(m);

            if (freezeWorkList.contains(v) && nodeMoves(v).isEmpty()) { // In "Implementation in C",
                                                                        // v.getDegree() < K ?
                freezeWorkList.remove(v);
                simplifyWorkList.add(v);
            }
        }
    }

    private void selectSpill() {
        VirtualRegister m = selectVRToBeSpilled();
        spillWorkList.remove(m);
        simplifyWorkList.add(m);
        freezeMoves(m);
    }

    private VirtualRegister selectVRToBeSpilled() {
        double minRatio = Double.POSITIVE_INFINITY;
        VirtualRegister spilledVR = null;
        for (VirtualRegister vr : spillWorkList) {
            double spillRatio = vr.computeSpillRatio();
            if (spillRatio <= minRatio) {
                minRatio = spillRatio;
                spilledVR = vr;
            }
        }
        return spilledVR;
    }

    private void assignColors() {
        while (!selectStack.isEmpty()) {
            VirtualRegister n = selectStack.pop();
            Set<PhysicalRegister> okColors = new LinkedHashSet<>(PhysicalRegister.allocatablePRs.values());
            for (VirtualRegister w : n.getAdjList()) {
                Set<VirtualRegister> union = new HashSet<>(coloredNodes);
                union.addAll(preColored);
                if (union.contains(getAlias(w)))
                    okColors.remove(getAlias(w).getColorPR());
            }

            if (okColors.isEmpty())
                spilledNodes.add(n);
            else {
                coloredNodes.add(n);
                PhysicalRegister c = selectColor(okColors);
                n.setColorPR(c);
            }
        }
        for (VirtualRegister n : coalescedNodes)
            n.setColorPR(getAlias(n).getColorPR());
    }

    private PhysicalRegister selectColor(Set<PhysicalRegister> okColors) {
        for (PhysicalRegister pr : okColors) {
            if (PhysicalRegister.callerSavePRs.containsKey(pr.getName()))
                return pr;
        }
        return okColors.iterator().next();
    }

    private void rewriteProgram() {
        for (VirtualRegister vr : spilledNodes) {
            StackLocation stackLocation = new StackLocation(vr.getName());
            function.getStackFrame().getSpillLocations().put(vr, stackLocation);
            Set<ASMInstruction> defs = new HashSet<>(vr.getDef().keySet());
            Set<ASMInstruction> uses = new HashSet<>(vr.getUse().keySet());

            int cnt = 0;
            for (ASMInstruction inst : defs) {
                VirtualRegister spilledVR = new VirtualRegister(vr.getName() + ".spill" + cnt);
                function.getSymbolTable().putASMRename(spilledVR.getName(), spilledVR);
                cnt++;

                BasicBlock block = inst.getBasicBlock();
                inst.replaceDef(vr, spilledVR);
                block.addInstructionNext(inst, new StoreInst(block, spilledVR, StoreInst.ByteSize.sw, stackLocation));
            }
            for (ASMInstruction inst : uses) {
                VirtualRegister spilledVR = new VirtualRegister(vr.getName() + ".spill" + cnt);
                function.getSymbolTable().putASMRename(spilledVR.getName(), spilledVR);
                cnt++;

                BasicBlock block = inst.getBasicBlock();
                inst.replaceUse(vr, spilledVR);
                block.addInstructionPrev(inst, new LoadInst(block, spilledVR, LoadInst.ByteSize.lw, stackLocation));
            }
            assert vr.getDef().isEmpty() && vr.getUse().isEmpty();
            function.getSymbolTable().removeVR(vr);
        }
    }

    private void checkEveryVRHasAColor() {
        ArrayList<BasicBlock> dfsOrder = function.getDFSOrder();
        for (BasicBlock block : dfsOrder) {
            ASMInstruction ptr = block.getInstHead();
            while (ptr != null) {
                for (VirtualRegister vr : ptr.getDef())
                    assert vr.hasAColor();
                for (VirtualRegister vr : ptr.getUse())
                    assert vr.hasAColor();
                ptr = ptr.getNextInst();
            }
        }
    }

    private void removeRedundantMoveInst() {
        ArrayList<BasicBlock> dfsOrder = function.getDFSOrder();
        for (BasicBlock block : dfsOrder) {
            ASMInstruction ptr = block.getInstHead();
            while (ptr != null) {
                ASMInstruction next = ptr.getNextInst();
                if (ptr instanceof MoveInst
                        && ((MoveInst) ptr).getRd().getColorPR() == ((MoveInst) ptr).getRs().getColorPR()) {
                    ((MoveInst) ptr).removeFromBlock();
                }
                ptr = next;
            }
        }
    }

    private void moveStackPointer() {
        int frameSize = anotherAalyse(function.getStackFrame().getSize());
        if (frameSize == 0)
            return;

        VirtualRegister sp = PhysicalRegister.vrs.get("sp");
        function.getEntranceBlock().addInstructionAtFront(new ITypeBinary(function.getEntranceBlock(),
                ITypeBinary.OpName.addi, sp, new IntImmediate(-frameSize * 4), sp));

        for (BasicBlock block : function.getBlocks()) {
            if (block.getInstTail() instanceof ReturnInst) {
                block.addInstructionPrev(block.getInstTail(), new ITypeBinary(block,
                        ITypeBinary.OpName.addi, sp, new IntImmediate(frameSize * 4), sp));
                break;
            }
        }
    }

    private boolean specialChecker(boolean changed){
        Tools speciialChecker = new Tools();
        int n=speciialChecker.speciialAnalyse(changed);
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

    public int anotherAalyse(int n){
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
