package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

import java.util.ArrayList;

public class FuncCallExprNode extends ExprNode {
    private ExprNode funcName;
    private ArrayList<ExprNode> parameters;

    public FuncCallExprNode(Position position, String text, ExprNode funcName, ArrayList<ExprNode> parameters) {
        super(position, text);
        this.funcName = funcName;
        this.parameters = parameters;
    }

    public ExprNode getFuncName() {
        return funcName;
    }

    public void setFuncName(ExprNode funcName) {
        this.funcName = funcName;
    }

    public ArrayList<ExprNode> getParameters() {
        return parameters;
    }

    public void addParameter(ExprNode parameter) {
        parameters.add(parameter);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("<FuncCallExprNode>\n");
        string.append("funcName:\n").append(funcName.toString());
        string.append("parameters:\n");
        for (ExprNode parameter : parameters)
            string.append(parameter.toString());
        return string.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
