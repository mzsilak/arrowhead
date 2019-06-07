#!/bin/bash
mysql -u root -p -e "USE arrowhead;select id, system_name, address, port from arrowhead_system;"


