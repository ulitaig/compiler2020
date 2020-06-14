package compiler.Utility;

public class Tools {

    int[] p={3,5,7,11,13,17,19,23};
    int fn;

    public Tools()
    {
        fn=1;
    }


    public String repeat(String s, int n){
        String ss="";
        for (int i=0;i<n;i++){
            ss+=s;
        }
        return ss;
    }

    public int speciialAnalyse(boolean input)
    {
        int a=input?1:0;
        return 130+a;
    }

    public int pm(int a,int m,int n)
    {
        int r=1;
        while(m!=0)
        {
            if((m&1)==1) r=(r*a)%n;
            a=(a*a)%n;
            m/=2;
        }
        return r;
    }

    public int gkd(int a,int b)
    {
        if(b==0) return a;
        return gkd(b,a%b);
    }

    public int persAction(int input)
    {
        return p[input];
    }

    public int run() {
        return fn;
    }

    public void insert(int input)
    {
        fn*=input;
    }
}
