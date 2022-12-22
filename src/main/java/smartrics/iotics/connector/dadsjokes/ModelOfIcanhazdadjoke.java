package smartrics.iotics.connector.dadsjokes;

import com.iotics.api.TwinAPIGrpc;
import com.iotics.sdk.identity.SimpleIdentityManager;
import smartrics.iotics.space.twins.GenericModelTwin;

import java.util.concurrent.Executor;

public class ModelOfIcanhazdadjoke extends GenericModelTwin {

    public ModelOfIcanhazdadjoke(SimpleIdentityManager sim, TwinAPIGrpc.TwinAPIFutureStub stub, Executor executor) {
        super(sim, "icanhazdadjoke_model_keyname", stub, executor,
                "ICanHazDadJoke Twin Model", "Dad Joke generator MODEL", "https://icanhazdadjoke.com/ont/joke", "#ffff00");
    }

}
