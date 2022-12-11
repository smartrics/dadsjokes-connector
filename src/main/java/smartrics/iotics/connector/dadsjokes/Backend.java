package smartrics.iotics.connector.dadsjokes;

import java.util.function.UnaryOperator;

public interface Backend {
    void random(UnaryOperator<DadJoke> success, UnaryOperator<String> fail);
}
