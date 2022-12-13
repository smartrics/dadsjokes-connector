package smartrics.iotics.connector.dadsjokes;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.iotics.api.*;
import com.iotics.sdk.identity.SimpleIdentityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartrics.iotics.connector.twins.AbstractTwinWithModel;
import smartrics.iotics.space.Builders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class TwinOfIcanhazdadjoke extends AbstractTwinWithModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwinOfIcanhazdadjoke.class);
    private final Backend backend;

    public TwinOfIcanhazdadjoke(Backend backend, SimpleIdentityManager sim,
                                TwinAPIGrpc.TwinAPIFutureStub twinStub,
                                FeedAPIGrpc.FeedAPIFutureStub feedStub,
                                Executor executor, TwinID modelDid) {
        super(sim, "icanhazdadjoke_keyname", twinStub, feedStub, executor, modelDid);
        this.backend = backend;
    }

    public ListenableFuture<DadJoke> shareRandomJoke() {
        SettableFuture<DadJoke> ret = SettableFuture.create();
        FeedID jokeFeedID = FeedID.newBuilder()
                .setTwinId(TwinOfIcanhazdadjoke.this.identity.did())
                .setId("random_joke")
                .build();
        FeedID statusFeedID = FeedID.newBuilder()
                .setTwinId(TwinOfIcanhazdadjoke.this.identity.did())
                .setId("status")
                .build();
        backend.random(dadJoke -> {
            String statusPayload;
            if (dadJoke.status() == 200) {
                String joke = makeJokePayload(dadJoke);
                ListenableFuture<ShareFeedDataResponse> f = TwinOfIcanhazdadjoke.super.share(jokeFeedID, joke);
                LOGGER.info("published {}", joke);
                statusPayload = makeStatusPayload(true, "OK");
                TwinOfIcanhazdadjoke.super.share(statusFeedID, statusPayload);
                LOGGER.info("published {}", statusPayload);
                ret.set(dadJoke);
            } else {
                String s = "Backend status: " + dadJoke.status();
                statusPayload = makeStatusPayload(false, s);
                TwinOfIcanhazdadjoke.super.share(statusFeedID, statusPayload);
                LOGGER.info("published {}", statusPayload);
                ret.setException(new IllegalStateException(s));
            }
        }, s -> {
            ListenableFuture<ShareFeedDataResponse> f1 = TwinOfIcanhazdadjoke.super.share(statusFeedID, makeStatusPayload(false, s));
            ret.setException(new IllegalStateException(s));
        });
        return ret;
    }

    private String makeStatusPayload(boolean status, String message) {
        Map<String, String> values = new HashMap<>();
        values.put("status", Boolean.toString(status));
        values.put("message", message);
        values.put("timestamp", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()));
        return new Gson().toJson(values);
    }

    private String makeJokePayload(DadJoke dadJoke) {
        Map<String, String> values = new HashMap<>();
        values.put("id", dadJoke.id());
        values.put("joke", dadJoke.joke());
        values.put("timestamp", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()));
        return new Gson().toJson(values);
    }

    public ListenableFuture<UpsertTwinResponse> make() {
        return this.twinStub.upsertTwin(UpsertTwinRequest.newBuilder()
                .setHeaders(Builders.newHeadersBuilder(sim.agentIdentity().did())
                        .build())
                .setPayload(UpsertTwinRequest.Payload.newBuilder()
                        .setTwinId(TwinID.newBuilder().setId(identity.did()).build())
                        .addProperties(Property.newBuilder()
                                .setKey(ON_RDFS + "#comment")
                                .setLiteralValue(Literal.newBuilder().setValue("Dad Joke generator").build())
                                .build())
                        .addProperties(Property.newBuilder()
                                .setKey(ON_RDFS + "#label")
                                .setLiteralValue (Literal.newBuilder().setValue("ICanHazDadJoke").build())
                                .build())
                        .addProperties(Property.newBuilder()
                                .setKey("https://data.iotics.com/app#model")
                                .setUriValue(Uri.newBuilder().setValue(modelDid.getId()).build())
                                .build())
                        .addProperties(Property.newBuilder()
                                .setKey(ON_RDF + "#type")
                                .setUriValue(Uri.newBuilder().setValue("https://icanhazdadjoke.com/ont/joke").build())
                                .build())
                        .addProperties(Property.newBuilder()
                                .setKey("http://www.w3.org/ns/dcat#endpointURL")
                                .setUriValue(Uri.newBuilder().setValue("https://icanhazdadjoke.com").build())
                                .build())
                        .addProperties(Property.newBuilder()
                                .setKey("http://data.iotics.com/public#hostAllowList")
                                .setUriValue(Uri.newBuilder().setValue("http://data.iotics.com/public#allHosts").build())
                                .build())
                        .addFeeds(UpsertFeedWithMeta.newBuilder()
                                .setId("random_joke")
                                .setStoreLast(true)
                                .addValues(Value.newBuilder()
                                        .setLabel("id").setComment("the ID of the joke")
                                        .setDataType("string")
                                        .build())
                                .addValues(Value.newBuilder()
                                        .setLabel("joke").setComment("the joke")
                                        .setDataType("string")
                                        .build())
                                .addValues(Value.newBuilder()
                                        .setLabel("timestamp").setComment("joke date")
                                        .setDataType("dateTime")
                                        .build())
                                .build())
                        .addFeeds(UpsertFeedWithMeta.newBuilder()
                                .setId("status")
                                .setStoreLast(true)
                                .addValues(Value.newBuilder()
                                        .setLabel("status").setComment("twin status")
                                        .setDataType("boolean")
                                        .build())
                                .addValues(Value.newBuilder()
                                        .setLabel("message").setComment("twin status message")
                                        .setDataType("string")
                                        .build())
                                .addValues(Value.newBuilder()
                                        .setLabel("timestamp").setComment("joke date")
                                        .setDataType("dateTime")
                                        .build())
                                .build())
                        .build())
                .build());
    }

    private static abstract class LoggingFutureCallback<T> implements FutureCallback<T>, Consumer<T> {

        private final String message;

        public LoggingFutureCallback(String m) {
            this.message = m;
        }

        @Override
        public void onSuccess(T t) {
            LOGGER.error("{} - {}", message, t);
            accept(t);
        }

        @Override
        public void onFailure(Throwable throwable) {
            LOGGER.error("{} - error in operation", message, throwable);
        }

    }

}
