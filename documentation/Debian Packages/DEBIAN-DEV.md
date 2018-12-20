# Guide to Debian Packages

## Useful resources:

Debian New Maintainers' Guide: https://www.debian.org/doc/manuals/maint-guide/index.en.html

How to use jdeb with Maven: https://github.com/tcurdt/jdeb/blob/master/docs/maven.md

The Debconf Programmer's Tutorial: http://www.fifi.org/doc/debconf-doc/tutorial.html

**The following changes and additions are required to add Debian/Ubuntu package generation to an Arrowhead core system.**

### File: pom.xml

- Set `artifactId` to `arrowhead-SYSTEMNAME`, for example: `arrowhead-authorization`. If multiple versions of the same
core system exist add a postfix, like `arrowhead-serviceregistry-sql`.

- Under `<plugins>` add:

```xml
<plugin>
<artifactId>jdeb</artifactId>
<groupId>org.vafer</groupId>
<version>1.7</version>
<executions>
  <execution>
    <phase>package</phase>
    <goals>
      <goal>jdeb</goal>
    </goals>
    <configuration>
      <snapshotExpand>true</snapshotExpand>
      <snapshotTemplate>[YYMMddHHmm].${git.commit.id.abbrev}</snapshotTemplate>
      <deb>target/${project.artifactId}_${revision}.deb</deb>
      <verbose>true</verbose>
      <controlDir>${basedir}/src/deb/control</controlDir>
      <dataSet>

        <data>
          <type>file</type>
          <src>${project.build.directory}/${project.build.finalName}.jar</src>
          <mapper>
            <type>perm</type>
            <prefix>/usr/share/arrowhead</prefix>
            <user>arrowhead</user>
            <filemode>755</filemode>
          </mapper>
        </data>
        
        <data>
          <type>link</type>
          <linkName>/usr/share/arrowhead/${project.artifactId}.jar</linkName>
          <linkTarget>/usr/share/arrowhead/${project.build.finalName}.jar</linkTarget>
        </data>

      </dataSet>
    </configuration>
  </execution>
</executions>
</plugin>
```

- Additional data sections should be added for files to be installed with the core systems. Examples:

The service definition file, used to start and stop the daemon (should most likely always be there):    
```xml
<data>
  <type>file</type>
  <src>${project.basedir}/src/deb/arrowhead-authorization.service</src>
  <mapper>
    <type>perm</type>
    <prefix>/etc/systemd/system</prefix>
    <filemode>664</filemode>
  </mapper>
</data>
```

Sql files to create empty database tables:
```xml
<data>
  <type>file</type>
  <src>${project.basedir}/src/deb/create_authorization_db_empty.sql</src>
  <dst>/usr/share/arrowhead/db/create_authorization_db_empty.sql</dst>
</data>
```

Additional libraries specific to a core system (not found in the `pom.xml` of the common module):
```xml
<data>
  <type>file</type>
  <src>${project.build.directory}/lib/bcprov-jdk15on-${bouncy.castle.version}.jar</src>
  <mapper>
    <type>perm</type>
    <prefix>/usr/share/arrowhead/lib</prefix>
  </mapper>
</data>
```

### File: src/deb/SYSTEM-NAME.service 

Service definition file, used to start/stop the daemon. Also contains a list of dependencies, which should be started
before this system.

```
[Unit]
Description=arrowhead-orchestrator
After=network.target mysql.target arrowhead-serviceregistry-sql.service
Requires=arrowhead-serviceregistry-sql.service arrowhead-authorization.service arrowhead-gatekeeper.service

[Service]
WorkingDirectory=/etc/arrowhead/systems/orchestrator
ExecStart=/usr/bin/java -jar /usr/share/arrowhead/arrowhead-orchestrator-4.1.1-SNAPSHOT.jar -d -daemon -tls
TimeoutStopSec=5
Type=simple
User=arrowhead
Group=arrowhead

[Install]
WantedBy=default.target
```

### File: src/deb/create_SYSTEMNAME_db_empty.sql 

Script to create empty MySQL tables if such is required - can be omitted if not.

Note that 'DROP [...]' statements should be removed and 'IF NOT EXISTS' added to all 'CREATE [...]' statements.

