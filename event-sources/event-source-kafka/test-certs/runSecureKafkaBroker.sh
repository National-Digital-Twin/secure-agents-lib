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

echo "This script assumes you have already run generateCerts.sh to create test certificates"
echo "If you have yet to do this then the docker run command will fail below with messages about missing files"
echo ""

echo "Removing any previous running Kafka Broker..."
echorun docker rm -f secure-kafka
echo ""

echo "Starting the Kafka Broker..."
echo "NB: You can send an interrupt (Ctrl+C) to stop the broker at any time"
echo ""
echorun docker run \
        --name=secure-kafka \
        -h secure-kafka \
        -p 9093:9093 \
        -v "${SCRIPT_DIR}:/etc/kafka/secrets" \
        -e KAFKA_NODE_ID=1 \
        -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP='SSL:SSL,CONTROLLER:SSL' \
        -e KAFKA_LISTENERS='SSL://:9093,CONTROLLER://secure-kafka:19093' \
        -e KAFKA_ADVERTISED_LISTENERS='SSL://localhost:9093' \
        -e KAFKA_JMX_HOSTNAME=localhost \
        -e KAFKA_PROCESS_ROLES='broker,controller' \
        -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
        -e KAFKA_CONTROLLER_QUORUM_VOTERS='1@secure-kafka:19093' \
        -e KAFKA_INTER_BROKER_LISTENER_NAME='SSL' \
        -e KAFKA_CONTROLLER_LISTENER_NAMES='CONTROLLER' \
        -e KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM=' ' \
        -e KAFKA_SSL_TRUSTSTORE_FILENAME='broker-truststore' \
        -e KAFKA_SSL_TRUSTSTORE_CREDENTIALS='credentials' \
        -e KAFKA_SSL_KEYSTORE_FILENAME='broker-keystore' \
        -e KAFKA_SSL_KEYSTORE_CREDENTIALS='credentials' \
        -e KAFKA_SSL_KEY_CREDENTIALS='credentials' \
        -e KAFKA_SSL_CLIENT_AUTH='required' \
        -e CLUSTER_ID='MkU3OEVBNTcwNTJENDM2Qk' \
        confluentinc/cp-kafka:7.7.0