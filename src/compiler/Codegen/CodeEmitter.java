package compiler.Codegen;

import compiler.Codegen.Instruction.*;
import compiler.Codegen.Operand.GlobalVariable;
import compiler.Utility.Tools;

import java.io.*;
import java.util.ArrayList;

public class CodeEmitter extends ASMHighVisitor {
    private File outputFile;
    private OutputStream os;
    private PrintWriter writer;
    private String indent;

    public CodeEmitter(String filename) throws FileNotFoundException {
        outputFile = new File(filename);
        os = new FileOutputStream(filename);
        writer = new PrintWriter(os);
        indent = "\t";
    }

    public void run(Module module) throws IOException {
        module.accept(this);
        writer.close();
        os.close();
    }

    private void print(String string) {
        System.out.print(string);
        writer.print(string);
    }

    private void println(String string) {
        System.out.println(string);
        writer.println(string);
    }

    @Override
    public void visit(Module module) {
        println(indent + ".text");
        println("");

        for (Function function : module.getFunctionMap().values())
            this.visit(function);
            //function.accept(this);

        println("");

        println(indent + ".section\t.sdata,\"aw\",@progbits");
        for (GlobalVariable gv : module.getGlobalVariableMap().values())
            gv.accept(this);
    }

    @Override
    public void visit(Function function) {
        print(indent + ".globl" + indent + function.getName());
        print(new Tools().repeat(" ",Integer.max(1, 24 - function.getName().length())));
        println("# -- Begin function " + function.getName());
        println(indent + ".p2align" + indent + "2");

        print(function.getName() + ":" + new Tools().repeat(" ",Integer.max(1, 31 - function.getName().length())));
        println("# @" + function.getName());

        ArrayList<BasicBlock> blocks = function.getBlocks();
        for (BasicBlock block : blocks)
            block.accept(this);

        println(new Tools().repeat(" ",40) + "# -- End function");
        println("");
    }

    @Override
    public void visit(BasicBlock block) {
        String name = block.getAsmName();
        println(name + ":" + new Tools().repeat(" ",40 - 1 - name.length()) + "# " + block.getName());

        ASMInstruction ptr = block.getInstHead();
        while (ptr != null) {
            println(ptr.emitCode());
            ptr = ptr.getNextInst();
        }
    }

    @Override
    public void visit(GlobalVariable gv) {
        if (!gv.isString()) {
            println(indent + ".globl" + indent + gv.getName());
            println(indent + ".p2align" + indent + "2");
        }
        println(gv.getName() + ":");
        println(gv.emitCode());
        println("");
    }
}
