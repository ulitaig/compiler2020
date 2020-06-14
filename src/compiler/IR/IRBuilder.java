package compiler.IR;

import compiler.AST.*;
import compiler.Entity.FunctionEntity;
import compiler.Entity.VariableEntity;
import compiler.Semantic.*;
import compiler.Instr.*;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.*;
import compiler.Instr.TypeSystem.VoidType;
import compiler.Type.*;
import compiler.Type.ArrayType;
import compiler.Utility.SemanticError;
import compiler.Utility.Pair;
import compiler.Utility.Tools;

import java.util.*;

public class IRBuilder implements ASTVisitor {
    private IR module;

    private Scope globalScope;
    private TypeTable astTypeTable;
    private IRTypeTable irTypeTable;

    private Function currentFunction;
    private BasicBlock currentBlock;

    private Stack<BasicBlock> loopBreakBlock;
    private Stack<BasicBlock> loopContinueBlock;

    private Function initializer;

    public IRBuilder(Scope globalScope, TypeTable astTypeTable) {
        module = new IR(astTypeTable);

        this.globalScope = globalScope;
        this.astTypeTable = astTypeTable;
        this.irTypeTable = module.getIrTypeTable();

        currentFunction = null;
        currentBlock = null;
        loopBreakBlock = new Stack<>();
        loopContinueBlock = new Stack<>();

        initializer = new Function(module, "__init__", new VoidType(), new ArrayList<>(), false);
        initializer.initialize();
        module.addFunction(initializer);
    }

    public IR getIR() {
        return module;
    }

    public Function getCurrentFunction() {
        return currentFunction;
    }

    public BasicBlock getCurrentBlock() {
        return currentBlock;
    }

    public void setCurrentBlock(BasicBlock currentBlock) {
        this.currentBlock = currentBlock;
    }

    @Override
    public void visit(ProgramNode node) throws SemanticError {
        for (UnitNode unit : node.getProgramUnits())
            if (unit instanceof ClassNode) {
                if (((ClassNode) unit).hasConstructor())
                    ((ClassNode) unit).getConstructor().addFunctionToIR(module, astTypeTable, irTypeTable);
                for (FunctionNode method : ((ClassNode) unit).getFuncList())
                    method.addFunctionToIR(module, astTypeTable, irTypeTable);
            }

        for (UnitNode unit : node.getProgramUnits())
            if (unit instanceof FunctionNode)
                ((FunctionNode) unit).addFunctionToIR(module, astTypeTable, irTypeTable);

        currentFunction = initializer;
        currentBlock = initializer.getEntranceBlock();

        for (UnitNode unit : node.getProgramUnits())
            if (unit instanceof VarNode)
                unit.accept(this);
        currentBlock.addInstruction(new Branch(currentBlock,null, currentFunction.getReturnBlock(), null));
        currentFunction.addBasicBlock(currentFunction.getReturnBlock());

        currentFunction = null;
        currentBlock = null;

        for (UnitNode unit : node.getProgramUnits())
            if (unit instanceof ClassNode)
                unit.accept(this);

        for (UnitNode unit : node.getProgramUnits())
            if (unit instanceof FunctionNode)
                unit.accept(this);
    }

    @Override
    public void visit(BasicTypeNode node) throws SemanticError {
    }

    @Override
    public void visit(ClassTypeNode node) throws SemanticError {
    }

    @Override
    public void visit(ArrayTypeNode node) throws SemanticError {
    }

    @Override
    public void visit(VarNode node) throws SemanticError {
        Type type = astTypeTable.get(node.getType());
        IRType irType = type.getIRType(irTypeTable);
        String name = node.getIdentifier();
        VariableEntity variableEntity = (VariableEntity) node.getScope().getEntity(name);
        if (node.getScope() == globalScope) {
            GlobalVariable globalVariable = new GlobalVariable(irType, name, null);
            Operand init;
            if (node.hasInitExpr()) { // visit ExprNode
                ExprNode initExpr = node.getInitExpr();
                initExpr.accept(this);
                init = initExpr.getResult();
                if (specialChecker(!init.isConstValue())) {
                    currentBlock.addInstruction(new Store(currentBlock, init, globalVariable));
                    init = type.getDefaultValue();
                }
            } else
                init = type.getDefaultValue();
            globalVariable.setInit(init);
            module.addGlobalVariable(globalVariable);
            variableEntity.setAllocaAddr(globalVariable);
        } else {
            Register allocaAddr = new Register(new PointerType(irType), name + "$addr");
            BasicBlock entranceBlock = currentFunction.getEntranceBlock();
            entranceBlock.addInstructionAtFront(new Store(entranceBlock, irType.getDefaultValue(), allocaAddr));
            entranceBlock.addInstructionAtFront(new Allocate(entranceBlock, allocaAddr, irType));
            currentFunction.getSymbolTable().put(allocaAddr.getName(), allocaAddr);
            variableEntity.setAllocaAddr(allocaAddr);

            if (node.hasInitExpr()) {
                Operand init;
                ExprNode initExpr = node.getInitExpr();
                initExpr.accept(this);
                init = initExpr.getResult();
                currentBlock.addInstruction(new Store(currentBlock, init, allocaAddr));
            }
        }
    }

