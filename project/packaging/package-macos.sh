#!/usr/bin/env bash
set -euo pipefail

SERVER_HOST="${1:-34.126.166.158}"
SERVER_PORT="${2:-8080}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
RELEASE_DIR="$PROJECT_DIR/release/macos"
INPUT_DIR="$PROJECT_DIR/target"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "This script must be run on macOS." >&2
  exit 1
fi

case "$(uname -m)" in
  arm64) JAVAFX_PLATFORM="mac-aarch64" ;;
  x86_64) JAVAFX_PLATFORM="mac" ;;
  *) echo "Unsupported macOS architecture: $(uname -m)" >&2; exit 1 ;;
esac

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/jpackage" ]]; then
  JPACKAGE="$JAVA_HOME/bin/jpackage"
else
  JPACKAGE="$(command -v jpackage)"
fi

cd "$PROJECT_DIR"
mvn --batch-mode -DskipTests "-Djavafx.platform=$JAVAFX_PLATFORM" package

rm -rf "$RELEASE_DIR/client" "$RELEASE_DIR/server"
mkdir -p "$RELEASE_DIR/client" "$RELEASE_DIR/server"

"$JPACKAGE" \
  --type app-image \
  --name AuctionClientNhom3 \
  --dest "$RELEASE_DIR/client" \
  --input "$INPUT_DIR" \
  --main-jar client-app.jar \
  --main-class com.nhom3.client.Main \
  --app-version 1.0.0 \
  --vendor "Nhom 3" \
  --java-options "-Dauction.server.host=$SERVER_HOST" \
  --java-options "-Dauction.server.port=$SERVER_PORT"

"$JPACKAGE" \
  --type app-image \
  --name AuctionServerNhom3 \
  --dest "$RELEASE_DIR/server" \
  --input "$INPUT_DIR" \
  --main-jar server-app.jar \
  --main-class com.nhom3.server.ServerMain \
  --app-version 1.0.0 \
  --vendor "Nhom 3" \
  --java-options "-Dauction.server.port=$SERVER_PORT"

cd "$RELEASE_DIR/client"
ditto -c -k --sequesterRsrc --keepParent AuctionClientNhom3.app ../AuctionClientNhom3-macos.zip

cd "$RELEASE_DIR/server"
ditto -c -k --sequesterRsrc --keepParent AuctionServerNhom3.app ../AuctionServerNhom3-macos.zip

echo "Created:"
echo "$RELEASE_DIR/AuctionClientNhom3-macos.zip"
echo "$RELEASE_DIR/AuctionServerNhom3-macos.zip"
