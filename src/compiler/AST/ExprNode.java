package compiler.AST;

import compiler.Entity.Entity;
import compiler.Type.Type;
import compiler.Utility.Position;
import compiler.Instr.Operand.Operand;

abstract public class ExprNode extends BaseNode {
    private String text;
    private Entity entity;
    private Boolean lvalue;
    private Type type;
    private Operand result;
    private Operand lvalueResult;

    public ExprNode(Position position, String text) {
        super(position);
        this.text = text;
        lvalue = null;
        entity = null;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Boolean getLvalue() {
        return lvalue;
    }

    public void setLvalue(boolean lValue) {
        this.lvalue = lValue;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Operand getResult() {
        return result;
    }

    public void setResult(Operand result) {
        this.result = result;
    }

    public Operand getLvalueResult() {
        return lvalueResult;
    }

    public void setLvalueResult(Operand lvalueResult) {
        this.lvalueResult = lvalueResult;
    }

    @Override
    abstract public String toString();
}