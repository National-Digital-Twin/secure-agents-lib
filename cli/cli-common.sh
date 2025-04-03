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

# Script that should be invoked by individual command scripts to actually run one of the commands
# Expects that the caller exports the JAR_NAME and CLASS_NAME environment variables to indicate the
# necessary JAR File, and class within that JAR to invoke
# It also expects that the first argument passed to it indicates the directory containing the calling
# script.  The lib/ directory containing the dependencies for the CLI is expected to be located under that
# directory

function error() {
  echo "$@" 1>&2
}

function abort() {
  echo "$@" 1>&2
  exit 255
}

SCRIPT_DIR=$1
shift
if [ -z "${SCRIPT_DIR}" ] || [ ! -d "${SCRIPT_DIR}" ]; then
  abort "Invoking script failed to pass a directory as first argument"
elif [ -z "${JAR_NAME}" ]; then
  abort "Invoking script failed to set JAR_NAME environment variable"
elif [ -z "${CLASS_NAME}" ]; then
  abort "Invoking script failed to set CLASS_NAME environment variable"
fi

# Search for the JAR file in common locations, there's basically two we care about:
#
#   1) The lib/ directory under the SCRIPT_DIR i.e. a production deployment
#   2) The target/ directory under the SCRIPT_DIR i.e. a developers local development environment
#
CLASSPATH="${SCRIPT_DIR}/lib/*"
if [ -f "${SCRIPT_DIR}/lib/${JAR_NAME}" ]; then
  # No need to modify Classpath since our desired JAR exists in the lib/ directory and thus is already
  # on the CLASSPATH
  CLASSPATH="${CLASSPATH}"
elif [ -f "${SCRIPT_DIR}/target/${JAR_NAME}" ]; then
  CLASSPATH="${SCRIPT_DIR}/target/${JAR_NAME}:${CLASSPATH}"
else
  error "Failed to locate required JAR file ${JAR_NAME} in any of the expected locations"
  if [ -f "${SCRIPT_DIR}/pom.xml" ]; then
    error "You may need to do a mvn package first"
  fi
  exit 255
fi

# Detect whether any Java Agents should be used
for AGENT in $(ls ${SCRIPT_DIR}/agents/*.jar); do
  JAVA_OPTIONS="${JAVA_OPTIONS} -javaagent:${AGENT}"
done

# shellcheck disable=SC2086
exec java ${JAVA_OPTIONS} -cp "${CLASSPATH}" ${CLASS_NAME} "$@"
