#!/bin/bash
# Run gateway tests in Docker
cd /build
./gradlew :gateway:test --no-daemon 2>&1
