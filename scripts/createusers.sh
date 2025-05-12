#!/bin/sh

. $HOME/.profile

USERCOUNT=4000000

cd
mkdir logs 2> /dev/null
cd voltdb-cvmsandbox/jars
java ${JVMOPTS} -jar CreateChargingDemoData.jar `cat $HOME/.vdbhostnames`  $USERCOUNT 30 100000