```mysql
CREATE DATABASE  IF NOT EXISTS `arrowhead`;
USE `arrowhead`;

CREATE TABLE IF NOT EXISTS `inter_cloud_authorization` (
  `id` bigint(20) NOT NULL,
  `consumer_cloud_id` bigint(20) NOT NULL,
  `arrowhead_service_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKj4pymxepq7mf82wx7f8e4hd9b` (`consumer_cloud_id`,`arrowhead_service_id`),
  KEY `FKsh4gbm0vs76weoq1lti6awtwf` (`arrowhead_service_id`),
  CONSTRAINT `FKsh4gbm0vs76weoq1lti6awtwf` FOREIGN KEY (`arrowhead_service_id`) REFERENCES `arrowhead_service` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKsw50x8tjybx1jjrkj6aamxt8c` FOREIGN KEY (`consumer_cloud_id`) REFERENCES `arrowhead_cloud` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `intra_cloud_authorization` (
  `id` bigint(20) NOT NULL,
  `consumer_system_id` bigint(20) NOT NULL,
  `provider_system_id` bigint(20) NOT NULL,
  `arrowhead_service_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4ie5ps7a6w40iqdte0u53mw1u` (`consumer_system_id`,`provider_system_id`,`arrowhead_service_id`),
  KEY `FKt01tq84ypy16yfpt2q9v7qn2b` (`provider_system_id`),
  KEY `FK1nx371ky16pl2rl0f4hk3puk4` (`arrowhead_service_id`),
  CONSTRAINT `FK1nx371ky16pl2rl0f4hk3puk4` FOREIGN KEY (`arrowhead_service_id`) REFERENCES `arrowhead_service` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK58r9imuaq3dy3o96w5xcxkemh` FOREIGN KEY (`consumer_system_id`) REFERENCES `arrowhead_system` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKt01tq84ypy16yfpt2q9v7qn2b` FOREIGN KEY (`provider_system_id`) REFERENCES `arrowhead_system` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

```

### File: src/deb/control 

Description file for the package manager. Contains an description of the system, the maintainer, and which packages
should be installed before this one. In general, all should depend on arrowhead-common.

```
Package: [[name]]
Version: [[version]]
Section: contrib/java
Priority: optional
Architecture: all
Maintainer: Thomas Pedersen <tp@cs.aau.dk>
Homepage: http://www.arrowhead.eu
Description: Arrowhead Gatekeeper System
Distribution: development
Depends: java-runtime-headless, virtual-mysql-server, arrowhead-common, arrowhead-serviceregistry-sql, arrowhead-authorization, arrowhead-gateway
```

### File: src/deb/postinst

The general structure of the post-installation script:

```bash
#!/bin/sh
# postinst script for arrowhead-authorization
#
# see: dh_installdeb(1)

set -e

. /usr/share/debconf/confmodule

SYSTEM_NAME="authorization"
PKG_NAME="arrowhead-authorization"

# summary of how this script can be called:
#        * <postinst> `configure' <most-recently-configured-version>
#        * <old-postinst> `abort-upgrade' <new version>
#        * <conflictor's-postinst> `abort-remove' `in-favour' <package>
#          <new-version>
#        * <postinst> `abort-remove'
#        * <deconfigured's-postinst> `abort-deconfigure' `in-favour'
#          <failed-install-package> <version> `removing'
#          <conflicting-package> <version>
# for details, see https://www.debian.org/doc/debian-policy/ or
# the debian-policy package

case "$1" in
    configure)
        . /usr/share/arrowhead/conf/ahconf.sh
        SYSTEM_DIR="${AH_SYSTEMS_DIR}/${SYSTEM_NAME}"
    ;;

    abort-upgrade|abort-remove|abort-deconfigure)
    ;;

    *)
        echo "postinst called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

# dh_installdeb will replace this with shell code automatically
# generated by other debhelper scripts.

#DEBHELPER#

exit 0
```

The following sections should be added under configure:

- Creation of MySQL databases. Functions in ahconf.sh from arrowhead-common package can be used to generate default
  tables. Default tables (like the 'system' and 'service') should be created by all systems that need them - if they
  already exist, nothing will be done.
  
```bash
echo "Configuring MySQL database..." >&2
ah_db_logs
ah_db_arrowhead_cloud
ah_db_arrowhead_service
ah_db_arrowhead_service_interface_list
ah_db_arrowhead_system
ah_db_hibernate_sequence
mysql -u root < /usr/share/arrowhead/db/create_authorization_db_empty.sql
ah_db_own_cloud
```
  
- If database is used, it should also create DB user by calling ah_db_user function from ahconf.sh

```bash
ah_db_user
```

- Create a directory for configuration under `/etc/arrowhead/systems`

```bash
if [ ! -d "${SYSTEM_DIR}" ]; then
    mkdir -p ${SYSTEM_DIR}
