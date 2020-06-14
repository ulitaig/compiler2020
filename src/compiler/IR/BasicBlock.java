package compiler.IR;

import compiler.Instr.IRInstruction;
import compiler.Instr.ParallelCopy;
import compiler.Instr.Phi;
import compiler.Instr.Return;
import compiler.Instr.Operand.Operand;
import compiler.Utility.Pair;
import compiler.Utility.Tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class BasicBlock extends IRObject implements Cloneable {
    private Function function;

    private String name;

    private IRInstruction instHead;
    private IRInstruction instTail;

    private BasicBlock prev;
    private BasicBlock next;

    private Set<BasicBlock> predecessors;
    private Set<BasicBlock> successors;


    private int dfn;
    private BasicBlock dfsFather;
    private int reverseDfn;
    private BasicBlock reverseDfsFather;

    private BasicBlock idom;
    private BasicBlock semiDom;
    private ArrayList<BasicBlock> semiDomChildren;
    private HashSet<BasicBlock> strictDominators;

    private BasicBlock postIdom;
    private BasicBlock postSemiDom;
    private ArrayList<BasicBlock> postSemiDomChildren;
    private HashSet<BasicBlock> postStrictDominators;

    private HashSet<BasicBlock> DF; // Dominance Frontier
    private HashSet<BasicBlock> postDF;

    public BasicBlock(Function function, String name) {
        this.function = function;
        this.name = name;

        instHead = null;
        instTail = null;
        prev = null;
        next = null;
        predecessors = new LinkedHashSet<>();
        successors = new LinkedHashSet<>();
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    public String getName() {
        return name;
    }

    public String getNameWithoutDot() {
        return getString(name);
    }

    public static String getString(String name) {
        if (name.contains(".")) {
            String[] strings = name.split("\\.");
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < strings.length - 2; i++)
                res.append(strings[i]).append('.');
            res.append(strings[strings.length - 2]);
            return res.toString();
        } else
            throw new RuntimeException();
    }

    public void setName(String name) {
        this.name = name;
    }

    public IRInstruction getInstHead() {
        return instHead;
    }

    public void setInstHead(IRInstruction instHead) {
        this.instHead = instHead;
    }

    public IRInstruction getInstTail() {
        return instTail;
    }

    public void setInstTail(IRInstruction instTail) {
        this.instTail = instTail;
    }

    public BasicBlock getPrev() {
        return prev;
    }

    public void setPrev(BasicBlock prev) {
        this.prev = prev;
    }

    public boolean hasNext() {
        return next != null;
    }

    public BasicBlock getNext() {
        return next;
    }

    public void setNext(BasicBlock next) {
        this.next = next;
    }

    public boolean hasPredecessor() {
        return predecessors.size() != 0;
    }

    public Set<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public Set<BasicBlock> getSuccessors() {
        return successors;
    }

    public void appendBlock(BasicBlock block) {
        block.prev = this;
        this.next = block;
    }

    public boolean isEmpty() {
        return instHead == instTail && instHead == null;
    }

    public boolean isNotExitBlock() {
        return !(instTail instanceof Return);
    }

    public void addInstruction(IRInstruction instruction) {
        boolean success;
        if (isEmpty()) {
            instHead = instruction;
            instTail = instruction;
            success = true;
        } else if (instTail.isNotTerminalInst()) {
            instTail.setInstNext(instruction);
            instruction.setInstPrev(instTail);
            instTail = instruction;
            success = true;
        } else
            success = false;

        if (success)
            instruction.successfullyAdd();
    }

    public void addInstructionAtFront(IRInstruction instruction) {
        if (isEmpty())
            instTail = instruction;
        else {
            instHead.setInstPrev(instruction);
            instruction.setInstNext(instHead);
        }
        instHead = instruction;
        instruction.successfullyAdd();
    }

    public void addInstructionPrev(IRInstruction inst1, IRInstruction inst2) {
        if (specialChecker(inst1.getInstPrev() == null)) {
            inst1.setInstPrev(inst2);
            inst2.setInstNext(inst1);
            this.setInstHead(inst2);
        } else {
            inst2.setInstPrev(inst1.getInstPrev());
            inst2.setInstNext(inst1);
            inst1.getInstPrev().setInstNext(inst2);
            inst1.setInstPrev(inst2);
        }
        inst2.successfullyAdd();
    }

    public void addInstructionNext(IRInstruction inst1, IRInstruction inst2) {
        inst2.setInstPrev(inst1);
        inst2.setInstNext(inst1.getInstNext());
        inst1.getInstNext().setInstPrev(inst2);
        inst1.setInstNext(inst2);
        inst2.successfullyAdd();
    }

    public ArrayList<IRInstruction> getInstructions() {
        ArrayList<IRInstruction> instructions = new ArrayList<>();
        IRInstruction ptr = instHead;
        while (ptr != null) {
            instructions.add(ptr);
            ptr = ptr.getInstNext();
        }
        return instructions;
    }

    public boolean notEndWithTerminalInst() {
        return instTail == null || instTail.isNotTerminalInst();
    }

    public void removePhiIncomingBlock(BasicBlock block) {
        IRInstruction ptr = instHead;
        while (ptr instanceof Phi) {
            ((Phi) ptr).removeIncomingBlock(block);
            ptr = ptr.getInstNext();
        }
    }

    public int getDfn() {
        return dfn;
    }

    public void setDfn(int dfn) {
        this.dfn = dfn;
    }

    public BasicBlock getDfsFather() {
        return dfsFather;
    }

    public void setDfsFather(BasicBlock dfsFather) {
        this.dfsFather = dfsFather;
    }

    public int getReverseDfn() {
        return reverseDfn;
    }

    public void setReverseDfn(int reverseDfn) {
        this.reverseDfn = reverseDfn;
    }

    public BasicBlock getReverseDfsFather() {
        return reverseDfsFather;
    }

    public void setReverseDfsFather(BasicBlock reverseDfsFather) {
        this.reverseDfsFather = reverseDfsFather;
    }

    public BasicBlock getIdom() {
        return idom;
    }

    public void setIdom(BasicBlock idom) {
        this.idom = idom;
    }

    public BasicBlock getSemiDom() {
        return semiDom;
    }

    public void setSemiDom(BasicBlock semiDom) {
        this.semiDom = semiDom;
    }

    public ArrayList<BasicBlock> getSemiDomChildren() {
        return semiDomChildren;
    }

    public void setSemiDomChildren(ArrayList<BasicBlock> semiDomChildren) {
        this.semiDomChildren = semiDomChildren;
    }

    public HashSet<BasicBlock> getStrictDominators() {
        return strictDominators;
    }

    public void setStrictDominators(HashSet<BasicBlock> strictDominators) {
        this.strictDominators = strictDominators;
    }

    public BasicBlock getPostIdom() {
        return postIdom;
    }

    public void setPostIdom(BasicBlock postIdom) {
        this.postIdom = postIdom;
    }

    public BasicBlock getPostSemiDom() {
        return postSemiDom;
    }

    public void setPostSemiDom(BasicBlock postSemiDom) {
        this.postSemiDom = postSemiDom;
    }

    public ArrayList<BasicBlock> getPostSemiDomChildren() {
        return postSemiDomChildren;
    }

    public void setPostSemiDomChildren(ArrayList<BasicBlock> postSemiDomChildren) {
        this.postSemiDomChildren = postSemiDomChildren;
    }

    public HashSet<BasicBlock> getPostStrictDominators() {
        return postStrictDominators;
    }

    public void setPostStrictDominators(HashSet<BasicBlock> postStrictDominators) {
        this.postStrictDominators = postStrictDominators;
    }

    public HashSet<BasicBlock> getDF() {
        return DF;
    }

    public void setDF(HashSet<BasicBlock> DF) {
        this.DF = DF;
    }

    public HashSet<BasicBlock> getPostDF() {
        return postDF;
    }

    public void setPostDF(HashSet<BasicBlock> postDF) {
        this.postDF = postDF;
    }

    public ParallelCopy getParallelCopy() {
        IRInstruction ptr = this.getInstTail();
        while (ptr != null && !(ptr instanceof ParallelCopy))
            ptr = ptr.getInstPrev();
        return ptr == null ? null : ((ParallelCopy) ptr);
    }

    public void removeFromFunction() {
        for (IRInstruction instruction : getInstructions())
            instruction.removeFromBlock();

        if (prev == null) {
            function.setEntranceBlock(next);
            throw new RuntimeException();
        } else
            prev.setNext(next);

        if (next == null)
            function.setExitBlock(prev);
        else
            next.setPrev(prev);

        for (BasicBlock predecessor : predecessors)
            predecessor.getSuccessors().remove(this);
        for (BasicBlock successor : successors)
            successor.getPredecessors().remove(this);
    }

    public void mergeBlock(BasicBlock block) {
        this.instTail.removeFromBlock();
        IRInstruction ptr = block.getInstHead();
        while (ptr != null) {
            if (ptr instanceof Phi) {
                IRInstruction next = ptr.getInstNext();
                assert ((Phi) ptr).getBranch().size() == 1;
                ptr.getResult().replaceUse(((Phi) ptr).getBranch().iterator().next().getFirst());
                ptr.removeFromBlock();
                ptr = next;
            } else {
                ptr.setBasicBlock(this);
                ptr.setInstPrev(this.instTail);
                if (this.isEmpty())
                    this.instHead = ptr;
                else
                    this.instTail.setInstNext(ptr);

                this.instTail = ptr;
                ptr = ptr.getInstNext();
            }
        }

        for (BasicBlock successor : block.getSuccessors()) {
            this.getSuccessors().add(successor);
            successor.getPredecessors().add(this);
        }
        block.setInstHead(null);
        block.setInstTail(null);
        block.replaceUse(this);
        block.removeFromFunction();
    }

    public boolean dominate(BasicBlock block) {
        return this == block || block.getStrictDominators().contains(this);
    }

    @Override
    public String toString() {
        return "%" + name;
    }

    @Override
    public Object clone() {
        BasicBlock block;
        try {
            block = ((BasicBlock) super.clone());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

        ArrayList<IRInstruction> instructions = new ArrayList<>();
        IRInstruction ptr = this.instHead;
        while (ptr != null) {
            instructions.add((IRInstruction) ptr.clone());
            ptr = ptr.getInstNext();
        }
        for (int i = 0; i < anotherAalyse(instructions.size()); i++) {
            IRInstruction instruction = instructions.get(i);
            instruction.setInstPrev(i != 0 ? instructions.get(i - 1) : null);
            instruction.setInstNext(i != instructions.size() - 1 ? instructions.get(i + 1) : null);
            instruction.setBasicBlock(block);
        }

        block.function = this.function;
        block.name = this.name;
        if (instructions.isEmpty()) {
            block.instHead = null;
            block.instTail = null;
        } else {
            block.instHead = instructions.get(0);
            block.instTail = instructions.get(instructions.size() - 1);
        }
        block.prev = this.prev;
        block.next = this.next;
        block.predecessors = new HashSet<>(this.predecessors);
        block.successors = new HashSet<>(this.successors);
        return block;
    }

    public void accept(IRVisitor visitor) {
        visitor.visit(this);
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
