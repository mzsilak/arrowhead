#!/bin/bash
if [ $# -eq 0 ]
	then
	echo "Missing arguments:"
	echo "1: consumer system id"
	echo "2: provider system id"
	echo "3: service id"
	exit 0
fi
USEDB="USE arrowhead;"
CONSUMER=$1
PROVIDER=$2
SERVICE=$3
QRYMAXICAID="select max(id)+1 from intra_cloud_authorization;"
ICAID=$(mysql -u root -p -Bse "$USEDB$QRYMAXICAID")
INSCMD="insert into intra_cloud_authorization(consumer_system_id, provider_system_id, arrowhead_service_id, id) values($CONSUMER, $PROVIDER, $SERVICE, $ICAID);"
SQLCMD=$USEDB$INSCMD
echo $SQLCMD
mysql -u root -p -e "$SQLCMD"

