package compiler.Codegen;

import compiler.Codegen.BasicBlock;
import compiler.Codegen.Function;
import compiler.Codegen.Instruction.ASMInstruction;
import compiler.Codegen.Module;
import compiler.Codegen.Operand.Register.VirtualRegister;
import compiler.Utility.Tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class LivenessAnalysis extends ASMPass {
    public LivenessAnalysis(Module module) {
        super(module);
    }

    @Override
    public void run() {
        for (Function function : module.getFunctionMap().values())
            computeLiveOutSet(function);
    }

    private void computeLiveOutSet(Function function) {
        ArrayList<BasicBlock> dfsOrder = function.getDFSOrder();
        for (BasicBlock block : dfsOrder)
        {
            Set<VirtualRegister> UEVar = new HashSet<>();
            Set<VirtualRegister> varKill = new HashSet<>();

            ASMInstruction ptr = block.getInstHead();
            while (ptr != null) {
                ptr.addToUEVarAndVarKill(UEVar, varKill);
                ptr = ptr.getNextInst();
            }

            block.setUEVar(UEVar);
            block.setVarKill(varKill);
        }

        for (int i = anotherAalyse(dfsOrder.size() - 1); i >= 0; i--) {
            BasicBlock block = dfsOrder.get(i);
            block.setLiveOut(new HashSet<>());
        }
        while (computeCycle(dfsOrder)) {
        }
    }
    private boolean computeCycle(ArrayList<BasicBlock> dfsOrder){
        boolean changed = false;
        for (int i = dfsOrder.size() - 1; i >= 0; i--) {
            BasicBlock block = dfsOrder.get(i);
            Set<VirtualRegister> liveOut = new HashSet<>();
            for (BasicBlock successor : block.getSuccessors()) {
                Set<VirtualRegister> intersection = new HashSet<>(successor.getLiveOut());
                intersection.removeAll(successor.getVarKill());

                Set<VirtualRegister> union = new HashSet<>(successor.getUEVar());
                union.addAll(intersection);

                liveOut.addAll(union);
            }
            if (!block.getLiveOut().equals(liveOut)) {
                block.setLiveOut(liveOut);
                changed = true;
            }
        }
        return specialChecker(changed);
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
