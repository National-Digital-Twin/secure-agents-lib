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


# Simple script that extracts the release notes from CHANGELOG.md
#
# Usage is ./extractReleaseNotes.sh <release> > release-notes.txt
#
# Basically looks for a line of the form # <release> and then outputs
# that plus all subsequent lines until the next header line (denoted)
# by starting with a #
#
# Exits 0 if some release notes were output, 1 if no matching release
# was found

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")
SCRIPT_DIR=$(cd "${SCRIPT_DIR}" && pwd)

RELEASE=$1
if [ -z "${RELEASE}" ]; then
  echo "No Release version specified, aborting" 1>&2
  exit 1
fi

STARTED=0
while IFS= read -r LINE; do
  if [ ${STARTED} -eq 1 ]; then
    if [[ "${LINE}" == \#* ]]; then
      exit 0
    fi
    echo "${LINE}" 
  elif [[ "${LINE}" == \#*${RELEASE} ]]; then
    STARTED=1
    echo "# Version ${RELEASE}"
  fi
done < "${SCRIPT_DIR}/CHANGELOG.md"

if [ ${STARTED} -eq 0 ]; then
  echo "Release ${RELEASE} not found, aborting" 1>&2
  exit 1
fi
exit 0