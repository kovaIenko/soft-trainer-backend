#!/bin/bash

set -e

source .env

# echo "--- Launching tests ---"
# gradle clean test

echo "--- Building JAR ---"
gradle clean bootJar -x test

echo "--- Launch Docker daemon ---"
open -a Docker

PROJECT_VERSION=$(gradle properties --no-daemon --console=plain -q | grep "^version:" | awk '{printf $2}')

echo "--- Building Docker Image ---"
aws ecr get-login-password --region "${AWS_REGION}" | docker login --username AWS --password-stdin "${AWS_ECR_ENDPOINT}"
docker build . -t "softtrainer-backend:${PROJECT_VERSION}"
docker tag "softtrainer-backend:${PROJECT_VERSION}" "${AWS_ECR_ENDPOINT}/softtrainer-backend:${PROJECT_VERSION}"
docker tag "softtrainer-backend:${PROJECT_VERSION}" "${AWS_ECR_ENDPOINT}/softtrainer-backend:latest"

echo "--- Pushing Docker Image ---"
docker push "${AWS_ECR_ENDPOINT}/softtrainer-backend:${PROJECT_VERSION}"
docker push "${AWS_ECR_ENDPOINT}/softtrainer-backend:latest"
