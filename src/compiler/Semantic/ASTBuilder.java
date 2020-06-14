package compiler.Semantic;

import compiler.AST.*;
import compiler.Parser.MXBaseVisitor;
import compiler.Parser.MXParser;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.Collection;

public class ASTBuilder  extends MXBaseVisitor<BaseNode> {

    /*public <T> T genericMethod(Class<T> tClass)throws InstantiationException ,
            IllegalAccessException{
        T instance = tClass.newInstance();
        return instance;
    }*/

    @Override
    //ProgramNode
    public BaseNode visitProgram(MXParser.ProgramContext ctx) {
        ArrayList<UnitNode> programUnits = new ArrayList<>();
        for (ParserRuleContext programUnit : ctx.programUnit()) {
            BaseNode unit = visit(programUnit);
            if (unit instanceof VarNodeList)
                programUnits.addAll(((VarNodeList) unit).getVarNodes());
            else if (unit != null)
                programUnits.add((UnitNode) unit);
        }
        return new ProgramNode(new Position(ctx.getStart()), programUnits);
    }

    @Override
    //UnitNode
    public BaseNode visitProgramUnit(MXParser.ProgramUnitContext ctx) {
        if (ctx.functionDef() != null) {
            return visit(ctx.functionDef());
        } else if (ctx.classDef() != null) {
            return visit(ctx.classDef());
        } else if (ctx.variableDef() != null) {
            return visit(ctx.variableDef());
        } else return null;
    }

    @Override
    //ClassNode
    public BaseNode visitClassDef(MXParser.ClassDefContext ctx) {
        ArrayList<VarNode> varList = new ArrayList<>();
        ArrayList<FunctionNode> funcList = new ArrayList<>();
        String identifier = ctx.Identifier().getText();
        for (ParserRuleContext variableDef : ctx.variableDef()) {
            VarNodeList varNodeList = (VarNodeList) visit(variableDef);
            varList.addAll(varNodeList.getVarNodes());
        }
        for (ParserRuleContext functionDef : ctx.functionDef())
            funcList.add((FunctionNode) visit(functionDef));

        FunctionNode constructor = null;
        if (ctx.constructorDef().size() > 1)
            throw new SemanticError(new Position(ctx.constructorDef(0).getStart()),
                    "Class \"" + identifier + "\" has multiple constructors.");
        for (MXParser.ConstructorDefContext constructorDef : ctx.constructorDef()) {
            if (!constructorDef.Identifier().getText().equals(identifier))
            throw new SemanticError(new Position(constructorDef.getStart()),
                    "Constructor name not equal\"" + identifier + "()\".");
            else{
                //visit(constructorDef);
                constructor = (FunctionNode) visit(constructorDef);
            }
        }

        return new ClassNode(new Position(ctx.getStart()), identifier, varList, constructor, funcList);
    }

    @Override
    //FunctionNode
    public BaseNode visitFunctionDef(MXParser.FunctionDefContext ctx) {
        String identifier = ctx.Identifier().getText();
        ArrayList<VarNode> parameters = new ArrayList<>();
        TypeNode type;
        if (ctx.type() != null)
            type = (TypeNode) visit(ctx.type());
        else
            type = new BasicTypeNode(new Position(ctx.Void().getSymbol()), "void");
        if (ctx.parameterList() != null)
            parameters = ((VarNodeList) visit(ctx.parameterList())).getVarNodes();
        StmtNode statement = (StmtNode) visit(ctx.block());
        return new FunctionNode(new Position(ctx.getStart()), type, identifier, parameters, statement);
    }

    @Override
    //FunctionNode
    public BaseNode visitConstructorDef(MXParser.ConstructorDefContext ctx) {
        String identifier = ctx.Identifier().getText();
        TypeNode type = new BasicTypeNode(new Position(ctx.getStart()), "void");
        ArrayList<VarNode> parameters = new ArrayList<>();
        if (ctx.parameterList() != null)
            parameters = ((VarNodeList) visit(ctx.parameterList())).getVarNodes();
        StmtNode statement = (StmtNode) visit(ctx.block());
        return new FunctionNode(new Position(ctx.getStart()), type, identifier, parameters, statement);
    }

