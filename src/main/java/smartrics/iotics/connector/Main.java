package smartrics.iotics.connector;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.iotics.api.*;
import com.iotics.sdk.identity.SimpleConfig;
import com.iotics.sdk.identity.SimpleIdentityManager;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smartrics.iotics.connector.dadsjokes.DadJoke;
import smartrics.iotics.connector.dadsjokes.Icanhazdadjoke;
import smartrics.iotics.connector.dadsjokes.ModelOfIcanhazdadjoke;
import smartrics.iotics.connector.dadsjokes.TwinOfIcanhazdadjoke;
import smartrics.iotics.space.HttpServiceRegistry;
import smartrics.iotics.space.IoticSpace;
import smartrics.iotics.space.grpc.HostManagedChannelBuilderFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {

    private final String dns;
    private final SimpleIdentityManager sim;
    private final IoticSpace ioticSpace;
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public Main(String dns, SimpleConfig user, SimpleConfig agent) throws IOException {
        this.dns = dns;
        HttpServiceRegistry sr = new HttpServiceRegistry(this.dns);

        ioticSpace = new IoticSpace(sr);
        ioticSpace.initialise();

        sim = SimpleIdentityManager.Builder
                .anIdentityManager()
                .withAgentKeyID("#test-agent-0")
                .withUserKeyID("#test-user-0")
                .withAgentKeyName(agent.keyName())
                .withUserKeyName(user.keyName())
                .withResolverAddress(ioticSpace.endpoints().resolver())
                .withUserSeed(user.seed())
                .withAgentSeed(agent.seed())
                .build();
    }

    public String agentDid() {
        return sim.agentIdentity().did();
    }

    public ManagedChannel hostManagedChannel() throws IOException {
        ManagedChannelBuilder channelBuilder = new HostManagedChannelBuilderFactory()
                .withSimpleIdentityManager(sim)
                .withSGrpcEndpoint(ioticSpace.endpoints().grpc())
                .withTokenTokenDuration(Duration.ofSeconds(10))
                .withMaxRetryAttempts(10)
                .makeManagedChannelBuilder();
        return channelBuilder.keepAliveWithoutCalls(true).build();
    }

    public static void main(String[] args) throws Exception {
        SimpleConfig user = SimpleConfig.fromEnv("USER_");
        SimpleConfig agent = SimpleConfig.fromEnv("AGENT_");

        // 1 check twin exists - if not make it
        // 2 publish every


        if (!user.isValid() || !agent.isValid()) {
            throw new IllegalStateException("invalid identity env variables");
        }
        Main ds = new Main("demo.iotics.space", user, agent);
        ManagedChannel channel = ds.hostManagedChannel();
        CountDownLatch l = new CountDownLatch(1);
        try {
            TwinAPIGrpc.TwinAPIFutureStub twinAPIStub = TwinAPIGrpc.newFutureStub(channel);
            FeedAPIGrpc.FeedAPIFutureStub feedAPIStub = FeedAPIGrpc.newFutureStub(channel);
            Icanhazdadjoke backend = new Icanhazdadjoke();
            ModelOfIcanhazdadjoke model = new ModelOfIcanhazdadjoke(ds.sim, twinAPIStub, MoreExecutors.directExecutor());
            ListenableFuture<TwinID> modelFuture = model.makeIfAbsent();
            TwinID modelID = modelFuture.get();
            LOGGER.info("model id: {}", modelID);
            TwinOfIcanhazdadjoke t = new TwinOfIcanhazdadjoke(backend, ds.sim, twinAPIStub, feedAPIStub, MoreExecutors.directExecutor(), modelID);
            ListenableFuture<DeleteTwinResponse> dFut = t.delete();
            LOGGER.info("delete: {}", dFut.get());
            ListenableFuture<UpsertTwinResponse> fut = t.make();
            LOGGER.info("upsert: {}", fut.get());

            new Timer().schedule(new TimerTask() {

                @Override
                public void run() {
                    ListenableFuture<DadJoke> f = t.shareRandomJoke();
                    Futures.addCallback(f, new FutureCallback<>() {
                        @Override
                        public void onSuccess(DadJoke dadJoke) {
                            LOGGER.info("sharing joke: {}", dadJoke);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            LOGGER.error("sharing joke failed", throwable);
                        }
                    }, MoreExecutors.directExecutor());
                }
            }, 0, 60000);

        } catch (Exception e) {
            LOGGER.error("exc when calling", e);
        } finally {
            LOGGER.info("waiting for cdl");
            l.await();
            LOGGER.info("channel shutting down");
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            LOGGER.info("channel shut down --");
        }
    }

}
