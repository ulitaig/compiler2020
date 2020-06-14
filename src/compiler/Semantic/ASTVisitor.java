package compiler.Semantic;

import compiler.AST.*;
import compiler.Utility.SemanticError;

public interface ASTVisitor {

    void visit(ProgramNode node) throws SemanticError;

    void visit(BasicTypeNode node) throws SemanticError;
    void visit(ClassTypeNode node) throws SemanticError;
    void visit(ArrayTypeNode node) throws SemanticError;

    void visit(VarNode node) throws SemanticError;
    void visit(FunctionNode node) throws SemanticError;
    void visit(ClassNode node) throws SemanticError;

    void visit(BlockNode node) throws SemanticError;
    void visit(VariableStmtNode node) throws SemanticError;
    void visit(IfStmtNode node) throws SemanticError;
    void visit(WhileStmtNode node) throws SemanticError;
    void visit(ForStmtNode node) throws SemanticError;
    void visit(ReturnStmtNode node) throws SemanticError;
    void visit(BreakStmtNode node) throws SemanticError;
    void visit(ContinueStmtNode node) throws SemanticError;
    void visit(ExprStmtNode node) throws SemanticError;

    void visit(PostfixExprNode node) throws SemanticError;
    void visit(UnaryExprNode node) throws SemanticError;
    void visit(BinaryExprNode node) throws SemanticError;
    void visit(NewExprNode node) throws SemanticError;
    void visit(MemberExprNode node) throws SemanticError;
    void visit(FuncCallExprNode node) throws SemanticError;
    void visit(SubscriptExprNode node) throws SemanticError;
    void visit(ThisExprNode node) throws SemanticError;
    void visit(IdExprNode node) throws SemanticError;

    void visit(BoolConstantNode node);
    void visit(IntConstantNode node);
    void visit(StringConstantNode node);
    void visit(NullNode node);
}
