package smartrics.iotics.connector;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.iotics.api.ListAllTwinsRequest;
import com.iotics.api.ListAllTwinsResponse;
import com.iotics.api.TwinAPIGrpc;
import com.iotics.sdk.identity.SimpleConfig;
import com.iotics.sdk.identity.SimpleIdentityManager;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import smartrics.iotics.space.Builders;
import smartrics.iotics.space.HttpServiceRegistry;
import smartrics.iotics.space.IoticSpace;
import smartrics.iotics.space.grpc.HostManagedChannelBuilderFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class Main {

    private final String dns;
    private final SimpleIdentityManager sim;
    private final IoticSpace ioticSpace;

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
        if(!user.isValid() || !agent.isValid()) {
            throw new IllegalStateException("invalid identity env variables");
        }
        Main ds = new Main("demo.iotics.space", user, agent);
        ManagedChannel channel = ds.hostManagedChannel();
        CountDownLatch completed = new CountDownLatch(1);
        try {
            TwinAPIGrpc.TwinAPIFutureStub twinAPIStub = TwinAPIGrpc.newFutureStub(channel);
            ListAllTwinsRequest listRequest = ListAllTwinsRequest.newBuilder()
                    .setHeaders(Builders.newHeadersBuilder(ds.agentDid()).build())
                    .build();
            ListenableFuture<ListAllTwinsResponse> future = twinAPIStub.listAllTwins(listRequest);
            future.addListener(() -> {
                try {
                    ListAllTwinsResponse result = future.get();
                    System.out.println(result);
                    completed.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            e.printStackTrace();
        }
        completed.await();
    }

}
