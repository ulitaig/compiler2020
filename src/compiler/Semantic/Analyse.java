package compiler.Semantic;

import compiler.AST.*;
import compiler.Entity.*;
import compiler.Type.*;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

import java.util.ArrayList;
import java.util.Stack;

public class Analyse implements ASTVisitor {
    private Scope globalScope;
    private Stack<Scope> scopeStack;
    private TypeTable typeTable;
    private SemanticError semanticError;

    private Scope currentScope() {
        return scopeStack.peek();
    }

    public Analyse() {
        scopeStack = new Stack<>();
        typeTable = new TypeTable();


        semanticError = new SemanticError(new Position(0,0),"");
    }

    public Scope getGlobalScope() {
        return globalScope;
    }

    public TypeTable getTypeTable() {
        return typeTable;
    }

    @Override
    public void visit(ProgramNode node) throws SemanticError {
        globalScope = new Scope(null, Scope.ScopeType.programScope,null, null);
        scopeStack.push(globalScope);
        node.setScope(globalScope);
        globalScope.addBuiltInFunction();

        ArrayList<UnitNode> UnitNodes = node.getProgramUnits();
        boolean error = false;

        for (UnitNode unit : UnitNodes)
            if (unit instanceof ClassNode) {
                ClassTypeNode classTypeNode = new ClassTypeNode(unit.getPosition(),((ClassNode) unit).getIdentifier());
                ClassType classType = ((ClassNode) unit).getClassType();
                typeTable.put(classTypeNode, classType);
            }
            else if(unit instanceof FunctionNode){
                globalScope.declareEntity(unit, VariableEntity.EntityType.global,FunctionEntity.EntityType.function, globalScope, typeTable);
            }
        for (UnitNode unit : UnitNodes)
            if (unit instanceof VarNode) {
                unit.accept(this);
                globalScope.declareEntity(unit, VariableEntity.EntityType.global,FunctionEntity.EntityType.function, globalScope, typeTable);
            } else if (unit instanceof FunctionNode)
                unit.accept(this);
            else if (unit instanceof ClassNode)
                unit.accept(this);

        Entity mainFunction = currentScope().getEntity("main");
        if (!(mainFunction instanceof FunctionEntity)) {
            semanticError.error("There is no Main function.");
            error = true;
        } else {
            //TypeNode A = ((FunctionEntity) mainFunction).getReturnType();
            //Type B = typeTable.get(A);
            //new IntType();
            //typeTable.get(((FunctionEntity) mainFunction).getReturnType()).getName();
            //typeTable.get(((FunctionEntity) mainFunction).getReturnType()).equals(new IntType());

            if (!typeTable.get(((FunctionEntity) mainFunction).getReturnType()).equals(new IntType())) {
                semanticError.error(mainFunction.getPosition(),
                        "Return type of Main function is not int.");
                error = true;
            }
            if (((FunctionEntity) mainFunction).getParameters().size() != 0) {
                semanticError.error(mainFunction.getPosition(),
                        "Find parameter in Main function.");
                error = true;
            }
        }

        scopeStack.pop();
        if (error)
            throw semanticError;
    }

    @Override
    public void visit(BasicTypeNode node) throws SemanticError {
        node.setScope(currentScope());

        if (!typeTable.hasType(node)) {
            semanticError.error(node.getPosition(), "Undefined type \"" + node.getIdentifier() + "\".");
            throw semanticError;
        }
    }

    @Override
    public void visit(ClassTypeNode node) throws SemanticError {
        node.setScope(currentScope());

        if (!typeTable.hasType(node)) {
            semanticError.error(node.getPosition(), "Undefined type \"" + node.getIdentifier() + "\".");
            throw semanticError;
        }
    }

    @Override
    public void visit(ArrayTypeNode node) throws SemanticError {
        node.setScope(currentScope());

        if (!typeTable.hasType(node.getBaseType())) {
            semanticError.error(node.getPosition(), "Undefined type \"" + node.getBaseType().getIdentifier() + "\".");
            throw semanticError;
        }
    }

