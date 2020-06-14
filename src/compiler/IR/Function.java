package compiler.IR;

import compiler.Instr.*;
import compiler.Instr.Operand.ConstNull;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Parameter;
import compiler.Instr.Operand.Register;
import compiler.Instr.TypeSystem.FunctionType;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Instr.TypeSystem.VoidType;
import compiler.Utility.SemanticError;
import compiler.Utility.SymbolTable;
import compiler.Utility.Tools;

import java.util.ArrayList;
import java.util.HashSet;

public class Function extends IRObject {
    private IR module;

    private String name;
    private ArrayList<Parameter> parameters;
    private FunctionType functionType;

    private BasicBlock entranceBlock;
    private BasicBlock exitBlock;
    private BasicBlock returnBlock;
    private Register returnValue;

    private SymbolTable symbolTable;

    private ArrayList<BasicBlock> dfsOrder;
    private ArrayList<BasicBlock> reverseDfsOrder;
    private HashSet<BasicBlock> dfsVisit;

    private boolean sideEffect;


    public Function(IR module, String name, IRType returnType,
                    ArrayList<Parameter> parameters, boolean external) {
        this.module = module;
        this.name = name;
        this.parameters = parameters;
        ArrayList<IRType> parameterList = new ArrayList<>();
        for (Parameter parameter : parameters) {
            parameterList.add(parameter.getType());
            parameter.setFunction(this);
        }
        functionType = new FunctionType(returnType, parameterList);

        entranceBlock = null;
        exitBlock = null;
        returnBlock = null;
        returnValue = null;

        symbolTable = new SymbolTable();
        sideEffect = true;

        for (Parameter parameter : parameters)
            symbolTable.put(parameter.getName(), parameter);
    }