    @Override
    //VarNodeList
    public BaseNode visitVariableDef(MXParser.VariableDefContext ctx) {
        TypeNode type = (TypeNode) visit(ctx.type());
        ArrayList<VarNode> varNodes = ((VarNodeList) visit(ctx.variableList())).getVarNodes();
        for (VarNode varNode : varNodes)
            varNode.setType(type);
        return new VarNodeList(new Position(ctx.getStart()), varNodes);
    }

    @Override
    //VarNodeList
    public BaseNode visitVariableList(MXParser.VariableListContext ctx) {
        ArrayList<VarNode> varNodes = new ArrayList<>();
        for (ParserRuleContext singleVariable : ctx.singleVariable())
            varNodes.add((VarNode) visit(singleVariable));
        return new VarNodeList(new Position(ctx.getStart()), varNodes);
    }

    @Override
    //VarNode
    public BaseNode visitSingleVariable(MXParser.SingleVariableContext ctx) {
        Position position = new Position(ctx.getStart());
        TypeNode type = new BasicTypeNode(position, "SingleVariable");
        String identifier = ctx.Identifier().getText();
        ExprNode initExpr = ctx.expr() != null ? (ExprNode) visit(ctx.expr()) : null;
        return new VarNode(position, type, identifier, initExpr);
    }

    @Override
    //VarNodeList
    public BaseNode visitParameterList(MXParser.ParameterListContext ctx) {
        ArrayList<VarNode> varNodes = new ArrayList<>();
        for (ParserRuleContext parameter : ctx.parameter())
            varNodes.add((VarNode) visit(parameter));
        return new VarNodeList(new Position(ctx.getStart()), varNodes);
    }

    @Override
    //VarNode
    public BaseNode visitParameter(MXParser.ParameterContext ctx) {

        TypeNode type = (TypeNode) visit(ctx.type());
        String identifier = ctx.Identifier().getText();
        return new VarNode(new Position(ctx.getStart()), type, identifier, null);
    }

    @Override
    //TypeNode
    public BaseNode visitType(MXParser.TypeContext ctx) {
        if (ctx.nonArrayType() != null)
            return visit(ctx.nonArrayType());
        else return new ArrayTypeNode(new Position(ctx.getStart()), (TypeNode) visit(ctx.type()));
    }

    @Override
    //BasicTypeNode or ClassTypeNode
    public BaseNode visitNonArrayType(MXParser.NonArrayTypeContext ctx) {
        Position position = new Position(ctx.getStart());
        if (ctx.Bool() != null)
            return new BasicTypeNode(position, "bool");
        else if (ctx.Int() != null)
            return new BasicTypeNode(position, "int");
        else if (ctx.String() != null)
            return new BasicTypeNode(position, "string");
        else
            return new ClassTypeNode(position, ctx.Identifier().getText());
    }

    @Override
    //BlockNode
    public BaseNode visitBlock(MXParser.BlockContext ctx) {
        ArrayList<StmtNode> statements = new ArrayList<>();
        for (ParserRuleContext statement : ctx.statement()) {
            StmtNode stmtNode = (StmtNode) visit(statement);
            if (stmtNode != null) statements.add(stmtNode);
        }
        return new BlockNode(new Position(ctx.getStart()), statements);
    }

    @Override
    //BlockNode
    public BaseNode visitBlockStmt(MXParser.BlockStmtContext ctx) {
        return visit(ctx.block());
    }

    @Override
    //VariableStmtNode
    public BaseNode visitVariableStmt(MXParser.VariableStmtContext ctx) {
        ArrayList<VarNode> varNodes = ((VarNodeList) visit(ctx.variableDef())).getVarNodes();
        return new VariableStmtNode(new Position(ctx.getStart()), varNodes);
    }

