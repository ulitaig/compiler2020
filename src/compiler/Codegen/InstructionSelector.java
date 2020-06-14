package compiler.Codegen;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.IR.IRVisitor;
import compiler.Instr.*;
import compiler.Instr.Call;
import compiler.Instr.Load;
import compiler.Instr.Move;
import compiler.Instr.Return;
import compiler.Instr.Store;
import compiler.IR.IR;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.*;
import compiler.Codegen.Instruction.*;
import compiler.Codegen.Instruction.BinaryInst.ITypeBinary;
import compiler.Codegen.Instruction.BinaryInst.RTypeBinary;
import compiler.Codegen.Instruction.Branch.BinaryBranch;
import compiler.Codegen.Instruction.Branch.UnaryBranch;
import compiler.Codegen.Operand.ASMOperand;
import compiler.Codegen.Operand.Address.BaseOffsetAddr;
import compiler.Codegen.Operand.Address.StackLocation;
import compiler.Codegen.Operand.Immediate.Immediate;
import compiler.Codegen.Operand.Immediate.IntImmediate;
import compiler.Codegen.Operand.Register.PhysicalRegister;
import compiler.Codegen.Operand.Register.VirtualRegister;
import compiler.Codegen.Operand.Immediate.RelocationImmediate;
import compiler.Utility.Tools;

import java.util.ArrayList;

import static compiler.Codegen.Instruction.BinaryInst.ITypeBinary.OpName.*;
import static compiler.Codegen.Instruction.BinaryInst.RTypeBinary.OpName.*;
import static compiler.Codegen.Instruction.Branch.BinaryBranch.OpName.*;
import static compiler.Codegen.Instruction.Branch.UnaryBranch.OpName.*;
import static compiler.Codegen.Instruction.UnaryInst.OpName.*;


//getName!!!!!!!!!

public class InstructionSelector implements IRVisitor {
    private compiler.Codegen.Module ASMModule;

    private compiler.Codegen.Function currentFunction;
    private compiler.Codegen.BasicBlock currentBlock;

    public InstructionSelector() {
        ASMModule = new compiler.Codegen.Module();
        currentFunction = null;
        currentBlock = null;
    }

    public compiler.Codegen.Module getASMModule() {
        return ASMModule;
    }

    @Override
    public void visit(IR module) {
        for (GlobalVariable IRGlobalVariable : module.getGlobalVariableMap().values()) {
            String name = IRGlobalVariable.getName();
            compiler.Codegen.Operand.GlobalVariable gv = new compiler.Codegen.Operand.GlobalVariable(name);
            ASMModule.getGlobalVariableMap().put(name, gv);

            Operand init = IRGlobalVariable.getInit();

            if (IRGlobalVariable.getType() instanceof ArrayType) {
                gv.setString(((ConstString) init).getValue());
            } else if (IRGlobalVariable.getType() instanceof IntegerType
                    && ((IntegerType) IRGlobalVariable.getType()).getBitWidth() == IntegerType.BitWidth.int1) {
                gv.setBool(((ConstBool) init).getValue() ? 1 : 0);
            } else if (IRGlobalVariable.getType() instanceof IntegerType
                    && ((IntegerType) IRGlobalVariable.getType()).getBitWidth() == IntegerType.BitWidth.int32) {
                gv.setInt((anotherAalyse((int) ((ConstInt) init).getValue())));
            } else if (IRGlobalVariable.getType() instanceof PointerType) {
                gv.setInt(0);
            }
        }
        for (Function IRExternalFunction : module.getExternalFunctionMap().values()) {
            String name = IRExternalFunction.getName();
            ASMModule.getExternalFunctionMap().put(name,
                    new compiler.Codegen.Function(ASMModule, name, null));
        }
        for (Function IRFunction : module.getFunctionMap().values()) {
            String functionName = IRFunction.getName();
            ASMModule.getFunctionMap().put(functionName,
                    new compiler.Codegen.Function(ASMModule, functionName, IRFunction));
        }

        for (Function IRFunction : module.getFunctionMap().values())
            IRFunction.accept(this);
    }

