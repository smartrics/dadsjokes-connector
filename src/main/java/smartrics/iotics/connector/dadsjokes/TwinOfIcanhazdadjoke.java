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
import smartrics.iotics.space.Builders;
import smartrics.iotics.space.twins.AbstractTwinWithModel;
import smartrics.iotics.space.twins.Maker;
import smartrics.iotics.space.twins.Publisher;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static smartrics.iotics.space.UriConstants.*;

public class TwinOfIcanhazdadjoke extends AbstractTwinWithModel implements Maker, Publisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwinOfIcanhazdadjoke.class);
    private final Backend backend;
    private final FeedAPIGrpc.FeedAPIFutureStub feedStub;
    private Integer count;

    public TwinOfIcanhazdadjoke(Backend backend, SimpleIdentityManager sim,
                                TwinAPIGrpc.TwinAPIFutureStub twinStub,
                                FeedAPIGrpc.FeedAPIFutureStub feedStub,
                                Executor executor, TwinID modelDid) {
        super(sim, "icanhazdadjoke_keyname", twinStub, executor, modelDid);
        this.backend = backend;
        this.count = 0;
        this.feedStub = feedStub;
    }

    public ListenableFuture<DadJoke> shareRandomJoke() {
        SettableFuture<DadJoke> ret = SettableFuture.create();
        FeedID jokeFeedID = FeedID.newBuilder()
                .setTwinId(TwinOfIcanhazdadjoke.this.getIdentity().did())
                .setId("random_joke")
                .build();
        FeedID statusFeedID = FeedID.newBuilder()
                .setTwinId(TwinOfIcanhazdadjoke.this.getIdentity().did())
                .setId("status")
                .build();
        backend.random(dadJoke -> {
            if (dadJoke.status() == 200) {
                String joke = makeJokePayload(dadJoke);
                ListenableFuture<ShareFeedDataResponse> f = Publisher.super.share(jokeFeedID, joke);
                final Publisher publisher = this;
                Futures.addCallback(f, new FutureCallback<>() {
                    @Override
                    public void onSuccess(ShareFeedDataResponse shareFeedDataResponse) {
                        LOGGER.info("published {}", joke);
                        String statusPayload = makeStatusPayload(true, "OK", TwinOfIcanhazdadjoke.this.incCount());
                        publisher.share(statusFeedID, statusPayload);
                        LOGGER.info("published {}", statusPayload);
                        ret.set(dadJoke);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        String s = "Backend status: " + dadJoke.status();
                        String statusPayload = makeStatusPayload(false, s, TwinOfIcanhazdadjoke.this.count);
                        publisher.share(statusFeedID, statusPayload);
                        LOGGER.info("published {}", statusPayload);
                        ret.setException(throwable);
                    }
                }, getExecutor());
            } else {
                String s = "Backend status: " + dadJoke.status();
                String statusPayload = makeStatusPayload(false, s, this.count);
                Publisher.super.share(statusFeedID, statusPayload);
                LOGGER.info("published {}", statusPayload);
                ret.setException(new IllegalStateException(s));
            }
        }, s -> {
            Publisher.super.share(statusFeedID, makeStatusPayload(false, s, this.count));
            ret.setException(new IllegalStateException(s));
        });
        return ret;
    }

    private Integer incCount() {
        this.count = this.count + 1;
        return this.count;
    }

    private String makeStatusPayload(boolean status, String message, Integer count) {
        Map<String, String> values = new HashMap<>();
        values.put("status", Boolean.toString(status));
        values.put("message", message);
        values.put("count", count.toString());
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
        return getTwinAPIFutureStub().upsertTwin(UpsertTwinRequest.newBuilder()
                .setHeaders(Builders.newHeadersBuilder(getSim().agentIdentity().did())
                        .build())
                .setPayload(UpsertTwinRequest.Payload.newBuilder()
                        .setTwinId(TwinID.newBuilder().setId(getIdentity().did()).build())
                        .setVisibility(Visibility.PUBLIC)
                        .addProperties(Property.newBuilder()
                                .setKey(ON_RDFS_COMMENT_PROP)
                                .setLiteralValue(Literal.newBuilder().setValue("Dad Joke generator").build())
                                .build())
                        .addProperties(Property.newBuilder()
                                .setKey(ON_RDFS_LABEL_PROP)
                                .setLiteralValue(Literal.newBuilder().setValue("ICanHazDadJoke").build())
                                .build())
                        .addProperties(Property.newBuilder()
                                .setKey(IOTICS_APP_MODEL_PROP)
                                .setUriValue(Uri.newBuilder().setValue(getModelDid().getId()).build())
                                .build())
                        .addProperties(Property.newBuilder()
                                .setKey(ON_RDF_TYPE_PROP)
                                .setUriValue(Uri.newBuilder().setValue("https://icanhazdadjoke.com/ont/joke").build())
                                .build())
                        .addProperties(Property.newBuilder()
                                .setKey("http://www.w3.org/ns/dcat#endpointURL")
                                .setUriValue(Uri.newBuilder().setValue("https://icanhazdadjoke.com").build())
                                .build())
                        .addProperties(Property.newBuilder()
                                .setKey(IOTICS_PUBLIC_ALLOW_LIST_PROP)
                                .setUriValue(Uri.newBuilder().setValue(IOTICS_PUBLIC_ALLOW_ALL_VALUE).build())
                                .build())
                        .addFeeds(UpsertFeedWithMeta.newBuilder()
                                .setId("random_joke")
                                .setStoreLast(true)
                                .addProperties(Property.newBuilder()
                                        .setKey(ON_RDFS_COMMENT_PROP)
                                        .setLiteralValue(Literal.newBuilder().setValue("Random joke feed").build())
                                        .build())
                                .addProperties(Property.newBuilder()
                                        .setKey(ON_RDFS_LABEL_PROP)
                                        .setLiteralValue(Literal.newBuilder().setValue("Joke").build())
                                        .build())
                                .addValues(Value.newBuilder()
                                        .setLabel("id").setComment("the ID of the joke")
                                        .setDataType("string")
                                        .build())
                                .addValues(Value.newBuilder()
                                        .setLabel("joke").setComment("the joke")
                                        .setDataType("string")
                                        .build())
                                .addValues(Value.newBuilder()
                                        .setLabel("timestamp").setComment("feed update date")
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
                                .addProperties(Property.newBuilder()
                                        .setKey(ON_RDFS_COMMENT_PROP)
                                        .setLiteralValue(Literal.newBuilder().setValue("Twin status").build())
                                        .build())
                                .addProperties(Property.newBuilder()
                                        .setKey(ON_RDFS_LABEL_PROP)
                                        .setLiteralValue(Literal.newBuilder().setValue("Status").build())
                                        .build())
                                .addValues(Value.newBuilder()
                                        .setLabel("message").setComment("twin status message")
                                        .setDataType("string")
                                        .build())
                                .addValues(Value.newBuilder()
                                        .setLabel("count").setComment("count of jokes published since start of the connector")
                                        .setDataType("integer")
                                        .build())
                                .addValues(Value.newBuilder()
                                        .setLabel("timestamp").setComment("feed update date")
                                        .setDataType("dateTime")
                                        .build())
                                .build())
                        .build())
                .build());
    }

    @Override
    public FeedAPIGrpc.FeedAPIFutureStub getFeedAPIFutureStub() {
        return this.feedStub;
    }
}
