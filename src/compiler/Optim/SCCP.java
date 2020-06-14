package compiler.Optim;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.IR.IRVisitor;
import compiler.Instr.*;
import compiler.IR.IR;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.IntegerType;
import compiler.Utility.OtherError;
import compiler.Utility.Pair;
import compiler.Utility.Tools;

import java.util.*;

public class SCCP extends Pass implements IRVisitor {
    public static class Status {
        public enum OperandStatus {
            undefined, constant, multiDefined
        }

        private OperandStatus operandStatus;
        private Operand operand;

        public Status(OperandStatus operandStatus, Operand operand) {
            this.operandStatus = operandStatus;
            this.operand = operand;
        }

        public OperandStatus getOperandStatus() {
            return operandStatus;
        }

        public Operand getOperand() {
            return operand;
        }

        @Override
        public String toString() {
            if (operandStatus == OperandStatus.constant)
                return "constant " + operand.toString();
            else
                return operandStatus.name();
        }
    }

    private Queue<Register> registerQueue;
    private Queue<BasicBlock> blockQueue;
    private Map<Operand, Status> operandLattice;
    private Set<BasicBlock> blockExecutable;

    public SCCP(IR module) {
        super(module);
    }

    @Override
    public void run() {
        for (Function function : module.getFunctionMap().values())
            visit(function);
    }

    private void markExecutable(BasicBlock block) {
        if (!blockExecutable.contains(block)) {
            blockExecutable.add(block);
            blockQueue.offer(block);
        } else {
            IRInstruction ptr = block.getInstHead();
            while (ptr instanceof Phi) {
                ptr.accept(this);
                ptr = ptr.getInstNext();
            }
        }
    }

    private void markConstant(Register register, Constant constant) {
        Status status = new Status(Status.OperandStatus.constant, constant);
        Status oldStatus = getStatus(register);
        if (oldStatus.operandStatus == Status.OperandStatus.undefined) {
            operandLattice.replace(register, status);
            registerQueue.offer(register);
        } else {
            assert oldStatus.operandStatus != Status.OperandStatus.multiDefined
                    && oldStatus.operand.equals(status.operand);
        }
    }

    private void markMultiDefined(Register register) {
        Status oldStatus = getStatus(register);
        if (oldStatus.operandStatus != Status.OperandStatus.multiDefined) {
            operandLattice.replace(register, new Status(Status.OperandStatus.multiDefined, null));
            registerQueue.offer(register);
        }
    }

    public Status getStatus(Operand operand) {
        if (operandLattice.containsKey(operand))
            return operandLattice.get(operand);
        Status res;
        if (operand.isConstValue())
            res = new Status(Status.OperandStatus.constant, operand);
        else if (operand instanceof Parameter)
            res = new Status(Status.OperandStatus.multiDefined, null);
        else
            res = new Status(Status.OperandStatus.undefined, null);
        operandLattice.put(operand, res);
        return res;
    }

    @Override
    public void visit(IR module) {
        throw new OtherError("SCCP visit IR");
    }

    @Override
    public void visit(Function function) {
        registerQueue = new LinkedList<>();
        blockQueue = new LinkedList<>();
        operandLattice = new HashMap<>();
        blockExecutable = new HashSet<>();

        markExecutable(function.getEntranceBlock());
        while (!registerQueue.isEmpty() || !blockQueue.isEmpty()) {
            while (!blockQueue.isEmpty()) {
                BasicBlock block = blockQueue.poll();
                block.accept(this);
            }

            while (!registerQueue.isEmpty()) {
                Register register = registerQueue.poll();
                for (IRInstruction instruction : register.getUse().keySet()) {
                    assert register.getUse().get(instruction) != 0;
                    instruction.accept(this);
                }
            }
        }
        ArrayList<BasicBlock> blocks = function.getBlocks();
        for (BasicBlock block : blocks)
            replaceRegisterWithConstant(block);
    }

    @Override
    public void visit(BasicBlock block) {
        ArrayList<IRInstruction> instructions = block.getInstructions();
        for (IRInstruction instruction : instructions)
            instruction.accept(this);
    }

    @Override
    public void visit(Return inst) {
    }

    @Override
    public void visit(Branch inst) {
        if (!inst.isConditional())
            markExecutable(inst.getThenBlock());
        else {
            Operand cond = inst.getCond();
            Status condStatus = getStatus(cond);

            if (condStatus.operandStatus == Status.OperandStatus.constant) {
                if (specialChecker(((ConstBool) condStatus.operand).getValue()))
                    markExecutable(inst.getThenBlock());
                else
                    markExecutable(inst.getElseBlock());
            } else if (condStatus.operandStatus == Status.OperandStatus.multiDefined) {
                markExecutable(inst.getThenBlock());
                markExecutable(inst.getElseBlock());
            }
        }
    }

