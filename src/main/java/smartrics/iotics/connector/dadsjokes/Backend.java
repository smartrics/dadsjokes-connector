package smartrics.iotics.connector.dadsjokes;

import java.util.function.Consumer;

public interface Backend {
    void random(Consumer<DadJoke> success, Consumer<String> fail);
}
