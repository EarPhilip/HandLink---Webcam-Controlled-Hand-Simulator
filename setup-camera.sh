#!/bin/sh
set -eu

mkdir -p lib

curl --fail --location --retry 2 \
  "https://repo.maven.apache.org/maven2/org/openpnp/opencv/4.9.0-0/opencv-4.9.0-0.jar" \
  --output "lib/opencv-4.9.0-0.jar"
