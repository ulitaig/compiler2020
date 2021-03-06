package compiler.Instr.Operand;

import compiler.Instr.IRInstruction;
import compiler.Instr.TypeSystem.IRType;

import java.util.Queue;
import java.util.Set;

public class Register extends Operand implements Cloneable {
    private String name;
    private IRInstruction def;

    public Register(IRType type, String name) {
        super(type);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return def.getBasicBlock().getFunction().getName() + toString();
    }

    public String getNameWithoutDot() {
        return getString(name);
    }

    public static String getString(String name) {
        if (name.contains(".")) {
            String[] strings = name.split("\\.");
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < strings.length - 2; i++)
                res.append(strings[i]).append('.');
            res.append(strings[strings.length - 2]);
            return res.toString();
        } else
            throw new RuntimeException();
    }

    public void setName(String name) {
        this.name = name;
    }

    public IRInstruction getDef() {
        return def;
    }

    public void setDef(IRInstruction def) {
        this.def = def;
    }

    @Override
    public boolean isConstValue() {
        return false;
    }

    @Override
    public void markBaseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue) {
        assert def != null;
        if (!live.contains(def)) {
            live.add(def);
            queue.offer(def);
        }
        if (def.getBasicBlock().isNotExitBlock() && !live.contains(def.getBasicBlock().getInstTail())) {
            live.add(def.getBasicBlock().getInstTail());
            queue.offer(def.getBasicBlock().getInstTail());
        }
    }

    @Override
    public String toString() {
        return "%" + name;
    }

    @Override
    public Object clone() {
        Register register;
        try {
            register = (Register) super.clone();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        register.name = this.name;
        register.def = this.def;
        return register;
    }
}
