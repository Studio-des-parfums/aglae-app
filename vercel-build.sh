#!/usr/bin/env bash
set -euo pipefail

echo "Installation de Temurin 17..."
curl -sL "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" -o /tmp/jdk17.tar.gz
mkdir -p /tmp/jdk17
tar -xzf /tmp/jdk17.tar.gz -C /tmp/jdk17 --strip-components=1
export JAVA_HOME=/tmp/jdk17
export PATH="$JAVA_HOME/bin:$PATH"

java -version

./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon
