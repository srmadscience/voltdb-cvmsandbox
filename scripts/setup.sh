#!/bin/sh

. $HOME/.profile

cd
cd voltdb-cvmsandbox/ddl

sqlcmd --servers=vdb1 < create_db.sql

