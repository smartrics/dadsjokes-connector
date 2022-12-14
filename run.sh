#!/usr/bin/env sh

export AGENT_SEED=a122ccc3a3dc6b928a97215a3d63384a5a82e6988bd73f9b4c05fc3b0f529021
export AGENT_KEYNAME=test-agent-key
export USER_SEED=f76c6443a3dc6b928a97215a3d63384a5a82e6988bd73f9b4c05fc3b0f529021
export USER_KEYNAME=test-user-key

java -Djava.library.path=./lib -classpath "target/dadsjokes-1.0-SNAPSHOT.jar:target/lib/*" smartrics.iotics.connector.Main
