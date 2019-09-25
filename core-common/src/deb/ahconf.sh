#!/bin/sh

AH_CONF_DIR="/etc/arrowhead"
AH_CLOUDS_DIR="${AH_CONF_DIR}/clouds"
AH_SYSTEMS_DIR="${AH_CONF_DIR}/systems"
AH_MYSQL_CONF="${AH_CONF_DIR}/mysql.cnf"

db_get arrowhead-core-common/mysql_password; AH_PASS_DB=$RET
db_get arrowhead-core-common/cert_password; AH_PASS_CERT=$RET
db_get arrowhead-core-common/cloudname; AH_CLOUD_NAME=$RET
db_get arrowhead-core-common/operator; AH_OPERATOR=$RET
db_get arrowhead-core-common/company; AH_COMPANY=$RET
db_get arrowhead-core-common/country; AH_COUNTRY=$RET

ah_cert () {
    dst_path=${1}
    dst_name=${2}
    cn=${3}

    file="${dst_path}/${dst_name}.p12"

    # The command has been renamed in newer versions of keytool
    gen_cmd="-genkeypair"
    keytool ${gen_cmd} --help >/dev/null 2>&1 || gen_cmd='-genkey'

    if [ ! -f "${file}" ]; then
        keytool ${gen_cmd} \
            -alias ${dst_name} \
            -keyalg RSA \
            -keysize 2048 \
            -dname "CN=${cn}, OU=${AH_OPERATOR}, O=${AH_COMPANY}, C=${AH_COUNTRY}" \
            -validity 3650 \
            -keypass ${AH_PASS_CERT} \
            -keystore ${file} \
            -storepass ${AH_PASS_CERT} \
            -storetype PKCS12 \
            -ext BasicConstraints:"Subject is a CA\nPath Length Constraint: None"

        chown :arrowhead ${file}
        chmod 640 ${file}
    fi
}

ah_cert_export () {
    src_path=${1}
    dst_name=${2}
    dst_path=${3}

    src_file="${src_path}/${dst_name}.p12"
    dst_file="${dst_path}/${dst_name}.crt"

    if [ ! -f "${dst_file}" ]; then
        keytool -exportcert \
            -rfc \
            -alias ${dst_name} \
            -storepass ${AH_PASS_CERT} \
            -keystore ${src_file} \
        | openssl x509 \
            -out ${dst_file}

        chown :arrowhead ${dst_file}
        chmod 640 ${dst_file}
    fi
}

ah_cert_export_pub () {
    src_path=${1}
    dst_name=${2}
    dst_path=${3}

    src_file="${src_path}/${dst_name}.p12"
    dst_file="${dst_path}/${dst_name}.pub"

    if [ ! -f "${dst_file}" ]; then
        keytool -exportcert \
            -rfc \
            -alias ${dst_name} \
            -storepass ${AH_PASS_CERT} \
            -keystore ${src_file} \
        | openssl x509 \
            -out ${dst_file} \
            -noout \
            -pubkey

        chown :arrowhead ${dst_file}
        chmod 640 ${dst_file}
    fi
}

ah_cert_import () {
    src_path=${1}
    src_name=${2}
    dst_path=${3}
    dst_name=${4}

    src_file="${src_path}/${src_name}.crt"
    dst_file="${dst_path}/${dst_name}.p12"

    keytool -import \
        -trustcacerts \
        -file ${src_file} \
        -alias ${src_name} \
        -keystore ${dst_file} \
        -keypass ${AH_PASS_CERT} \
        -storepass ${AH_PASS_CERT} \
        -storetype PKCS12 \
        -noprompt
}

