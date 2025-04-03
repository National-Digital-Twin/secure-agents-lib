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


# Source our common setup to detect the image to use
SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")
SCRIPT_DIR=$(cd "${SCRIPT_DIR}" && pwd)
source "${SCRIPT_DIR}/common.shrc"

CAPTURE_DIR=$1
CAPTURE_FORMAT=${2:-yaml}

if [ -z "${CAPTURE_DIR}" ]; then
  echo "No capture directory supplied"
  exit 1
fi
mkdir -p "${CAPTURE_DIR}"
if [ $? -ne 0 ]; then
  echo "Failed to create requested capture directory ${CAPTURE_DIR}"
  exit 2
fi

echorun docker run -i --rm -v ${CAPTURE_DIR}:/capture \
       ${IMAGE} capture \
       --bootstrap-servers ${BOOTSTRAP_SERVERS} --topic ${TOPIC} \
       --max-stalls 1 --poll-timeout 5 \
       --read-policy BEGINNING --capture-directory /capture \
       --capture-format "${CAPTURE_FORMAT}"