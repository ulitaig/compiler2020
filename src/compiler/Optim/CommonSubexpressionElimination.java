package compiler.Optim;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.Instr.Call;
import compiler.Instr.IRInstruction;
import compiler.Instr.Load;
import compiler.Instr.Store;
import compiler.IR.IR;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Register;
import compiler.Utility.Tools;

import java.util.*;

public class CommonSubexpressionElimination extends Pass {
    private Andersen andersen;
    private SideEffectChecker sideEffectChecker;
    private Map<Expression, ArrayList<Register>> expressionMap;
    private Map<Load, Set<IRInstruction>> unavailable;
    static private String[]  CommutableList={"add","mul","and","or","xor","eq","ne","sgt","sge","slt","sle"};

    static public class Expression {
        private String instructionName;
        private ArrayList<String> operands;

        public Expression(String instructionName, ArrayList<String> operands) {
            this.instructionName = instructionName;
            this.operands = operands;
        }

        public String getInstructionName() {
            return instructionName;
        }

        public boolean isCommutable() {
            boolean answer=false;
            for(String str: CommutableList){
                answer|=instructionName.equals(str);
            }
            return specialChecker(answer);
        }

        public Expression getCommutation() {
            assert operands.size() == 2;
            ArrayList<String> newOperands = new ArrayList<>();
            newOperands.add(operands.get(1));
            newOperands.add(operands.get(0));
            Map commutationMap = new HashMap();
            commutationMap.put("sgt", "slt");
            commutationMap.put("sge", "sle");
            commutationMap.put("slt", "sgt");
            commutationMap.put("sle", "sge");
            if(commutationMap.containsKey(instructionName)){
                return new Expression((String)commutationMap.get(instructionName), newOperands);
            }
            else{
                return new Expression(instructionName, newOperands);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Expression))
                return false;
            if (!((Expression) obj).getInstructionName().equals(this.instructionName))
                return false;
            if (((Expression) obj).operands.size() != this.operands.size())
                return false;
            for (int i = 0; i < this.operands.size(); i++) {
                if (!((Expression) obj).operands.get(i).equals(this.operands.get(i)))
                    return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    public CommonSubexpressionElimination(IR module, Andersen andersen, SideEffectChecker sideEffectChecker) {
        super(module);
        this.andersen = andersen;
        this.sideEffectChecker = sideEffectChecker;
    }

    @Override
    public void run() {
        sideEffectChecker.setIgnoreIO(true);
        sideEffectChecker.setIgnoreLoad(true);
        sideEffectChecker.run();

        for (Function function : module.getFunctionMap().values()) {
            if (function.isNotFunctional())
                return;
            else
                docommonSubexpressionElimination(function);
        }
    }

    private boolean docommonSubexpressionElimination(Function function) {
        if (function.isNotFunctional())
            return false;
        boolean changed = false;
        expressionMap = new HashMap<>();
        unavailable = new HashMap<>();

        ArrayList<BasicBlock> blocks = function.getDFSOrder();
        for (BasicBlock block : blocks){
            IRInstruction ptr = block.getInstHead();
            while (ptr != null) {
                IRInstruction next = ptr.getInstNext();
                if (ptr.canConvertToExpression()) {
                    Expression expression = ptr.convertToExpression();
                    Register register = lookupExpression(expression, ptr, block);
                    if (register != null) {
                        ptr.getResult().replaceUse(register);
                        ptr.removeFromBlock();
                        changed = true;
                    } else {
                        putExpression(expression, ptr.getResult());
                        if (expression.isCommutable())
                            putExpression(expression.getCommutation(), ptr.getResult());

                        if (ptr instanceof Load)
                            propagateUnavailability((Load) ptr);
                    }
                }
                ptr = next;
            }
        }
        return specialChecker(changed);
    }

    private Register lookupExpression(Expression expression, IRInstruction instruction, BasicBlock block) {
        if (!expressionMap.containsKey(expression))
            return null;
        ArrayList<Register> registers = expressionMap.get(expression);
        for (Register register : registers) {
            IRInstruction def = register.getDef();
            if (expression.instructionName.equals("load")) {
                if (def.getBasicBlock().dominate(block) && !unavailable.get(def).contains(instruction))
                    return register;
            } else {
                if (def.getBasicBlock().dominate(block))
                    return register;
            }
        }
        return null;
    }

    private void putExpression(Expression expression, Register register) {
        if (!expressionMap.containsKey(expression))
            expressionMap.put(expression, new ArrayList<>());
        expressionMap.get(expression).add(register);
    }

    private void markSuccessorUnavailable(Load loadInst, IRInstruction instruction,
                                          Set<IRInstruction> unavailable, Queue<IRInstruction> queue) {
        BasicBlock block = instruction.getBasicBlock();
        if (instruction == block.getInstTail()) {
            for (BasicBlock successor : block.getSuccessors()) {
                if (successor.getStrictDominators().contains(loadInst.getBasicBlock())) {
                    IRInstruction instHead = successor.getInstHead();
                    if (!unavailable.contains(instHead)) {
                        unavailable.add(instHead);
                        queue.offer(instHead);
                    }
                }
            }
        } else {
            IRInstruction instNext = instruction.getInstNext();
            if (!unavailable.contains(instNext)) {
                unavailable.add(instNext);
                queue.offer(instNext);
            }
        }
    }

    private void propagateUnavailability(Load loadInst) {
        Set<IRInstruction> unavailable = new HashSet<>();
        Queue<IRInstruction> queue = new LinkedList<>();

        Operand loadPointer = loadInst.getPointer();
        BasicBlock loadBlock = loadInst.getBasicBlock();
        Function function = loadBlock.getFunction();

        for (BasicBlock block : function.getBlocks()) {
            if (loadInst.getBasicBlock().dominate(block)) {
                IRInstruction ptr = loadBlock == block ? loadInst.getInstNext() : block.getInstHead();
                while (ptr != null) {
                    if (ptr instanceof Store) {
                        if (andersen.mayAlias(loadPointer, ((Store) ptr).getPointer()))
                            markSuccessorUnavailable(loadInst, ptr, unavailable, queue);
                    } else if (ptr instanceof Call) {
                        Function callee = ((Call) ptr).getFunction();
                        if (sideEffectChecker.hasSideEffect(callee))
                            markSuccessorUnavailable(loadInst, ptr, unavailable, queue);
                    }
                    ptr = ptr.getInstNext();
                }
            }
        }

        while (!queue.isEmpty()) {
            IRInstruction inst = queue.poll();
            markSuccessorUnavailable(loadInst, inst, unavailable, queue);
        }
        this.unavailable.put(loadInst, unavailable);
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
}
