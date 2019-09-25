#!/bin/bash
USEDB="USE arrowhead;"
DELCMD="delete from intra_cloud_authorization where id=$1;"
SQLCMD=$USEDB$DELCMD
echo $SQLCMD
mysql -u root -p -e "$SQLCMD"

