package smartrics.iotics.connector.dadsjokes;

import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class Icanhazdadjoke implements Backend {
    private static final String BASE = "https://icanhazdadjoke.com/";

    private static final String URL_RANDOM = BASE + "/";

    private static final Logger LOGGER = LoggerFactory.getLogger(Icanhazdadjoke.class);

    private final OkHttpClient cli;

    public Icanhazdadjoke() {
        cli = new OkHttpClient();
    }

    public void random(Consumer<DadJoke> success, Consumer<String> fail) {
        request(URL_RANDOM, success, fail);
    }

    private void request(String url, Consumer<DadJoke> success, Consumer<String> fail) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", "IOTICS Dad Joke connector (https://github.com/smartrics/dadsjokes-connector)")
                .addHeader("Accept", "application/json")
                .build();

        Call call = cli.newCall(request);
        call.enqueue(new DadJokeCallback(success, fail));
    }

    public record DadJokeCallback(Consumer<DadJoke> success, Consumer<String> fail) implements Callback {

        public static DadJoke parse(String body) {
            Gson gson = new Gson();
            return gson.fromJson(body, DadJoke.class);
        }

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            try {
                fail.accept(e.getMessage());
            } catch (Exception exception) {
                LOGGER.debug("Unable to invoke callback onFailure", e);
            }
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) {
                Optional.ofNullable(response.body()).ifPresent(responseBody -> {
                    try {
                        DadJoke resp = parse(responseBody.string());
                        if (resp.status() == 200) {
                            success.accept(resp);
                        } else {
                            fail.accept("Failure when getting dad joke from API. Status: " + resp.status());
                        }
                    } catch (Exception exception) {
                        LOGGER.debug("Unable to invoke callback onResponse", exception);
                    }
                });
        }
    }
}
