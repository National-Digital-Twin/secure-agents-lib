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

function error() {
  echo "$@" 1>&2
}

function abort() {
  echo "$@" 1>&2
  exit 255
}

function echorun() {
  echo "$@"
  "$@"
}

function detectBranch() {
  if [ -n "${BRANCH}" ]; then
    echo "${BRANCH}"
  else
    local CURRENT_BRANCH=$(git branch --show-current 2>/dev/null)
    if [ -z "${CURRENT_BRANCH}" ]; then
      CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null)
    fi
    echo "${CURRENT_BRANCH}"
  fi
}

function selectTag() {
  local USER_TAG=$1
  if [ -n "${USER_TAG}" ]; then
    echo "${USER_TAG//\//-}"
  elif [ "${BRANCH}" == "main" ]; then
    echo "latest"
  else
    echo "${BRANCH//\//-}"
  fi
}

command -v docker >/dev/null 2>&1 || abort "This script requires the docker command on your PATH"

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")
SCRIPT_DIR=$(cd "${SCRIPT_DIR}" && pwd)

BRANCH=$(detectBranch)
DOCKER_TAG=$(selectTag "$1")
DOCKER_REPO=$2

echo "Docker Tag is ${DOCKER_TAG}"
if [ -n "${DOCKER_REPO}" ]; then
  echo "Docker Registry is ${DOCKER_REPO}, images will be pushed to this repository"
else
  echo "No Docker Registry defined, images will be built locally only"
fi
if [ -n "${BRANCH}" ]; then
  echo "Current Branch is ${BRANCH}"
fi
PROJECT_VERSION=
if command -v mvn >/dev/null 2>&1; then
  PROJECT_VERSION=$(cd "${SCRIPT_DIR}" && mvn help:evaluate --batch-mode -Dexpression=project.version 2>/dev/null | grep -v "\[")
else
  PROJECT_VERSION=$(grep "<version>" "${SCRIPT_DIR}/pom.xml" 2>/dev/null | head -n 1 | awk -F "[><]" '{print $3}')
fi
if [ -z "${PROJECT_VERSION}" ]; then
  abort "Failed to detect Project Version"
fi
echo "Detected Project Version is ${PROJECT_VERSION}"
echo ""

# Need to enable Docker Buildkit
export DOCKER_BUILDKIT=1

function buildImage() {
  local IMAGE_NAME="$1"
  local BUILD_TARGET="$1"
  if [ -n "${DOCKER_REPO}" ]; then
    IMAGE_NAME="${DOCKER_REPO}/${IMAGE_NAME}"
  fi
  shift 1

  local DOCKER_ARGS=(
    "docker"
  )
  # If TARGET_PLATFORMS is set add the --platform flag so we get a multi-platform image build.  This of course assumes
  # that docker buildx is available as plain docker will not support this
  if [ -n "${TARGET_PLATFORMS}" ]; then
    DOCKER_ARGS+=(
      "buildx"
      "build"
      "--platform"
      "${TARGET_PLATFORMS}"
    )
    # If a Docker repository is specified add the --push argument so the resulting multi-platform manifest and all the
    # image layers get pushed accordingly
    if [ -n "${DOCKER_REPO}" ]; then
      DOCKER_ARGS+=("--push")

      # If this is the main branch automatically apply the latest tag here since we won't be calling the separate
      # pushImage() function as we're relying on buildx to push everything
      if [ "${DOCKER_TAG}" != "latest" ] && [ "${BRANCH}" == "main" ]; then
        DOCKER_ARGS+=(
          "-t"
          "${IMAGE_NAME}:latest"
        )
      fi
    fi
  else
    DOCKER_ARGS+=("build")
  fi

  # Set the Tag and Dockerfile plus inject the PROJECT_VERSION as a build argument
  DOCKER_ARGS+=(
    "--target"
    "${BUILD_TARGET}"
    "-t"
    "${IMAGE_NAME}:${DOCKER_TAG}"
    "-f"
    "${SCRIPT_DIR}/docker/Dockerfile"
    "--build-arg"
    "PROJECT_VERSION=${PROJECT_VERSION}"
  )

  if [ $# -gt 0 ]; then
    DOCKER_ARGS+=("$@")
  fi
  if [ -n "${EXTRA_BUILD_ARGS}" ]; then
    DOCKER_ARGS+=( ${EXTRA_BUILD_ARGS} )
  fi
  DOCKER_ARGS+=(
    "${SCRIPT_DIR}"
  )

  echo "Building Docker Image ${IMAGE_NAME}:${DOCKER_TAG}..."
  # shellcheck disable=SC2015
  echorun "${DOCKER_ARGS[@]}" || abort "Docker Build failed"
}

function pushImage() {
  local IMAGE_NAME=$1
  if [ -n "${DOCKER_REPO}" ]; then
    IMAGE_NAME="${DOCKER_REPO}/${IMAGE_NAME}"
    echo "Pushing image ${IMAGE_NAME}:${DOCKER_TAG}..."
    echorun docker push "${IMAGE_NAME}:${DOCKER_TAG}" || abort "Docker push failed"
    if [ "${DOCKER_TAG}" != "latest" ] && [ "${BRANCH}" == "main" ]; then
      echorun docker tag "${IMAGE_NAME}:${DOCKER_TAG}" "${IMAGE_NAME}:latest" || abort "Docker tag failed"
      echorun docker push "${IMAGE_NAME}:latest" || abort "Docker push failed"
    fi
    echo ""
  fi
}

function buildAndPushImage() {
  buildImage "$@"
  if [ -z "${TARGET_PLATFORMS}" ]; then
    # NB - When TARGET_PLATFORMS is set and we're doing a multi-platform build we add the --push argument to the docker
    #      build command instead which automatically pushes the image manifest and layers to the Docker repository
    # Therefore only need an explicit push when doing a single platform build i.e. local developer build
    pushImage "$@"
  fi
}

buildAndPushImage "secure-agent-debug-tools"
