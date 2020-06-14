package compiler.Instr.Operand;

import compiler.Instr.TypeSystem.IRType;

public class ConstString extends Constant {
    private String value;

    public ConstString(IRType type, String value) {
        super(type);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Constant castToType(IRType objectType) {
        // This method will never be called.
        throw new RuntimeException("ConstString cast to " + objectType.toString());
    }

    @Override
    public String toString() {
        String text = value;
        text = text.replace("\\", "\\5C");
        text = text.replace("\n", "\\0A");
        text = text.replace("\"", "\\22");
        text = text.replace("\0", "\\00");

        return "c\"" + text + "\"";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConstString && value.equals(((ConstString) obj).value);
    }
}