fi
```

- Generate a signed system certificate in this dir (use functions in ahconf.sh again)

```bash
ah_cert_signed_system ${SYSTEM_NAME}
```

- Insert data into MySQL database if required (Gatekeeper currently does this)

```bash
if [ $(mysql -u root arrowhead -sse "SELECT COUNT(*) FROM arrowhead_cloud;") -eq 0 ]; then
    pubkey64=$(\
        keytool -export \
            -alias ${SYSTEM_NAME} \
            -storepass ${AH_PASS_CERT}\
            -keystore ${SYSTEM_DIR}/${SYSTEM_NAME}.p12 \
        | openssl x509 \
            -inform der \
            -pubkey \
            -noout \
        | tail -n +2 | head -n -1 | sed ':a;N;$!ba;s/\n//g')

    mysql -u root arrowhead <<EOF
LOCK TABLES arrowhead_cloud WRITE;
INSERT INTO arrowhead_cloud VALUES (1,'localhost','${pubkey64}','${AH_CLOUD_NAME}','${SYSTEM_NAME}','${AH_OPERATOR}',8447,'Y');
UNLOCK TABLES;
EOF
fi

if [ $(mysql -u root arrowhead -sse "SELECT COUNT(*) FROM own_cloud;") -eq 0 ]; then
    mysql -u root arrowhead <<EOF
LOCK TABLES own_cloud WRITE;
INSERT INTO own_cloud VALUES (1);
UNLOCK TABLES;
EOF
fi
```

- Create 'default.conf' file in this dir containing all the config values for the core system

```bash
if [ ! -f "${SYSTEM_DIR}/default.conf" ]; then
    /bin/cat <<EOF >${SYSTEM_DIR}/default.conf
############################################
###       APPLICATION PARAMETERS         ###
############################################

# Database parameters (mandatory)
db_user=arrowhead
db_password=${AH_PASS_DB}
db_address=jdbc:mysql://127.0.0.1:3306/arrowhead

# Certificate related paths and passwords (mandatory in secure mode)
keystore=${SYSTEM_DIR}/${SYSTEM_NAME}.p12
keystorepass=${AH_PASS_CERT}
keypass=${AH_PASS_CERT}
truststore=${AH_CONF_DIR}/truststore.p12
truststorepass=${AH_PASS_CERT}

# Authorization web-server parameters
address=0.0.0.0
insecure_port=8444
secure_port=8445

# Service Registry web-server parameters (to register the Authorization services)
sr_address=0.0.0.0
sr_insecure_port=8442
sr_secure_port=8443

#Allow querying access to the authorization tables for application systems (true/false - only has effect in secure mode)
enable_auth_for_cloud=false

############################################
###          LOGGING PARAMETERS          ###
############################################

# Define the root logger with appender file
log4j.rootLogger=INFO, DB, FILE

# Database related config
# Define the DB appender
log4j.appender.DB=org.apache.log4j.jdbc.JDBCAppender


# Set Database URL
log4j.appender.DB.URL=jdbc:mysql://127.0.0.1:3306/arrowhead
# Set database user name and password
log4j.appender.DB.user=arrowhead
log4j.appender.DB.password=${AH_PASS_DB}
# Set the SQL statement to be executed.
log4j.appender.DB.sql=INSERT INTO logs VALUES(DEFAULT,'%d{yyyy-MM-dd HH:mm:ss}','%C','%p','%m')
# Define the layout for file appender
log4j.appender.DB.layout=org.apache.log4j.PatternLayout
# Disable Hibernate verbose logging
log4j.logger.org.hibernate=fatal

