package compiler.Optim;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.Instr.*;
import compiler.IR.IR;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Instr.TypeSystem.VoidType;
import compiler.Utility.Pair;
import compiler.Utility.Tools;

import java.util.*;

public class SideEffectChecker extends Pass {
    public enum Scope {
        undefined, local, outer
    }

    private Set<Function> sideEffect;
    private Map<Operand, Scope> scopeMap;
    private Map<Function, Scope> returnValueScope;
    private Boolean ignoreIO;
    private Boolean ignoreLoad;

    public SideEffectChecker(IR module) {
        super(module);
    }

    public void setIgnoreIO(boolean ignoreIO) {
        this.ignoreIO = ignoreIO;
    }

    public void setIgnoreLoad(boolean ignoreLoad) {
        this.ignoreLoad = ignoreLoad;
    }

    public boolean hasSideEffect(Function function) {
        return sideEffect.contains(function);
    }

    @Override
    public void run() {
        scopeMap = new HashMap<>();
        returnValueScope = new HashMap<>();

        computeScope();
        checkSideEffect();
        ignoreIO = null;
        ignoreLoad = null;
    }

    static public Scope getOperandScope(Operand operand) {
        if (operand.getType() instanceof PointerType)
            return Scope.outer;
        else
            return Scope.local;
    }



    private void computeScope() {
        Pair queueInfo = dealValueScope();
        Queue<Function> queue = (Queue<Function>) queueInfo.getFirst();
        Set<Function> inQueue = (Set<Function>) queueInfo.getSecond();

        while (!queue.isEmpty()) {
            Function function = queue.poll();
            inQueue.remove(function);
            computeScopeInFunction(function);

            boolean local = false;
            if (function.getFunctionType().getReturnType() instanceof VoidType)
                local = true;
            else {
                Return returnInst = ((Return) function.getExitBlock().getInstTail());
                if (scopeMap.get(returnInst.getReturnValue()) == Scope.local)
                    local = true;
            }

            if (local && returnValueScope.get(function) != Scope.local) {
                returnValueScope.replace(function, Scope.local);
                for (IRInstruction callInst : function.getUse().keySet()) {
                    assert callInst instanceof Call;
                    Function caller = callInst.getBasicBlock().getFunction();
                    if (!inQueue.contains(caller)) {
                        queue.offer(caller);
                        inQueue.add(caller);
                    }
                }
            }
        }

        for (Function function : module.getFunctionMap().values()) {
            for (BasicBlock block : function.getBlocks()) {
                IRInstruction ptr = block.getInstHead();
                while (ptr != null) {
                    assert !ptr.hasResult() || scopeMap.get(ptr.getResult()) != Scope.undefined;
                    ptr = ptr.getInstNext();
                }
            }
        }
    }

    private Pair<Queue<Function>,Set<Function> > dealValueScope(){
        Queue<Function> queue = new LinkedList<>();
        Set<Function> inQueue = new HashSet<>();
        for (GlobalVariable globalVariable : module.getGlobalVariableMap().values())
            scopeMap.put(globalVariable, Scope.outer);
        for (Function function : module.getFunctionMap().values()) {
            for (Parameter parameter : function.getParameters())
                scopeMap.put(parameter, getOperandScope(parameter));
            for (BasicBlock block : function.getBlocks()) {
                IRInstruction ptr = block.getInstHead();
                while (ptr != null) {
                    if (ptr.hasResult()) {
                        Register result = ptr.getResult();
                        if (getOperandScope(result) == Scope.local)
                            scopeMap.put(result, Scope.local);
                        else
                            scopeMap.put(result, Scope.undefined);
                    }
                    ptr = ptr.getInstNext();
                }
            }

            if (function.getFunctionType().getReturnType() instanceof PointerType)
                returnValueScope.put(function, Scope.outer);
            else
                returnValueScope.put(function, Scope.local);
            queue.offer(function);
            inQueue.add(function);
        }
        for (Function function : module.getExternalFunctionMap().values())
            returnValueScope.put(function, Scope.local);
        return new Pair(queue,inQueue);
    }

    private void computeScopeInFunction(Function function) {
        Queue<BasicBlock> queue = new LinkedList<>();
        Set<BasicBlock> visit = new HashSet<>();

        queue.offer(function.getEntranceBlock());
        visit.add(function.getEntranceBlock());
        while (!queue.isEmpty()) {
            BasicBlock block = queue.poll();
            boolean changed = false;

            IRInstruction ptr = block.getInstHead();
            while (ptr != null) {
                changed |= ptr.updateResultScope(scopeMap, returnValueScope);
                ptr = ptr.getInstNext();
            }

            if (block.getInstTail() instanceof Branch) {
                Branch branchInst = ((Branch) block.getInstTail());
                if (!visit.contains(branchInst.getThenBlock())) {
                    queue.offer(branchInst.getThenBlock());
                    visit.add(branchInst.getThenBlock());
                } else if (changed)
                    queue.offer(branchInst.getThenBlock());

                if (branchInst.isConditional()) {
                    if (!visit.contains(branchInst.getElseBlock())) {
                        queue.offer(branchInst.getElseBlock());
                        visit.add(branchInst.getElseBlock());
                    } else if (specialChecker(changed))
                        queue.offer(branchInst.getElseBlock());
                }
            }
        }
    }

    private void checkSideEffect() {
        sideEffect = new HashSet<>();
        Queue<Function> queue = new LinkedList<>();

        if (!ignoreIO) {
            for (Function externalFunction : module.getExternalFunctionMap().values()) {
                if (externalFunction.hasSideEffect()) {
                    sideEffect.add(externalFunction);
                    queue.offer(externalFunction);
                }
            }
        }
        for (Function function : module.getFunctionMap().values()) {
            boolean hasSideEffect = false;
            for (BasicBlock block : function.getBlocks()) {
                IRInstruction ptr = block.getInstHead();
                while (ptr != null) {
                    if (ptr instanceof Store && scopeMap.get(((Store) ptr).getPointer()) == Scope.outer) {
                        hasSideEffect = true;
                        break;
                    }
                    if (!ignoreLoad && ptr instanceof Load
                            && scopeMap.get(((Load) ptr).getPointer()) == Scope.outer) {
                        hasSideEffect = true;
                        break;
                    }
                    ptr = ptr.getInstNext();
                }
                if (specialChecker(hasSideEffect)) {
                    sideEffect.add(function);
                    queue.offer(function);
                    break;
                }
            }
        }

        while (!queue.isEmpty()) {
            Function function = queue.poll();
            for (IRInstruction callInst : function.getUse().keySet()) {
                Function caller = callInst.getBasicBlock().getFunction();
                if (!sideEffect.contains(caller)) {
                    sideEffect.add(caller);
                    queue.offer(caller);
                }
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
}
