# dadsjokes-connector
An IOTICS connector to create digital twins of dad's jokes' generators 

## Build

Build with:

`mvn package`

The jar file is created in `target/` and the dependencies copied in `target/lib`.

## Run

Export the following environment variable with the appropriate values as documented in [IOTICS Docs](http://docs.iotics.com).
```shell
USER_SEED=<your user seed>
USER_KEYNAME=<your user key name>
AGENT_SEED=<this agent seed>
AGENT_KEYNAME=<this agent key name>
SPACE=<your space>.iotics.space
SHARE_PERIOD_SEC=<default 60s>
```

Copy the ffi library at `lib/lib-iotics-id-sdk.so` into the lib directory where all your dependencoes are.

Use the following command line:

`java -Djava.library.path=./lib -classpath "dadsjokes-1.0-SNAPSHOT.jar:lib/*" smartrics.iotics.connector.Main`

