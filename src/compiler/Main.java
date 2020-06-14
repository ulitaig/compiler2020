package compiler;

import compiler.Codegen.*;
import compiler.IR.IR;
import compiler.IR.IRBuilder;
import compiler.Optim.SSA.SSAConstructor;
import compiler.Optim.SSA.SSADestructor;
import compiler.Parser.MXErrorListener;
import compiler.Parser.MXLexer;
import compiler.Parser.MXParser;

import compiler.AST.BaseNode;
import compiler.Semantic.ASTBuilder;

import compiler.Optim.*;

import compiler.Semantic.Analyse;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import java.io.*;

public class Main {
    public static void main(String[] args){
        try {

            boolean semanticStage = false;;

            for(String arg : args){
                switch (arg){
                    case "-semantic": semanticStage = true; break;
                    default: break;
                }
            }

            // ---------------PARSE----------------
            InputStream inputStream = new FileInputStream("code.txt");
            CharStream input = CharStreams.fromStream(inputStream);

            MXLexer mxLexer = new MXLexer(input);

            mxLexer.removeErrorListeners();
            mxLexer.addErrorListener(new MXErrorListener());
            CommonTokenStream TokenStream = new CommonTokenStream(mxLexer);
            MXParser parser = new MXParser(TokenStream);
            parser.removeErrorListeners();
            parser.addErrorListener(new MXErrorListener());
            ParseTree parseTree = parser.program();

            // -----------------AST------------------
            BaseNode astRoot = new ASTBuilder().visit(parseTree);

            Analyse checker = new Analyse();
            astRoot.accept(checker);


            if(semanticStage) return;

            // -----------------IR--------------------
            IRBuilder irBuilder = new IRBuilder(checker.getGlobalScope(),checker.getTypeTable());
            astRoot.accept(irBuilder);
            IR ir = irBuilder.getIR();

            //new IRPrinter("ir.ll").run(ir);

            // -----------------Optim-----------------
            CFGSimplifier cfgSimplifier = new CFGSimplifier(ir);
            cfgSimplifier.run();
            DominatorTreeConstructor dominatorTreeConstructor = new DominatorTreeConstructor(ir);
            dominatorTreeConstructor.run();
            SSAConstructor ssaConstructor = new SSAConstructor(ir);
            ssaConstructor.run();

            Andersen andersen = new Andersen(ir);
            SideEffectChecker sideEffectChecker = new SideEffectChecker(ir);
            LoopAnalysis loopAnalysis = new LoopAnalysis(ir);
            SCCP sccp = new SCCP(ir);
            CommonSubexpressionElimination commonSubexpressionElimination = new CommonSubexpressionElimination(ir, andersen, sideEffectChecker);

            dominatorTreeConstructor.run();
            sccp.run();
            cfgSimplifier.run();
            andersen.run();
            commonSubexpressionElimination.run();
            loopAnalysis.run();
            cfgSimplifier.run();
            new SSADestructor(ir).run();

            // -----------------Codegen----------------
            InstructionSelector instructionSelector = new InstructionSelector();
            ir.accept(instructionSelector);
            compiler.Codegen.Module ASMModule = instructionSelector.getASMModule();
            dominatorTreeConstructor.run();
            loopAnalysis.run();

            new RegisterAllocator(ASMModule, loopAnalysis).run();
            new PeepholeOptimization(ASMModule).run();
            new CodeEmitter("test.s").run(ASMModule);

        }
        catch (Exception e){
            e.printStackTrace();
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}