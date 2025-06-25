#!/bin/sh

. $HOME/.profile

USERCOUNT=4000000

cd
mkdir logs 2> /dev/null
cd voltdb-cvmsandbox/ddl

sqlcmd --servers=vdb1 < create_db.sql

cd
cd voltdb-cvmsandbox/scripts

java -jar $HOME/bin/addtodeploymentdotxml.jar vdb1 deployment topics.xml

$HOME/bin/reload_dashboards.sh voltdb-cvmsandbox.json

java  ${JVMOPTS}  -jar $HOME/bin/addtodeploymentdotxml.jar `cat $HOME/.vdbhostnames`  deployment $HOME/voltdb-voltdb-cvmsandbox/scripts/export_and_import.xml

cd ../jars
java ${JVMOPTS} -jar CreateChargingDemoData.jar `cat $HOME/.vdbhostnames`  $USERCOUNT 30 100000
