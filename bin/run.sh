#!/usr/bin/env sh

HOME="./target"

JAVA_OPTS="$JAVA_OPTS -Djava.library.path=$HOME/lib"

CP="$HOME/dadsjokes-1.0-SNAPSHOT.jar"
CP="$CP:$HOME/lib/*"

MAIN_CLASS="smartrics.iotics.connector.Main"

java $JAVA_OPTS -classpath "$CP" $MAIN_CLASS