    @Override
    public void visit(Function function) {
        String functionName = function.getName();
        currentFunction = ASMModule.getFunctionMap().get(functionName);
        currentBlock = currentFunction.getEntranceBlock();

        StackFrame stackFrame = new StackFrame(currentFunction);
        currentFunction.setStackFrame(stackFrame);

        VirtualRegister savedRA = new VirtualRegister(PhysicalRegister.raVR.getName() + ".save");
        currentFunction.getSymbolTable().putASM(savedRA.getName(), savedRA);
        currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock,
                savedRA, PhysicalRegister.raVR));

        for (VirtualRegister vr : PhysicalRegister.calleeSaveVRs) {
            VirtualRegister savedVR = new VirtualRegister(vr.getName() + ".save");
            currentFunction.getSymbolTable().putASM(savedVR.getName(), savedVR);
            currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock, savedVR, vr));
        }

        ArrayList<Parameter> IRParameters = function.getParameters();
        int upperZ = anotherAalyse(Integer.min(IRParameters.size(), 8));
        for (int i = 0; i < upperZ; i++) {
            Parameter parameter = IRParameters.get(i);
            VirtualRegister vr = currentFunction.getSymbolTable().getVR(parameter.getName());
            currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock,
                    vr, PhysicalRegister.argVR.get(i)));
        }

        upperZ = anotherAalyse(IRParameters.size());
        for (int i = 8; i < upperZ; i++) {
            Parameter parameter = IRParameters.get(i);
            VirtualRegister vr = currentFunction.getSymbolTable().getVR(parameter.getName());
            StackLocation stackLocation = new StackLocation(parameter.getName() + ".para");
            stackFrame.addFormalParameterLocation(stackLocation);
            currentBlock.addInstruction(new compiler.Codegen.Instruction.LoadInst(currentBlock, vr,
                    compiler.Codegen.Instruction.LoadInst.ByteSize.lw, stackLocation));
        }

        for (BasicBlock block : function.getBlocks())
            block.accept(this);
    }

    @Override
    public void visit(BasicBlock block) {
        currentBlock = currentFunction.getBlockMap().get(block.getName());
        IRInstruction ptr = block.getInstHead();
        while (ptr != null) {
            ptr.accept(this);
            ptr = ptr.getInstNext();
        }
    }

    @Override
    public void visit(Return inst) {
        if (specialChecker(!(inst.getType() instanceof VoidType))) {
            VirtualRegister returnValue = getVROfOperand(inst.getReturnValue());
            currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock,
                    PhysicalRegister.argVR.get(0), returnValue));
        }

        for (VirtualRegister vr : PhysicalRegister.calleeSaveVRs) {
            VirtualRegister savedVR = currentFunction.getSymbolTable().getVR(vr.getName() + ".save");
            currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock, vr, savedVR));
        }

        VirtualRegister savedRA = currentFunction.getSymbolTable().getVR(
                PhysicalRegister.raVR.getName() + ".save");
        currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock,
                PhysicalRegister.raVR, savedRA));

        currentBlock.addInstruction(new compiler.Codegen.Instruction.ReturnInst(currentBlock));
    }

    @Override
    public void visit(Branch inst) {
        if (inst.isConditional()) {
            Operand cond = inst.getCond();
            compiler.Codegen.BasicBlock thenBlock = currentFunction.getBlockMap().get(inst.getThenBlock().getName());
            compiler.Codegen.BasicBlock elseBlock = currentFunction.getBlockMap().get(inst.getElseBlock().getName());

            if (cond instanceof Register
                    && specialChecker(((Register) cond).getDef() instanceof Icmp)
                    && cond.onlyHaveOneBranchUse()) {
                Icmp icmp = ((Icmp) ((Register) cond).getDef());
                if (icmp.shouldSwap(true))
                    icmp.swapOps();

                IRType type = icmp.getIrType();
                Icmp.IcmpName op = icmp.getOperator();
                Operand op1 = icmp.getOp1();
                Operand op2 = icmp.getOp2();
                VirtualRegister rs1 = currentFunction.getSymbolTable().getVR(op1.getName());
                VirtualRegister rs2;
                if (type instanceof IntegerType) {
                    if (op2 instanceof Constant) {
                        long value = op2 instanceof ConstBool
                                ? (((ConstBool) op2).getValue() ? 1 : 0) : ((ConstInt) op2).getValue();

                        if (value != 0) {
                            rs2 = new VirtualRegister("loadImmediate");
                            currentFunction.getSymbolTable().putASMRename(rs2.getName(), rs2);
                            if (needToLoadImmediate(value)) {
                                currentBlock.addInstruction(new LoadImmediate(currentBlock,
                                        rs2, new IntImmediate(value)));
                            } else {
                                currentBlock.addInstruction(new ITypeBinary(currentBlock, addi, PhysicalRegister.zeroVR,
                                        new IntImmediate(value), rs2));
                            }
                        } else
                            rs2 = PhysicalRegister.zeroVR;
                    } else
                        rs2 = currentFunction.getSymbolTable().getVR(op2.getName());
                } else if (type instanceof PointerType) {
                    if (op2 instanceof Constant) {
                        rs2 = PhysicalRegister.zeroVR;
                    } else
                        rs2 = currentFunction.getSymbolTable().getVR(op2.getName());
                } else
                    throw new RuntimeException();

                BinaryBranch.OpName branchOp = op == Icmp.IcmpName.eq ? bne
                        : op == Icmp.IcmpName.ne ? beq
                        : op == Icmp.IcmpName.sgt ? ble
                        : op == Icmp.IcmpName.sge ? blt
                        : op == Icmp.IcmpName.slt ? bge
                        : bgt;
                currentBlock.addInstruction(new BinaryBranch(currentBlock, branchOp, rs1, rs2, elseBlock));
                currentBlock.addInstruction(new JumpInst(currentBlock, thenBlock));
                return;
            }

            VirtualRegister condVR = currentFunction.getSymbolTable().getVR(cond.getName());
            currentBlock.addInstruction(new UnaryBranch(currentBlock, beqz, condVR, elseBlock));
            currentBlock.addInstruction(new JumpInst(currentBlock, thenBlock));
        } else {
            compiler.Codegen.BasicBlock thenBlock = currentFunction.getBlockMap().get(inst.getThenBlock().getName());
            currentBlock.addInstruction(new JumpInst(currentBlock, thenBlock));
        }
    }

    @Override
    public void visit(BinaryOp inst) {
        if (inst.shouldSwapOperands())
            inst.swapOperands();

        Operand lhs = inst.getLhs();
        Operand rhs = inst.getRhs();
        VirtualRegister lhsOperand;
        ASMOperand rhsOperand;
        VirtualRegister result = currentFunction.getSymbolTable().getVR(inst.getResult().getName());

        Object opName;
        BinaryOp.BinaryOpName instOp = inst.getOp();
        switch (instOp) {
            case add: case and: case or: case xor:
                lhsOperand = getVROfOperand(lhs);
                rhsOperand = getOperand(rhs);
                if (specialChecker(rhsOperand instanceof Immediate)) {
                    opName = instOp == BinaryOp.BinaryOpName.add ? addi
                            : instOp == BinaryOp.BinaryOpName.and ? andi
                            : instOp == BinaryOp.BinaryOpName.or ? ori
                            : xori;
                    currentBlock.addInstruction(new ITypeBinary(currentBlock, ((ITypeBinary.OpName) opName),
                            lhsOperand, ((Immediate) rhsOperand), result));
                } else {
                    opName = instOp == BinaryOp.BinaryOpName.add ? add
                            : instOp == BinaryOp.BinaryOpName.and ? and
                            : instOp == BinaryOp.BinaryOpName.or ? or
                            : xor;
                    currentBlock.addInstruction(new RTypeBinary(currentBlock, ((RTypeBinary.OpName) opName),
                            lhsOperand, ((VirtualRegister) rhsOperand), result));
                }
                break;
            case sub:
                lhsOperand = getVROfOperand(lhs);
                rhsOperand = getOperand(rhs);
                if (rhsOperand instanceof Immediate) {
                    assert rhsOperand instanceof IntImmediate;
                    ((IntImmediate) rhsOperand).minusImmediate();
                    currentBlock.addInstruction(new ITypeBinary(currentBlock, ITypeBinary.OpName.addi,
                            lhsOperand, ((Immediate) rhsOperand), result));
                } else {
                    currentBlock.addInstruction(new RTypeBinary(currentBlock, RTypeBinary.OpName.sub,
                            lhsOperand, ((VirtualRegister) rhsOperand), result));
                }
                break;
            case mul: case sdiv: case srem:
                opName = instOp == BinaryOp.BinaryOpName.mul ? RTypeBinary.OpName.mul
                        : instOp == BinaryOp.BinaryOpName.sdiv ? RTypeBinary.OpName.div
                        : RTypeBinary.OpName.rem;
                lhsOperand = getVROfOperand(lhs);
                rhsOperand = getVROfOperand(rhs);
                currentBlock.addInstruction(new RTypeBinary(currentBlock, ((RTypeBinary.OpName) opName),
                        lhsOperand, ((VirtualRegister) rhsOperand), result));
                break;
            case shl: case ashr:
                if (rhs instanceof ConstInt && (((ConstInt) rhs).getValue() >= 32 || ((ConstInt) rhs).getValue() <= 0))
                    break;
                lhsOperand = getVROfOperand(lhs);
                rhsOperand = getOperand(rhs);
                if (rhsOperand instanceof Immediate) {
                    opName = instOp == BinaryOp.BinaryOpName.shl ? slli : srai;
                    currentBlock.addInstruction(new ITypeBinary(currentBlock, ((ITypeBinary.OpName) opName),
                            lhsOperand, ((Immediate) rhsOperand), result));
                } else {
                    opName = instOp == BinaryOp.BinaryOpName.shl ? sll : sra;
                    currentBlock.addInstruction(new RTypeBinary(currentBlock, ((RTypeBinary.OpName) opName),
                            lhsOperand, ((VirtualRegister) rhsOperand), result));
                }
                break;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void visit(Allocate inst) {
    }

    @Override
    public void visit(Load inst) {
        VirtualRegister rd = currentFunction.getSymbolTable().getVR(inst.getResult().getName());
        assert inst.getType() instanceof IntegerType || inst.getType() instanceof PointerType;
        compiler.Codegen.Instruction.LoadInst.ByteSize size = inst.getType().getBytes() == 1
                ? compiler.Codegen.Instruction.LoadInst.ByteSize.lb
                : compiler.Codegen.Instruction.LoadInst.ByteSize.lw;

        if (inst.getPointer() instanceof GlobalVariable) {
            compiler.Codegen.Operand.GlobalVariable gv =
                    ASMModule.getGlobalVariableMap().get(inst.getPointer().getName());
            VirtualRegister lui = new VirtualRegister("luiHigh");
            currentFunction.getSymbolTable().putASMRename(lui.getName(), lui);
            currentBlock.addInstruction(new LoadUpperImmediate(currentBlock, lui,
                    new RelocationImmediate(RelocationImmediate.Type.high, gv)));
            currentBlock.addInstruction(new compiler.Codegen.Instruction.LoadInst(currentBlock, rd, size,
                    new BaseOffsetAddr(lui, new RelocationImmediate(RelocationImmediate.Type.low, gv))));
        } else {
            if (inst.getPointer() instanceof ConstNull) {
                currentBlock.addInstruction(new compiler.Codegen.Instruction.LoadInst(currentBlock, rd, size,
                        new BaseOffsetAddr(PhysicalRegister.zeroVR, new IntImmediate(0))));
            } else {
                VirtualRegister pointer = currentFunction.getSymbolTable().getVR(inst.getPointer().getName());
                if (specialChecker(currentFunction.getGepAddrMap().containsKey(pointer))) {
                    BaseOffsetAddr addr = currentFunction.getGepAddrMap().get(pointer);
                    currentBlock.addInstruction(new compiler.Codegen.Instruction.LoadInst(currentBlock,
                            rd, size, addr));
                } else {
                    currentBlock.addInstruction(new compiler.Codegen.Instruction.LoadInst(currentBlock,
                            rd, size, new BaseOffsetAddr(pointer, new IntImmediate(0))));
                }
            }
        }
    }

    @Override
    public void visit(Store inst) {
        VirtualRegister value = getVROfOperand(inst.getValue());
        IRType irType = inst.getValue().getType();
        assert irType instanceof IntegerType || irType instanceof PointerType;
        compiler.Codegen.Instruction.StoreInst.ByteSize size = irType.getBytes() == 1
                ? compiler.Codegen.Instruction.StoreInst.ByteSize.sb
                : compiler.Codegen.Instruction.StoreInst.ByteSize.sw;

        if (inst.getPointer() instanceof GlobalVariable) {
            compiler.Codegen.Operand.GlobalVariable gv =
                    ASMModule.getGlobalVariableMap().get(inst.getPointer().getName());
            VirtualRegister lui = new VirtualRegister("luiHigh");
            currentFunction.getSymbolTable().putASMRename(lui.getName(), lui);
            currentBlock.addInstruction(new LoadUpperImmediate(currentBlock, lui,
                    new RelocationImmediate(RelocationImmediate.Type.high, gv)));
            currentBlock.addInstruction(new compiler.Codegen.Instruction.StoreInst(currentBlock, value, size,
                    new BaseOffsetAddr(lui, new RelocationImmediate(RelocationImmediate.Type.low, gv))));
        } else {
            if (inst.getPointer() instanceof ConstNull) {
                currentBlock.addInstruction(new compiler.Codegen.Instruction.StoreInst(currentBlock, value, size,
                        new BaseOffsetAddr(PhysicalRegister.zeroVR, new IntImmediate(0))));
            } else {
                VirtualRegister pointer = currentFunction.getSymbolTable().getVR(inst.getPointer().getName());
                if (specialChecker(currentFunction.getGepAddrMap().containsKey(pointer))) {
                    BaseOffsetAddr addr = currentFunction.getGepAddrMap().get(pointer);
                    currentBlock.addInstruction(new compiler.Codegen.Instruction.StoreInst(currentBlock,
                            value, size, addr));
                } else {
                    currentBlock.addInstruction(new compiler.Codegen.Instruction.StoreInst(currentBlock,
                            value, size, new BaseOffsetAddr(pointer, new IntImmediate(0))));
                }
            }
        }
    }

    @Override
    public void visit(GetElemPtr inst) {
        VirtualRegister rd = currentFunction.getSymbolTable().getVR(inst.getResult().getName());

        if (inst.getPointer() instanceof GlobalVariable) {
            currentBlock.addInstruction(new LoadAddressInst(currentBlock, rd,
                    ASMModule.getGlobalVariableMap().get(inst.getPointer().getName())));
        } else if (inst.getIndex().size() == 1) {
            VirtualRegister pointer = currentFunction.getSymbolTable().getVR(inst.getPointer().getName());
            Operand index = inst.getIndex().get(0);
            if (index instanceof Constant) {
                assert index instanceof ConstInt;
                long value = ((ConstInt) index).getValue() * 4;
                ptrASMO(rd, pointer, value);
            } else {
                VirtualRegister rs1 = currentFunction.getSymbolTable().getVR(index.getName());
                VirtualRegister rs2 = new VirtualRegister("slli");
                currentFunction.getSymbolTable().putASMRename(rs2.getName(), rs2);
                currentBlock.addInstruction(new ITypeBinary(currentBlock, slli, rs1, new IntImmediate(2), rs2));
                currentBlock.addInstruction(new RTypeBinary(currentBlock, add, pointer, rs2, rd));
            }
        } else {
            if (inst.getPointer() instanceof ConstNull) {
                currentBlock.addInstruction(new ITypeBinary(currentBlock, addi, PhysicalRegister.zeroVR,
                        new IntImmediate(((int) ((ConstInt) inst.getIndex().get(1)).getValue())), rd));
            } else {
                assert inst.getPointer().getType() instanceof PointerType
                        && ((PointerType) inst.getPointer().getType()).getBaseType() instanceof StructureType;
                assert inst.getIndex().size() == 2;
                assert inst.getIndex().get(0) instanceof ConstInt
                        && ((ConstInt) inst.getIndex().get(0)).getValue() == 0;
                assert inst.getIndex().get(1) instanceof ConstInt;
                VirtualRegister pointer = currentFunction.getSymbolTable().getVR(inst.getPointer().getName());
                StructureType structureType = ((StructureType) ((PointerType)
                        inst.getPointer().getType()).getBaseType());
                int index = anotherAalyse((int) ((ConstInt) inst.getIndex().get(1)).getValue());
                int offset = structureType.calcOffset(index);
                if (!(structureType.getMemberList().get(index) instanceof PointerType))
                    currentFunction.getGepAddrMap().put(rd, new BaseOffsetAddr(pointer, new IntImmediate(offset)));
                else {
                    ptrASMO(rd, pointer, offset);
                }
            }
        }
    }

    private void ptrASMO(VirtualRegister rd, VirtualRegister pointer, long value) {
        ASMOperand rs = getOperand(new ConstInt(IntegerType.BitWidth.int32, value));
        if (rs instanceof Immediate)
            currentBlock.addInstruction(new ITypeBinary(currentBlock, addi, pointer, ((Immediate) rs), rd));
        else {
            assert rs instanceof VirtualRegister;
            currentBlock.addInstruction(new RTypeBinary(currentBlock, add, pointer,
                    ((VirtualRegister) rs), rd));
        }
    }

    @Override
    public void visit(BitCastTo inst) {
        VirtualRegister src = currentFunction.getSymbolTable().getVR(inst.getSrc().getName());
        VirtualRegister dest = currentFunction.getSymbolTable().getVR(inst.getResult().getName());
        currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock, dest, src));
    }

    @Override
    public void visit(Icmp inst) {
        if (inst.getResult().onlyHaveOneBranchUse()) {
            return;
        }

        if (specialChecker(inst.shouldSwap(true)))
            inst.swapOps();

        IRType type = inst.getIrType();
        Operand op1 = inst.getOp1();
        Operand op2 = inst.getOp2();
        VirtualRegister rd = currentFunction.getSymbolTable().getVR(inst.getResult().getName());
        if (type instanceof IntegerType) {
            VirtualRegister rs1 = currentFunction.getSymbolTable().getVR(op1.getName());
            if (op2 instanceof Constant) {
                inst.convertLeGeToLtGt();
                op1 = inst.getOp1();
                op2 = inst.getOp2();
                Icmp.IcmpName op = inst.getOperator();

                long value = op2 instanceof ConstBool
                        ? (((ConstBool) op2).getValue() ? 1 : 0) : ((ConstInt) op2).getValue();
                VirtualRegister rs2 = new VirtualRegister("loadImmediate");
                VirtualRegister rs3 = new VirtualRegister("xor");
                switch (op) {
                    case slt:
                        if (needToLoadImmediate(value)) {
                            currentFunction.getSymbolTable().putASMRename(rs2.getName(), rs2);
                            currentBlock.addInstruction(new LoadImmediate(currentBlock, rs2, new IntImmediate(value)));
                            currentBlock.addInstruction(new RTypeBinary(currentBlock, slt, rs1, rs2, rd));
                        } else if (value != 0) {
                            currentBlock.addInstruction(new ITypeBinary(currentBlock, slti, rs1,
                                    new IntImmediate(value), rd));
                        } else {
                            currentBlock.addInstruction(new UnaryInst(currentBlock, sltz, rs1, rd));
                        }
                        break;
                    case sgt:
                        if (needToLoadImmediate(value)) {
                            currentFunction.getSymbolTable().putASMRename(rs2.getName(), rs2);
                            currentBlock.addInstruction(new LoadImmediate(currentBlock, rs2, new IntImmediate(value)));
                            currentBlock.addInstruction(new RTypeBinary(currentBlock, slt, rs2, rs1, rd));
                        } else if (value != 0) {
                            currentFunction.getSymbolTable().putASMRename(rs2.getName(), rs2);
                            currentBlock.addInstruction(new ITypeBinary(currentBlock, addi, PhysicalRegister.zeroVR,
                                    new IntImmediate(value), rs2));
                            currentBlock.addInstruction(new RTypeBinary(currentBlock, slt, rs2, rs1, rd));
                        } else { // value == 0
                            currentBlock.addInstruction(new UnaryInst(currentBlock, sgtz, rs1, rd));
                        }
                        break;
                    case eq: case ne:
                        UnaryInst.OpName opName = op == Icmp.IcmpName.eq ? seqz : snez;
                        if (needToLoadImmediate(value)) {
                            currentFunction.getSymbolTable().putASMRename(rs2.getName(), rs2);
                            currentFunction.getSymbolTable().putASMRename(rs3.getName(), rs3);

                            currentBlock.addInstruction(new LoadImmediate(currentBlock, rs2, new IntImmediate(value)));
                            currentBlock.addInstruction(new RTypeBinary(currentBlock, xor, rs1, rs2, rs3));
                            currentBlock.addInstruction(new UnaryInst(currentBlock, opName, rs3, rd));
                        } else if (value != 0) {
                            currentFunction.getSymbolTable().putASMRename(rs3.getName(), rs3);
                            currentBlock.addInstruction(new ITypeBinary(currentBlock, xori, rs1,
                                    new IntImmediate(value), rs3));
                            currentBlock.addInstruction(new UnaryInst(currentBlock, opName, rs3, rd));
                        } else {
                            currentBlock.addInstruction(new UnaryInst(currentBlock, opName, rs1, rd));
                        }
                        break;
                    default:
                        throw new RuntimeException();
                }
            } else {
                Icmp.IcmpName op = inst.getOperator();
                VirtualRegister rs2 = currentFunction.getSymbolTable().getVR(op2.getName());
                VirtualRegister rs3 = new VirtualRegister("slt");
                VirtualRegister rs4 = new VirtualRegister("xor");
                switch (op) {
                    case slt:
                        currentBlock.addInstruction(new RTypeBinary(currentBlock, slt, rs1, rs2, rd));
                        break;
                    case sgt:
                        currentBlock.addInstruction(new RTypeBinary(currentBlock, slt, rs2, rs1, rd));
                        break;
                    case sle:
                        currentFunction.getSymbolTable().putASMRename(rs3.getName(), rs3);
                        currentBlock.addInstruction(new RTypeBinary(currentBlock, slt, rs2, rs1, rs3));
                        currentBlock.addInstruction(new ITypeBinary(currentBlock, xori, rs3,
                                new IntImmediate(1), rd));
                        break;
                    case sge:
                        currentFunction.getSymbolTable().putASMRename(rs3.getName(), rs3);
                        currentBlock.addInstruction(new RTypeBinary(currentBlock, slt, rs1, rs2, rs3));
                        currentBlock.addInstruction(new ITypeBinary(currentBlock, xori, rs3,
                                new IntImmediate(1), rd));
                        break;
                    case eq:
                        currentFunction.getSymbolTable().putASMRename(rs4.getName(), rs4);
                        currentBlock.addInstruction(new RTypeBinary(currentBlock, xor, rs1, rs2, rs4));
                        currentBlock.addInstruction(new UnaryInst(currentBlock, seqz, rs4, rd));
                        break;
                    case ne:
                        currentFunction.getSymbolTable().putASMRename(rs4.getName(), rs4);
                        currentBlock.addInstruction(new RTypeBinary(currentBlock, xor, rs1, rs2, rs4));
                        currentBlock.addInstruction(new UnaryInst(currentBlock, snez, rs4, rd));
                        break;
                    default:
                        throw new RuntimeException();
                }
            }
        } else if (type instanceof PointerType) {
            VirtualRegister rs1 = currentFunction.getSymbolTable().getVR(op1.getName());
            Icmp.IcmpName op = inst.getOperator();
            if (specialChecker(op2 instanceof Constant)) {
                cmpOpSwich(rd, op, rs1);
            } else {
                VirtualRegister rs2 = currentFunction.getSymbolTable().getVR(op2.getName());
                VirtualRegister rs3 = new VirtualRegister("xor");

                currentFunction.getSymbolTable().putASMRename(rs3.getName(), rs3);
                currentBlock.addInstruction(new RTypeBinary(currentBlock, xor, rs1, rs2, rs3));
                cmpOpSwich(rd, op, rs3);
            }
        } else
            throw new RuntimeException();
    }

    private void cmpOpSwich(VirtualRegister rd, Icmp.IcmpName op, VirtualRegister rs3) {
        switch (op) {
            case eq:
                currentBlock.addInstruction(new UnaryInst(currentBlock, seqz, rs3, rd));
                break;
            case ne:
                currentBlock.addInstruction(new UnaryInst(currentBlock, snez, rs3, rd));
                break;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void visit(Phi inst) {
    }

    @Override
    public void visit(Call inst) {
        compiler.Codegen.Function callee;
        if (ASMModule.getFunctionMap().containsKey(inst.getFunction().getName()))
            callee = ASMModule.getFunctionMap().get(inst.getFunction().getName());
        else
            callee = ASMModule.getExternalFunctionMap().get(inst.getFunction().getName());
        ArrayList<Operand> parameters = inst.getParameters();

        for (int i = 0; i < anotherAalyse(Integer.min(8, parameters.size())); i++) {
            VirtualRegister parameter = getVROfOperand(parameters.get(i));
            currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock,
                    PhysicalRegister.argVR.get(i), parameter));
        }

        StackFrame stackFrame = currentFunction.getStackFrame();
        if (stackFrame.getParameterLocation().containsKey(callee)) {
            ArrayList<StackLocation> stackLocations = stackFrame.getParameterLocation().get(callee);
            for (int i = 8; i < parameters.size(); i++) {
                VirtualRegister parameter = getVROfOperand(parameters.get(i));
                currentBlock.addInstruction(new compiler.Codegen.Instruction.StoreInst(currentBlock, parameter,
                        compiler.Codegen.Instruction.StoreInst.ByteSize.sw, stackLocations.get(i - 8)));
            }
        } else {
            ArrayList<StackLocation> stackLocations = new ArrayList<>();
            for (int i = 8; i < parameters.size(); i++) {
                VirtualRegister parameter = getVROfOperand(parameters.get(i));
                StackLocation stackLocation = new StackLocation(".para" + i);
                stackLocations.add(stackLocation);

                currentBlock.addInstruction(new compiler.Codegen.Instruction.StoreInst(currentBlock, parameter,
                        compiler.Codegen.Instruction.StoreInst.ByteSize.sw, stackLocation));
            }
            stackFrame.getParameterLocation().put(callee, stackLocations);
        }

        compiler.Codegen.Instruction.CallInst callInst = new compiler.Codegen.Instruction.CallInst(currentBlock,
                callee);
        currentBlock.addInstruction(callInst);

        if (!inst.isVoidCall()) {
            VirtualRegister result = currentFunction.getSymbolTable().getVR(inst.getResult().getName());
            currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock,
                    result, PhysicalRegister.argVR.get(0)));
        }
    }

    @Override
    public void visit(Move inst) {
        VirtualRegister dest = currentFunction.getSymbolTable().getVR(inst.getResult().getName());
        if (inst.getSource() instanceof Constant) {
            ASMOperand src = getOperand(inst.getSource());
            if (src instanceof VirtualRegister) {
                currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock,
                        dest, ((VirtualRegister) src)));
            } else {
                assert src instanceof Immediate;
                currentBlock.addInstruction(new ITypeBinary(currentBlock, addi,
                        PhysicalRegister.zeroVR, ((Immediate) src), dest));
            }
        } else {
            VirtualRegister src = currentFunction.getSymbolTable().getVR(inst.getSource().getName());
            currentBlock.addInstruction(new compiler.Codegen.Instruction.MoveInst(currentBlock, dest, src));
        }
    }

    static private boolean needToLoadImmediate(long value) {
        return value >= (1 << 11) || value < -(1 << 11);
    }

    private VirtualRegister getVROfOperand(Operand operand) {
        if (operand instanceof ConstBool) {
            if (((ConstBool) operand).getValue()) {
                VirtualRegister constBool = new VirtualRegister("constBool");
                currentFunction.getSymbolTable().putASMRename(constBool.getName(), constBool);
                currentBlock.addInstruction(new ITypeBinary(currentBlock, ITypeBinary.OpName.addi,
                        PhysicalRegister.zeroVR, new IntImmediate(1), constBool));
                return constBool;
            } else
                return PhysicalRegister.zeroVR;
        } else if (operand instanceof ConstInt) {
            long value = ((ConstInt) operand).getValue();
            if (specialChecker(value == 0))
                return PhysicalRegister.zeroVR;
            else {
                VirtualRegister constInt = new VirtualRegister("constInt");
                currentFunction.getSymbolTable().putASMRename(constInt.getName(), constInt);
                if (needToLoadImmediate(value)) {
                    currentBlock.addInstruction(new LoadImmediate(currentBlock, constInt, new IntImmediate(value)));
                } else {
                    currentBlock.addInstruction(new ITypeBinary(currentBlock, ITypeBinary.OpName.addi,
                            PhysicalRegister.zeroVR, new IntImmediate(value), constInt));
                }
                return constInt;
            }
        } else if (operand instanceof ConstNull) {
            return PhysicalRegister.zeroVR;
        } else if (operand instanceof GlobalVariable) {
            throw new RuntimeException();
        } else if (operand instanceof Parameter) {
            return currentFunction.getSymbolTable().getVR(operand.getName());
        } else if (operand instanceof Register) {
            return currentFunction.getSymbolTable().getVR(operand.getName());
        } else
            throw new RuntimeException();
    }

    private ASMOperand getOperand(Operand operand) {
        if (operand instanceof ConstBool) {
            boolean value = ((ConstBool) operand).getValue();
            return new IntImmediate(value ? 1 : 0);
        } else if (operand instanceof ConstInt) {
            long value = ((ConstInt) operand).getValue();
            if (needToLoadImmediate(value))
                return getVROfOperand(operand);
            else
                return new IntImmediate(value);
        } else if (operand instanceof ConstNull) {
            return PhysicalRegister.zeroVR;
        } else if (operand instanceof GlobalVariable) {
            throw new RuntimeException();
        } else if (operand instanceof Register || operand instanceof Parameter) {
            return getVROfOperand(operand);
        } else
            throw new RuntimeException();
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