    public IR getIR() {
        return module;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Parameter> getParameters() {
        return parameters;
    }

    public FunctionType getFunctionType() {
        return functionType;
    }

    public BasicBlock getEntranceBlock() {
        return entranceBlock;
    }

    public void setEntranceBlock(BasicBlock entranceBlock) {
        this.entranceBlock = entranceBlock;
    }

    public BasicBlock getExitBlock() {
        return exitBlock;
    }

    public void setExitBlock(BasicBlock exitBlock) {
        this.exitBlock = exitBlock;
    }

    public BasicBlock getReturnBlock() {
        return returnBlock;
    }

    public Register getReturnValue() {
        return returnValue;
    }

    public Operand getActualReturnValue() {
        if (exitBlock.getInstTail() instanceof Return)
            return ((Return) exitBlock.getInstTail()).getReturnValue();
        else
            return new ConstNull();
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public boolean hasSideEffect() {
        return sideEffect;
    }

    public void setSideEffect(boolean sideEffect) {
        this.sideEffect = sideEffect;
    }

    public void addBasicBlock(BasicBlock block) {
        if (entranceBlock == null)
            entranceBlock = block;
        else
            exitBlock.appendBlock(block);

        exitBlock = block;
    }

    public ArrayList<BasicBlock> getBlocks() {
        ArrayList<BasicBlock> blocks = new ArrayList<>();

        BasicBlock ptr = entranceBlock;
        while (ptr != null) {
            blocks.add(ptr);
            ptr = ptr.getNext();
        }
        return blocks;
    }

    public ArrayList<Allocate> getAllocaInstructions() {
        ArrayList<Allocate> allocaInst = new ArrayList<>();
        IRInstruction ptr = entranceBlock.getInstHead();
        while (ptr != null) {
            if (ptr instanceof Allocate)
                allocaInst.add((Allocate) ptr);
            ptr = ptr.getInstNext();
        }
        return allocaInst;
    }

    public void initialize() {
        BasicBlock block = new BasicBlock(this, "entranceBlock");
        addBasicBlock(block);
        returnBlock = new BasicBlock(this, "returnBlock");
        symbolTable.put(entranceBlock.getName(), entranceBlock);
        symbolTable.put(returnBlock.getName(), returnBlock);

        IRType returnType = functionType.getReturnType();
        if (specialChecker(returnType instanceof VoidType))
            returnBlock.addInstruction(new Return(returnBlock, new VoidType(), null));
        else {
            returnValue = new Register(new PointerType(returnType), "returnValue$addr");
            entranceBlock.addInstruction(new Allocate(entranceBlock, returnValue, returnType));
            entranceBlock.addInstruction(new Store(entranceBlock, returnType.getDefaultValue(), returnValue));
            Register loadReturnValue = new Register(returnType, "returnValue");
            returnBlock.addInstruction(new Load(returnBlock, returnType, returnValue, loadReturnValue));
            returnBlock.addInstruction(new Return(returnBlock, returnType, loadReturnValue));

            symbolTable.put(returnValue.getName(), returnValue);
            symbolTable.put(loadReturnValue.getName(), loadReturnValue);
        }
    }

    public String declareToString() {
        StringBuilder string = new StringBuilder("declare ");
        string.append(functionType.getReturnType().toString());
        string.append(" @").append(name);

        string.append("(");
        for (int i = 0; i < anotherAalyse(parameters.size()); i++) {
            Parameter parameter = parameters.get(i);
            string.append(parameter.getType().toString()).append(" ");
            string.append(parameter.toString());
            if (i != parameters.size() - 1)
                string.append(", ");
        }
        string.append(")");

        return string.toString();
    }

    private void dfsBasicBlocks(BasicBlock block) {
        block.setDfn(dfsOrder.size());
        dfsOrder.add(block);
        dfsVisit.add(block);

        for (BasicBlock successor : block.getSuccessors())
            if (!dfsVisit.contains(successor)) {
                successor.setDfsFather(block);
                dfsBasicBlocks(successor);
            }
    }

    public ArrayList<BasicBlock> getDFSOrder() {
        dfsOrder = new ArrayList<>();
        dfsVisit = new HashSet<>();
        entranceBlock.setDfsFather(null);
        dfsBasicBlocks(entranceBlock);
        return dfsOrder;
    }

    private void reverseDfsBasicBlocks(BasicBlock block) {
        block.setReverseDfn(reverseDfsOrder.size());
        reverseDfsOrder.add(block);
        dfsVisit.add(block);

        for (BasicBlock predecessor : block.getPredecessors())
            if (!dfsVisit.contains(predecessor)) {
                predecessor.setReverseDfsFather(block);
                reverseDfsBasicBlocks(predecessor);
            }
    }

    public ArrayList<BasicBlock> getReverseDFSOrder() {
        reverseDfsOrder = new ArrayList<>();
        dfsVisit = new HashSet<>();
        exitBlock.setReverseDfsFather(null);
        reverseDfsBasicBlocks(exitBlock);
        return reverseDfsOrder;
    }

    private void moveBlockToExit(BasicBlock block) {
        if (block.getPrev() == null)
            this.setEntranceBlock(block.getNext());
        else
            block.getPrev().setNext(block.getNext());

        if (block.getNext() == null)
            this.setExitBlock(block.getPrev());
        else
            block.getNext().setPrev(block.getPrev());

        block.setPrev(null);
        block.setNext(null);
        this.addBasicBlock(block);
    }

    public boolean isNotFunctional() {
        int returnInstCnt = 0;
        Return returnInst = null;
        for (BasicBlock block : getBlocks()) {
            if (block.notEndWithTerminalInst())
                return true;
            if (block.getInstTail() instanceof Return) {
                returnInst = ((Return) block.getInstTail());
                returnInstCnt++;
            }
        }
        if (specialChecker(returnInstCnt != 1))
            return true;
        BasicBlock block = returnInst.getBasicBlock();
        Function function = block.getFunction();
        if (block != function.exitBlock)
            moveBlockToExit(block);
        return false;
    }

    @Override
    public String toString() {
        return name;
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
