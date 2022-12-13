package smartrics.iotics.connector.dadsjokes;

import com.iotics.sdk.identity.SimpleIdentityManager;

public class Twin {

    private final Backend backend;
    private final SimpleIdentityManager sim;

    public Twin(Backend backend, SimpleIdentityManager sim) {
        this.backend = backend;
        this.sim = sim;
    }


}
