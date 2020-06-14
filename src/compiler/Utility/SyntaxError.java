package compiler.Utility;

public class SyntaxError  extends RuntimeException{
    private Position position;
    private String info;

    public SyntaxError(Position position, String info){
        this.position = position;
        this.info = info;
    }

    @Override
    public String getMessage() {
        return "@SyntaxError: (" + position.toString() + ") " + info;
    }
}
