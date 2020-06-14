package compiler.Codegen;

import compiler.Codegen.BasicBlock;
import compiler.Codegen.Function;
import compiler.Codegen.Instruction.*;
import compiler.Codegen.Module;
import compiler.Codegen.Operand.Address.Address;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PeepholeOptimization extends ASMPass {
    public PeepholeOptimization(Module module) {
        super(module);
    }

    @Override
    public void run() {
        for (Function function : module.getFunctionMap().values())
            peephole(function);
    }

    private void peephole(Function function) {
        ArrayList<BasicBlock> dfsOrder = function.getDFSOrder();
        Set<BasicBlock> positionFixed = new HashSet<>();

        for (BasicBlock block : dfsOrder) {
            if (positionFixed.contains(block))
                continue;
            BasicBlock curBlock = block;
            BasicBlock lastBlock = null;
            while (true) {
                positionFixed.add(curBlock);
                if (lastBlock != null) {
                    function.splitBlockFromFunction(curBlock);
                    function.addBasicBlockNext(lastBlock, curBlock);
                    JumpInst jump = ((JumpInst) lastBlock.getInstTail());
                    if (jump.getPrevInst() == null){
                        lastBlock.removeTailJumpAll();
                    }
                    else{
                        jump.getPrevInst().setNextInst(null);
                        lastBlock.removeTailJumpHalf(jump);
                    }

                    jump.setDest(null);
                }
                if (!(curBlock.getInstTail() instanceof JumpInst)
                        || positionFixed.contains(((JumpInst) curBlock.getInstTail()).getDest()))
                    break;
                lastBlock = curBlock;
                curBlock = ((JumpInst) curBlock.getInstTail()).getDest();
            }
        }

        ArrayList<BasicBlock> blocks = function.getBlocks();
        for (BasicBlock block : blocks) {
            ASMInstruction ptr = block.getInstHead();
            while (ptr != null) {
                ASMInstruction next = ptr.getNextInst();
                if (ptr.getPrevInst() != null && ptr instanceof LoadInst && ptr.getPrevInst() instanceof StoreInst) {
                    StoreInst prev = ((StoreInst) ptr.getPrevInst());
                    LoadInst cur = ((LoadInst) ptr);
                    Address storeAddr = prev.getAddr();
                    Address loadAddr = cur.getAddr();
                    if (loadAddr.equals(storeAddr)) {
                        block.addInstructionNext(prev, new MoveInst(block, cur.getRd(), prev.getRs()));
                        cur.removeFromBlock();
                    }
                }
                ptr = next;
            }
        }

    }
}
