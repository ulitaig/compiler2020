// Generated from D:/Courses/compiler/compiler/src/compiler/Parser\MX.g4 by ANTLR 4.8
package compiler.Parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link MXParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface MXVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link MXParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(MXParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#programUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgramUnit(MXParser.ProgramUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#classDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDef(MXParser.ClassDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#functionDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDef(MXParser.FunctionDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#constructorDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorDef(MXParser.ConstructorDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#variableDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDef(MXParser.VariableDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#variableList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableList(MXParser.VariableListContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#singleVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleVariable(MXParser.SingleVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#parameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterList(MXParser.ParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(MXParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(MXParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#nonArrayType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonArrayType(MXParser.NonArrayTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(MXParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by the {@code blockStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockStmt(MXParser.BlockStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code variableStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableStmt(MXParser.VariableStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprStmt(MXParser.ExprStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code controlStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitControlStmt(MXParser.ControlStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code emptyStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyStmt(MXParser.EmptyStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ifStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStmt(MXParser.IfStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code whileStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStmt(MXParser.WhileStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code forStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStmt(MXParser.ForStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code returnStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStmt(MXParser.ReturnStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code breakStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStmt(MXParser.BreakStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code continueStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStmt(MXParser.ContinueStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code newExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNewExpr(MXParser.NewExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code thisExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThisExpr(MXParser.ThisExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unaryExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpr(MXParser.UnaryExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subscriptExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubscriptExpr(MXParser.SubscriptExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code memberExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberExpr(MXParser.MemberExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code binaryExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryExpr(MXParser.BinaryExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code funcCallExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncCallExpr(MXParser.FuncCallExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code postfixExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixExpr(MXParser.PostfixExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubExpr(MXParser.SubExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code constExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstExpr(MXParser.ConstExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code idExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdExpr(MXParser.IdExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#exprList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprList(MXParser.ExprListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code noCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNoCreator(MXParser.NoCreatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arrayCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayCreator(MXParser.ArrayCreatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code classCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassCreator(MXParser.ClassCreatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code singleCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleCreator(MXParser.SingleCreatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link MXParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(MXParser.ConstantContext ctx);
}