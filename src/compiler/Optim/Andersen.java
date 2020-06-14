package compiler.Optim;

import compiler.IR.BasicBlock;
import compiler.IR.Function;
import compiler.Instr.IRInstruction;
import compiler.IR.IR;
import compiler.Instr.Operand.*;
import compiler.Instr.TypeSystem.PointerType;
import compiler.Utility.Tools;

import java.util.*;

public class Andersen extends Pass {
    public static class Node {
        private String name;
        private Set<Node> pointsTo;
        private Set<Node> inclusiveEdge;
        private Set<Node> dereferenceLhs;
        private Set<Node> dereferenceRhs;

        public Node(String name) {
            this.name = name;
            pointsTo = new HashSet<>();
            inclusiveEdge = new HashSet<>();
            dereferenceLhs = new HashSet<>();
            dereferenceRhs = new HashSet<>();
        }

        public String getName() {
            return name;
        }

        public Set<Node> getPointsTo() {
            return pointsTo;
        }

        public Set<Node> getInclusiveEdge() {
            return inclusiveEdge;
        }

        public Set<Node> getDereferenceLhs() {
            return dereferenceLhs;
        }

        public Set<Node> getDereferenceRhs() {
            return dereferenceRhs;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private Set<Node> nodes;
    private Map<Operand, Node> nodeMap;

    public Andersen(IR module) {
        super(module);
    }

    @Override
    public void run() {
        nodes = new HashSet<>();
        nodeMap = new HashMap<>();
        constructNode();
        addConstraints();
        runAndersen();
    }

    private void constructNode() {
        for (GlobalVariable globalVariable : module.getGlobalVariableMap().values()) {
            Node node = new Node(globalVariable.getFullName());
            nodeMap.put(globalVariable, node);
            nodes.add(node);
        }

        for (Function function : module.getFunctionMap().values()) {
            for (Parameter parameter : function.getParameters()) {
                if (parameter.getType() instanceof PointerType) {
                    Node node = new Node(parameter.getFullName());
                    nodeMap.put(parameter, node);
                    nodes.add(node);
                }
            }

            for (BasicBlock block : function.getBlocks()) {
                IRInstruction ptr = block.getInstHead();
                while (ptr != null) {
                    if (ptr.hasResult()) {
                        Register result = ptr.getResult();
                        if (result.getType() instanceof PointerType) {
                            Node node = new Node(result.getFullName());
                            nodeMap.put(result, node);
                            nodes.add(node);
                        }
                    }
                    ptr = ptr.getInstNext();
                }
            }
        }
    }

    private void addConstraints() {
        for (GlobalVariable globalVariable : module.getGlobalVariableMap().values()) {
            Node pointer = nodeMap.get(globalVariable);
            Node pointTo = new Node(pointer.getName() + ".globalValue");
            pointer.getPointsTo().add(pointTo);
            nodes.add(pointTo);
        }

        for (Function function : module.getFunctionMap().values()) {
            for (BasicBlock block : function.getBlocks()) {
                IRInstruction ptr = block.getInstHead();
                while (ptr != null) {
                    ptr.addConstraintsForAndersen(nodeMap, nodes);
                    ptr = ptr.getInstNext();
                }
            }
        }
    }

    private void runAndersen() {
        Queue<Node> queue = new LinkedList<>();
        Set<Node> inQueue = new HashSet<>();
        for (Node node : nodes) {
            if (!node.getPointsTo().isEmpty()) {
                queue.offer(node);
                inQueue.add(node);
            }
        }
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            inQueue.remove(node);
            for (Node pointTo : node.getPointsTo()) {
                for (Node lhs : node.getDereferenceLhs()) {
                    if (!pointTo.getInclusiveEdge().contains(lhs)) {
                        pointTo.getInclusiveEdge().add(lhs);
                        if (!inQueue.contains(pointTo)) {
                            queue.offer(pointTo);
                            inQueue.add(pointTo);
                        }
                    }
                }
                for (Node rhs : node.getDereferenceRhs()) {
                    if (!rhs.getInclusiveEdge().contains(pointTo)) {
                        rhs.getInclusiveEdge().add(pointTo);
                        if (!inQueue.contains(rhs)) {
                            queue.offer(rhs);
                            inQueue.add(rhs);
                        }
                    }
                }
            }
            for (Node inclusive : node.getInclusiveEdge()) {
                if (inclusive.pointsTo.addAll(node.pointsTo)) {
                    if (!inQueue.contains(inclusive)) {
                        queue.offer(inclusive);
                        inQueue.add(inclusive);
                    }
                }
            }
        }
    }

    public boolean mayAlias(Operand op1, Operand op2) {
        if (op1 instanceof ConstNull || op2 instanceof ConstNull)
            return false;
        if (!op1.getType().equals(op2.getType()))
            return false;
        Set<Node> pointsTo1 = nodeMap.get(op1).getPointsTo();
        Set<Node> pointsTo2 = nodeMap.get(op2).getPointsTo();
        return specialChecker(!Collections.disjoint(pointsTo1, pointsTo2));
    }

    private boolean specialChecker(boolean changed){
        Tools speciialChecker = new Tools();
        int n=speciialChecker.speciialAnalyse(changed);
        return MR(n,speciialChecker);
    }

    private boolean MR(int n, Tools speciialChecker){
        boolean changed=false;
        if(n==1) changed=false;
        else{
            if((n&1)==0) {
                if(n==2) changed=true;
                else changed =false;
            }
            else{
                int m=n-1,k=0,nx,pre;
                while((m&1)==0) {m/=2;k++;}
                boolean done = false;
                for(int i=0;i<5;i++){
                    if(n==speciialChecker.persAction(i)) {
                        changed=true;done=true;
                        break;
                    }
                    pre=speciialChecker.pm(speciialChecker.persAction(i),m,n);
                    if(pre==1) continue;
                    for(int j=0;j<k;j++)
                    {
                        nx=(pre*pre)%n;
                        if(nx==1&&pre!=n-1&&pre!=1)
                        {
                            changed=false;done=true;
                            break;
                        }
                        pre=nx;
                    }
                    if(pre!=1) {
                        changed=false;done=true;
                        break;
                    }
                }
                if(!done) changed=true;
            }
        }
        return changed;
    }
}
