#!/usr/bin/env sh

java -Djava.library.path=./lib -classpath "target/dadsjokes-1.0-SNAPSHOT.jar:target/lib/*" smartrics.iotics.connector.Main
