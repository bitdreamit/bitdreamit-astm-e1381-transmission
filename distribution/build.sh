#!/bin/bash
# Build script for bitdreamit-astm-e1381-transmission (IntelliJ IDEA module style)
# Requires: JDK 8, jar command
# Mirth 4.5.2 JARs expected at ../mirth-libs/

set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$PROJECT_DIR/out"
mkdir -p "$OUT_DIR"

# Build classpath from mirth-libs
SERVER_CP="$PROJECT_DIR/../mirth-libs/server/mirth-server.jar"
SERVER_CP="$SERVER_CP:$PROJECT_DIR/../mirth-libs/server/donkey-server.jar"
SERVER_CP="$SERVER_CP:$PROJECT_DIR/../mirth-libs/server/mirth-client-core.jar"

CLIENT_CP="$PROJECT_DIR/../mirth-libs/client/mirth-client.jar"
CLIENT_CP="$CLIENT_CP:$PROJECT_DIR/../mirth-libs/client/mirth-client-core.jar"

TEST_CP="$SERVER_CP:$PROJECT_DIR/../mirth-libs/test/junit-4.13.2.jar"
TEST_CP="$TEST_CP:$PROJECT_DIR/../mirth-libs/test/hamcrest-core-1.3.jar"

# Compile shared
mkdir -p "$OUT_DIR/shared"
javac -d "$OUT_DIR/shared" -sourcepath "$PROJECT_DIR/shared/src" \
    $(find "$PROJECT_DIR/shared/src" -name "*.java")

# Compile server
mkdir -p "$OUT_DIR/server"
javac -cp "$OUT_DIR/shared:$SERVER_CP" \
    -d "$OUT_DIR/server" -sourcepath "$PROJECT_DIR/server/src" \
    $(find "$PROJECT_DIR/server/src" -name "*.java")

# Compile client
mkdir -p "$OUT_DIR/client"
javac -cp "$OUT_DIR/shared:$CLIENT_CP" \
    -d "$OUT_DIR/client" -sourcepath "$PROJECT_DIR/client/src" \
    $(find "$PROJECT_DIR/client/src" -name "*.java")

# Compile tests
mkdir -p "$OUT_DIR/test"
javac -cp "$OUT_DIR/shared:$OUT_DIR/server:$TEST_CP" \
    -d "$OUT_DIR/test" -sourcepath "$PROJECT_DIR/test/src" \
    $(find "$PROJECT_DIR/test/src" -name "*.java")

# Package server jar
jar cf "$OUT_DIR/bitdreamit-astm-e1381-transmission-server.jar" \
    -C "$OUT_DIR/shared" . \
    -C "$OUT_DIR/server" . \
    -C "$PROJECT_DIR/server/resources" .

# Package client jar
jar cf "$OUT_DIR/bitdreamit-astm-e1381-transmission-client.jar" \
    -C "$OUT_DIR/shared" . \
    -C "$OUT_DIR/client" . \
    -C "$PROJECT_DIR/client/resources" .

echo "Build complete:"
echo "  $OUT_DIR/bitdreamit-astm-e1381-transmission-server.jar"
echo "  $OUT_DIR/bitdreamit-astm-e1381-transmission-client.jar"
echo "  Tests compiled in $OUT_DIR/test"