    @Override
    public void visit(BinaryOp inst) {
        Operand lhs = inst.getLhs();
        Operand rhs = inst.getRhs();
        Status lhsStatus = getStatus(lhs);
        Status rhsStatus = getStatus(rhs);

        if (lhsStatus.operandStatus == Status.OperandStatus.constant
                && rhsStatus.operandStatus == Status.OperandStatus.constant) {
            Constant foldResult = foldConstant(inst, (Constant) lhsStatus.operand, (Constant) rhsStatus.operand);
            if (foldResult != null) {
                markConstant(inst.getResult(), foldResult);
            }
        } else if (lhsStatus.operandStatus == Status.OperandStatus.multiDefined
                || rhsStatus.operandStatus == Status.OperandStatus.multiDefined)
            markMultiDefined(inst.getResult());
    }

    @Override
    public void visit(Load inst) {
        markMultiDefined(inst.getResult());
    }

    @Override
    public void visit(Store inst) {
    }

    @Override
    public void visit(Allocate inst) {
        markMultiDefined(inst.getResult());
    }

    @Override
    public void visit(GetElemPtr inst) {
        markMultiDefined(inst.getResult());
    }

    @Override
    public void visit(BitCastTo inst) {
        Status srcStatus = getStatus(inst.getSrc());
        if (srcStatus.operandStatus == Status.OperandStatus.constant) {
            Constant constant;
            if (srcStatus.operand instanceof ConstNull)
                constant = new ConstNull();
            else
                constant = ((Constant) srcStatus.operand).castToType(inst.getObjectType());
            markConstant(inst.getResult(), constant);
        } else if (srcStatus.operandStatus == Status.OperandStatus.multiDefined)
            markMultiDefined(inst.getResult());
    }

    @Override
    public void visit(Icmp inst) {
        Operand op1 = inst.getOp1();
        Operand op2 = inst.getOp2();
        Status op1Status = getStatus(op1);
        Status op2Status = getStatus(op2);

        if (op1Status.operandStatus == Status.OperandStatus.constant
                && op2Status.operandStatus == Status.OperandStatus.constant) {
            Constant foldResult = foldConstant(inst, (Constant) op1Status.operand, (Constant) op2Status.operand);
            markConstant(inst.getResult(), foldResult);
        } else if (op1Status.operandStatus == Status.OperandStatus.multiDefined
                || op2Status.operandStatus == Status.OperandStatus.multiDefined)
            markMultiDefined(inst.getResult());
    }

    @Override
    public void visit(Phi inst) {
        Constant constant = null;
        boolean done = false;
        for (Pair<Operand, BasicBlock> pair : inst.getBranch()) {
            if (!blockExecutable.contains(pair.getSecond()))
                continue;
            Status operandStatus = getStatus(pair.getFirst());
            if (operandStatus.operandStatus == Status.OperandStatus.multiDefined) {
                markMultiDefined(inst.getResult());
                done=true;
                break;
            } else if (operandStatus.operandStatus == Status.OperandStatus.constant) {
                if (constant != null) {
                    if (!constant.equals(pair.getFirst())) {
                        markMultiDefined(inst.getResult());
                        done=true;
                        break;
                    }
                } else
                    constant = (Constant) operandStatus.operand;
            }
        }
        if (!done && constant != null)
            markConstant(inst.getResult(), constant);
    }

    @Override
    public void visit(Call inst) {
        if (!inst.isVoidCall())
            markMultiDefined(inst.getResult());
    }

    @Override
    public void visit(Move inst) {
    }