    @Override
    public void visit(FunctionNode node) throws SemanticError {
        String functionName;
        if (node.getScope().inClassScope()) {
            ClassType classType = (ClassType) node.getScope().getClassType();

            String className = classType.getName();
            String methodName = node.getIdentifier();
            functionName = className + "." + methodName;
        } else {
            functionName = node.getIdentifier();
        }
        Function function = module.getFunctionMap().get(functionName);

        currentFunction = function;
        currentBlock = function.getEntranceBlock();

        node.getStatement().accept(this);

        currentBlock.addInstruction(new Branch(currentBlock,
                null, currentFunction.getReturnBlock(), null));
        function.addBasicBlock(function.getReturnBlock());

        if (specialChecker(node.getIdentifier().equals("main"))) {
            function = module.getFunctionMap().get("__init__");
            currentFunction.getEntranceBlock().addInstructionAtFront(
                    new Call(currentFunction.getEntranceBlock(), function, new ArrayList<>(), null));
        }

        currentFunction = null;
        currentBlock = null;
    }

    @Override
    public void visit(ClassNode node) throws SemanticError {
        if (specialChecker(node.hasConstructor()))
            node.getConstructor().accept(this);
        for (FunctionNode method : node.getFuncList())
            method.accept(this);
    }

    @Override
    public void visit(BlockNode node) throws SemanticError {
        ArrayList<StmtNode> statements = node.getStatements();
        for (StmtNode statement : statements)
            statement.accept(this);
    }

    @Override
    public void visit(VariableStmtNode node) throws SemanticError {
        for (VarNode variable : node.getVarList())
            variable.accept(this);
    }

    @Override
    public void visit(IfStmtNode node) throws SemanticError {
        BasicBlock thenBlock = new BasicBlock(currentFunction, "ifThenBlock");
        BasicBlock elseBlock = node.hasElseBody() ? new BasicBlock(currentFunction, "ifElseBlock") : null;
        BasicBlock mergeBlock = new BasicBlock(currentFunction, "ifMergeBlock");

        node.getCond().accept(this);
        Operand condResult = node.getCond().getResult();

        if (node.hasElseBody())
            currentBlock.addInstruction(new Branch(currentBlock, condResult, thenBlock, elseBlock));
        else
            currentBlock.addInstruction(new Branch(currentBlock, condResult, thenBlock, mergeBlock));

        currentBlock = thenBlock;
        node.getThenBody().accept(this);
        currentBlock.addInstruction(new Branch(currentBlock, null, mergeBlock, null));
        currentFunction.addBasicBlock(thenBlock);

        if (node.hasElseBody()) {
            currentBlock = elseBlock;
            node.getElseBody().accept(this);
            currentBlock.addInstruction(new Branch(currentBlock, null, mergeBlock, null));
            currentFunction.addBasicBlock(elseBlock);
        }

        currentBlock = mergeBlock;
        currentFunction.addBasicBlock(mergeBlock);


        currentFunction.getSymbolTable().put(thenBlock.getName(), thenBlock);
        if (specialChecker(node.hasElseBody())) {
            currentFunction.getSymbolTable().put(elseBlock.getName(), elseBlock);
        }
        currentFunction.getSymbolTable().put(mergeBlock.getName(), mergeBlock);
    }

    @Override
    public void visit(WhileStmtNode node) throws SemanticError {
        BasicBlock condBlock = new BasicBlock(currentFunction, "whileCondBlock");
        BasicBlock bodyBlock = new BasicBlock(currentFunction, "whileBodyBlock");
        BasicBlock mergeBlock = new BasicBlock(currentFunction, "whileMergeBlock");

        addInstForLoop(condBlock, bodyBlock, mergeBlock, node.getCond());
        loopContinueBlock.push(condBlock);
        currentBlock = bodyBlock;
        node.getBody().accept(this); // visit StmtNode
        currentBlock.addInstruction(new Branch(currentBlock, null, condBlock, null));
        currentFunction.addBasicBlock(bodyBlock);

        loopBreakBlock.pop();
        loopContinueBlock.pop();

        currentBlock = mergeBlock;
        currentFunction.addBasicBlock(mergeBlock);


        currentFunction.getSymbolTable().put(condBlock.getName(), condBlock);
        currentFunction.getSymbolTable().put(bodyBlock.getName(), bodyBlock);
        currentFunction.getSymbolTable().put(mergeBlock.getName(), mergeBlock);
    }

    private void addInstForLoop(BasicBlock condBlock, BasicBlock bodyBlock, BasicBlock mergeBlock, ExprNode cond) {
        currentBlock.addInstruction(new Branch(currentBlock, null, condBlock, null));

        currentBlock = condBlock;
        cond.accept(this);
        Operand condResult = cond.getResult();
        currentBlock.addInstruction(new Branch(currentBlock, condResult, bodyBlock, mergeBlock));
        currentFunction.addBasicBlock(condBlock);

        loopBreakBlock.push(mergeBlock);
    }

