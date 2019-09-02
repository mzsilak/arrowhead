#!/usr/bin/env bash

set -e

AH_PASS_CERT="123456"

cloud_name=${1}
system="authorization"

dir="$(dirname "$0")"
src_file="${dir}/../certificates/${cloud_name}/${system}.p12"
dst_file="${system}.pub"

echo "Exporting to ${dst_file}..."

keytool -exportcert \
    -rfc \
    -alias "${system}.${cloud_name}.aitia.arrowhead.eu" \
    -storepass ${AH_PASS_CERT} \
    -keystore ${src_file} \
| openssl x509 \
    -out ${dst_file} \
    -noout \
    -pubkey
