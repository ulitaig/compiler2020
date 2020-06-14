package compiler.IR;

import compiler.Instr.*;
import compiler.Instr.Operand.ConstInt;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Register;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.IntegerType;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Utility.Tools;

import java.util.ArrayList;

public class NewArrayMalloc { //asd
    static BasicBlock currentBlock;
    static Function currentFunction;
    static Function function;
    static Register mallocResult;
    static ArrayList<Operand> parameters;
    static ArrayList<Operand> index;

    static private void addBytes(int cur, ArrayList<Operand> sizeList, IRType irType) {
        int baseSize = anotherAalyse(irType.getBytes());
        Register bytesMul = new Register(new IntegerType(IntegerType.BitWidth.int32), "bytesMul");
        Register bytes = new Register(new IntegerType(IntegerType.BitWidth.int32), "bytes");
        currentFunction.getSymbolTable().put(bytesMul.getName(), bytesMul);
        currentFunction.getSymbolTable().put(bytes.getName(), bytes);
        currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.mul,
                sizeList.get(cur), new ConstInt(IntegerType.BitWidth.int32, baseSize), bytesMul));
        currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.add,
                bytesMul, new ConstInt(IntegerType.BitWidth.int32, 4), bytes));
        parameters.add(bytes);
    }

    static private Register addInst(int cur, ArrayList<Operand> sizeList, IRType irType){
        // Call malloc
        currentBlock.addInstruction(new Call(currentBlock, function, parameters, mallocResult));
        // Cast to int32
        Register mallocInt32 = new Register(new PointerType(new IntegerType(IntegerType.BitWidth.int32)),
                "mallocInt32");
        currentFunction.getSymbolTable().put(mallocInt32.getName(), mallocInt32);
        currentBlock.addInstruction(new BitCastTo(currentBlock, mallocResult,
                new PointerType(new IntegerType(IntegerType.BitWidth.int32)), mallocInt32));
        // Store size
        currentBlock.addInstruction(new Store(currentBlock, sizeList.get(cur), mallocInt32));
        // GetElemPtr to next
        Register arrayHeadInt32 = new Register(new PointerType(new IntegerType(IntegerType.BitWidth.int32)),
                "arrayHeadInt32");
        currentFunction.getSymbolTable().put(arrayHeadInt32.getName(), arrayHeadInt32);
        index = new ArrayList<>();
        index.add(new ConstInt(IntegerType.BitWidth.int32, 1));
        currentBlock.addInstruction(new GetElemPtr(currentBlock, mallocInt32, index, arrayHeadInt32));
        // Cast to object type
        Register arrayHead = new Register(irType, "arrayHead");
        currentFunction.getSymbolTable().put(arrayHead.getName(), arrayHead);
        currentBlock.addInstruction(new BitCastTo(currentBlock, arrayHeadInt32, irType, arrayHead));
        return arrayHead;
    }

    static private void generateForNextDimension(int cur, ArrayList<Operand> sizeList, IRType irType,IR module, IRBuilder irBuilder,Register arrayHead){
        if (specialChecker(cur != anotherAalyse(sizeList.size() - 1))) {
            Register arrayTail = new Register(irType, "arrayTail");
            currentFunction.getSymbolTable().put(arrayTail.getName(), arrayTail);
            index = new ArrayList<>();
            index.add(sizeList.get(cur));
            currentBlock.addInstruction(new GetElemPtr(currentBlock, arrayHead, index, arrayTail));
            Register arrayPtrAddr = new Register(new PointerType(irType), "arrayPtr$addr");
            currentFunction.getEntranceBlock().addInstructionAtFront(
                    new Store(currentFunction.getEntranceBlock(), irType.getDefaultValue(), arrayPtrAddr));
            currentFunction.getEntranceBlock().addInstructionAtFront(
                    new Allocate(currentFunction.getEntranceBlock(), arrayPtrAddr, irType));
            currentBlock.addInstruction(new Store(currentBlock, arrayHead, arrayPtrAddr));

            BasicBlock loopCond = new BasicBlock(currentFunction, "newLoopCond");
            BasicBlock loopBody = new BasicBlock(currentFunction, "newLoopBody");
            BasicBlock loopMerge = new BasicBlock(currentFunction, "newLoopMerge");
            currentFunction.getSymbolTable().put(loopCond.getName(), loopCond);
            currentFunction.getSymbolTable().put(loopBody.getName(), loopBody);
            currentFunction.getSymbolTable().put(loopMerge.getName(), loopMerge);
            currentBlock.addInstruction(new Branch(currentBlock, null, loopCond, null));
            currentBlock = loopCond;
            irBuilder.setCurrentBlock(loopCond);
            currentFunction.addBasicBlock(loopCond);
            Register arrayPointer = new Register(irType, "arrayPointer");
            Register cmpResult = new Register(new IntegerType(IntegerType.BitWidth.int1), "ptrCmpResult");
            currentFunction.getSymbolTable().put(arrayPointer.getName(), arrayPointer);
            currentFunction.getSymbolTable().put(cmpResult.getName(), cmpResult);
            currentBlock.addInstruction(new Load(currentBlock, irType, arrayPtrAddr, arrayPointer));
            currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.slt,
                    irType, arrayPointer, arrayTail, cmpResult));

            currentBlock.addInstruction(new Branch(currentBlock, cmpResult, loopBody, loopMerge));
            currentBlock = loopBody;
            irBuilder.setCurrentBlock(currentBlock);
            currentFunction.addBasicBlock(loopBody);
            Operand arrayHeadNextDim = generate(cur + 1, sizeList,
                    ((PointerType) irType).getBaseType(), module, irBuilder);
            currentBlock = irBuilder.getCurrentBlock();
            currentBlock.addInstruction(new Store(currentBlock, arrayHeadNextDim, arrayPointer));
            Register nextArrayPtr = new Register(irType, "nextArrayPtr");
            currentFunction.getSymbolTable().put(nextArrayPtr.getName(), nextArrayPtr);
            index = new ArrayList<>();
            index.add(new ConstInt(IntegerType.BitWidth.int32, 1));
            currentBlock.addInstruction(new GetElemPtr(currentBlock, arrayPointer, index, nextArrayPtr));
            currentBlock.addInstruction(new Store(currentBlock, nextArrayPtr, arrayPtrAddr));
            currentBlock.addInstruction(new Branch(currentBlock, null, loopCond, null));
            currentBlock = loopMerge;
            irBuilder.setCurrentBlock(currentBlock);
            currentFunction.addBasicBlock(loopMerge);
        }
    }

    static public Operand generate(int cur, ArrayList<Operand> sizeList, IRType irType,
                                   IR module, IRBuilder irBuilder) {
        currentBlock = irBuilder.getCurrentBlock();
        currentFunction = irBuilder.getCurrentFunction();
        function = module.getExternalFunctionMap().get("malloc");
        parameters = new ArrayList<>();
        mallocResult = new Register(new PointerType(new IntegerType(IntegerType.BitWidth.int8)),
                "malloc");
        currentFunction.getSymbolTable().put(mallocResult.getName(), mallocResult);

        addBytes(cur,sizeList,irType);
        Register arrayHead = addInst(cur,sizeList,irType);

        generateForNextDimension(cur,sizeList,irType,module,irBuilder,arrayHead);
        return arrayHead;
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

    static private int anotherAalyse(int n){
        Tools anotherAalyse = new Tools();
        return anotherAalyseforT(n,anotherAalyse);
    }

    static private int anotherAalyseforT(int n,Tools anotherAalyse) {
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