    @Override
    public void visit(VarNode node) throws SemanticError {
        node.setScope(currentScope());

        node.getType().accept(this);
        if (node.hasInitExpr()) {
            node.getInitExpr().accept(this);
            Type lType = typeTable.get(node.getType());
            Type rType = node.getInitExpr().getType();
            if (Type.canNotAssign(lType, rType)) {
                semanticError.error(node.getPosition(), "Type of rhs \"" + rType.toString() + "\" is not \"" + lType.toString() + "\".");
                throw semanticError;
            }
        }
    }

    @Override
    public void visit(FunctionNode node) throws SemanticError {
        Scope scope = new Scope(currentScope(), Scope.ScopeType.functionScope, node.getType(), currentScope().getClassType());
        scopeStack.push(scope);
        node.setScope(scope);

        try {
            node.getType().accept(this);
        } catch (SemanticError ignored) {
            scopeStack.pop();
            throw semanticError;
        }

        ArrayList<VariableEntity> entityParameters = currentScope().getFunctionEntityIncludeConstructor(node.getIdentifier()).getParameters();
        ArrayList<VarNode> parameters = node.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            VarNode parameter = parameters.get(i);
            parameter.accept(this);
            scope.declareEntity(entityParameters.get(i), parameter.getPosition());
        }
        node.getStatement().accept(this);