    @Override
    //ExprStmtNode
    public BaseNode visitExprStmt(MXParser.ExprStmtContext ctx) {
        ExprNode expr = (ExprNode) visit(ctx.expr());
        return new ExprStmtNode(new Position(ctx.getStart()), expr);
    }

    /*@Override
    public BaseNode visitControlStmt(MXParser.ControlStmtContext ctx) {
        // return ExprStmtNode
    }*/

    @Override
    //IfStmtNode
    public BaseNode visitIfStmt(MXParser.IfStmtContext ctx) {
        ExprNode cond = (ExprNode) visit(ctx.expr());
        StmtNode thenPart = (StmtNode) visit(ctx.statement(0));
        if (thenPart == null)
            thenPart = new BlockNode(new Position(ctx.statement(0).getStart()), new ArrayList<>());
        StmtNode elsePart =  null;
        if(ctx.statement(1) != null)
            elsePart = (StmtNode) visit(ctx.statement(1));
        return new IfStmtNode(new Position(ctx.getStart()), cond, thenPart, elsePart);
    }

    @Override
    //WhileStmtNode
    public BaseNode visitWhileStmt(MXParser.WhileStmtContext ctx) {
        ExprNode cond = (ExprNode) visit(ctx.expr());
        StmtNode body = (StmtNode) visit(ctx.statement());
        if (body == null)
            body = new BlockNode(new Position(ctx.statement().getStart()), new ArrayList<>());
        return new WhileStmtNode(new Position(ctx.getStart()), cond, body);
    }

    @Override
    //ForStmtNode
    public BaseNode visitForStmt(MXParser.ForStmtContext ctx) {
        ExprNode init = ctx.init != null ? (ExprNode) visit(ctx.init) : null;
        ExprNode cond = ctx.cond != null ? (ExprNode) visit(ctx.cond) : null;
        ExprNode step = ctx.step != null ? (ExprNode) visit(ctx.step) : null;
        StmtNode body = (StmtNode) visit(ctx.statement());
        if (body == null)
            body = new BlockNode(new Position(ctx.statement().getStart()), new ArrayList<>());
        return new ForStmtNode(new Position(ctx.getStart()), init, cond, step, body);
    }

    @Override
    //ReturnStmtNode
    public BaseNode visitReturnStmt(MXParser.ReturnStmtContext ctx) {
        ExprNode returnValue = ctx.expr() != null ? (ExprNode) visit(ctx.expr()) : null;
        return new ReturnStmtNode(new Position(ctx.getStart()), returnValue);
    }

    @Override
    //BreakStmtNode
    public BaseNode visitBreakStmt(MXParser.BreakStmtContext ctx) {
        return new BreakStmtNode(new Position(ctx.getStart()));
    }

    @Override
    //ContinueStmtNode
    public BaseNode visitContinueStmt(MXParser.ContinueStmtContext ctx) {
        return new ContinueStmtNode(new Position(ctx.getStart()));
    }

    @Override
    public BaseNode visitEmptyStmt(MXParser.EmptyStmtContext ctx) {
        return null;
    }

    @Override
    //ThisExprNode
    public BaseNode visitThisExpr(MXParser.ThisExprContext ctx) {
        return new ThisExprNode(new Position(ctx.getStart()), ctx.getText());
    }

    @Override
    //ConstExprNode
    public BaseNode visitConstExpr(MXParser.ConstExprContext ctx) {
        return visit(ctx.constant());
    }

    @Override
    //IdExprNode
    public BaseNode visitIdExpr(MXParser.IdExprContext ctx) {
        String identifier = ctx.Identifier().getText();
        return new IdExprNode(new Position(ctx.getStart()), ctx.getText(), identifier);
    }

