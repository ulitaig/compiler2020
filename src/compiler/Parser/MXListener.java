// Generated from D:/Courses/compiler/compiler/src/compiler/Parser\MX.g4 by ANTLR 4.8
package compiler.Parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link MXParser}.
 */
public interface MXListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link MXParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(MXParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(MXParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#programUnit}.
	 * @param ctx the parse tree
	 */
	void enterProgramUnit(MXParser.ProgramUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#programUnit}.
	 * @param ctx the parse tree
	 */
	void exitProgramUnit(MXParser.ProgramUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#classDef}.
	 * @param ctx the parse tree
	 */
	void enterClassDef(MXParser.ClassDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#classDef}.
	 * @param ctx the parse tree
	 */
	void exitClassDef(MXParser.ClassDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#functionDef}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDef(MXParser.FunctionDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#functionDef}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDef(MXParser.FunctionDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#constructorDef}.
	 * @param ctx the parse tree
	 */
	void enterConstructorDef(MXParser.ConstructorDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#constructorDef}.
	 * @param ctx the parse tree
	 */
	void exitConstructorDef(MXParser.ConstructorDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#variableDef}.
	 * @param ctx the parse tree
	 */
	void enterVariableDef(MXParser.VariableDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#variableDef}.
	 * @param ctx the parse tree
	 */
	void exitVariableDef(MXParser.VariableDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#variableList}.
	 * @param ctx the parse tree
	 */
	void enterVariableList(MXParser.VariableListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#variableList}.
	 * @param ctx the parse tree
	 */
	void exitVariableList(MXParser.VariableListContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#singleVariable}.
	 * @param ctx the parse tree
	 */
	void enterSingleVariable(MXParser.SingleVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#singleVariable}.
	 * @param ctx the parse tree
	 */
	void exitSingleVariable(MXParser.SingleVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void enterParameterList(MXParser.ParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void exitParameterList(MXParser.ParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(MXParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(MXParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(MXParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(MXParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#nonArrayType}.
	 * @param ctx the parse tree
	 */
	void enterNonArrayType(MXParser.NonArrayTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#nonArrayType}.
	 * @param ctx the parse tree
	 */
	void exitNonArrayType(MXParser.NonArrayTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(MXParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(MXParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by the {@code blockStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterBlockStmt(MXParser.BlockStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code blockStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitBlockStmt(MXParser.BlockStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code variableStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterVariableStmt(MXParser.VariableStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code variableStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitVariableStmt(MXParser.VariableStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exprStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterExprStmt(MXParser.ExprStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exprStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitExprStmt(MXParser.ExprStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code controlStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterControlStmt(MXParser.ControlStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code controlStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitControlStmt(MXParser.ControlStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code emptyStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStmt(MXParser.EmptyStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code emptyStmt}
	 * labeled alternative in {@link MXParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStmt(MXParser.EmptyStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ifStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStmt(MXParser.IfStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ifStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStmt(MXParser.IfStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code whileStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStmt(MXParser.WhileStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code whileStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStmt(MXParser.WhileStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code forStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStmt(MXParser.ForStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code forStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStmt(MXParser.ForStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code returnStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStmt(MXParser.ReturnStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code returnStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStmt(MXParser.ReturnStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code breakStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void enterBreakStmt(MXParser.BreakStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code breakStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void exitBreakStmt(MXParser.BreakStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code continueStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void enterContinueStmt(MXParser.ContinueStmtContext ctx);
	/**
	 * Exit a parse tree produced by the {@code continueStmt}
	 * labeled alternative in {@link MXParser#controlStatement}.
	 * @param ctx the parse tree
	 */
	void exitContinueStmt(MXParser.ContinueStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code newExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterNewExpr(MXParser.NewExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code newExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitNewExpr(MXParser.NewExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code thisExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterThisExpr(MXParser.ThisExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code thisExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitThisExpr(MXParser.ThisExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unaryExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpr(MXParser.UnaryExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unaryExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpr(MXParser.UnaryExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subscriptExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterSubscriptExpr(MXParser.SubscriptExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subscriptExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitSubscriptExpr(MXParser.SubscriptExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code memberExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterMemberExpr(MXParser.MemberExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code memberExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitMemberExpr(MXParser.MemberExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterBinaryExpr(MXParser.BinaryExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitBinaryExpr(MXParser.BinaryExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code funcCallExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterFuncCallExpr(MXParser.FuncCallExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code funcCallExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitFuncCallExpr(MXParser.FuncCallExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code postfixExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterPostfixExpr(MXParser.PostfixExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code postfixExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitPostfixExpr(MXParser.PostfixExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterSubExpr(MXParser.SubExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitSubExpr(MXParser.SubExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterConstExpr(MXParser.ConstExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitConstExpr(MXParser.ConstExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code idExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterIdExpr(MXParser.IdExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code idExpr}
	 * labeled alternative in {@link MXParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitIdExpr(MXParser.IdExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#exprList}.
	 * @param ctx the parse tree
	 */
	void enterExprList(MXParser.ExprListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#exprList}.
	 * @param ctx the parse tree
	 */
	void exitExprList(MXParser.ExprListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code noCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 */
	void enterNoCreator(MXParser.NoCreatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code noCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 */
	void exitNoCreator(MXParser.NoCreatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arrayCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 */
	void enterArrayCreator(MXParser.ArrayCreatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arrayCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 */
	void exitArrayCreator(MXParser.ArrayCreatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code classCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 */
	void enterClassCreator(MXParser.ClassCreatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code classCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 */
	void exitClassCreator(MXParser.ClassCreatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 */
	void enterSingleCreator(MXParser.SingleCreatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleCreator}
	 * labeled alternative in {@link MXParser#creator}.
	 * @param ctx the parse tree
	 */
	void exitSingleCreator(MXParser.SingleCreatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MXParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(MXParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link MXParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(MXParser.ConstantContext ctx);
}