        if (!typeTable.get(node.getType()).equals(new VoidType()) && !node.getIdentifier().equals("main")) {
            if (!scope.checkReturn()) {
                semanticError.error(node.getPosition(), "Function has no return.");
                throw semanticError;
            }
        }
        scopeStack.pop();

    }

    @Override
    public void visit(ClassNode node) throws SemanticError {
        Scope scope = new Scope(currentScope(), Scope.ScopeType.classScope, null,
                typeTable.get(new ClassTypeNode(new Position(0, 0), node.getIdentifier())));
        scopeStack.push(scope);
        node.setScope(scope);

        boolean error = false;
        ArrayList<VarNode> varList = node.getVarList();
        for (VarNode member : varList){
            member.accept(this);
            scope.declareEntity(member, VariableEntity.EntityType.member, FunctionEntity.EntityType.function, globalScope, typeTable);
        }

        ArrayList<FunctionNode> funcList = node.getFuncList();
        for (FunctionNode method : funcList) {
            if (method.getIdentifier().equals(node.getIdentifier())) {
                semanticError.error(method.getPosition(), "Return type for constructor is invalid.");
                error = true;
                continue;
            }
            scope.declareEntity(method, VariableEntity.EntityType.global, FunctionEntity.EntityType.method, globalScope, typeTable);
        }

        if (node.hasConstructor()) {
            if (node.getConstructor().getParameters().size() != 0) {
                semanticError.error(node.getConstructor().getPosition(), "Constructor should have no parameter.");
                error = true;
            } else {
                scope.declareConstructor(node.getConstructor());
                node.getConstructor().accept(this);
            }
        }

        for (FunctionNode method : funcList) {
            if (!method.getIdentifier().equals(node.getIdentifier()))
                method.accept(this);
        }

        scopeStack.pop();

        if (error)
            throw semanticError;
    }

    @Override
    public void visit(BlockNode node) throws SemanticError {
        Scope scope = new Scope(currentScope(), Scope.ScopeType.blockScope,
                currentScope().getFunctionReturnType(), currentScope().getClassType());
        scopeStack.push(scope);
        node.setScope(scope);

        ArrayList<StmtNode> statements = node.getStatements();
        for (StmtNode statement : statements)
            statement.accept(this);

        scopeStack.pop();
    }

    @Override
    public void visit(VariableStmtNode node) throws SemanticError {
        node.setScope(currentScope());

        ArrayList<VarNode> varList = node.getVarList();
        for (VarNode var : varList) {
            var.accept(this);
            currentScope().declareEntity(var, VariableEntity.EntityType.local, FunctionEntity.EntityType.function, globalScope, typeTable);
        }
    }

    @Override
    public void visit(IfStmtNode node) throws SemanticError {
        node.setScope(currentScope());

        node.getCond().accept(this);
        if (!node.getCond().getType().equals(new BoolType())) {
            semanticError.error(node.getCond().getPosition(), "The condition should be bool type.");
            throw semanticError;
        }

        if (node.hasThenBody()) {
            if (node.getThenBody() instanceof BlockNode)
                node.getThenBody().accept(this);
            else {
                Scope scope = new Scope(currentScope(), Scope.ScopeType.blockScope,
                        currentScope().getFunctionReturnType(), currentScope().getClassType());
                scopeStack.push(scope);

                node.getThenBody().accept(this);
                scopeStack.pop();
            }
        }

        if (node.hasElseBody()) {
            if (node.getElseBody() instanceof BlockNode)
                node.getElseBody().accept(this);
            else {
                Scope scope = new Scope(currentScope(), Scope.ScopeType.blockScope,
                        currentScope().getFunctionReturnType(), currentScope().getClassType());
                scopeStack.push(scope);

                node.getElseBody().accept(this);
                scopeStack.pop();
            }
        }
    }

    @Override
    public void visit(WhileStmtNode node) throws SemanticError{
        node.setScope(currentScope());

        node.getCond().accept(this);
        if (!node.getCond().getType().equals(new BoolType())) {
            semanticError.error(node.getCond().getPosition(), "Condition should be a bool.");
            throw semanticError;
        }

        if (node.hasBody()) {
            Scope scope = new Scope(currentScope(), Scope.ScopeType.loopScope,
                    currentScope().getFunctionReturnType(), currentScope().getClassType());
            scopeStack.push(scope);
            node.getBody().accept(this);
            scopeStack.pop();
        }

    }

    @Override
    public void visit(ForStmtNode node) throws SemanticError {
        node.setScope(currentScope());

        if (node.hasInit())
            node.getInit().accept(this);
        if (node.hasCond()) {
            node.getCond().accept(this);
            if (!node.getCond().getType().equals(new BoolType())) {
                semanticError.error(node.getCond().getPosition(), "The condition should be bool type.");
                throw semanticError;
            }
        }
        if (node.hasStep())
            node.getStep().accept(this);


        if (node.hasBody()) {
            Scope scope = new Scope(currentScope(), Scope.ScopeType.loopScope,
                    currentScope().getFunctionReturnType(), currentScope().getClassType());
            scopeStack.push(scope);
            node.getBody().accept(this);
            scopeStack.pop();
        }
    }

    @Override
    public void visit(ReturnStmtNode node) throws SemanticError {
        node.setScope(currentScope());

        if (!currentScope().inFunctionScope()) {
            semanticError.error(node.getPosition(), "The return statement is not in a function scope.");
            throw semanticError;
        }
        Type lType = typeTable.get(currentScope().getFunctionReturnType());
        if (node.hasReturnValue()) {
            node.getReturnValue().accept(this);
            ExprNode returnValue = node.getReturnValue();
            if (lType.equals(new VoidType())) {
                semanticError.error(returnValue.getPosition(), "The function requires void return type.");
                throw semanticError;
            }
            Type rType = returnValue.getType();
            if (Type.canNotAssign(lType, rType)) {
                semanticError.error(returnValue.getPosition(),
                        "\"" + returnValue.getText() + "\" is not " + lType.toString() + " type.");
                throw semanticError;
            }
        } else {
            if (!lType.equals(new VoidType())) {
                semanticError.error(node.getPosition(), "The function should have no return value.");
                throw semanticError;
            }
        }

        currentScope().setReturn();
    }

    @Override
    public void visit(BreakStmtNode node) throws SemanticError {
        node.setScope(currentScope());

        if (!currentScope().inLoopScope()) {
            semanticError.error(node.getPosition(), "The break statement is not in a loop scope.");
            throw semanticError;
        }
    }

    @Override
    public void visit(ContinueStmtNode node) throws SemanticError {
        node.setScope(currentScope());

        if (!currentScope().inLoopScope()) {
            semanticError.error(node.getPosition(), "The continue statement is not in a loop scope.");
            throw semanticError;
        }
    }

    @Override
    public void visit(ExprStmtNode node) throws SemanticError {
        node.setScope(currentScope());

        node.getExpr().accept(this);
    }

    @Override
    public void visit(PostfixExprNode node) throws SemanticError {
        node.setScope(currentScope());

        node.getExpr().accept(this);

        Position position = node.getExpr().getPosition();
        String text = node.getExpr().getText();
        if (!(node.getExpr().getType() instanceof IntType)) {
            semanticError.error(position, "\"" + text + "\" is not int type.");
            throw semanticError;
        }
        if (!node.getExpr().getLvalue()) {
            semanticError.error(position, "\"" + text + "\" is not lvalue.");
            throw semanticError;
        }
        node.setLvalue(false);
        node.setType(new IntType());
    }

    @Override
    public void visit(UnaryExprNode node) throws SemanticError {
        node.setScope(currentScope());

        node.getExpr().accept(this);

        UnaryExprNode.Operator op = node.getOp();

        ExprNode expr = node.getExpr();
        Position position = expr.getPosition();
        String text = expr.getText();
        Type type = expr.getType();

        if (op == UnaryExprNode.Operator.preInc || op == UnaryExprNode.Operator.preDec) {
            // ++a
            if (!(type instanceof IntType)) {
                semanticError.error(position, "\"" + text + "\" is not int type.");
                throw semanticError;
            }
            if (!expr.getLvalue()) {
                semanticError.error(position, "\"" + text + "\" is not lvalue.");
                throw semanticError;
            }
            node.setLvalue(true);
            node.setType(new IntType());
        } else if (op == UnaryExprNode.Operator.signPos || op == UnaryExprNode.Operator.signNeg) {
            // +a, -a
            if (!(type instanceof IntType)) {
                semanticError.error(position, "\"" + text + "\" is not int type.");
                throw semanticError;
            }
            node.setLvalue(false);
            node.setType(new IntType());
        } else if (op == UnaryExprNode.Operator.logicalNot) {
            // !a
            if (!(type instanceof BoolType)) {
                semanticError.error(position, "\"" + text + "\" is not bool type.");
                throw semanticError;
            }
            node.setLvalue(false);
            node.setType(new BoolType());
        } else {
            // ~a
            assert op == UnaryExprNode.Operator.bitwiseComplement;
            if (!(type instanceof IntType)) {
                semanticError.error(position, "\"" + text + "\" is not int type.");
                throw semanticError;
            }
            node.setLvalue(false);
            node.setType(new IntType());
        }
    }

    @Override
    public void visit(BinaryExprNode node) throws SemanticError {
        node.setScope(currentScope());

        node.getLhs().accept(this);
        node.getRhs().accept(this);

        BinaryExprNode.Operator op = node.getOp();

        ExprNode lExpr = node.getLhs();
        ExprNode rExpr = node.getRhs();
        Position lPosition = lExpr.getPosition();
        Position rPosition = rExpr.getPosition();
        Type lType = lExpr.getType();
        Type rType = rExpr.getType();
        String lText = lExpr.getText();
        String rText = rExpr.getText();

        if (op == BinaryExprNode.Operator.mul ||
                op == BinaryExprNode.Operator.div ||
                op == BinaryExprNode.Operator.mod ||
                op == BinaryExprNode.Operator.sub ||
                op == BinaryExprNode.Operator.shiftLeft ||
                op == BinaryExprNode.Operator.shiftRight ||
                op == BinaryExprNode.Operator.bitwiseAnd ||
                op == BinaryExprNode.Operator.bitwiseXor ||
                op == BinaryExprNode.Operator.bitwiseOr) {
            // *  /  %  -  <<  >>  &  ^  |  for int
            if (!(lType instanceof IntType)) {
                semanticError.error(lPosition, "\"" + lText + "\" is not int type.");
                throw semanticError;
            }
            if (!(rType instanceof IntType)) {
                semanticError.error(rPosition, "\"" + rText + "\" is not int type.");
                throw semanticError;
            }
            node.setLvalue(false);
            node.setType(new IntType());
        } else if (op == BinaryExprNode.Operator.add) {
            // +  for int or string
            if ((lType instanceof IntType) && (rType instanceof IntType)) {
                node.setLvalue(false);
                node.setType(new IntType());
            } else if ((lType instanceof StringType) && (rType instanceof StringType)) {
                node.setLvalue(false);
                node.setType(new StringType());
            } else {
                semanticError.error(node.getPosition(), "Invalid expression \"" + node.getText() + "\".");
                throw semanticError;
            }
        } else if (op == BinaryExprNode.Operator.less ||
                op == BinaryExprNode.Operator.greater ||
                op == BinaryExprNode.Operator.lessEqual ||
                op == BinaryExprNode.Operator.greaterEqual) {
            // <  >  <=  >=  for int or string
            if (((lType instanceof IntType) && (rType instanceof IntType)) ||             // int
                    ((lType instanceof StringType) && (rType instanceof StringType))) {   // string
                node.setLvalue(false);
                node.setType(new BoolType());
            } else {
                semanticError.error(node.getPosition(), "Invalid expression \"" + node.getText() + "\".");
                throw semanticError;
            }
        } else if (op == BinaryExprNode.Operator.equal
                || op == BinaryExprNode.Operator.notEqual) {
            // ==  !=  for int, bool, string, ArrayType and ClassType
            if (((lType instanceof IntType) && (rType instanceof IntType)) ||
                    ((lType instanceof BoolType) && (rType instanceof BoolType)) ||
                    ((lType instanceof StringType) && (rType instanceof StringType)) ||
                    ((lType instanceof ArrayType) && (rType instanceof NullType)) ||
                    ((lType instanceof NullType) && (rType instanceof ArrayType)) ||
                    ((lType instanceof ClassType) && (rType instanceof NullType)) ||
                    ((lType instanceof NullType) && (rType instanceof ClassType)) ||
                    ((lType instanceof NullType) && (rType instanceof NullType))) {
                node.setLvalue(false);
                node.setType(new BoolType());
            } else {
                semanticError.error(node.getPosition(), "Invalid expression \"" + node.getText() + "\".");
                throw semanticError;
            }
        } else if (op == BinaryExprNode.Operator.logicalAnd ||
                op == BinaryExprNode.Operator.logicalOr) {
            // &&  ||  for bool
            if (!(lType instanceof BoolType)) {
                semanticError.error(lPosition, "\"" + lText + "\" is not bool type.");
                throw semanticError;
            }
            if (!(rType instanceof BoolType)) {
                semanticError.error(rPosition, "\"" + rText + "\" is not bool type.");
                throw semanticError;
            }
            node.setLvalue(false);
            node.setType(new BoolType());
        } else {
            assert op == BinaryExprNode.Operator.assign;
            if (!lExpr.getLvalue()) {
                semanticError.error(lPosition, "\"" + lText + "\" is not lvalue.");
                throw semanticError;
            }
            if (Type.canNotAssign(lType, rType)) {
                semanticError.error(node.getPosition(), "Type of rhs \"" +
                        rType.toString() + "\" is not \"" + lType.toString() + "\".");
                throw semanticError;
            }
            node.setLvalue(false);
            node.setType(lType);
        }
    }

    @Override
    public void visit(NewExprNode node) throws SemanticError {
        node.setScope(currentScope());

        if (node.getDim() == -1) {
            assert node.getExprForDim() == null;
            throw semanticError;
        }

        node.getBaseType().accept(this);

        if (node.getDim() == 0) {
            Type type = typeTable.get(node.getBaseType());
            if (type.equals(new IntType()) || type.equals(new BoolType()) || type.equals(new StringType())) {
                semanticError.error(node.getBaseType().getPosition(),
                        "Cannot create an instance of type " + type.toString() + ".");
                throw semanticError;
            }
            node.setLvalue(true);
            node.setType(type);
        } else {
            ArrayList<ExprNode> exprForDim = node.getExprForDim();
            for (ExprNode expr : exprForDim) {
                assert expr != null;
                expr.accept(this);
                if (!(expr.getType() instanceof IntType)) {
                    semanticError.error(expr.getPosition(),
                            "Expression \"" + expr.getText() + "\" is not int type.");
                    throw semanticError;
                }
            }

            Type baseType = typeTable.get(node.getBaseType());
            node.setLvalue(true);
            node.setType(new ArrayType(baseType, node.getDim()));
        }
    }

    @Override
    public void visit(MemberExprNode node) throws SemanticError {
        node.setScope(currentScope());

        node.getExpr().accept(this);

        ExprNode expr = node.getExpr();
        Type type = expr.getType();
        String name = node.getIdentifier();

        String errorMessage = "\"" + expr.getText() + "\" has no member or method named \"" + name + "\".";
        if (type instanceof ArrayType) {
            if (!((ArrayType) type).hasMethod(name)) {
                semanticError.error(expr.getPosition(), errorMessage);
                throw semanticError;
            }
            node.setLvalue(false);
            node.setType(new FunctionType(name, type));
        } else if (type instanceof StringType) {
            if (!((StringType) type).hasMethod(name)) {
                semanticError.error(expr.getPosition(), errorMessage);
                throw semanticError;
            }
            node.setLvalue(false);
            node.setType(new FunctionType(name, type));
        } else if (type instanceof ClassType) {
            if (!((ClassType) type).hasMemberOrMethod(name)) {
                semanticError.error(expr.getPosition(), errorMessage);
                throw semanticError;
            }
            if (((ClassType) type).hasMember(name)) {
                VariableEntity member = ((ClassType) type).getMember(name);
                TypeNode memberType = member.getType();
                node.setLvalue(true);
                node.setType(typeTable.get(memberType));
            } else {
                assert ((ClassType) type).hasMethod(name);
                node.setLvalue(false);
                node.setType(new FunctionType(name, type));
            }
        } else {
            semanticError.error(expr.getPosition(), errorMessage);
            throw semanticError;
        }
    }

    @Override
    public void visit(FuncCallExprNode node) throws SemanticError {
        node.setScope(currentScope());

        ExprNode funcName = node.getFuncName();
        if (funcName instanceof MemberExprNode) {
            funcName.accept(this);
            if (!(funcName.getType() instanceof FunctionType)) {
                semanticError.error(funcName.getPosition(), "\"" + funcName.getText() + "\" is not a method.");
                throw semanticError;
            }
        } else if (funcName instanceof IdExprNode) {
            Entity entity = currentScope().getEntity(((IdExprNode) funcName).getIdentifier());
            if (entity == null) {
                semanticError.error(funcName.getPosition(),
                        "Unresolved reference \"" + ((IdExprNode) funcName).getIdentifier() + "\".");
                throw semanticError;
            } else if (entity instanceof VariableEntity) {
                semanticError.error(funcName.getPosition(),
                        "\"" + ((IdExprNode) funcName).getIdentifier() + "\" is not a function.");
                throw semanticError;
            }
        } else {
            semanticError.error(funcName.getPosition(), "\"" + node.getText() + "\" is not a function.");
            throw semanticError;
        }

        ArrayList<ExprNode> parameters = node.getParameters();
        for (ExprNode parameter : parameters)
            parameter.accept(this);

        FunctionEntity function;
        ArrayList<VariableEntity> funcParameters;
        if (funcName instanceof MemberExprNode) {
            assert funcName.getType() instanceof FunctionType;
            FunctionType methodType = (FunctionType) funcName.getType();
            if (methodType.getType() instanceof ArrayType) {
                function = ((ArrayType) methodType.getType()).getMethod(methodType.getName());
            } else if (methodType.getType() instanceof StringType) {
                function = ((StringType) methodType.getType()).getMethod(methodType.getName());
            } else {
                assert methodType.getType() instanceof ClassType;
                function = ((ClassType) methodType.getType()).getMethod(methodType.getName());
            }
        } else
            function = (FunctionEntity) currentScope().getEntity(((IdExprNode) funcName).getIdentifier());

        funcParameters = (function).getParameters();
        if (parameters.size() != funcParameters.size()) {
            semanticError.error(node.getPosition(), "Number of parameters is not consistent.");
            throw semanticError;
        }
        for (int i = 0; i < parameters.size(); i++) {
            ExprNode rhs = parameters.get(i);
            VariableEntity lhs = funcParameters.get(i);
            Type rType = rhs.getType();
            Type lType = typeTable.get(lhs.getType());
            if (Type.canNotAssign(lType, rType)) {
                semanticError.error(rhs.getPosition(), "Type of \"" + rhs.getText()
                        + "\" is not \"" + lType.toString() + "\".");
                throw semanticError;
            }
        }

        node.setLvalue(false);
        node.setEntity(function);
        node.setType(typeTable.get(function.getReturnType()));
    }

    @Override
    public void visit(SubscriptExprNode node) throws SemanticError {
        node.setScope(currentScope());

        node.getName().accept(this);
        node.getIndex().accept(this);

        ExprNode name = node.getName();
        ExprNode index = node.getIndex();

        Type nameType = name.getType();
        if (!(nameType instanceof ArrayType)) {
            semanticError.error(name.getPosition(), "\"" + name.getText() + "\" is not array type.");
            throw semanticError;
        }
        Type indexType = index.getType();
        if (!(indexType instanceof IntType)) {
            semanticError.error(index.getPosition(), "\"" + index.getText() + "\" is not int type");
            throw semanticError;
        }

        node.setLvalue(true);
        Type baseType = ((ArrayType) nameType).getBaseType();
        int dims = ((ArrayType) nameType).getDims();
        if (dims == 1)
            node.setType(baseType);
        else
            node.setType(new ArrayType(baseType, dims - 1));
    }

    @Override
    public void visit(ThisExprNode node) throws SemanticError {
        node.setScope(currentScope());

        if (!currentScope().inMethodScope()) {
            semanticError.error(node.getPosition(), "The \"this\" is not in a method.");
            throw semanticError;
        }
        node.setLvalue(true);
        node.setType(currentScope().getClassType());
    }

    @Override
    public void visit(IdExprNode node) throws SemanticError {
        node.setScope(currentScope());

        Entity entity = currentScope().getEntity(node.getIdentifier());
        if (entity == null) {
            semanticError.error(node.getPosition(),
                    "Unresolved reference \"" + node.getIdentifier() + "\".");
            throw semanticError;
        } else if (entity instanceof FunctionEntity) {
            semanticError.error(node.getPosition(),
                    "\"" + node.getIdentifier() + "\" is not a variable reference.");
            throw semanticError;
        }

        assert entity instanceof VariableEntity;
        entity.setReferred();
        node.setEntity(entity);
        node.setLvalue(true);
        node.setType(typeTable.get(((VariableEntity) entity).getType()));
    }

    @Override
    public void visit(BoolConstantNode node) {
        node.setScope(currentScope());

        node.setLvalue(false);
        node.setType(new BoolType());
    }

    @Override
    public void visit(IntConstantNode node) {
        node.setScope(currentScope());

        node.setLvalue(false);
        node.setType(new IntType());
    }

    @Override
    public void visit(StringConstantNode node) {
        node.setScope(currentScope());

        node.setLvalue(false);
        node.setType(new StringType());
    }

    @Override
    public void visit(NullNode node) {
        node.setScope(currentScope());

        node.setLvalue(false);
        node.setType(new NullType());
    }
}
