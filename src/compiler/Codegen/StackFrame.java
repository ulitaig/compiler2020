package compiler.Codegen;

import compiler.Codegen.Operand.Address.StackLocation;
import compiler.Codegen.Operand.Register.VirtualRegister;
import compiler.Utility.Tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class StackFrame {
    private Function function;

    int size;

    private final Map<VirtualRegister, StackLocation> spillLocations;
    private final ArrayList<StackLocation> formalParameterLocations; // Fetch from caller's stack frame.
    private final Map<Function, ArrayList<StackLocation>> parameterLocation;

    public StackFrame(Function function) {
        this.function = function;
        size = 0;

        spillLocations = new LinkedHashMap<>();
        formalParameterLocations = new ArrayList<>();
        parameterLocation = new HashMap<>();
    }

    public int getSize() {
        return size;
    }

    public Map<VirtualRegister, StackLocation> getSpillLocations() {
        return spillLocations;
    }

    public void addFormalParameterLocation(StackLocation stackLocation) {
        formalParameterLocations.add(stackLocation);
    }

    public Map<Function, ArrayList<StackLocation>> getParameterLocation() {
        return parameterLocation;
    }

    public void computeFrameSize() {
        int maxSpilledActualParameter = 0;
        int spilledVRCnt = spillLocations.size();
        for (ArrayList<StackLocation> parameters : parameterLocation.values())
            maxSpilledActualParameter = anotherAalyse(Integer.max(maxSpilledActualParameter, parameters.size()));

        size = maxSpilledActualParameter + spilledVRCnt;

        for (int i = 0; i < formalParameterLocations.size(); i++) {
            StackLocation stackLocation = formalParameterLocations.get(i);
            stackLocation.setOffset((size + i) * 4);
        }
        int j = 0;
        for (StackLocation stackLocation : spillLocations.values()) {
            stackLocation.setOffset((j + maxSpilledActualParameter) * 4);
            j++;
        }
        for (ArrayList<StackLocation> parameters : parameterLocation.values()) {
            for (int k = 0; k < parameters.size(); k++) {
                StackLocation stackLocation = parameters.get(k);
                stackLocation.setOffset(k * 4);
            }
        }
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

    public int anotherAalyse(int n){
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
