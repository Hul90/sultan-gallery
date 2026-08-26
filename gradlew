#!/usr/bin/env sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROP_FILE="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

get_prop() {
  key="$1"
  sed -n "s/^${key}=//p" "$PROP_FILE" | head -n 1 | sed 's/\\:/\:/g'
}

DIST_URL=$(get_prop distributionUrl)
DIST_URL=$(printf '%s' "$DIST_URL" | sed 's/\\:/:/g')
DIST_NAME=$(printf '%s' "$DIST_URL" | sed 's#.*/##')
DIST_VERSION=$(printf '%s' "$DIST_NAME" | sed 's/^gradle-//; s/-bin\.zip$//; s/-all\.zip$//')

GRADLE_USER_HOME=${GRADLE_USER_HOME:-"$HOME/.gradle"}
INSTALL_DIR="$GRADLE_USER_HOME/wrapper/dists/sultan-gallery-gradle/$DIST_VERSION"
GRADLE_BIN="$INSTALL_DIR/gradle-$DIST_VERSION/bin/gradle"
ZIP_FILE="$INSTALL_DIR/$DIST_NAME"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$INSTALL_DIR"
  if [ ! -f "$ZIP_FILE" ]; then
    echo "Downloading Gradle $DIST_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 --connect-timeout 20 -o "$ZIP_FILE" "$DIST_URL"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP_FILE" "$DIST_URL"
    else
      echo "ERROR: curl or wget is required to download Gradle." >&2
      exit 1
    fi
  fi

  TMP_DIR="$INSTALL_DIR/.extracting"
  rm -rf "$TMP_DIR"
  mkdir -p "$TMP_DIR"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP_FILE" -d "$TMP_DIR"
  elif command -v python3 >/dev/null 2>&1; then
    python3 - "$ZIP_FILE" "$TMP_DIR" <<'PY'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
    z.extractall(sys.argv[2])
PY
  else
    echo "ERROR: unzip or python3 is required to extract Gradle." >&2
    exit 1
  fi
  rm -rf "$INSTALL_DIR/gradle-$DIST_VERSION"
  mv "$TMP_DIR/gradle-$DIST_VERSION" "$INSTALL_DIR/gradle-$DIST_VERSION"
  rm -rf "$TMP_DIR"
  chmod +x "$GRADLE_BIN"
fi

exec "$GRADLE_BIN" "$@"
