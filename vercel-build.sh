#!/usr/bin/env bash
set -euo pipefail

if ! command -v java >/dev/null 2>&1; then
  echo "Java introuvable, installation de Temurin 17..."
  curl -sL "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" -o /tmp/jdk17.tar.gz
  mkdir -p /tmp/jdk17
  tar -xzf /tmp/jdk17.tar.gz -C /tmp/jdk17 --strip-components=1
  export JAVA_HOME=/tmp/jdk17
  export PATH="$JAVA_HOME/bin:$PATH"
fi

java -version

./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon
