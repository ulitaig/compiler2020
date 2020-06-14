package compiler.Type;

import compiler.AST.*;
import compiler.Entity.*;
import compiler.Utility.Position;
import compiler.Utility.Tools;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.IRType;
import compiler.Instr.TypeSystem.IRTypeTable;
import compiler.Instr.TypeSystem.PointerType;

import java.util.ArrayList;

public class ArrayType extends Type {
    private Type baseType;
    private int dims;

    private ArrayList<FunctionEntity> methods;

    public ArrayType(Type baseType, int dims) {
        super(baseType.getName(), 0);
        this.baseType = baseType;
        this.dims = dims;

        methods = new ArrayList<>();
        Position location = new Position(0, 0);
        ArrayList<VariableEntity> parameters;
        FunctionEntity method;

        parameters = new ArrayList<>();
        method = new FunctionEntity("size", location,
                new BasicTypeNode(location, "int"), parameters, null,
                FunctionEntity.EntityType.method);
        methods.add(method);
    }

    public Type getBaseType() {
        return baseType;
    }

    public int getDims() {
        return dims;
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
    public boolean equals(Object obj) {
        if (obj instanceof ArrayType)
            return baseType.equals(((ArrayType) obj).baseType) && dims == ((ArrayType) obj).dims;
        else
            return false;
    }

    @Override
    public String toString() {
        return getName() + new Tools().repeat("[]",dims);
    }

    @Override
    public IRType getIRType(IRTypeTable irTypeTable) {
        IRType res = baseType.getIRType(irTypeTable);
        for (int i = 0; i < dims; i++)
            res = new PointerType(res);
        return res;
    }

    @Override
    public Operand getDefaultValue() {
        return new ConstNull();
    }
}
