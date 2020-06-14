package compiler.Optim.SSA;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.Instr.*;
import compiler.IR.IR;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Register;
import compiler.Optim.Pass;
import compiler.Utility.Tools;

import java.util.*;

public class SSAConstructor extends Pass {
    private ArrayList<Allocate> allocaInst;
    private Map<BasicBlock, Map<Allocate, Phi>> phiInstMap;
    private Map<Load, Allocate> useAlloca;
    private Map<Store, Allocate> defAlloca;
    private Map<BasicBlock, Map<Allocate, Operand>> renameTable;
    private Set<BasicBlock> visit;

    public SSAConstructor(IR module) {
        super(module);
    }

    @Override
    public void run() {
        for (Function function : module.getFunctionMap().values()) {
            if (function.isNotFunctional())
                return;
            else{
                allocaInst = function.getAllocaInstructions();
                phiInstMap = new HashMap<>();
                useAlloca = new HashMap<>();
                defAlloca = new HashMap<>();
                renameTable = new HashMap<>();
                constructSSA(function);
            }
        }
    }

    private void constructSSA(Function function) {

        for (BasicBlock block : function.getBlocks()) {
            phiInstMap.put(block, new HashMap<>());
            renameTable.put(block, new HashMap<>());
        }

        for (Allocate alloca : allocaInst) {
            ArrayList<Store> defs = new ArrayList<>();
            for (IRInstruction useInst : alloca.getResult().getUse().keySet()) {
                if (useInst instanceof Load)
                    useAlloca.put((Load) useInst, alloca);
                else {
                    defs.add((Store) useInst);
                    defAlloca.put((Store) useInst, alloca);
                }
            }

            Queue<BasicBlock> queue = new LinkedList<>();
            HashSet<BasicBlock> visitSet = new HashSet<>();
            HashSet<BasicBlock> phiSet = new HashSet<>();
            for (Store def : defs) {
                queue.offer(def.getBasicBlock());
                visitSet.add(def.getBasicBlock());
            }
            while (!queue.isEmpty()) {
                BasicBlock block = queue.poll();
                for (BasicBlock df : block.getDF()) {
                    if (!phiSet.contains(df)) {
                        addPhi(df, alloca);
                        phiSet.add(df);
                        if (!visitSet.contains(df)) {
                            queue.offer(df);
                            visitSet.add(df);
                        }
                    }
                }
            }
            alloca.removeFromBlock();
        }
        loadInstElimination(function);

        visit = new HashSet<>();
        rename(function.getEntranceBlock(), null);
    }

    private void addPhi(BasicBlock block, Allocate alloca) {
        String name = alloca.getResult().getName().split("\\$")[0];
        Register result = new Register(alloca.getType(), name);
        phiInstMap.get(block).put(alloca, new Phi(block, new LinkedHashSet<>(), result));
        block.getFunction().getSymbolTable().put(result.getName(), result);
    }

    private void loadInstElimination(Function function) {
        for (BasicBlock block : function.getBlocks()) {
            ArrayList<IRInstruction> instructions = block.getInstructions();
            for (IRInstruction instruction : instructions) {
                if (instruction instanceof Load && instruction.getResult().getUse().isEmpty())
                    instruction.removeFromBlock();
            }
        }
    }

    private void rename(BasicBlock block, BasicBlock predecessor) {
        Map<Allocate, Phi> map = phiInstMap.get(block);
        for (Allocate alloca : map.keySet()) {
            Phi phiInst = map.get(alloca);
            Operand value;
            if (!renameTable.get(predecessor).containsKey(alloca)
                    || renameTable.get(predecessor).get(alloca) == null) {
                value = alloca.getType().getDefaultValue();
            } else
                value = renameTable.get(predecessor).get(alloca);
            phiInst.addBranch(value, predecessor);
        }
        if (predecessor != null) {
            for (Allocate alloca : allocaInst) {
                if (!map.containsKey(alloca))
                    renameTable.get(block).put(alloca, renameTable.get(predecessor).get(alloca));
            }
        }

        if (specialChecker(block))
            return;
        visit.add(block);

        for (Allocate alloca : map.keySet())
            renameTable.get(block).put(alloca, map.get(alloca).getResult());

        ArrayList<IRInstruction> instructions = block.getInstructions();
        for (IRInstruction instruction : instructions) {
            if (instruction instanceof Load && useAlloca.containsKey(instruction)) {
                Allocate alloca = useAlloca.get(instruction);
                assert renameTable.containsKey(block);
                assert renameTable.get(block).containsKey(alloca);
                Operand value = renameTable.get(block).get(alloca);
                instruction.getResult().replaceUse(value);
                instruction.removeFromBlock();
            } else if (instruction instanceof Store && defAlloca.containsKey(instruction)) {
                Allocate alloca = defAlloca.get(instruction);
                if (!renameTable.get(block).containsKey(alloca))
                    renameTable.get(block).put(alloca, ((Store) instruction).getValue());
                else
                    renameTable.get(block).replace(alloca, ((Store) instruction).getValue());
                instruction.removeFromBlock();
            }
        }

        for (BasicBlock successor : block.getSuccessors())
            rename(successor, block);

        for (Phi phiInst : map.values())
            block.addInstructionAtFront(phiInst);
    }

    private boolean specialChecker(BasicBlock block){
        Tools speciialChecker = new Tools();
        int n=speciialChecker.speciialAnalyse(visit.contains(block));
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
