package compiler.AST;

import compiler.Semantic.ASTVisitor;
import compiler.Type.ClassType;
import compiler.Entity.*;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;


import java.util.ArrayList;

public class ClassNode extends UnitNode {
    private String identifier;
    private ArrayList<VarNode> varList;
    private FunctionNode constructor;
    private ArrayList<FunctionNode> funcList;

    public ClassNode(Position position, String identifier, ArrayList<VarNode> varList,
                     FunctionNode constructor, ArrayList<FunctionNode> funcList) {
        super(position);
        this.identifier = identifier;
        this.varList = varList;
        this.constructor = constructor;
        this.funcList = funcList;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ArrayList<VarNode> getVarList() {
        return varList;
    }

    public void addVar(VarNode var) {
        varList.add(var);
    }

    public ArrayList<FunctionNode> getFuncList() {
        return funcList;
    }

    public void addFunction(FunctionNode function) {
        funcList.add(function);
    }

    public boolean hasConstructor() {
        return constructor != null;
    }

    public FunctionNode getConstructor() {
        return constructor;
    }

    public ClassType getClassType() {
        ArrayList<VariableEntity> members = new ArrayList<>();
        FunctionEntity constructor = null;
        ArrayList<FunctionEntity> methods = new ArrayList<>();

        for (VarNode varNode : varList)
            members.add(varNode.getEntity(VariableEntity.EntityType.member));
        if (this.constructor != null)
            constructor = this.constructor.getEntity(FunctionEntity.EntityType.constructor);
        for (FunctionNode functionNode : funcList)
            methods.add(functionNode.getEntity(FunctionEntity.EntityType.method));

        return new ClassType(identifier, members, constructor, methods);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("<ClassNode>\n");
        string.append("identifier = ").append(identifier).append("\n");
        string.append("members:\n");
        for (VarNode var : varList)
            string.append(var.toString());
        if (hasConstructor())
            string.append("constructor = ").append(constructor.toString());
        string.append("methods:\n");
        for (FunctionNode method : funcList)
            string.append(method.toString());
        return string.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}