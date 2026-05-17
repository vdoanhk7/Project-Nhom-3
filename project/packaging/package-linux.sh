#!/usr/bin/env bash
set -euo pipefail

SERVER_HOST="${1:-34.126.166.158}"
SERVER_PORT="${2:-8080}"
TYPE="${3:-deb}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
RELEASE_DIR="$PROJECT_DIR/release/linux"
BUILD_DIR="$PROJECT_DIR/target"
CLIENT_INPUT_DIR="$BUILD_DIR/package-input/client"
SERVER_INPUT_DIR="$BUILD_DIR/package-input/server"

if [[ "$(uname -s)" != "Linux" ]]; then
  echo "This script must be run on Linux." >&2
  exit 1
fi

if [[ "$TYPE" != "deb" && "$TYPE" != "rpm" && "$TYPE" != "app-image" ]]; then
  echo "Package type must be deb, rpm, or app-image." >&2
  exit 1
fi

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/jpackage" ]]; then
  JPACKAGE="$JAVA_HOME/bin/jpackage"
else
  JPACKAGE="$(command -v jpackage)"
fi

cd "$PROJECT_DIR"
mvn --batch-mode -DskipTests -Djavafx.platform=linux package

rm -rf "$RELEASE_DIR/client" "$RELEASE_DIR/server"
mkdir -p "$RELEASE_DIR/client" "$RELEASE_DIR/server"
rm -rf "$BUILD_DIR/package-input"
mkdir -p "$CLIENT_INPUT_DIR" "$SERVER_INPUT_DIR"
cp "$BUILD_DIR/client-app.jar" "$CLIENT_INPUT_DIR/"
cp "$BUILD_DIR/server-app.jar" "$SERVER_INPUT_DIR/"

LINUX_PACKAGE_OPTIONS=()
if [[ "$TYPE" != "app-image" ]]; then
  LINUX_PACKAGE_OPTIONS+=(--linux-app-category Utility)
fi

"$JPACKAGE" \
  --type "$TYPE" \
  --name auction-client-nhom3 \
  --dest "$RELEASE_DIR/client" \
  --input "$CLIENT_INPUT_DIR" \
  --main-jar client-app.jar \
  --main-class com.nhom3.client.Main \
  --app-version 1.0.0 \
  --vendor "Nhom 3" \
  "${LINUX_PACKAGE_OPTIONS[@]}" \
  --java-options "-Dauction.server.host=$SERVER_HOST" \
  --java-options "-Dauction.server.port=$SERVER_PORT"

"$JPACKAGE" \
  --type "$TYPE" \
  --name auction-server-nhom3 \
  --dest "$RELEASE_DIR/server" \
  --input "$SERVER_INPUT_DIR" \
  --main-jar server-app.jar \
  --main-class com.nhom3.server.ServerMain \
  --app-version 1.0.0 \
  --vendor "Nhom 3" \
  "${LINUX_PACKAGE_OPTIONS[@]}" \
  --java-options "-Dauction.server.port=$SERVER_PORT"

if [[ "$TYPE" == "app-image" ]]; then
  tar -czf "$RELEASE_DIR/AuctionClientNhom3-linux.tar.gz" -C "$RELEASE_DIR/client" auction-client-nhom3
  tar -czf "$RELEASE_DIR/AuctionServerNhom3-linux.tar.gz" -C "$RELEASE_DIR/server" auction-server-nhom3

  echo "Created:"
  echo "$RELEASE_DIR/AuctionClientNhom3-linux.tar.gz"
  echo "$RELEASE_DIR/AuctionServerNhom3-linux.tar.gz"
else
  echo "Created Linux packages in:"
  echo "$RELEASE_DIR/client"
  echo "$RELEASE_DIR/server"
fi
