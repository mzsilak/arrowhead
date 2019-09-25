#!/usr/bin/env bash

mvn install:install-file -DgroupId=eu.arrowhead -DartifactId=arrowhead-common -Dversion=4.1.3-SNAPSHOT \
    -Dfile=$(dirname "$0")/../common/target/arrowhead-common-4.1.3-SNAPSHOT.jar -Dpackaging=jar \
    -DgeneratePom=true -DlocalRepositoryPath=$(pwd) -DcreateChecksum=true

echo
echo "-------------------------------------------------------"
echo " REMEMBER TO ADD DEPENDENCIES TO POM FILE MANUALLY !!! "
echo "-------------------------------------------------------"
echo