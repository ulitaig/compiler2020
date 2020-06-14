package compiler.Type;

import compiler.AST.*;
import compiler.Entity.*;
import compiler.Utility.Position;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.IRTypeTable;


import java.util.ArrayList;

public class StringType extends Type {
    private ArrayList<FunctionEntity> methods;

    public StringType() {
        super("string", 0);

        methods = new ArrayList<>();

        Position position = new Position(0, 0);
        ArrayList<VariableEntity> parameters;
        FunctionEntity method;

        parameters = new ArrayList<>();
        method = new FunctionEntity("length", position,
                new BasicTypeNode(position, "int"), parameters, null,
                FunctionEntity.EntityType.method);
        methods.add(method);

        parameters = new ArrayList<>();
        parameters.add(VariableEntity.newEntity("left", "int"));
        parameters.add(VariableEntity.newEntity("right", "int"));
        method = new FunctionEntity("substring", position,
                new BasicTypeNode(position, "string"), parameters, null,
                FunctionEntity.EntityType.method);
        methods.add(method);

        parameters = new ArrayList<>();
        method = new FunctionEntity("parseInt", position,
                new BasicTypeNode(position, "int"), parameters, null,
                FunctionEntity.EntityType.method);
        methods.add(method);

        parameters = new ArrayList<>();
        parameters.add(VariableEntity.newEntity("pos", "int"));
        method = new FunctionEntity("ord", position,
                new BasicTypeNode(position, "int"), parameters, null,
                FunctionEntity.EntityType.method);
        methods.add(method);
    }

    public ArrayList<FunctionEntity> getMethods() {
        return methods;
    }

    public boolean hasMethod(String name) {
        for (FunctionEntity method : methods)
            if (method.getName().equals(name))
                return true;
        return false;
    }

    public FunctionEntity getMethod(String name) {
        for (FunctionEntity method : methods)
            if (method.getName().equals(name))
                return method;
        return null;
    }

    @Override
    public IRType getIRType(IRTypeTable irTypeTable) {
        return irTypeTable.get(this);
    }

    @Override
    public Operand getDefaultValue() {
        return new ConstNull();
    }
}
