package compiler.Codegen;

abstract public class ASMPass {
    protected Module module;

    public ASMPass(Module module) {
        this.module = module;
    }

    abstract public void run();
}
