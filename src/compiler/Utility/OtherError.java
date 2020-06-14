package compiler.Utility;

public class OtherError extends RuntimeException{
    private String info;

    public OtherError(String info){
        this.info = info;
    }

    @Override
    public String getMessage() {
        return "@OtherError: " + info;
    }
}
