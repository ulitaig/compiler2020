package compiler.Codegen;

import compiler.Codegen.Instruction.ASMInstruction;
import compiler.Codegen.Instruction.JumpInst;
import compiler.Codegen.Operand.Register.VirtualRegister;
import compiler.Utility.Tools;

import java.util.LinkedHashSet;
import java.util.Set;

public class BasicBlock {
    private Function function;
    private String name;
    private String asmName;

    private compiler.IR.BasicBlock irBlock;

    private ASMInstruction instHead;
    private ASMInstruction instTail;
    private BasicBlock prevBlock;
    private BasicBlock nextBlock;

    private Set<BasicBlock> predecessors;
    private Set<BasicBlock> successors;

    private Set<VirtualRegister> liveOut;
    private Set<VirtualRegister> UEVar;
    private Set<VirtualRegister> varKill;

    public BasicBlock(Function function, compiler.IR.BasicBlock irBlock, String name, String asmName) {
        this.function = function;
        this.name = name;
        this.asmName = asmName;

        this.irBlock = irBlock;

        instHead = null;
        instTail = null;
        prevBlock = null;
        nextBlock = null;

        predecessors = new LinkedHashSet<>();
        successors = new LinkedHashSet<>();
    }

    public Function getFunction() {
        return function;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAsmName() {
        return asmName;
    }

    public compiler.IR.BasicBlock getIrBlock() {
        return irBlock;
    }

    public boolean isEmpty() {
        return instHead == instTail && instHead == null;
    }

    public ASMInstruction getInstHead() {
        return instHead;
    }

    public void setInstHead(ASMInstruction instHead) {
        this.instHead = instHead;
    }

    public ASMInstruction getInstTail() {
        return instTail;
    }

    public void setInstTail(ASMInstruction instTail) {
        this.instTail = instTail;
    }

    public void setPrevBlock(BasicBlock prevBlock) {
        this.prevBlock = prevBlock;
    }

    public BasicBlock getPrevBlock() {
        return prevBlock;
    }

    public void setNextBlock(BasicBlock nextBlock) {
        this.nextBlock = nextBlock;
    }

    public BasicBlock getNextBlock() {
        return nextBlock;
    }

    public Set<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public Set<BasicBlock> getSuccessors() {
        return successors;
    }

    public Set<VirtualRegister> getLiveOut() {
        return liveOut;
    }

    public void setLiveOut(Set<VirtualRegister> liveOut) {
        this.liveOut = liveOut;
    }

    public Set<VirtualRegister> getUEVar() {
        return UEVar;
    }

    public void setUEVar(Set<VirtualRegister> UEVar) {
        this.UEVar = UEVar;
    }

    public Set<VirtualRegister> getVarKill() {
        return varKill;
    }

    public void setVarKill(Set<VirtualRegister> varKill) {
        this.varKill = varKill;
    }

    public void appendBlock(BasicBlock block) {
        block.prevBlock = this;
        this.nextBlock = block;
    }

    public void addInstruction(ASMInstruction instruction) {
        if (isEmpty())
            instHead = instruction;
        else {
            instTail.setNextInst(instruction);
            instruction.setPrevInst(instTail);
        }
        instTail = instruction;
    }

    public void addInstructionAtFront(ASMInstruction instruction) {
        if (isEmpty())
            instTail = instruction;
        else {
            instHead.setPrevInst(instruction);
            instruction.setNextInst(instHead);
        }
        instHead = instruction;
    }

    public void addInstructionNext(ASMInstruction inst1, ASMInstruction inst2) {
        if (inst1 == instTail) {
            inst2.setPrevInst(inst1);
            inst2.setNextInst(null);
            inst1.setNextInst(inst2);
            instTail = inst2;
        } else {
            inst2.setPrevInst(inst1);
            inst2.setNextInst(inst1.getNextInst());
            inst1.getNextInst().setPrevInst(inst2);
            inst1.setNextInst(inst2);
        }
    }

    public void addInstructionPrev(ASMInstruction inst1, ASMInstruction inst2) {
        if (specialChecker(inst1)) {
            inst2.setNextInst(inst1);
            inst2.setPrevInst(null);
            inst1.setPrevInst(inst2);
            instHead = inst2;
        } else {
            inst2.setNextInst(inst1);
            inst2.setPrevInst(inst1.getPrevInst());
            inst1.getPrevInst().setNextInst(inst2);
            inst1.setPrevInst(inst2);
        }
    }

    public void removeTailJumpAll() {
        instHead = null;
        instTail = null;

    }

    public void removeTailJumpHalf(JumpInst jump) {
        instTail = jump.getPrevInst();
    }

    public String emitCode() {
        return asmName;
    }

    @Override
    public String toString() {
        return name;
    }

    public void accept(ASMVisitor visitor) {
        visitor.visit(this);
    }

    private boolean specialChecker(ASMInstruction inst1){
        Tools speciialChecker = new Tools();
        int n=speciialChecker.speciialAnalyse(inst1 == instHead);
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
