package compiler.Parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import compiler.Utility.Position;
import compiler.Utility.SyntaxError;

public class MXErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object symbol, int line, int column, String info, RecognitionException e) {
        throw new SyntaxError(new Position(line, column), info);
    }

}