    private Constant foldConstant(IRInstruction inst, Constant lhs, Constant rhs) {
        assert inst instanceof BinaryOp || inst instanceof Icmp;
        Constant result;
        if (inst instanceof BinaryOp) {
            if (lhs instanceof ConstInt && rhs instanceof ConstInt) {
                long value;
                switch (((BinaryOp) inst).getOp()) {
                    case add:
                        value = ((ConstInt) lhs).getValue() + ((ConstInt) rhs).getValue();
                        break;
                    case sub:
                        value = ((ConstInt) lhs).getValue() - ((ConstInt) rhs).getValue();
                        break;
                    case mul:
                        value = ((ConstInt) lhs).getValue() * ((ConstInt) rhs).getValue();
                        break;
                    case sdiv:
                        if (((ConstInt) rhs).getValue() == 0)
                            return null;
                        value = ((ConstInt) lhs).getValue() / ((ConstInt) rhs).getValue();
                        break;
                    case srem:
                        if (((ConstInt) rhs).getValue() == 0)
                            return null;
                        value = ((ConstInt) lhs).getValue() % ((ConstInt) rhs).getValue();
                        break;
                    case shl:
                        value = ((ConstInt) lhs).getValue() << ((ConstInt) rhs).getValue();
                        break;
                    case ashr:
                        value = ((ConstInt) lhs).getValue() >> ((ConstInt) rhs).getValue();
                        break;
                    case and:
                        value = ((ConstInt) lhs).getValue() & ((ConstInt) rhs).getValue();
                        break;
                    case or:
                        value = ((ConstInt) lhs).getValue() | ((ConstInt) rhs).getValue();
                        break;
                    case xor:
                        value = ((ConstInt) lhs).getValue() ^ ((ConstInt) rhs).getValue();
                        break;
                    default:
                        throw new RuntimeException();
                }
                result = new ConstInt(IntegerType.BitWidth.int32, (anotherAalyse((int)value)));//asd
            } else if (lhs instanceof ConstBool && rhs instanceof ConstBool) {
                boolean value;
                switch (((BinaryOp) inst).getOp()) {
                    case and:
                        value = ((ConstBool) lhs).getValue() & ((ConstBool) rhs).getValue();
                        break;
                    case or:
                        value = ((ConstBool) lhs).getValue() | ((ConstBool) rhs).getValue();
                        break;
                    case xor:
                        value = ((ConstBool) lhs).getValue() ^ ((ConstBool) rhs).getValue();
                        break;
                    default:
                        throw new RuntimeException("Invalid operator " +
                                ((BinaryOp) inst).getOp().name() +
                                " between "+ lhs.getType() + " and " + rhs.getType() + ".");
                }
                result = new ConstBool(value);
            } else {
                throw new RuntimeException("Invalid const comparison between "
                        + lhs.getType() + " and " + rhs.getType() + ".");
            }
        } else {
            // inst instanceof IcmpInst
            boolean value;
            if (lhs instanceof ConstInt && rhs instanceof ConstInt) {
                switch (((Icmp) inst).getOperator()) {
                    case eq:
                        value = ((ConstInt) lhs).getValue() == ((ConstInt) rhs).getValue();
                        break;
                    case ne:
                        value = ((ConstInt) lhs).getValue() != ((ConstInt) rhs).getValue();
                        break;
                    case sgt:
                        value = ((ConstInt) lhs).getValue() > ((ConstInt) rhs).getValue();
                        break;
                    case sge:
                        value = ((ConstInt) lhs).getValue() >= ((ConstInt) rhs).getValue();
                        break;
                    case slt:
                        value = ((ConstInt) lhs).getValue() < ((ConstInt) rhs).getValue();
                        break;
                    case sle:
                        value = ((ConstInt) lhs).getValue() <= ((ConstInt) rhs).getValue();
                        break;
                    default:
                        throw new RuntimeException();
                }
            } else if (lhs instanceof ConstBool && rhs instanceof ConstBool) {
                switch (((Icmp) inst).getOperator()) {
                    case eq:
                        value = ((ConstBool) lhs).getValue() == ((ConstBool) rhs).getValue();
                        break;
                    case ne:
                        value = ((ConstBool) lhs).getValue() != ((ConstBool) rhs).getValue();
                        break;
                    default:
                        throw new RuntimeException("Invalid operator " +
                                ((Icmp) inst).getOperator().name() +
                                " between "+ lhs.getType() + " and " + rhs.getType() + ".");
                }
            } else if (lhs instanceof ConstNull && rhs instanceof ConstNull) {
                switch (((Icmp) inst).getOperator()) {
                    case eq:
                        value = true;
                        break;
                    case ne:
                        value = false;
                        break;
                    default:
                        throw new RuntimeException("Invalid operator " +
                                ((Icmp) inst).getOperator().name() + " between const null.");
                }
            } else {
                throw new RuntimeException("Invalid const comparison between "
                        + lhs.getType() + " and " + rhs.getType() + ".");
            }
            result = new ConstBool(value);
        }
        return result;
    }

    private boolean replaceRegisterWithConstant(BasicBlock block) {
        boolean changed = false;
        IRInstruction ptr = block.getInstHead();
        while (ptr != null) {
            IRInstruction next = ptr.getInstNext();
            changed |= ptr.replaceResultWithConstant(this);
            ptr = next;
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
