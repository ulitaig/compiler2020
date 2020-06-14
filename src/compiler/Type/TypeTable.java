package compiler.Type;

import compiler.AST.*;
import compiler.Utility.Position;
import compiler.Utility.SemanticError;
import compiler.Utility.Tools;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

public class TypeTable {
    private Map<TypeNode, Type> typeTable;

    public TypeTable() {
        typeTable = new HashMap<>();

        Position position = new Position(0, 0);
        typeTable.put(new BasicTypeNode(position, "int"), new IntType());
        typeTable.put(new BasicTypeNode(position, "bool"), new BoolType());
        typeTable.put(new BasicTypeNode(position, "string"), new StringType());
        typeTable.put(new BasicTypeNode(position, "void"), new VoidType());
    }

    public Map<TypeNode, Type> getTypeTable() {
        return typeTable;
    }

    public boolean hasType(TypeNode typeNode) {
        return typeTable.containsKey(typeNode);
    }

    public void put(TypeNode typeNode, Type type) throws SemanticError {
        if (hasType(typeNode)) {
            throw new SemanticError(typeNode.getPosition(),"Duplicate definition of type \"" + typeNode.getIdentifier() + "\".");
        } else
            typeTable.put(typeNode, type);
    }

    public Type get(TypeNode typeNode) {
        if (typeNode instanceof ArrayTypeNode) {
            TypeNode baseType = ((ArrayTypeNode) typeNode).getBaseType();
            int dims = ((ArrayTypeNode) typeNode).getDims();
            return new ArrayType(typeTable.get(baseType), dims);
        } else
            return typeTable.get(typeNode);
    }
}
