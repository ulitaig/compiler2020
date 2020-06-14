package compiler.IR;

import compiler.Instr.*;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.*;
import compiler.Utility.Tools;

import java.io.*;

public class IRPrinter implements IRVisitor {
    private OutputStream os;
    private PrintWriter writer;
    private String indent;

    public IRPrinter(String filename) {
        try {
            os = new FileOutputStream(filename);
            writer = new PrintWriter(os);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

        indent = "    ";
    }

    public void run(IR module) {
        module.accept(this);

        try {
            writer.close();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private void print(String string) {
        writer.print(string);
    }

    private void println(String string) {
        writer.println(string);
    }

    @Override
    public void visit(IR module) {
        println("source_filename = \"code.txt\"");
        println("");

        // ------ STRUCTURE ------
        if (module.getStructureMap().size() > 0) {
            for (String name : module.getStructureMap().keySet())
                println(module.getStructureMap().get(name).structureToString());
            println("");
        }

        // ------ GLOBAL VARIABLE ------
        if (module.getGlobalVariableMap().size() > 0) {
            for (String name : module.getGlobalVariableMap().keySet())
                println(module.getGlobalVariableMap().get(name).definitionToString());
            println("");
        }

        // ------ EXTERNAL FUNCTION ------
        for (String name : module.getExternalFunctionMap().keySet())
            println(module.getExternalFunctionMap().get(name).declareToString());
        println("");

        for (String name : module.getFunctionMap().keySet()) {
            module.getFunctionMap().get(name).accept(this); // visit Function
            println("");
        }
    }

    @Override
    public void visit(Function function) {
        println(function.declareToString().replace("declare", "define") + " {");

        BasicBlock ptr = function.getEntranceBlock();
        while (ptr != null) {
            ptr.accept(this); // visit BasicBlock
            if (ptr.hasNext())
                println("");
            ptr = ptr.getNext();
        }

        println("}");
    }

    @Override
    public void visit(BasicBlock block) {
        print(block.getName() + ":");
        if (block.hasPredecessor()) {
            print(new Tools().repeat(" ",(50 - (block.getName().length() + 1))));
            print("; preds = ");
            int size = block.getPredecessors().size();
            int cnt = 0;
            for (BasicBlock predecessor : block.getPredecessors()) {
                print(predecessor.toString());
                if (++cnt != size)
                    print(", ");
            }
        }
        println("");

        IRInstruction ptr = block.getInstHead();
        while (ptr != null) {
            ptr.accept(this); // visit IRInstruction
            ptr = ptr.getInstNext();
        }
    }

    @Override
    public void visit(Return inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(Branch inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(BinaryOp inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(Allocate inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(Load inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(Store inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(GetElemPtr inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(BitCastTo inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(Icmp inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(Phi inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(Call inst) {
        println(indent + inst.toString());
    }

    @Override
    public void visit(Move inst) {
        println(indent + inst.toString());
    }
}
