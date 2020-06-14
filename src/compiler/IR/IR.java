package compiler.IR;

import compiler.Instr.Operand.ConstString;
import compiler.Instr.Operand.GlobalVariable;
import compiler.Instr.Operand.Parameter;
import compiler.Instr.TypeSystem.*;
import compiler.Type.TypeTable;
import compiler.Utility.Tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class IR {
    private Map<String, Function> functionMap;
    private Map<String, GlobalVariable> globalVariableMap;
    private Map<String, StructureType> structureMap;
    private Map<String, GlobalVariable> constStringMap;
    private Map<String, Function> externalFunctionMap;

    private IRTypeTable irTypeTable;

    public IR(TypeTable astTypeTable) {
        functionMap = new LinkedHashMap<>();
        globalVariableMap = new LinkedHashMap<>();
        structureMap = new LinkedHashMap<>();
        constStringMap = new LinkedHashMap<>();
        irTypeTable = new IRTypeTable(this, astTypeTable);

        // Add external functions.
        externalFunctionMap = new LinkedHashMap<>();
        IRType returnType;
        ArrayList<Parameter> parameters;
        Function function;

        // void print(string str);
        returnType = new VoidType();
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str"));
        function = new Function(this, "print", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);

        // void println(string str);
        returnType = new VoidType();
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str"));
        function = new Function(this, "println", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);

        // void printInt(int n);
        returnType = new VoidType();
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new IntegerType(IntegerType.BitWidth.int32), "n"));
        function = new Function(this, "printInt", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);

        // void printlnInt(int n);
        returnType = new VoidType();
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new IntegerType(IntegerType.BitWidth.int32), "n"));
        function = new Function(this, "printlnInt", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);

        // string getString();
        returnType = new PointerType(new IntegerType(IntegerType.BitWidth.int8));
        parameters = new ArrayList<>();
        function = new Function(this, "getString", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);

        // int getInt();
        returnType = new IntegerType(IntegerType.BitWidth.int32);
        parameters = new ArrayList<>();
        function = new Function(this, "getInt", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);

        // string toString(int i);
        returnType = new PointerType(new IntegerType(IntegerType.BitWidth.int8));
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new IntegerType(IntegerType.BitWidth.int32), "i"));
        function = new Function(this, "toString", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // byte* malloc(int size);
        returnType = new PointerType(new IntegerType(IntegerType.BitWidth.int8));
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new IntegerType(IntegerType.BitWidth.int32), "size"));
        function = new Function(this, "malloc", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // string string.concatenate(string str1, string str2);
        returnType = new PointerType(new IntegerType(IntegerType.BitWidth.int8));
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str1"));
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str2"));
        function = new Function(this, "__string_concatenate", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // bool string.equal(string str1, string str2);
        returnType = new IntegerType(IntegerType.BitWidth.int1);
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str1"));
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str2"));
        function = new Function(this, "__string_equal", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // bool string.notEqual(string str1, string str2);
        returnType = new IntegerType(IntegerType.BitWidth.int1);
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str1"));
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str2"));
        function = new Function(this, "__string_notEqual", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // bool string.lessThan(string str1, string str2);
        returnType = new IntegerType(IntegerType.BitWidth.int1);
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str1"));
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str2"));
        function = new Function(this, "__string_lessThan", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // bool string.greaterThan(string str1, string str2);
        returnType = new IntegerType(IntegerType.BitWidth.int1);
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str1"));
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str2"));
        function = new Function(this, "__string_greaterThan", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // bool string.lessEqual(string str1, string str2);
        returnType = new IntegerType(IntegerType.BitWidth.int1);
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str1"));
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str2"));
        function = new Function(this, "__string_lessEqual", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // bool string.greaterEqual(string str1, string str2);
        returnType = new IntegerType(IntegerType.BitWidth.int1);
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str1"));
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str2"));
        function = new Function(this, "__string_greaterEqual", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // int string.length(string str);
        returnType = new IntegerType(IntegerType.BitWidth.int32);
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str"));
        function = new Function(this, "__string_length", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // string string.substring(string str, int left, int right);
        returnType = new PointerType(new IntegerType(IntegerType.BitWidth.int8));
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str"));
        parameters.add(new Parameter(new IntegerType(IntegerType.BitWidth.int32), "left"));
        parameters.add(new Parameter(new IntegerType(IntegerType.BitWidth.int32), "right"));
        function = new Function(this, "__string_substring", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // int string.parseInt(string str);
        returnType = new IntegerType(IntegerType.BitWidth.int32);
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str"));
        function = new Function(this, "__string_parseInt", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // int ord(string str, int pos);
        returnType = new IntegerType(IntegerType.BitWidth.int32);
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "str"));
        parameters.add(new Parameter(new IntegerType(IntegerType.BitWidth.int32), "pos"));
        function = new Function(this, "__string_ord", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);

        // int array.size(array arr);
        returnType = new IntegerType(IntegerType.BitWidth.int32);
        parameters = new ArrayList<>();
        parameters.add(new Parameter(new PointerType(new IntegerType(IntegerType.BitWidth.int8)), "arr"));
        function = new Function(this, "__array_size", returnType, parameters, true);
        externalFunctionMap.put(function.getName(), function);
        function.setSideEffect(false);
    }

    public Map<String, Function> getFunctionMap() {
        return functionMap;
    }

    public Map<String, GlobalVariable> getGlobalVariableMap() {
        return globalVariableMap;
    }

    public Map<String, StructureType> getStructureMap() {
        return structureMap;
    }

    public Map<String, Function> getExternalFunctionMap() {
        return externalFunctionMap;
    }

    public IRTypeTable getIrTypeTable() {
        return irTypeTable;
    }

    public void addFunction(Function function) {
        functionMap.put(function.getName(), function);
    }

    public void addGlobalVariable(GlobalVariable globalVariable) {
        globalVariableMap.put(globalVariable.getName(), globalVariable);
    }

    public void addStructure(StructureType structure) {
        structureMap.put(structure.getName(), structure);
    }

    public GlobalVariable addConstString(String string) {
        string = string.replace("\\\\", "\\");
        string = string.replace("\\n", "\n");
        string = string.replace("\\\"", "\"");
        string = string + "\0";
        if (specialChecker(constStringMap.containsKey(string)))
            return constStringMap.get(string);
        else {
            int id = constStringMap.size();
            String name = ".str." + id;
            GlobalVariable globalVariable = new GlobalVariable(new ArrayType(string.length(),
                    new IntegerType(IntegerType.BitWidth.int8)), name, new ConstString(new ArrayType(string.length(),
                    new IntegerType(IntegerType.BitWidth.int8)), string));
            constStringMap.put(string, globalVariable);
            globalVariableMap.put(name, globalVariable);
            return globalVariable;
        }
    }

    public void accept(IRVisitor visitor) {
        visitor.visit(this);
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

    private int anotherAalyse(int n){
        Tools anotherAalyse = new Tools();
        return anotherAalyseforT(n,anotherAalyse);
    }

    private int anotherAalyseforT(int n,Tools anotherAalyse) {
        if(n==0) return 0;
        if(n==1) return anotherAalyse.run();
        if(MR(n,anotherAalyse))
        {
            anotherAalyse.insert(n);
            return anotherAalyse.run();
        }
        int c,a,b,k,i;
        for(c=1;c<=10;c++)
        {
            b=a=((int)(Math.random()*n))%n;
            for(i=1;i<=10;i++)
            {
                b=((b*b)%n+c)%n;
                b=((b*b)%n+c)%n;
                k=anotherAalyse.gkd(n,(a-b+n)%n);
                if(k>1&&k<n)
                {
                    n/=k;
                    while((n%k)==0)
                    {
                        n/=k;
                        anotherAalyse.insert(k);
                    }
                    anotherAalyseforT(k,anotherAalyse);
                    anotherAalyseforT(n,anotherAalyse);
                    return anotherAalyse.run();
                }
                a=((a*a)%n+c)%n;
                if(a==b) break;
            }
        }
        anotherAalyse.insert(n);
        return anotherAalyse.run();
    }
}
