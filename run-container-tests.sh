#!/bin/bash
# Script to run container tests with proper Docker configuration for WSL

# Set Docker environment
export DOCKER_HOST=unix:///var/run/docker.sock
export TESTCONTAINERS_RYUK_DISABLED=true
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock

# Verify Docker is accessible
echo "Checking Docker connectivity..."
if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker is not accessible. Please start Docker daemon."
    exit 1
fi
echo "Docker is accessible!"

# Run the container tests
cd /mnt/c/Users/bwend/repos/ops-scribe
mvn failsafe:integration-test -Pe2e-containers -Dit.test=OracleVectorStoreContainerIT --batch-mode
