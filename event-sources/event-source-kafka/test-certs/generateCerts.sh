#!/usr/bin/env bash
#
# Copyright (C) Telicent Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")
SCRIPT_DIR=$(cd "${SCRIPT_DIR}" && pwd)

BROKER_KEYSTORE="${SCRIPT_DIR}/broker-keystore"
CLIENT_KEYSTORE="${SCRIPT_DIR}/client-keystore"
BROKER_TRUSTSTORE="${SCRIPT_DIR}/broker-truststore"
CLIENT_TRUSTSTORE="${SCRIPT_DIR}/client-truststore"
KEYSTORE_PASSWORD="${1:-squirrel}"

echo "${KEYSTORE_PASSWORD}" > "${SCRIPT_DIR}/credentials"

function fail() {
  local STATUS=$1
  shift
  echo "[ERROR]: $@"
  exit ${STATUS}
}

function echorun() {
  echo "$@"
  "$@"
}

function createKeystore() {
    local KEYSTORE=$1
    local CN=$2

    echo "Creating ${KEYSTORE} for ${CN}..."

    rm -f "${KEYSTORE}"
    echorun keytool -keystore "${KEYSTORE}" -alias localhost \
        -validity 30 -genkey -keyalg RSA -storetype pkcs12 \
        -dname "cn=$2, ou=Engineering, o=IANode Ltd, c=UK" \
        -storepass ${KEYSTORE_PASSWORD} || fail 2 "Failed to generate $2 Keystore"
        
    rm -f "${KEYSTORE}.csr"
    echorun keytool -certreq -alias localhost -keyalg RSA \
        -file "${KEYSTORE}.csr" -keystore "${KEYSTORE}" \
        -storepass ${KEYSTORE_PASSWORD} || fail 3 "Failed to generate $2 Certificate Signing Request"
}

function generateRootCACertificate() {
  pushd "${SCRIPT_DIR}"
  rm -f serial.txt index.txt
  echo 01 > serial.txt
  touch index.txt

  rm -f "${SCRIPT_DIR}/cacert.pem"
  echorun openssl req -x509 -config "${SCRIPT_DIR}/openssl-ca.cnf" \
          -newkey rsa:4096 -sha256 -nodes -batch \
          -out "${SCRIPT_DIR}/cacert.pem" -outform PEM \
          -subj "/CN=Platform Team/OU=Engineering/O=IANode Ltd/C=UK" || fail 4 "Failed to create CA Certificate"
}

function importToTrustStore() {
    local TRUSTSTORE=$1
    echo "Importing CA Root Certificate into ${TRUSTSTORE}..."
    rm -f "${TRUSTSTORE}"
    echorun keytool -keystore "${TRUSTSTORE}" -alias CARoot -import \
        -file "${SCRIPT_DIR}/cacert.pem" -noprompt \
        -storepass ${KEYSTORE_PASSWORD} || fail 5 "Failed to import CA Certificate into $1"
}

function signCertificate() {
    local KEYSTORE=$1
    echo "Signing ${KEYSTORE} Certificate with CA Root Certificate..."
    echorun openssl ca -config "${SCRIPT_DIR}/openssl-ca.cnf" -policy signing_policy \
        -extensions signing_req -out "${KEYSTORE}.crt" \
        -days 30 -batch \
        -infiles "${KEYSTORE}.csr" || fail 6 "Failed to sign the ${KEYSTORE} Certificate"
        
    echo "Importing CA Root Certificate and Signed ${KEYSTORE} Certificate..."
    echorun keytool -keystore "${KEYSTORE}" -alias CARoot -import \
            -file "${SCRIPT_DIR}/cacert.pem" -noprompt \
            -storepass ${KEYSTORE_PASSWORD} || fail 7 "Failed to import CA Certificate into ${KEYSTORE}"
            
    echorun keytool -keystore "${KEYSTORE}" -alias localhost \
            -import -file "${KEYSTORE}.crt" -noprompt \
            -storepass ${KEYSTORE_PASSWORD} || fail 8 "Failed to import signed Certificate into ${KEYSTORE}"
}

if [ -z "${KEYSTORE_PASSWORD}" ]; then
  fail 1 "Empty keystore password provided, either provide no arguments to the script to use default password (squirrel) or provide a non-empty password"
fi

# Create Key and Trust Stores for Broker and Client
createKeystore "${BROKER_KEYSTORE}" "Kafka Broker"
createKeystore "${CLIENT_KEYSTORE}" "Kafka Client"

# Generate a Root CA Certificate
generateRootCACertificate

# Import Root CA Certificate into Trust Stores
importToTrustStore "${BROKER_TRUSTSTORE}"
importToTrustStore "${CLIENT_TRUSTSTORE}"

# Sign certificates with Root CA Certificate
signCertificate "${BROKER_KEYSTORE}"
signCertificate "${CLIENT_KEYSTORE}"

# Generate a Kafka Client Properties file
cat > "${SCRIPT_DIR}/client.properties" <<EOF
security.protocol=SSL
ssl.truststore.location=${SCRIPT_DIR}/client-truststore
ssl.truststore.password=${KEYSTORE_PASSWORD}
ssl.keystore.location=${SCRIPT_DIR}/client-keystore
ssl.keystore.password=${KEYSTORE_PASSWORD}
ssl.key.password=${KEYSTORE_PASSWORD}
# NB - Since we don't know what hostnames client and server have in test containers we don't add
#      any hostnames to the certificates so have to explicitly disable hostname verification
ssl.endpoint.identification.algorithm=
EOF

# Report on what's just been done
echo "Temporary CA Root, SSL Certificates, Key and Trust Stores generated!"
echo ""
echo "Broker Trust Store is ${BROKER_TRUSTSTORE}"
echo "Broker Key Store is ${BROKER_KEYSTORE}"
echo "Client Trust Store is ${CLIENT_TRUSTSTORE}"
echo "Client Key Store is ${CLIENT_KEYSTORE}"
echo "Password used to access Trust and Key Stores is found in ${SCRIPT_DIR}/credentials"
echo "Kafka Client Properties file is ${SCRIPT_DIR}/client.properties"
echo ""

