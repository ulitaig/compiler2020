package compiler.AST;

import compiler.Entity.VariableEntity;
import compiler.Semantic.ASTVisitor;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;

public class VarNode extends UnitNode {
    private TypeNode type;
    private String identifier;
    private ExprNode varExpr;

    public VarNode(Position position, TypeNode type, String identifier, ExprNode varExpr) {
        super(position);
        this.type = type;
        this.identifier = identifier;
        this.varExpr = varExpr;
    }

    public void setType(TypeNode type) {
        this.type = type;
    }

    public TypeNode getType() {
        return type;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setVarExpr(ExprNode varExpr) {
        this.varExpr = varExpr;
    }

    public ExprNode getInitExpr() {
        return this.varExpr;
    }

    public boolean hasInitExpr() {
        return varExpr != null;
    }

    public VariableEntity getEntity(VariableEntity.EntityType entityType) {
        return new VariableEntity(identifier, getPosition(), type, varExpr, entityType);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("<VarNode>\n");
        string.append("type:\n").append(type.toString());
        string.append("identifier = ").append(identifier).append("\n");
        if (hasInitExpr())
            string.append("initExpr:\n").append(varExpr.toString());
        return string.toString();
    }

    @Override
    public void accept(ASTVisitor visitor) throws SemanticError {
        visitor.visit(this);
    }
}
