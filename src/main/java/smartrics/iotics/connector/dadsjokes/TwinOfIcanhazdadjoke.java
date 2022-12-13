package smartrics.iotics.connector.dadsjokes;

import com.google.common.util.concurrent.*;
import com.iotics.api.*;
import com.iotics.sdk.identity.SimpleIdentityManager;
import smartrics.iotics.connector.twins.AbstractTwin;
import smartrics.iotics.connector.twins.AbstractTwinWithModel;
import smartrics.iotics.space.Builders;

import java.util.concurrent.Executor;

public class TwinOfIcanhazdadjoke extends AbstractTwinWithModel {

    private final Backend backend;

    public TwinOfIcanhazdadjoke(Backend backend, SimpleIdentityManager sim, TwinAPIGrpc.TwinAPIFutureStub stub, Executor executor, TwinID modelDid) {
        super(sim, "icanhazdadjoke_keyname", stub, executor, modelDid);
        this.backend = backend;
    }

    public ListenableFuture<UpsertTwinResponse> make() {
        return this.twinStub.upsertTwin(UpsertTwinRequest.newBuilder()
                .setHeaders(Builders.newHeadersBuilder(sim.agentIdentity().did())
                        .build())
                .setPayload(UpsertTwinRequest.Payload.newBuilder()
                        .setTwinId(TwinID.newBuilder().setId(identity.did()).build())
                        .addProperties(Property.newBuilder()
                                .setKey(ON_RDFS + "#comment")
                                .setLiteralValue (Literal.newBuilder().setValue("Dad Joke generator").build())
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
                                .build())
                        .build())
                .build());
    }
}