    @Override
    public void visit(ForStmtNode node) throws SemanticError {
        BasicBlock condBlock = node.hasCond() ? new BasicBlock(currentFunction, "forCondBlock") : null;
        BasicBlock stepBlock = node.hasStep() ? new BasicBlock(currentFunction, "forStepBlock") : null;
        BasicBlock bodyBlock = new BasicBlock(currentFunction, "forBodyBlock");
        BasicBlock mergeBlock = new BasicBlock(currentFunction, "forMergeBlock");

        if (node.hasInit())
            node.getInit().accept(this); // visit ExprNode

        if (node.hasCond()) {
            addInstForLoop(condBlock, bodyBlock, mergeBlock, node.getCond());
            loopContinueBlock.push(node.hasStep() ? stepBlock : condBlock);
            currentBlock = bodyBlock;
            node.getBody().accept(this); // visit StmtNode
            if (node.hasStep())
                currentBlock.addInstruction(new Branch(currentBlock, null, stepBlock, null));
            else
                currentBlock.addInstruction(new Branch(currentBlock, null, condBlock, null));
            currentFunction.addBasicBlock(bodyBlock);

            loopBreakBlock.pop();
            loopContinueBlock.pop();

            if (node.hasStep()) {
                currentBlock = stepBlock;
                node.getStep().accept(this); // visit ExprNode
                currentBlock.addInstruction(new Branch(currentBlock, null, condBlock, null));
                currentFunction.addBasicBlock(stepBlock);
            }
        } else {
            currentBlock.addInstruction(new Branch(currentBlock, null, bodyBlock, null));

            loopBreakBlock.push(mergeBlock);
            loopContinueBlock.push(node.hasStep() ? stepBlock : condBlock);
            currentBlock = bodyBlock;
            node.getBody().accept(this); // visit StmtNode
            if (node.hasStep())
                currentBlock.addInstruction(new Branch(currentBlock, null, stepBlock, null));
            else
                currentBlock.addInstruction(new Branch(currentBlock, null, bodyBlock, null));
            currentFunction.addBasicBlock(bodyBlock);

            loopBreakBlock.pop();
            loopContinueBlock.pop();

            if (node.hasStep()) {
                currentBlock = stepBlock;
                node.getStep().accept(this); // visit ExprNode
                currentBlock.addInstruction(new Branch(currentBlock, null, bodyBlock, null));
                currentFunction.addBasicBlock(stepBlock);
            }
        }

        currentBlock = mergeBlock;
        currentFunction.addBasicBlock(mergeBlock);


        if (node.hasCond()) {
            currentFunction.getSymbolTable().put(condBlock.getName(), condBlock);
        }
        if (specialChecker(node.hasStep())) {
            currentFunction.getSymbolTable().put(stepBlock.getName(), stepBlock);
        }
        currentFunction.getSymbolTable().put(bodyBlock.getName(), bodyBlock);
        currentFunction.getSymbolTable().put(mergeBlock.getName(), mergeBlock);
    }

    @Override
    public void visit(ReturnStmtNode node) throws SemanticError {
        if (specialChecker(node.hasReturnValue())) {
            node.getReturnValue().accept(this); // visit ExprNode
            Operand result = node.getReturnValue().getResult();
            currentBlock.addInstruction(new Store(currentBlock, result, currentFunction.getReturnValue()));
        }
        currentBlock.addInstruction(new Branch(currentBlock,
                null, currentFunction.getReturnBlock(), null));
    }

    @Override
    public void visit(BreakStmtNode node) throws SemanticError {
        currentBlock.addInstruction(new Branch(currentBlock, null, loopBreakBlock.peek(), null));
    }

    @Override
    public void visit(ContinueStmtNode node) throws SemanticError {
        currentBlock.addInstruction(new Branch(currentBlock,
                null, loopContinueBlock.peek(), null));
    }

    @Override
    public void visit(ExprStmtNode node) throws SemanticError {
        node.getExpr().accept(this); // visit ExprNode
    }

