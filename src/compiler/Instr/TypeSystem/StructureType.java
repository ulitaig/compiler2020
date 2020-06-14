package compiler.Instr.TypeSystem;

import compiler.Instr.Operand.Operand;

import java.util.ArrayList;

public class StructureType extends IRType {
    private String name;
    private ArrayList<IRType> memberList;

    public StructureType(String name, ArrayList<IRType> memberList) {
        this.name = name;
        this.memberList = memberList;
    }

    static private int align(int size, int base) {
        if (base == 0)
            return 0;
        if (size % base == 0)
            return size;
        else
            return size + (base - (size % base));
    }

    public String getName() {
        return name;
    }

    public ArrayList<IRType> getMemberList() {
        return memberList;
    }

    public int calcOffset(int index) {
        assert index >= 0 && index < memberList.size();
        int offset = 0;
        for (int i = 0; i <= index; i++) {
            int typeSize = memberList.get(i).getBytes();
            offset = align(offset, typeSize) + (i == index ? 0 : typeSize);
        }
        return offset;
    }

    @Override
    public Operand getDefaultValue() {
        // This method will never be called.
        throw new RuntimeException();
    }

    @Override
    public int getBytes() {
        int size = 0;
        int max = 0;
        for (IRType irType : memberList) {
            int typeSize = irType.getBytes();
            size = align(size, typeSize) + typeSize;
            max = Math.max(max, typeSize);
        }
        size = align(size, max);
        return size;
    }

    public String structureToString() {
        StringBuilder string = new StringBuilder(this.toString());
        string.append(" = type { ");
        for (int i = 0; i < memberList.size(); i++) {
            string.append(memberList.get(i).toString());
            if (i != memberList.size() - 1)
                string.append(", ");
        }
        string.append(" }");
        return string.toString();
    }

    @Override
    public String toString() {
        return "%" + name;
    }
}
