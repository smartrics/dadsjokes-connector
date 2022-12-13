package smartrics.iotics.connector.twins;

import com.iotics.api.TwinAPIGrpc;
import com.iotics.api.TwinID;
import com.iotics.sdk.identity.SimpleIdentityManager;

import java.util.concurrent.Executor;

public abstract class AbstractTwinWithModel extends AbstractTwin {

    protected final TwinID modelDid;

    public AbstractTwinWithModel(SimpleIdentityManager sim, String keyName, TwinAPIGrpc.TwinAPIFutureStub stub, Executor executor, TwinID modelDid) {
        super(sim, keyName, stub, executor);
        this.modelDid = modelDid;
    }
}