# File related config
# Define the file appender
log4j.appender.FILE=org.apache.log4j.FileAppender
# Set the name of the file
log4j.appender.FILE.File=/var/log/arrowhead/${SYSTEM_NAME}.log
# Set the immediate flush to true (default)
log4j.appender.FILE.ImmediateFlush=true
# Set the threshold to debug mode
log4j.appender.FILE.Threshold=debug
# Set the append to false, overwrite
log4j.appender.FILE.Append=false
# Define the layout for file appender
log4j.appender.FILE.layout=org.apache.log4j.PatternLayout
log4j.appender.FILE.layout.conversionPattern=%d{yyyy-MM-dd HH:mm:ss}, %C, %p, %m%n
EOF
    chown root:arrowhead ${SYSTEM_DIR}/default.conf
    chmod 640 ${SYSTEM_DIR}/default.conf
fi
```

- Reload/restart the daemon

```bash
echo "Restarting ${PKG_NAME}..." >&2
systemctl daemon-reload
systemctl restart ${PKG_NAME}.service
```

### File: src/deb/postrm 

Should delete files and directories created by postinst.

```bash
#!/bin/sh
# postrm script for arrowhead-gatekeeper
#
# see: dh_installdeb(1)

set -e

. /usr/share/debconf/confmodule

SYSTEM_NAME="gatekeeper"

# summary of how this script can be called:
#        * <postrm> `remove'
#        * <postrm> `purge'
#        * <old-postrm> `upgrade' <new-version>
#        * <new-postrm> `failed-upgrade' <old-version>
#        * <new-postrm> `abort-install'
#        * <new-postrm> `abort-install' <old-version>
#        * <new-postrm> `abort-upgrade' <old-version>
#        * <disappearer's-postrm> `disappear' <overwriter>
#          <overwriter-version>
# for details, see https://www.debian.org/doc/debian-policy/ or
# the debian-policy package


case "$1" in
    purge)
        AH_CONF_DIR="/etc/arrowhead"
        AH_SYSTEMS_DIR="${AH_CONF_DIR}/systems"
        SYSTEM_DIR="${AH_SYSTEMS_DIR}/${SYSTEM_NAME}"
        
        rm -f \
            /var/log/arrowhead/${SYSTEM_NAME}.log \
            ${SYSTEM_DIR}/default.conf \
            ${SYSTEM_DIR}/${SYSTEM_NAME}.p12
        rmdir ${SYSTEM_DIR} 2>/dev/null || true
        rmdir /var/log/arrowhead 2>/dev/null || true
        db_purge
    ;;
    remove|upgrade|failed-upgrade|abort-install|abort-upgrade|disappear)
    ;;

    *)
        echo "postrm called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

# dh_installdeb will replace this with shell code automatically
# generated by other debhelper scripts.

#DEBHELPER#

exit 0

```

### File: src/deb/preinst 

Currently does nothing.

```bash
#!/bin/sh
# preinst script for arrowhead-authorization
#
# see: dh_installdeb(1)

set -e

# summary of how this script can be called:
#        * <new-preinst> `install'
#        * <new-preinst> `install' <old-version>
#        * <new-preinst> `upgrade' <old-version>
#        * <old-preinst> `abort-upgrade' <new-version>
# for details, see https://www.debian.org/doc/debian-policy/ or
# the debian-policy package


case "$1" in
    install|upgrade)
    ;;

    abort-upgrade)
    ;;

    *)
        echo "preinst called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

# dh_installdeb will replace this with shell code automatically
# generated by other debhelper scripts.

#DEBHELPER#

exit 0

```

### File: src/deb/prerm 

Currently only stops the daemon.

```bash
#!/bin/sh
# prerm script for arrowhead-authorization
#
# see: dh_installdeb(1)

set -e

PKG_NAME="arrowhead-authorization"

# summary of how this script can be called:
#        * <prerm> `remove'
#        * <old-prerm> `upgrade' <new-version>
#        * <new-prerm> `failed-upgrade' <old-version>
#        * <conflictor's-prerm> `remove' `in-favour' <package> <new-version>
#        * <deconfigured's-prerm> `deconfigure' `in-favour'
#          <package-being-installed> <version> `removing'
#          <conflicting-package> <version>
# for details, see https://www.debian.org/doc/debian-policy/ or
# the debian-policy package


case "$1" in
    remove)
        systemctl stop ${PKG_NAME}.service
    ;;

    upgrade|deconfigure)
    ;;

    failed-upgrade)
    ;;

    *)
        echo "prerm called with unknown argument \`$1'" >&2
        exit 1
    ;;
esac

# dh_installdeb will replace this with shell code automatically
# generated by other debhelper scripts.

#DEBHELPER#

exit 0

```