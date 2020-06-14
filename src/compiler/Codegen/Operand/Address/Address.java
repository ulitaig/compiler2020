package compiler.Codegen.Operand.Address;

import compiler.Codegen.Instruction.ASMInstruction;
import compiler.Codegen.Operand.Register.VirtualRegister;

import java.util.Set;

abstract public class Address {
    public void addToUEVarAndVarKill(Set<VirtualRegister> UEVar, Set<VirtualRegister> varKill) {

    }

    public void addBaseUse(ASMInstruction use) {

    }

    public void removeBaseUse(ASMInstruction use) {

    }

    public void replaceUse(VirtualRegister oldVR, VirtualRegister newVR) {

    }

    abstract public String emitCode();

    @Override
    abstract public String toString();

    @Override
    abstract public boolean equals(Object obj);
}
