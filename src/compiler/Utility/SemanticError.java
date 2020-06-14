package compiler.Utility;

public class SemanticError extends RuntimeException{
    private Position position;
    private String info;

    public SemanticError(Position position, String info){
        this.position = position;
        this.info = info;
    }

    public SemanticError(String info){
        this.position = new Position(0,0);
        this.info = info;
    }

    public void error(Position position, String info){
        this.position = position;
        this.info = info;
    }

    public void error(String info){
        this.info = info;
    }

    public void error(Position position){
        this.position = position;
    }

    @Override
    public String getMessage() {
        return "@SemanticError: (" + position.toString() + ") " + info;
    }
}