    @Override
    //ExprNode
    public BaseNode visitSubExpr(MXParser.SubExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    //SubscriptExprNode
    public BaseNode visitSubscriptExpr(MXParser.SubscriptExprContext ctx) {
        ExprNode name = (ExprNode) visit(ctx.expr(0));
        ExprNode index = (ExprNode) visit(ctx.expr(1));
        return new SubscriptExprNode(new Position(ctx.getStart()), ctx.getText(), name, index);
    }

    @Override
    //MemberExprNode
    public BaseNode visitMemberExpr(MXParser.MemberExprContext ctx) {
        ExprNode expr = (ExprNode) visit(ctx.expr());
        String identifier = ctx.Identifier().getText();
        return new MemberExprNode(new Position(ctx.getStart()), ctx.getText(), expr, identifier);
    }

    @Override
    //FuncCallExprNode
    public BaseNode visitFuncCallExpr(MXParser.FuncCallExprContext ctx) {
        Position position = new Position(ctx.getStart());
        ExprNode funcName = (ExprNode) visit(ctx.expr());
        if (ctx.exprList() != null){
            FuncCallExprNode funcCallExprNode = (FuncCallExprNode) visit(ctx.exprList());
            return new FuncCallExprNode(position, ctx.getText(), funcName, funcCallExprNode.getParameters());
        }
        else {
            return new FuncCallExprNode(position, ctx.getText(), funcName, new ArrayList<>());
        }
    }

    @Override
    //NewExprNode
    public BaseNode visitNewExpr(MXParser.NewExprContext ctx) {
        NewExprNode newExprNode = (NewExprNode) visit(ctx.creator());
        return new NewExprNode(newExprNode.getPosition(), ctx.getText(),
                newExprNode.getBaseType(), newExprNode.getExprForDim(), newExprNode.getDim());
    }

    @Override
    //PostfixExprNode
    public BaseNode visitPostfixExpr(MXParser.PostfixExprContext ctx) {
        PostfixExprNode.Operator op = null;
        String opCtx = ctx.op.getText();
        if(opCtx.equals("++"))
                op = PostfixExprNode.Operator.postInc;
        else if(opCtx.equals("--"))
                op = PostfixExprNode.Operator.postDec;
        ExprNode expr = (ExprNode) visit(ctx.expr());
        return new PostfixExprNode(new Position(ctx.getStart()), ctx.getText(), op, expr);
    }

    @Override
    //UnaryExprNode
    public BaseNode visitUnaryExpr(MXParser.UnaryExprContext ctx) {
        UnaryExprNode.Operator op = null;
        String opCtx = ctx.op.getText();
        switch (opCtx) {
            case "++":
                op = UnaryExprNode.Operator.preInc;
                break;
            case "--":
                op = UnaryExprNode.Operator.preDec;
                break;
            case "+":
                op = UnaryExprNode.Operator.signPos;
                break;
            case "-":
                op = UnaryExprNode.Operator.signNeg;
                break;
            case "!":
                op = UnaryExprNode.Operator.logicalNot;
                break;
            case "~":
                op = UnaryExprNode.Operator.bitwiseComplement;
                break;
        }
        ExprNode expr = (ExprNode) visit(ctx.expr());
        return new UnaryExprNode(new Position(ctx.getStart()), ctx.getText(), op, expr);
    }

    @Override
    //BinaryExprNode
    public BaseNode visitBinaryExpr(MXParser.BinaryExprContext ctx) {
        BinaryExprNode.Operator op = null;
        String opCtx = ctx.op.getText();
        switch (opCtx) {
            case "+":
                op = BinaryExprNode.Operator.add;
                break;
            case "-":
                op = BinaryExprNode.Operator.sub;
                break;
            case "*":
                op = BinaryExprNode.Operator.mul;
                break;
            case "/":
                op = BinaryExprNode.Operator.div;
                break;
            case "%":
                op = BinaryExprNode.Operator.mod;
                break;
            case "<":
                op = BinaryExprNode.Operator.less;
                break;
            case ">":
                op = BinaryExprNode.Operator.greater;
                break;
            case "<=":
                op = BinaryExprNode.Operator.lessEqual;
                break;
            case ">=":
                op = BinaryExprNode.Operator.greaterEqual;
                break;
            case "==":
                op = BinaryExprNode.Operator.equal;
                break;
            case "!=":
                op = BinaryExprNode.Operator.notEqual;
                break;
            case "&":
                op = BinaryExprNode.Operator.bitwiseAnd;
                break;
            case "^":
                op = BinaryExprNode.Operator.bitwiseXor;
                break;
            case "|":
                op = BinaryExprNode.Operator.bitwiseOr;
                break;
            case "&&":
                op = BinaryExprNode.Operator.logicalAnd;
                break;
            case "||":
                op = BinaryExprNode.Operator.logicalOr;
                break;
            case "=":
                op = BinaryExprNode.Operator.assign;
                break;
            case "<<":
                op = BinaryExprNode.Operator.shiftLeft;
                break;
            case ">>":
                op = BinaryExprNode.Operator.shiftRight;
                break;
        }
        ExprNode lhs = (ExprNode) visit(ctx.lhs);
        ExprNode rhs = (ExprNode) visit(ctx.rhs);
        return new BinaryExprNode(new Position(ctx.getStart()), ctx.getText(), op, lhs, rhs);
    }



    @Override
    //FuncCallExprNode
    public BaseNode visitExprList(MXParser.ExprListContext ctx) {
        ArrayList<ExprNode> parameters = new ArrayList<>();
        for (ParserRuleContext expr : ctx.expr())
            parameters.add((ExprNode) visit(expr));
        return new FuncCallExprNode(new Position(ctx.getStart()), ctx.getText(), null, parameters);
    }

    @Override
    //Error
    public BaseNode visitNoCreator(MXParser.NoCreatorContext ctx) {
        throw new SemanticError(new Position(ctx.getStart()), "Invalid syntax \"" + ctx.getText() + "\".");
    }

    @Override
    //NewExprNode
    public BaseNode visitArrayCreator(MXParser.ArrayCreatorContext ctx) {
        TypeNode baseType = (TypeNode) visit(ctx.nonArrayType());

        int dim = 0;
        for (ParseTree child : ctx.children)
            if (child.getText().equals("["))
                dim++;

        ArrayList<ExprNode> exprForDim = new ArrayList<>();
        for (ParseTree expr : ctx.expr())
            exprForDim.add((ExprNode) visit(expr));

        return new NewExprNode(new Position(ctx.getStart()), ctx.getText(), baseType, exprForDim, dim);
    }

    @Override
    //NewExprNode
    public BaseNode visitClassCreator(MXParser.ClassCreatorContext ctx) {
        TypeNode baseType = (TypeNode) visit(ctx.nonArrayType());
        return new NewExprNode(new Position(ctx.getStart()), ctx.getText(), baseType, new ArrayList<>(), 0);
    }

    @Override
    //NewExprNode
    public BaseNode visitSingleCreator(MXParser.SingleCreatorContext ctx) {
        TypeNode baseType = (TypeNode) visit(ctx.nonArrayType());
        return new NewExprNode(new Position(ctx.getStart()), ctx.getText(), baseType, new ArrayList<>(), 0);
    }

    @Override
    //ConstExprNode
    public BaseNode visitConstant(MXParser.ConstantContext ctx) {
        Position position = new Position(ctx.getStart());
        String value = ctx.getText();
        if (ctx.BoolConstant() != null)
            return new BoolConstantNode(position, ctx.getText(), value.equals("true"));
        else if (ctx.IntegerConstant() != null)
            return new IntConstantNode(position, ctx.getText(), Long.parseLong(value));
        else if (ctx.StringConstant() != null)
            return new StringConstantNode(position, ctx.getText(), value.substring(1, value.length() - 1));
        else
            return new NullNode(position, ctx.getText());
    }

}
