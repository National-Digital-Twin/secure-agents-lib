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

BOOTSTRAP_SERVERS=${BOOTSTRAP_SERVERS:-$1}
if [ -z "${BOOTSTRAP_SERVERS}" ]; then
  echo "Failed to provide Kafka Bootstrap Servers either via BOOTSTRAP_SERVERS environment variable or first argument to this script"
  exit 1
fi
echo "Using Kafka Bootstrap Servers ${BOOTSTRAP_SERVERS}"

TMPDIR=${TMPDIR:-/tmp}
echo "Using temporary directory ${TMPDIR} for logs"
echo ""

# Setup cleanup ASAP
FAKE_APP_PIDS=()
FAKE_APP_LOGS=()
function cleanup() {
  echo "Interrupted..."
  if [ "${#FAKE_APP_PIDS[@]}" -gt 0 ]; then
    kill "${FAKE_APP_PIDS[@]}"
  fi
  exit 0
}
trap "cleanup" SIGINT SIGTERM

function runFakeApp() {
  local APP_NAME=$1
  shift
  echo "$@"
  "$@" > "${TMPDIR}/${APP_NAME}.log" 2>&1 &
  FAKE_APP_PIDS+=( "$!" )
  echo "Fake application ${APP_NAME} with logging to ${TMPDIR}/${APP_NAME}.log"
  FAKE_APP_LOGS+=( "${TMPDIR}/${APP_NAME}.log")
  echo ""
}

# Detect an available debug script
DEBUG_SCRIPT=
if [ -f "${SCRIPT_DIR}/debug" ]; then
  DEBUG_SCRIPT="${SCRIPT_DIR}/debug"
elif [ -f "${SCRIPT_DIR}/cli-debug/debug" ]; then
  DEBUG_SCRIPT="${SCRIPT_DIR}/cli-debug/debug"
elif [ -f "debug" ]; then
  DEBUG_SCRIPT="debug"
elif [ -f "/app/debug" ]; then
  DEBUG_SCRIPT="/app/debug"
fi

COMMON_ARGS=(
  "${DEBUG_SCRIPT}"
  "fake-reporter"
  "--bootstrap-servers"
  "${BOOTSTRAP_SERVERS}"
  "--topic"
  "knowledge"
)

ADAPTER_ARGS=(
  "--app-id"
  "ExampleAdapter"
  "--app-name"
  "Example Adapter"
  "--component-type"
  "adapter"
  "--input-name"
  "data.csv"
  "--input-type"
  "csv"
  "--output-name"
  "raw-data"
  "--output-type"
  "topic"
  "--error-chance"
  "0.05"
)
runFakeApp "ExampleAdapter" "${COMMON_ARGS[@]}" "${ADAPTER_ARGS[@]}"

MAPPER_ARGS=(
  "--app-id"
  "ExampleCleaner"
  "--app-name"
  "Data Cleaner"
  "--input-name"
  "raw-data"
  "--input-type"
  "topic"
  "--output-name"
  "clean-data"
  "--output-type"
  "topic"
  "--error-chance"
  "0.25"
  "--error-interval"
  "30"
)
runFakeApp "ExampleCleaner" "${COMMON_ARGS[@]}" "${MAPPER_ARGS[@]}"

MAPPER_ARGS=(
  "--app-id"
  "KnowledgeGenerator"
  "--app-name"
  "Knowledge Generation"
  "--input-name"
  "clean-data"
  "--input-type"
  "topic"
  "--output-name"
  "knowledge"
  "--output-type"
  "topic"
  "--error-chance"
  "0.0"
)
runFakeApp "KnowledgeGenerator" "${COMMON_ARGS[@]}" "${MAPPER_ARGS[@]}"

PROJECTOR_ARGS=(
  "--app-id"
  "ExampleProjector"
  "--app-name"
  "Super Awesome Projector"
  "--component-type"
  "projector"
  "--input-name"
  "knowledge"
  "--input-type"
  "topic"
  "--output-name"
  "my-database"
  "--output-type"
  "secureagent"
  "--error-chance"
  "0.4"
  "--error-interval"
  "10"
)
runFakeApp "ExampleProjector" "${COMMON_ARGS[@]}" "${PROJECTOR_ARGS[@]}"

echo ""
echo "${#FAKE_APP_PIDS[@]} fake applications are running"
echo "Waiting for jobs to run, interrupt this process to abort..."
wait $(jobs -p)

# If things run successfully we'll never get here because a SIGINT/SIGTERM is trapped and results in an exit.  However,
# if things aren't working the wait will fall through so dump the tail of each of the logs before exiting
echo "Fake Applications unexpectedly failed to start:"
for FAKE_APP_LOG in "${FAKE_APP_LOGS[@]}"; do
  echo "$(basename ${FAKE_APP_LOG}) reported following in its logs:"
  tail "${FAKE_APP_LOG}"
  echo ""
done

