#!/bin/sh

. $HOME/.profile


if 
	[ "$#" -ne 4 ]
then
	echo Using Defaults...
	USERCOUNT=4000000
	TPMS=10
	DURATION=3600
	QUERY=120
else
	USERCOUNT=$1
	TPMS=$2
	DURATION=$3
	QUERY=$4
fi
cd
mkdir logs 2> /dev/null
cd voltdb-cvmsandbox/jars
java ${JVMOPTS} -jar ChargingDemoTransactions.jar `cat $HOME/.vdbhostnames` $USERCOUNT $TPMS $DURATION $QUERY
