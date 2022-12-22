#!/usr/bin/env sh
HOME=/opt/iotics/dadsjokes-connector
export $(cat $HOME/env | xargs)

cd $HOME && java -Djava.library.path=$HOME/lib -classpath "$HOME/dadsjokes-1.0-SNAPSHOT.jar:$HOME/lib/*" smartrics.iotics.connector.Main &

