package compiler.Entity;

import compiler.AST.*;
import compiler.Instr.Operand.Operand;
import compiler.Utility.Position;

public class VariableEntity extends Entity {
    public enum EntityType {
        global, local, parameter, member
    }

    private TypeNode type;
    private ExprNode initExpr;
    private EntityType entityType;
    private Operand allocaAddr;

    public VariableEntity(String name, Position position, TypeNode type, ExprNode initExpr, EntityType entityType) {
        super(name, position);
        this.type = type;
        this.initExpr = initExpr;
        this.entityType = entityType;
    }

    public static VariableEntity newEntity(String identifier, String typeName) {
        Position location = new Position(0, 0);
        return new VariableEntity(identifier, new Position(0, 0),
                new BasicTypeNode(location, typeName),
                null, VariableEntity.EntityType.parameter);
    }

    public TypeNode getType() {
        return type;
    }

    public ExprNode getInitExpr() {
        return initExpr;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Operand getAllocaAddr() {
        return allocaAddr;
    }

    public void setAllocaAddr(Operand allocaAddr) {
        this.allocaAddr = allocaAddr;
    }
}
