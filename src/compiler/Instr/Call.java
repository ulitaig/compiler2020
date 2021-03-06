package compiler.Instr;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.IR.IRObject;
import compiler.IR.IRVisitor;
import compiler.Instr.Operand.ConstNull;
import compiler.Instr.Operand.Operand;
import compiler.Instr.Operand.Parameter;
import compiler.Instr.Operand.Register;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Instr.TypeSystem.VoidType;
import compiler.Optim.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Call extends IRInstruction {
    private Function function;
    private ArrayList<Operand> parameters;
    private Register result;

    public Call(BasicBlock basicBlock, Function function, ArrayList<Operand> parameters, Register result) {
        super(basicBlock);
        this.function = function;
        this.parameters = parameters;
        this.result = result;

        if (result != null)
            assert result.getType().equals(function.getFunctionType().getReturnType());
        else
            assert function.getFunctionType().getReturnType().equals(new VoidType());

        assert parameters.size() == function.getParameters().size();
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) instanceof ConstNull) {
                assert function.getParameters().get(i).getType() instanceof PointerType;
                assert function.getFunctionType().getParameterList().get(i) instanceof PointerType;
                assert function.getParameters().get(i).getType()
                        .equals(function.getFunctionType().getParameterList().get(i));
            } else {
                assert parameters.get(i).getType().equals(function.getParameters().get(i).getType());
                assert parameters.get(i).getType().equals(function.getFunctionType().getParameterList().get(i));
            }
        }
    }

    @Override
    public void successfullyAdd() {
        for (Operand parameter : parameters)
            parameter.addUse(this);

        if (result != null)
            result.setDef(this);

        function.addUse(this);
    }

    public Function getFunction() {
        return function;
    }

    public ArrayList<Operand> getParameters() {
        return parameters;
    }

    @Override
    public Register getResult() {
        return result;
    }

    public boolean isVoidCall() {
        return result == null;
    }

    @Override
    public void replaceUse(IRObject oldUse, IRObject newUse) {
        if (function == oldUse) {
            function.removeUse(this);
            function = (Function) newUse;
            function.addUse(this);
        } else {
            for (int i = 0; i < parameters.size(); i++)
                if (parameters.get(i) == oldUse) {
                    parameters.get(i).removeUse(this);
                    parameters.set(i, (Operand) newUse);
                    parameters.get(i).addUse(this);
                }
        }
    }

    @Override
    public void removeFromBlock() {
        for (Operand parameter : parameters)
            parameter.removeUse(this);
        function.removeUse(this);
        super.removeFromBlock();
    }

    @Override
    public void markUseAsLive(Set<IRInstruction> live, Queue<IRInstruction> queue) {
        for (Operand parameter : parameters)
            parameter.markBaseAsLive(live, queue);
    }

    @Override
    public boolean replaceResultWithConstant(SCCP sccp) {
        if (this.isVoidCall())
            return false;
        SCCP.Status status = sccp.getStatus(result);
        if (status.getOperandStatus() == SCCP.Status.OperandStatus.constant) {
            result.replaceUse(status.getOperand());
            this.removeFromBlock();
            return true;
        } else
            return false;
    }

    @Override
    public CommonSubexpressionElimination.Expression convertToExpression() {
        throw new RuntimeException("Convert call instruction to expression.");
    }

    @Override
    public void clonedUseReplace(Map<BasicBlock, BasicBlock> blockMap, Map<Operand, Operand> operandMap) {
        for (int i = 0; i < parameters.size(); i++) {
            Operand operand = parameters.get(i);
            if (operand instanceof Parameter || operand instanceof Register) {
                assert operandMap.containsKey(operand);
                parameters.set(i, operandMap.get(operand));
            }
            parameters.get(i).addUse(this);
        }
        function.addUse(this);
    }

    @Override
    public void addConstraintsForAndersen(Map<Operand, Andersen.Node> nodeMap, Set<Andersen.Node> nodes) {
        if (this.getBasicBlock().getFunction().getIR().getExternalFunctionMap().containsValue(this.function)) {
            if (!isVoidCall() && result.getType() instanceof PointerType) {
                assert nodeMap.containsKey(result);
                Andersen.Node pointer = nodeMap.get(result);
                Andersen.Node pointTo = new Andersen.Node(pointer.getName()
                        + ".returnValue:" + function.getName());
                pointer.getPointsTo().add(pointTo);
                nodes.add(pointTo);
            }
        } else {
            for (int i = 0; i < parameters.size(); i++) {
                Parameter formal = function.getParameters().get(i);
                Operand actual = parameters.get(i);
                if (actual.getType() instanceof PointerType) {
                    assert formal.getType() instanceof PointerType;
                    if (!(actual instanceof ConstNull)) {
                        assert nodeMap.containsKey(actual);
                        assert nodeMap.containsKey(formal);
                        nodeMap.get(actual).getInclusiveEdge().add(nodeMap.get(formal));
                    }
                } else
                    assert !(formal.getType() instanceof PointerType);
            }

            if (!isVoidCall() && result.getType() instanceof PointerType) {
                Operand returnValue = function.getActualReturnValue();
                assert returnValue != null && returnValue.getType() instanceof PointerType;
                if (!(returnValue instanceof ConstNull)) {
                    assert nodeMap.containsKey(result);
                    assert nodeMap.containsKey(returnValue);
                    nodeMap.get(returnValue).getInclusiveEdge().add(nodeMap.get(result));
                }
            }
        }
    }

    @Override
    public boolean updateResultScope(Map<Operand, SideEffectChecker.Scope> scopeMap,
                                     Map<Function, SideEffectChecker.Scope> returnValueScope) {
        if (isVoidCall())
            return false;
        if (SideEffectChecker.getOperandScope(result) == SideEffectChecker.Scope.local) {
            if (scopeMap.get(result) != SideEffectChecker.Scope.local) {
                scopeMap.replace(result, SideEffectChecker.Scope.local);
                return true;
            } else
                return false;
        } else {
            SideEffectChecker.Scope scope = returnValueScope.get(function);
            if (scopeMap.get(result) != scope) {
                scopeMap.replace(result, scope);
                return true;
            } else
                return false;
        }
    }

    /*@Override
    public boolean checkLoopInvariant(LoopAnalysis.LoopNode loop, LICM licm) {
        if (isVoidCall())
            return false;
        if (licm.isLoopInvariant(result, loop))
            return false;
        if (licm.hasSideEffect(function) ||
                function == this.getBasicBlock().getFunction().getIR().getExternalFunctionMap().get("malloc"))
            return false;
        for (Operand parameter : parameters) {
            if (!licm.isLoopInvariant(parameter, loop))
                return false;
        }
        licm.markLoopInvariant(result);
        return true;
    }*/

    @Override
    public boolean canBeHoisted(LoopAnalysis.LoopNode loop) {
        for (Operand parameter : parameters) {
            if (!loop.defOutOfLoop(parameter))
                return false;
        }
        return true;
    }

    @Override
    public boolean combineInst(Queue<IRInstruction> queue, Set<IRInstruction> inQueue) {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        if (result != null) {
            assert !(function.getFunctionType().getReturnType() instanceof VoidType);
            string.append(result.toString()).append(" = ");
        } else
            assert function.getFunctionType().getReturnType() instanceof VoidType;
        string.append("call ");
        string.append(function.getFunctionType().getReturnType().toString()).append(" ");
        string.append("@").append(function.getName()).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            string.append(function.getParameters().get(i).getType()).append(" ").append(parameters.get(i).toString());
            if (i != parameters.size() - 1)
                string.append(", ");
        }
        string.append(")");
        return string.toString();
    }

    @Override
    public Object clone() {
        Call callInst = (Call) super.clone();
        callInst.function = this.function;
        callInst.parameters = new ArrayList<>(this.parameters);
        if (this.result != null) {
            callInst.result = (Register) this.result.clone();
            callInst.result.setDef(callInst);
        } else
            callInst.result = null;

        return callInst;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