    @Override
    public void visit(PostfixExprNode node) throws SemanticError {
        node.getExpr().accept(this); // visit ExprNode

        Register result;
        Operand exprResult = node.getExpr().getResult();
        Operand addr = node.getExpr().getLvalueResult();
        if (specialChecker(node.getOp() == PostfixExprNode.Operator.postInc)) {
            result = new Register(new IntegerType(IntegerType.BitWidth.int32), "postInc");
            currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.add,
                    exprResult, new ConstInt(IntegerType.BitWidth.int32, 1), result));
        } else {
            result = new Register(new IntegerType(IntegerType.BitWidth.int32), "postDec");
            currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.sub,
                    exprResult, new ConstInt(IntegerType.BitWidth.int32, 1), result));
        }
        currentBlock.addInstruction(new Store(currentBlock, result, addr));

        node.setResult(exprResult);
        node.setLvalueResult(null);
        currentFunction.getSymbolTable().put(result.getName(), result);
    }

    @Override
    public void visit(UnaryExprNode node) throws SemanticError {
        node.getExpr().accept(this);

        UnaryExprNode.Operator op = node.getOp();
        Operand exprResult = node.getExpr().getResult();
        if (op == UnaryExprNode.Operator.preInc) {
            Operand addr = node.getExpr().getLvalueResult();
            Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "preInc");
            currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.add,
                    exprResult, new ConstInt(IntegerType.BitWidth.int32, 1), result));
            currentBlock.addInstruction(new Store(currentBlock, result, addr));

            node.setResult(result);
            node.setLvalueResult(addr);
            currentFunction.getSymbolTable().put(result.getName(), result);
        } else if (op == UnaryExprNode.Operator.preDec) {
            Operand addr = node.getExpr().getLvalueResult();
            Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "preDec");
            currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.sub,
                    exprResult, new ConstInt(IntegerType.BitWidth.int32, 1), result));
            currentBlock.addInstruction(new Store(currentBlock, result, addr));

            node.setResult(result);
            node.setLvalueResult(addr);
            currentFunction.getSymbolTable().put(result.getName(), result);
        } else if (op == UnaryExprNode.Operator.signPos) {
            node.setResult(exprResult);
            node.setLvalueResult(null);
        } else if (op == UnaryExprNode.Operator.signNeg) {
            if (exprResult.isConstValue()) {
                assert exprResult instanceof ConstInt;
                node.setResult(new ConstInt(IntegerType.BitWidth.int32, -((ConstInt) exprResult).getValue()));
                node.setLvalueResult(null);
            } else {
                Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "signNeg");
                currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.sub,
                        new ConstInt(IntegerType.BitWidth.int32, 0), exprResult, result));

                node.setResult(result);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
            }
        } else if (specialChecker(op == UnaryExprNode.Operator.logicalNot)) {
            Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "logicalNot");
            currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.xor,
                    new ConstBool(true), exprResult, result));

            node.setResult(result);
            node.setLvalueResult(null);
            currentFunction.getSymbolTable().put(result.getName(), result);
        } else {
            Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "bitwiseComplement");
            currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.xor,
                    new ConstInt(IntegerType.BitWidth.int32, -1), exprResult, result));

            node.setResult(result);
            node.setLvalueResult(null);
            currentFunction.getSymbolTable().put(result.getName(), result);
        }
    }

    @Override
    public void visit(BinaryExprNode node) throws SemanticError {
        BinaryExprNode.Operator op = node.getOp();
        if (op != BinaryExprNode.Operator.logicalAnd && op != BinaryExprNode.Operator.logicalOr) {
            node.getLhs().accept(this);
            node.getRhs().accept(this);

            Operand lhsResult = node.getLhs().getResult();
            Operand rhsResult = node.getRhs().getResult();
            if (op == BinaryExprNode.Operator.mul) {
                Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "mul");
                currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.mul,
                        lhsResult, rhsResult, result));

                node.setResult(result);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
            } else if (op == BinaryExprNode.Operator.div) {
                Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "div");
                currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.sdiv,
                        lhsResult, rhsResult, result));

                node.setResult(result);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
            } else if (op == BinaryExprNode.Operator.mod) {
                Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "mod");
                currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.srem,
                        lhsResult, rhsResult, result));

                node.setResult(result);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
            } else if (op == BinaryExprNode.Operator.sub) {
                Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "sub");
                currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.sub,
                        lhsResult, rhsResult, result));

                node.setResult(result);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
            } else if (op == BinaryExprNode.Operator.shiftLeft) {
                Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "shiftLeft");
                currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.shl,
                        lhsResult, rhsResult, result));

                node.setResult(result);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
            } else if (op == BinaryExprNode.Operator.shiftRight) {
                Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "shiftRight");
                currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.ashr,
                        lhsResult, rhsResult, result));

                node.setResult(result);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
            } else if (op == BinaryExprNode.Operator.bitwiseAnd) {
                Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "bitwiseAnd");
                currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.and,
                        lhsResult, rhsResult, result));

                node.setResult(result);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
            } else if (op == BinaryExprNode.Operator.bitwiseXor) {
                Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "bitwiseXor");
                currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.xor,
                        lhsResult, rhsResult, result));

                node.setResult(result);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
            } else if (op == BinaryExprNode.Operator.bitwiseOr) {
                Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "bitwiseOr");
                currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.or,
                        lhsResult, rhsResult, result));

                node.setResult(result);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
            } else if (op == BinaryExprNode.Operator.add) {
                if (specialChecker((node.getLhs().getType() instanceof IntType) && (node.getRhs().getType() instanceof IntType))) {
                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int32), "add");
                    currentBlock.addInstruction(new BinaryOp(currentBlock, BinaryOp.BinaryOpName.add,
                            lhsResult, rhsResult, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else {
                    Function function = module.getExternalFunctionMap().get("__string_concatenate");
                    ArrayList<Operand> parameters = new ArrayList<>();
                    parameters.add(lhsResult);
                    parameters.add(rhsResult);

                    Register result = new Register(new PointerType(new IntegerType(IntegerType.BitWidth.int8)),
                            "stringConcatenate");
                    currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                }
            } else if (op == BinaryExprNode.Operator.less) {
                if ((node.getLhs().getType() instanceof IntType) && (node.getRhs().getType() instanceof IntType)) {
                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "lessThan");
                    currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.slt,
                            new IntegerType(IntegerType.BitWidth.int32), lhsResult, rhsResult, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else {
                    Function function = module.getExternalFunctionMap().get("__string_lessThan");
                    ArrayList<Operand> parameters = new ArrayList<>();
                    parameters.add(lhsResult);
                    parameters.add(rhsResult);

                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1),
                            "stringLessThan");
                    currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                }
            } else if (op == BinaryExprNode.Operator.greater) {
                if ((specialChecker(node.getLhs().getType() instanceof IntType) && (node.getRhs().getType() instanceof IntType))) {
                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "greaterThan");
                    currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.sgt,
                            new IntegerType(IntegerType.BitWidth.int32), lhsResult, rhsResult, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else {
                    Function function = module.getExternalFunctionMap().get("__string_greaterThan");
                    ArrayList<Operand> parameters = new ArrayList<>();
                    parameters.add(lhsResult);
                    parameters.add(rhsResult);

                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1),
                            "stringGreaterThan");
                    currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                }
            } else if (op == BinaryExprNode.Operator.lessEqual) {
                if ((node.getLhs().getType() instanceof IntType) && (node.getRhs().getType() instanceof IntType)) {
                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "lessEqual");
                    currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.sle,
                            new IntegerType(IntegerType.BitWidth.int32), lhsResult, rhsResult, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else {
                    Function function = module.getExternalFunctionMap().get("__string_lessEqual");
                    ArrayList<Operand> parameters = new ArrayList<>();
                    parameters.add(lhsResult);
                    parameters.add(rhsResult);

                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1),
                            "stringLessEqual");
                    currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                }
            } else if (op == BinaryExprNode.Operator.greaterEqual) {
                if ((node.getLhs().getType() instanceof IntType) && (node.getRhs().getType() instanceof IntType)) {
                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "greaterEqual");
                    currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.sge,
                            new IntegerType(IntegerType.BitWidth.int32), lhsResult, rhsResult, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else {
                    Function function = module.getExternalFunctionMap().get("__string_greaterEqual");
                    ArrayList<Operand> parameters = new ArrayList<>();
                    parameters.add(lhsResult);
                    parameters.add(rhsResult);

                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1),
                            "stringGreaterEqual");
                    currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                }
            } else if (op == BinaryExprNode.Operator.equal) {
                Type lType = node.getLhs().getType();
                Type rType = node.getRhs().getType();
                if (specialChecker((lType instanceof IntType) && (rType instanceof IntType))) {
                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "equal");
                    currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.eq,
                            new IntegerType(IntegerType.BitWidth.int32), lhsResult, rhsResult, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else if ((lType instanceof BoolType) && (rType instanceof BoolType)) {
                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "equal");
                    currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.eq,
                            new IntegerType(IntegerType.BitWidth.int1), lhsResult, rhsResult, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else if ((lType instanceof StringType) && (rType instanceof StringType)) {
                    Function function = module.getExternalFunctionMap().get("__string_equal");
                    ArrayList<Operand> parameters = new ArrayList<>();
                    parameters.add(lhsResult);
                    parameters.add(rhsResult);

                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "stringEqual");
                    currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else if ((lType instanceof ArrayType) && (rType instanceof NullType)) {
                    arrryNullDeal(node, lhsResult, rhsResult);
                } else if ((lType instanceof NullType) && (rType instanceof ArrayType)) {
                    nullArrayDeal(node, lhsResult, rhsResult);
                } else if ((lType instanceof ClassType) && (rType instanceof NullType)) {
                    arrryNullDeal(node, lhsResult, rhsResult);
                } else if ((lType instanceof NullType) && (rType instanceof ClassType)) {
                    nullArrayDeal(node, lhsResult, rhsResult);
                } else if (specialChecker((lType instanceof NullType) && (rType instanceof NullType))) {
                    node.setResult(new ConstBool(true));
                    node.setLvalueResult(null);
                }
            } else if (op == BinaryExprNode.Operator.notEqual) {
                Type lType = node.getLhs().getType();
                Type rType = node.getRhs().getType();
                if ((lType instanceof IntType) && (rType instanceof IntType)) {
                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "notEqual");
                    currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.ne,
                            new IntegerType(IntegerType.BitWidth.int32), lhsResult, rhsResult, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else if ((lType instanceof BoolType) && (rType instanceof BoolType)) {
                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "notEqual");
                    currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.ne,
                            new IntegerType(IntegerType.BitWidth.int1), lhsResult, rhsResult, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else if ((lType instanceof StringType) && (rType instanceof StringType)) {
                    Function function = module.getExternalFunctionMap().get("__string_notEqual");
                    ArrayList<Operand> parameters = new ArrayList<>();
                    parameters.add(lhsResult);
                    parameters.add(rhsResult);

                    Register result = new Register(new IntegerType(IntegerType.BitWidth.int1),
                            "stringNotEqual");
                    currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));

                    node.setResult(result);
                    node.setLvalueResult(null);
                    currentFunction.getSymbolTable().put(result.getName(), result);
                } else if ((lType instanceof ArrayType) && (rType instanceof NullType)) {
                    arrayNoNullDeal(node, lhsResult, rhsResult);
                } else if ((lType instanceof NullType) && (rType instanceof ArrayType)) {
                    nullNoArrayDeal(node, lhsResult, rhsResult);
                } else if ((lType instanceof ClassType) && (rType instanceof NullType)) {
                    arrayNoNullDeal(node, lhsResult, rhsResult);
                } else if ((lType instanceof NullType) && (rType instanceof ClassType)) {
                    nullNoArrayDeal(node, lhsResult, rhsResult);
                } else if ((lType instanceof NullType) && (rType instanceof NullType)) {
                    node.setResult(new ConstBool(false));
                    node.setLvalueResult(null);
                }
            } else {
                currentBlock.addInstruction(new Store(currentBlock, rhsResult, node.getLhs().getLvalueResult()));

                node.setResult(rhsResult);
                node.setLvalueResult(null);
            }
        } else if (specialChecker(op == BinaryExprNode.Operator.logicalAnd)) {
            BasicBlock branchBlock = new BasicBlock(currentFunction, "logicalAndBranch");
            BasicBlock mergeBlock = new BasicBlock(currentFunction, "logicalAndMerge");
            BasicBlock phi1;
            BasicBlock phi2;

            node.getLhs().accept(this); // visit ExprNode
            Operand lhsResult = node.getLhs().getResult();
            currentBlock.addInstruction(new Branch(currentBlock, lhsResult, branchBlock, mergeBlock));
            phi1 = currentBlock;

            currentBlock = branchBlock;
            node.getRhs().accept(this); // visit ExprNode
            Operand rhsResult = node.getRhs().getResult();
            currentBlock.addInstruction(new Branch(currentBlock, null, mergeBlock, null));
            currentFunction.addBasicBlock(branchBlock);
            phi2 = branchBlock;

            currentBlock = mergeBlock;
            Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "logicalAnd");
            Set<Pair<Operand, BasicBlock>> branch = new LinkedHashSet<>();
            branch.add(new Pair<>(new ConstBool(false), phi1));
            branch.add(new Pair<>(rhsResult, phi2));
            currentBlock.addInstruction(new Phi(currentBlock, branch, result));
            currentFunction.addBasicBlock(mergeBlock);

            node.setResult(result);
            node.setLvalueResult(null);
            currentFunction.getSymbolTable().put(result.getName(), result);
            currentFunction.getSymbolTable().put(branchBlock.getName(), branchBlock);
            currentFunction.getSymbolTable().put(mergeBlock.getName(), mergeBlock);
        } else {
            BasicBlock branchBlock = new BasicBlock(currentFunction, "logicalOrBranch");
            BasicBlock mergeBlock = new BasicBlock(currentFunction, "logicalOrMerge");
            BasicBlock phi1;
            BasicBlock phi2;

            node.getLhs().accept(this); // visit ExprNode
            Operand lhsResult = node.getLhs().getResult();
            currentBlock.addInstruction(new Branch(currentBlock, lhsResult, mergeBlock, branchBlock));
            phi1 = currentBlock;

            currentBlock = branchBlock;
            node.getRhs().accept(this); // visit ExprNode
            Operand rhsResult = node.getRhs().getResult();
            currentBlock.addInstruction(new Branch(currentBlock, null, mergeBlock, null));
            currentFunction.addBasicBlock(branchBlock);
            phi2 = branchBlock;

            currentBlock = mergeBlock;
            Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "logicalOr");
            Set<Pair<Operand, BasicBlock>> branch = new LinkedHashSet<>();
            branch.add(new Pair<>(new ConstBool(true), phi1));
            branch.add(new Pair<>(rhsResult, phi2));
            currentBlock.addInstruction(new Phi(currentBlock, branch, result));
            currentFunction.addBasicBlock(mergeBlock);

            node.setResult(result);
            node.setLvalueResult(null);
            currentFunction.getSymbolTable().put(result.getName(), result);
            currentFunction.getSymbolTable().put(branchBlock.getName(), branchBlock);
            currentFunction.getSymbolTable().put(mergeBlock.getName(), mergeBlock);
        }
    }

    private void nullNoArrayDeal(BinaryExprNode node, Operand lhsResult, Operand rhsResult) {
        Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "notEqual");
        currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.ne,
                rhsResult.getType(), lhsResult, rhsResult, result));

        node.setResult(result);
        node.setLvalueResult(null);
        currentFunction.getSymbolTable().put(result.getName(), result);
    }

    private void arrayNoNullDeal(BinaryExprNode node, Operand lhsResult, Operand rhsResult) {
        Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "notEqual");
        currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.ne,
                lhsResult.getType(), lhsResult, rhsResult, result));

        node.setResult(result);
        node.setLvalueResult(null);
        currentFunction.getSymbolTable().put(result.getName(), result);
    }

    private void nullArrayDeal(BinaryExprNode node, Operand lhsResult, Operand rhsResult) {
        Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "equal");
        currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.eq,
                rhsResult.getType(), lhsResult, rhsResult, result));

        node.setResult(result);
        node.setLvalueResult(null);
        currentFunction.getSymbolTable().put(result.getName(), result);
    }

    private void arrryNullDeal(BinaryExprNode node, Operand lhsResult, Operand rhsResult) {
        Register result = new Register(new IntegerType(IntegerType.BitWidth.int1), "equal");
        currentBlock.addInstruction(new Icmp(currentBlock, Icmp.IcmpName.eq,
                lhsResult.getType(), lhsResult, rhsResult, result));

        node.setResult(result);
        node.setLvalueResult(null);
        currentFunction.getSymbolTable().put(result.getName(), result);
    }

    @Override
    public void visit(NewExprNode node) throws SemanticError {
        if (node.getDim() == 0) {
            Type type = astTypeTable.get(node.getBaseType());
            assert type instanceof ClassType;

            Function function = module.getExternalFunctionMap().get("malloc");
            int size = module.getStructureMap().get("class." + type.getName()).getBytes();
            ArrayList<Operand> parameters = new ArrayList<>();
            parameters.add(new ConstInt(IntegerType.BitWidth.int32, size));

            Register result = new Register(new PointerType(new IntegerType(IntegerType.BitWidth.int8)),
                    "malloc");
            Register cast = new Register(type.getIRType(irTypeTable), "classPtr");
            currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));
            currentBlock.addInstruction(new BitCastTo(currentBlock, result, type.getIRType(irTypeTable), cast));

            if (specialChecker(((ClassType) type).hasConstructor())) {
                function = module.getFunctionMap().get(type.getName() + "." + type.getName());
                parameters = new ArrayList<>();
                parameters.add(cast);
                currentBlock.addInstruction(new Call(currentBlock, function, parameters, null));
            }

            node.setResult(cast);
            node.setLvalueResult(null);
            currentFunction.getSymbolTable().put(result.getName(), result);
            currentFunction.getSymbolTable().put(cast.getName(), cast);
        } else {
            ArrayList<ExprNode> exprForDim = node.getExprForDim();
            IRType irType = astTypeTable.get(node.getBaseType()).getIRType(irTypeTable);
            for (int i = 0; i < anotherAalyse(node.getDim()); i++)
                irType = new PointerType(irType);

            ArrayList<Operand> sizeList = new ArrayList<>();
            for (ExprNode expr : exprForDim) {
                expr.accept(this);
                sizeList.add(expr.getResult());
            }

            Operand result = NewArrayMalloc.generate(0, sizeList, irType, module, this);
            node.setResult(result);
            node.setLvalueResult(null);
        }
    }

    @Override
    public void visit(MemberExprNode node) throws SemanticError {
        node.getExpr().accept(this);

        Type type = node.getExpr().getType();

        String name = node.getIdentifier();
        ArrayList<VariableEntity> members = ((ClassType) type).getMembers();
        int pos;
        for (pos = 0; pos < anotherAalyse(members.size()); pos++)
            if (members.get(pos).getName().equals(name))
                break;

        Operand pointer = node.getExpr().getResult();
        ArrayList<Operand> index = new ArrayList<>();
        index.add(new ConstInt(IntegerType.BitWidth.int32, 0));
        index.add(new ConstInt(IntegerType.BitWidth.int32, pos));
        IRType irType = astTypeTable.get(members.get(pos).getType()).getIRType(irTypeTable);
        Register result = new Register(new PointerType(irType), type.getName() + "." + name + "$addr");
        Register load = new Register(irType, type.getName() + "." + name);
        currentBlock.addInstruction(new GetElemPtr(currentBlock, pointer, index, result));
        currentBlock.addInstruction(new Load(currentBlock, irType, result, load));

        node.setResult(load);
        node.setLvalueResult(result);
        currentFunction.getSymbolTable().put(result.getName(), result);
        currentFunction.getSymbolTable().put(load.getName(), load);
    }

    @Override
    public void visit(FuncCallExprNode node) throws SemanticError {
        ExprNode funcName = node.getFuncName();
        Function function;
        if (funcName instanceof MemberExprNode) {
            ExprNode expr = ((MemberExprNode) funcName).getExpr();
            String name = ((MemberExprNode) funcName).getIdentifier();
            Type type = expr.getType();
            expr.accept(this); // visit ExprNode
            Operand ptrResult = expr.getResult();
            if (type instanceof ArrayType) {
                Register pointer;
                if (specialChecker(!ptrResult.getType().equals(new PointerType(new IntegerType(IntegerType.BitWidth.int32))))) {
                    pointer = new Register(new PointerType(new IntegerType(IntegerType.BitWidth.int32)), "cast");
                    currentBlock.addInstruction(new BitCastTo(currentBlock, ptrResult,
                            new PointerType(new IntegerType(IntegerType.BitWidth.int32)), pointer));
                    currentFunction.getSymbolTable().put(pointer.getName(), pointer);
                } else
                    pointer = (Register) ptrResult;
                ArrayList<Operand> index = new ArrayList<>();
                index.add(new ConstInt(IntegerType.BitWidth.int32, -1));
                Register result = new Register(pointer.getType(), "elementPtr");
                Register size = new Register(new IntegerType(IntegerType.BitWidth.int32), "arraySize");
                currentBlock.addInstruction(new GetElemPtr(currentBlock, pointer, index, result));
                currentBlock.addInstruction(new Load(currentBlock,
                        new IntegerType(IntegerType.BitWidth.int32), result, size));

                node.setResult(size);
                node.setLvalueResult(null);
                currentFunction.getSymbolTable().put(result.getName(), result);
                currentFunction.getSymbolTable().put(size.getName(), size);
            } else {
                if (type instanceof StringType) {
                    function = module.getExternalFunctionMap().get("__string_" + name);
                } else {
                    assert type instanceof ClassType;
                    function = module.getFunctionMap().get(type.getName() + "." + name);
                }
                ArrayList<Operand> parameters = new ArrayList<>();
                IRType returnType = function.getFunctionType().getReturnType();
                Register result = returnType instanceof VoidType ? null : new Register(returnType, "call");
                parameters.add(ptrResult);
                for (ExprNode parameterExpr : node.getParameters()) {
                    parameterExpr.accept(this);
                    parameters.add(parameterExpr.getResult());
                }

                currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));
                if (result != null)
                    currentFunction.getSymbolTable().put(result.getName(), result);

                node.setResult(result);
                node.setLvalueResult(null);
            }
        } else {
            String name = ((IdExprNode) funcName).getIdentifier();
            FunctionEntity functionEntity = node.getScope().getFunctionEntity(name);
            if (functionEntity.getEntityType() == FunctionEntity.EntityType.function) {
                if (module.getFunctionMap().containsKey(name))
                    function = module.getFunctionMap().get(name);
                else
                    function = module.getExternalFunctionMap().get(name);
                ArrayList<Operand> parameters = new ArrayList<>();
                IRType returnType = function.getFunctionType().getReturnType();
                Register result = returnType instanceof VoidType ? null : new Register(returnType, "call");
                for (ExprNode parameterExpr : node.getParameters()) {
                    parameterExpr.accept(this); // visit ExprNode
                    parameters.add(parameterExpr.getResult());
                }

                currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));
                if (result != null)
                    currentFunction.getSymbolTable().put(result.getName(), result);

                node.setResult(result);
            } else {
                ClassType type = (ClassType) node.getScope().getClassType();
                function = module.getFunctionMap().get(type.getName() + "." + name);
                assert function != null;

                Register thisAllocaAddr = (Register) currentFunction.getSymbolTable().get("this$addr");
                IRType baseType = ((PointerType) thisAllocaAddr.getType()).getBaseType();
                Register ptrResult = new Register(baseType, "this");
                currentBlock.addInstruction(new Load(currentBlock, baseType, thisAllocaAddr, ptrResult));

                ArrayList<Operand> parameters = new ArrayList<>();
                IRType returnType = function.getFunctionType().getReturnType();
                Register result = returnType instanceof VoidType ? null : new Register(returnType, "call");
                parameters.add(ptrResult);
                for (ExprNode parameterExpr : node.getParameters()) {
                    parameterExpr.accept(this);
                    parameters.add(parameterExpr.getResult());
                }

                currentBlock.addInstruction(new Call(currentBlock, function, parameters, result));
                currentFunction.getSymbolTable().put(ptrResult.getName(), ptrResult);
                if (specialChecker(result != null))
                    currentFunction.getSymbolTable().put(result.getName(), result);

                node.setResult(result);
            }
            node.setLvalueResult(null);
        }
    }

    @Override
    public void visit(SubscriptExprNode node) throws SemanticError {
        node.getName().accept(this); // visit ExprNode
        node.getIndex().accept(this); // visit ExprNode

        Operand arrayPtr = node.getName().getResult();
        ArrayList<Operand> index = new ArrayList<>();
        index.add(node.getIndex().getResult());
        Register result = new Register(arrayPtr.getType(), "elementPtr");
        currentBlock.addInstruction(new GetElemPtr(currentBlock, arrayPtr, index, result));

        Register arrayLoad = new Register(((PointerType) arrayPtr.getType()).getBaseType(), "arrayLoad");
        currentBlock.addInstruction(new Load(currentBlock,
                ((PointerType) arrayPtr.getType()).getBaseType(), result, arrayLoad));

        node.setResult(arrayLoad);
        node.setLvalueResult(result);
        currentFunction.getSymbolTable().put(result.getName(), result);
        currentFunction.getSymbolTable().put(arrayLoad.getName(), arrayLoad);
    }

    @Override
    public void visit(ThisExprNode node) throws SemanticError {
        Register thisAllocaAddr = (Register) currentFunction.getSymbolTable().get("this$addr");
        IRType irType = ((PointerType) thisAllocaAddr.getType()).getBaseType();
        Register result = new Register(irType, "this");
        currentBlock.addInstruction(new Load(currentBlock, irType, thisAllocaAddr, result));

        node.setResult(result);
        node.setLvalueResult(null);
        currentFunction.getSymbolTable().put(result.getName(), result);
    }

    @Override
    public void visit(IdExprNode node) throws SemanticError {
        Operand allocaAddr = ((VariableEntity) node.getEntity()).getAllocaAddr();
        if (allocaAddr != null) {
            IRType irType;
            if (((VariableEntity) node.getEntity()).getEntityType() == VariableEntity.EntityType.global)
                irType = allocaAddr.getType();
            else
                irType = ((PointerType) allocaAddr.getType()).getBaseType();
            Register result = new Register(irType, node.getIdentifier());
            currentBlock.addInstruction(new Load(currentBlock, irType, allocaAddr, result));

            node.setResult(result);
            node.setLvalueResult(allocaAddr);
            currentFunction.getSymbolTable().put(result.getName(), result);
        } else {
            Register thisAllocaAddr = (Register) currentFunction.getSymbolTable().get("this$addr");

            IRType baseType = ((PointerType) thisAllocaAddr.getType()).getBaseType();
            Register thisPtr = new Register(baseType, "this");
            currentBlock.addInstruction(new Load(currentBlock, baseType, thisAllocaAddr, thisPtr));

            Type type = node.getScope().getClassType();
            String name = node.getIdentifier();
            ArrayList<VariableEntity> members = ((ClassType) type).getMembers();
            int pos;
            for (pos = 0; pos < members.size(); pos++)
                if (members.get(pos).getName().equals(name))
                    break;

            ArrayList<Operand> index = new ArrayList<>();
            pos = anotherAalyse(pos);
            index.add(new ConstInt(IntegerType.BitWidth.int32, 0));
            index.add(new ConstInt(IntegerType.BitWidth.int32, pos));
            IRType irType = astTypeTable.get(members.get(pos).getType()).getIRType(irTypeTable);
            Register result = new Register(new PointerType(irType), type.getName() + "." + name + "$addr");
            Register load = new Register(irType, type.getName() + "." + name);
            currentBlock.addInstruction(new GetElemPtr(currentBlock, thisPtr, index, result));
            currentBlock.addInstruction(new Load(currentBlock, irType, result, load));

            node.setResult(load);
            node.setLvalueResult(result);
            currentFunction.getSymbolTable().put(thisPtr.getName(), thisPtr);
            currentFunction.getSymbolTable().put(result.getName(), result);
            currentFunction.getSymbolTable().put(load.getName(), load);
        }
    }

    @Override
    public void visit(BoolConstantNode node) {
        node.setResult(new ConstBool(node.getValue()));
        node.setLvalueResult(null);
    }

    @Override
    public void visit(IntConstantNode node) {
        node.setResult(new ConstInt(IntegerType.BitWidth.int32, node.getValue()));
        node.setLvalueResult(null);
    }

    @Override
    public void visit(StringConstantNode node) {
        GlobalVariable string = module.addConstString(node.getValue());
        ArrayList<Operand> index = new ArrayList<>();
        index.add(new ConstInt(IntegerType.BitWidth.int32, 0));
        index.add(new ConstInt(IntegerType.BitWidth.int32, 0));
        Register result = new Register(new PointerType(new IntegerType(IntegerType.BitWidth.int8)),
                "stringConstant");
        currentBlock.addInstruction(new GetElemPtr(currentBlock, string, index, result));

        node.setResult(result);
        node.setLvalueResult(null);
        currentFunction.getSymbolTable().put(result.getName(), result);
    }

    @Override
    public void visit(NullNode node) {
        node.setResult(new ConstNull());
        node.setLvalueResult(null);
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
