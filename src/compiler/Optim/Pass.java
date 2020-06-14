package compiler.Optim;

import compiler.IR.IR;

abstract public class Pass {
    protected IR module;
    protected boolean changed;


    public Pass(IR module) {
        this.module = module;
    }

    abstract public void run();
}