ah_cert_signed () {
    dst_path=${1}
    dst_name=${2}
    cn=${3}
    src_path=${4}
    src_name=${5}

    src_file="${src_path}/${src_name}.p12"
    dst_file="${dst_path}/${dst_name}.p12"
    
    if [ ! -f "${dst_file}" ]; then
        ah_cert ${dst_path} ${dst_name} ${cn}

        keytool -export \
            -alias ${src_name} \
            -storepass ${AH_PASS_CERT} \
            -keystore ${src_file} \
        | keytool -import \
            -trustcacerts \
            -alias ${src_name} \
            -keystore ${dst_file} \
            -keypass ${AH_PASS_CERT} \
            -storepass ${AH_PASS_CERT} \
            -storetype PKCS12 \
            -noprompt

        keytool -certreq \
            -alias ${dst_name} \
            -keypass ${AH_PASS_CERT} \
            -keystore ${dst_file} \
            -storepass ${AH_PASS_CERT} \
        | keytool -gencert \
            -alias ${src_name} \
            -validity 3650 \
            -keypass ${AH_PASS_CERT} \
            -keystore ${src_file} \
            -storepass ${AH_PASS_CERT} \
        | keytool -importcert \
            -alias ${dst_name} \
            -keypass ${AH_PASS_CERT} \
            -keystore ${dst_file} \
            -storepass ${AH_PASS_CERT} \
            -noprompt
    fi
}

ah_cert_signed_system () {
    name=${1}

    path="${AH_SYSTEMS_DIR}/${name}"
    file="${path}/${name}.p12"

    if [ ! -f "${file}" ]; then
        ah_cert_signed \
            "${path}" \
            ${name} \
            "${name}.${AH_CLOUD_NAME}.${AH_OPERATOR}.arrowhead.eu" \
            ${AH_CLOUDS_DIR} \
            ${AH_CLOUD_NAME}

        ah_cert_import "${AH_CONF_DIR}" "master" "${path}" ${name}
    fi
}

ah_cert_trust () {
    dst_path=${1}
    src_path=${2}
    src_name=${3}

    src_file="${src_path}/${src_name}.p12"
    dst_file="${dst_path}/truststore.p12"
    
    if [ ! -f "${dst_file}" ]; then
        keytool -export \
            -alias ${src_name} \
            -storepass ${AH_PASS_CERT} \
            -keystore ${src_file} \
        | keytool -import \
            -trustcacerts \
            -alias ${src_name} \
            -keystore ${dst_file} \
            -keypass ${AH_PASS_CERT} \
            -storepass ${AH_PASS_CERT} \
            -storetype PKCS12 \
            -noprompt

        chown :arrowhead ${dst_file}
        chmod 640 ${dst_file}
    fi
}

ah_db_user () {
    if [ ! -f "${AH_MYSQL_CONF}" ]; then
        touch "${AH_MYSQL_CONF}"
        chmod 0600 "${AH_MYSQL_CONF}"
        cat >"${AH_MYSQL_CONF}" <<EOF
[client]
password="${AH_PASS_DB}"
EOF
    fi

    if ! mysql --defaults-extra-file="${AH_MYSQL_CONF}" -u arrowhead -e "SHOW DATABASES" >/dev/null 2>/dev/null; then
        if mysql -u root -e "SHOW DATABASES" >/dev/null 2>/dev/null; then
            mysql -u root <<EOF
CREATE DATABASE IF NOT EXISTS arrowhead;
CREATE USER IF NOT EXISTS arrowhead@localhost IDENTIFIED BY '${AH_PASS_DB}';
GRANT ALL PRIVILEGES ON arrowhead.* TO arrowhead@'localhost';
FLUSH PRIVILEGES;
EOF
        else
            db_input critical arrowhead-core-common/mysql_password_root || true
            db_go || true
            db_get arrowhead-core-common/mysql_password_root; AH_MYSQL_ROOT=$RET
            db_unregister arrowhead-core-common/mysql_password_root

            OPT_FILE="$(mktemp -q --tmpdir "arrowhead-core-common.XXXXXX")"
            trap 'rm -f "${OPT_FILE}"' EXIT
            chmod 0600 "${OPT_FILE}"

            cat >"${OPT_FILE}" <<EOF
[client]
password="${AH_MYSQL_ROOT}"
EOF

            mysql --defaults-extra-file="${OPT_FILE}" -u root <<EOF
CREATE DATABASE IF NOT EXISTS arrowhead;
CREATE USER IF NOT EXISTS arrowhead@localhost IDENTIFIED BY '${AH_PASS_DB}';
GRANT ALL PRIVILEGES ON arrowhead.* TO arrowhead@'localhost';
FLUSH PRIVILEGES;
EOF
        fi
    fi
}
