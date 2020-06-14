package compiler.Instr.TypeSystem;

import compiler.Entity.VariableEntity;
import compiler.IR.IR;
import compiler.Type.*;
import compiler.Type.VoidType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IRTypeTable {
    private IR module;
    private Map<Type, IRType> typeTable;

    public IRTypeTable(IR module, TypeTable astTypeTable) {
        this.module = module;
        typeTable = new HashMap<>();

        for (Type astType : astTypeTable.getTypeTable().values()) {
            if (astType instanceof IntType)
                typeTable.put(astType, new IntegerType(IntegerType.BitWidth.int32));
            else if (astType instanceof BoolType)
                typeTable.put(astType, new IntegerType(IntegerType.BitWidth.int1));
            else if (astType instanceof StringType)
                typeTable.put(astType, new PointerType(new IntegerType(IntegerType.BitWidth.int8)));
            else if (astType instanceof VoidType)
                typeTable.put(astType, new compiler.Instr.TypeSystem.VoidType());
            else {
                assert astType instanceof ClassType;
                ArrayList<IRType> memberList = new ArrayList<>(); // To be modified later;
                typeTable.put(astType, new StructureType("class." + astType.getName(), memberList));
            }
        }

        for (Type astType : typeTable.keySet())
            if (astType instanceof ClassType) {
                ArrayList<IRType> memberList = ((StructureType) typeTable.get(astType)).getMemberList();
                for (VariableEntity member : ((ClassType) astType).getMembers()) {
                    Type memberType = astTypeTable.get(member.getType());
                    IRType irType = memberType.getIRType(this);
                    memberList.add(irType);
                }
                String name = "class." + astType.getName();
                StructureType structureType = new StructureType(name, memberList);
                module.addStructure(structureType);
            }
    }

    public IRType get(Type type) {
        return typeTable.get(type);
    }
}
