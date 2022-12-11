package smartrics.iotics.connector.dadsjokes;

import com.google.gson.Gson;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.function.UnaryOperator;

public class Icanhazdadjoke implements Backend {
    private static String BASE = "https://icanhazdadjoke.com/";

    private static String URL_RANDOM = BASE + "/";

    private static Logger LOGGER = LoggerFactory.getLogger(Icanhazdadjoke.class);

    private final OkHttpClient cli;

    public Icanhazdadjoke() {
        cli = new OkHttpClient();
    }

    public void random(UnaryOperator<DadJoke> success, UnaryOperator<String> fail) {
        request(URL_RANDOM, success, fail);
    }

    private void request(String url, UnaryOperator<DadJoke> success, UnaryOperator<String> fail)  {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Accept", "application/json")
                .build();

        Call call = cli.newCall(request);
        call.enqueue(new DadJokeCallback(success, fail));
    }

    public record DadJokeCallback(UnaryOperator<DadJoke> success, UnaryOperator<String> fail) implements Callback {

        public static DadJoke parse(String body) {
            Gson gson = new Gson();
            return gson.fromJson(body, DadJoke.class);
        }

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            try {
                fail.apply(e.getMessage());
            } catch (Exception exception) {
                LOGGER.debug("Unable to invoke callback onFailure", e);
            }
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) {
            try {
                DadJoke resp = parse(response.body().string());
                if (resp.status() == 200) {
                    success.apply(resp);
                } else {
                    fail.apply("Failure when getting dad joke from API. Status: " + resp.status());
                }
            } catch (Exception exception) {
                LOGGER.debug("Unable to invoke callback onResponse", exception);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://dad-jokes.p.rapidapi.com/random/joke")
                .get()
                .addHeader("X-RapidAPI-Key", "d081563120mshbdf35e7688d670ap1f30e5jsn2894b3935f27")
                .addHeader("X-RapidAPI-Host", "dad-jokes.p.rapidapi.com")
                .build();

        Response response = client.newCall(request).execute();

        CountDownLatch l = new CountDownLatch(1);
        Icanhazdadjoke b = new Icanhazdadjoke();
        b.random(dadJoke -> {
            System.out.println(dadJoke);
            l.countDown();
            return dadJoke;
        }, s -> {
            System.out.println(s);
            l.countDown();
            return s;
        });
        l.await();
    }

}
