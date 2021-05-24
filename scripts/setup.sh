#!/bin/sh

. $HOME/.profile

cd
cd voltdb-cvmsandbox/ddl

sqlcmd --servers=vdb1 < create_db.sql

cd
cd voltdb-cvmsandbox/scripts

java -jar $HOME/bin/addtodeploymentdotxml.jar vdb1,vdb2,vdb3 deployment topics.xml

