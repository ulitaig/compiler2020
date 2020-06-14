package compiler.Semantic;

import compiler.AST.*;
import compiler.Entity.*;
import compiler.Type.*;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Scope {
    public enum ScopeType {
        programScope, classScope, functionScope, blockScope, loopScope
    }

    private Scope parentScope;
    private ArrayList<Scope> childrenScope;

    private Map<String, Entity> entities;
    private ScopeType scopeType;
    private TypeNode functionReturnType;
    private Type classType;
    boolean hasReturnStmt;

    public Scope(Scope parentScope, ScopeType scopeType, TypeNode functionReturnType, Type classType) {
        this.parentScope = parentScope;
        if (parentScope != null)
            parentScope.childrenScope.add(this);
        this.childrenScope = new ArrayList<>();
        this.entities = new HashMap<>();
        this.scopeType = scopeType;
        this.functionReturnType = functionReturnType;
        this.classType = classType;
        hasReturnStmt = false;
    }

    public void setReturn() {
        if (scopeType == ScopeType.functionScope)
            hasReturnStmt = true;
        else if(parentScope != null)
            parentScope.setReturn();
    }

    public boolean checkReturn(){
        return hasReturnStmt;
    }

    public Scope getParentScope() {
        return parentScope;
    }

    public ArrayList<Scope> getChildrenScope() {
        return childrenScope;
    }

    public Map<String, Entity> getEntities() {
        return entities;
    }

    public ScopeType getScopeType() {
        return scopeType;
    }

    public TypeNode getFunctionReturnType() {
        return functionReturnType;
    }

    public Type getClassType() {
        return classType;
    }

    public void addBuiltInFunction() {
        Position position = new Position(0, 0);
        ArrayList<VariableEntity> parameters;
        FunctionEntity function;

        parameters = new ArrayList<>();
        parameters.add(VariableEntity.newEntity("str", "string"));
        function = new FunctionEntity("print", position,
                new BasicTypeNode(position, "void"), parameters, null,
                FunctionEntity.EntityType.function);
        entities.put("print", function);

        parameters = new ArrayList<>();
        parameters.add(VariableEntity.newEntity("str", "string"));
        function = new FunctionEntity("println", position,
                new BasicTypeNode(position, "void"), parameters, null,
                FunctionEntity.EntityType.function);
        entities.put("println", function);

        parameters = new ArrayList<>();
        parameters.add(VariableEntity.newEntity("n", "int"));
        function = new FunctionEntity("printInt", position,
                new BasicTypeNode(position, "void"), parameters, null,
                FunctionEntity.EntityType.function);
        entities.put("printInt", function);

        parameters = new ArrayList<>();
        parameters.add(VariableEntity.newEntity("n", "int"));
        function = new FunctionEntity("printlnInt", position,
                new BasicTypeNode(position, "void"), parameters, null,
                FunctionEntity.EntityType.function);
        entities.put("printlnInt", function);

        parameters = new ArrayList<>();
        function = new FunctionEntity("getString", position,
                new BasicTypeNode(position, "string"), parameters, null,
                FunctionEntity.EntityType.function);
        entities.put("getString", function);

        parameters = new ArrayList<>();
        function = new FunctionEntity("getInt", position,
                new BasicTypeNode(position, "int"), parameters, null,
                FunctionEntity.EntityType.function);
        entities.put("getInt", function);

        parameters = new ArrayList<>();
        parameters.add(VariableEntity.newEntity("i", "int"));
        function = new FunctionEntity("toString", position,
                new BasicTypeNode(position, "string"), parameters, null,
                FunctionEntity.EntityType.function);
        entities.put("toString", function);
    }

    public void definedFunctionOrClass(String name, Position position, Scope globalScope,
                                       TypeTable typeTable) throws SemanticError {
        if (globalScope.entities.containsKey(name)) {
            Entity globalEntity = globalScope.entities.get(name);
            if (globalEntity instanceof FunctionEntity) {
                throw new SemanticError(position, "\"" + name + "\" is defined in global scope.");
            }
        } else if (typeTable.hasType(new ClassTypeNode(position, name))) {
            throw new SemanticError(position, "There is a class named \"" + name + "\".");
        }
    }

    public void declareEntity(UnitNode unit,
                              VariableEntity.EntityType varType, FunctionEntity.EntityType funcType,
                              Scope globalScope, TypeTable typeTable) throws SemanticError {
        Entity entity = null;

        if (unit instanceof VarNode) {
            entity = ((VarNode) unit).getEntity(varType);
        } else if (unit instanceof FunctionNode) {
            entity = ((FunctionNode) unit).getEntity(funcType);
        }

        assert entity != null;
        if (entities.containsKey(entity.getName())) {
            throw new SemanticError(unit.getPosition(), "Duplicate declaration of \"" + entity.getName() + "\".");
        } else
            entities.put(entity.getName(), entity);
    }

    public void declareEntity(Entity entity, Position position) throws SemanticError {
        assert entity != null;
        if (entities.containsKey(entity.getName())) {
            throw new SemanticError(position, "Duplicate declaration of \"" + entity.getName() + "\".");
        } else
            entities.put(entity.getName(), entity);
    }

    public void declareConstructor(FunctionNode unit) {
        FunctionEntity entity = unit.getEntity(FunctionEntity.EntityType.constructor);
        assert !entities.containsKey(entity.getName());
        entities.put(entity.getName(), entity);
    }

    public Entity getEntity(String name) {
        if (entities.containsKey(name)
                && (entities.get(name) instanceof VariableEntity
                || ((FunctionEntity) entities.get(name)).getEntityType() != FunctionEntity.EntityType.constructor))
            return entities.get(name);
        else if (parentScope != null)
            return parentScope.getEntity(name);
        else
            return null;
    }

    public FunctionEntity getFunctionEntity(String name) {
        if (entities.containsKey(name)
                && entities.get(name) instanceof FunctionEntity
                && ((FunctionEntity) entities.get(name)).getEntityType() != FunctionEntity.EntityType.constructor)
            return (FunctionEntity) entities.get(name);
        else if (parentScope != null)
            return parentScope.getFunctionEntity(name);
        else
            return null;
    }

    public FunctionEntity getFunctionEntityIncludeConstructor(String name) {
        if (entities.containsKey(name) && entities.get(name) instanceof FunctionEntity)
            return (FunctionEntity) entities.get(name);
        else if (parentScope != null)
            return parentScope.getFunctionEntityIncludeConstructor(name);
        else
            return null;
    }

    public boolean inClassScope() {
        if (scopeType == ScopeType.classScope)
            return true;
        else if (scopeType == ScopeType.programScope)
            return false;
        else
            return parentScope.inClassScope();
    }

    public boolean inFunctionScope() {
        if (scopeType == ScopeType.functionScope)
            return true;
        else if (scopeType == ScopeType.programScope)
            return false;
        else
            return parentScope.inFunctionScope();
    }

    public boolean inLoopScope() {
        if (scopeType == ScopeType.loopScope)
            return true;
        else if (scopeType == ScopeType.programScope)
            return false;
        else
            return parentScope.inLoopScope();
    }

    public boolean inMethodScope() {
        return inClassScope() && inFunctionScope();
    }
}
