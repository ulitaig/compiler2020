package compiler.Entity;

import compiler.AST.*;
import compiler.Utility.Position;

import java.util.ArrayList;

public class FunctionEntity extends Entity {
    public enum EntityType {
        function, method, constructor
    }

    private TypeNode returnType;
    private ArrayList<VariableEntity> parameters;
    private StmtNode bodyStmt;
    private EntityType entityType;

    public FunctionEntity(String name, Position position, TypeNode returnType,
                          ArrayList<VariableEntity> parameters, StmtNode bodyStmt, EntityType entityType) {
        super(name, position);
        this.returnType = returnType;
        this.parameters = parameters;
        this.bodyStmt = bodyStmt;
        this.entityType = entityType;
    }

    public TypeNode getReturnType() {
        return returnType;
    }

    public ArrayList<VariableEntity> getParameters() {
        return parameters;
    }

    public StmtNode getBodyStmt() {
        return bodyStmt;
    }

    public EntityType getEntityType() {
        return entityType;
    }
}
