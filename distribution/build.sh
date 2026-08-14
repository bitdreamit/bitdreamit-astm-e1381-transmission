#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Build script for bitdreamit-astm-e1381-transmission (production-ready)
# -----------------------------------------------------------------------------
# Produces three jars that match the names referenced in transmissionmode.xml:
#   out/bitdreamit-astm-e1381-transmission-shared.jar
#   out/bitdreamit-astm-e1381-transmission-server.jar
#   out/bitdreamit-astm-e1381-transmission-client.jar
#
# Requirements:
#   - JDK 8+ (tested with OpenJDK 17)
#   - Mirth Connect 4.5+ jars unpacked at ../mirth-libs/{server,client,test}/
#
# Usage:
#   cd distribution && ./build.sh            # build all jars
#   cd distribution && ./build.sh clean      # remove out/ folder
#   cd distribution && ./build.sh test      # build + run JUnit tests
# -----------------------------------------------------------------------------
set -e

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$PROJECT_DIR/out"

# --- Resolve Mirth libraries (layout documented in README) ------------------
MIRTH_LIBS_DIR="${MIRTH_LIBS_DIR:-$PROJECT_DIR/../mirth-libs}"
SERVER_LIB="$MIRTH_LIBS_DIR/server"
CLIENT_LIB="$MIRTH_LIBS_DIR/client"
TEST_LIB="$MIRTH_LIBS_DIR/test"

# Shared (model classes) - on both SERVER and CLIENT classpaths
SHARED_MODEL_JAR="$CLIENT_LIB/mirth-client-core.jar"

# Server-side classpath
SERVER_CP="$SERVER_LIB/mirth-server.jar"
SERVER_CP="$SERVER_CP:$SERVER_LIB/donkey-server.jar"
SERVER_CP="$SERVER_CP:$SHARED_MODEL_JAR"

# Client-side classpath
CLIENT_CP="$CLIENT_LIB/mirth-client.jar"
CLIENT_CP="$CLIENT_CP:$SHARED_MODEL_JAR"

# Test classpath (junit + hamcrest + server-side for testing Frame etc.)
TEST_CP="$SERVER_CP:$TEST_LIB/junit-4.13.2.jar"
TEST_CP="$TEST_CP:$TEST_LIB/hamcrest-core-1.3.jar"

# --- Helpers ----------------------------------------------------------------
clean() {
    echo "[clean] removing $OUT_DIR"
    rm -rf "$OUT_DIR"
}

build() {
    echo "[build] project dir: $PROJECT_DIR"
    echo "[build] mirth libs: $MIRTH_LIBS_DIR"
    mkdir -p "$OUT_DIR/shared" "$OUT_DIR/server" "$OUT_DIR/client" "$OUT_DIR/test"

    # 1. Compile shared module (needs Mirth model classes for TransmissionModeProperties)
    echo "[build] compiling shared..."
    javac -d "$OUT_DIR/shared" \
        -cp "$SHARED_MODEL_JAR" \
        -sourcepath "$PROJECT_DIR/shared/src" \
        $(find "$PROJECT_DIR/shared/src" -name "*.java")

    # 2. Compile server module (needs shared + server classpath)
    echo "[build] compiling server..."
    javac -cp "$OUT_DIR/shared:$SERVER_CP" \
        -d "$OUT_DIR/server" \
        -sourcepath "$PROJECT_DIR/server/src" \
        $(find "$PROJECT_DIR/server/src" -name "*.java")

    # 3. Compile client module (needs shared + client classpath)
    echo "[build] compiling client..."
    javac -cp "$OUT_DIR/shared:$CLIENT_CP" \
        -d "$OUT_DIR/client" \
        -sourcepath "$PROJECT_DIR/client/src" \
        $(find "$PROJECT_DIR/client/src" -name "*.java")

    # 4. Compile tests (needs shared + server + junit)
    echo "[build] compiling tests..."
    javac -cp "$OUT_DIR/shared:$OUT_DIR/server:$TEST_CP" \
        -d "$OUT_DIR/test" \
        -sourcepath "$PROJECT_DIR/test/src" \
        $(find "$PROJECT_DIR/test/src" -name "*.java")

    # 5. Package shared jar (Constants + Properties + Frame + FrameException + RetryMetrics)
    echo "[build] packaging shared jar..."
    jar cf "$OUT_DIR/bitdreamit-astm-e1381-transmission-shared.jar" \
        -C "$OUT_DIR/shared" .

    # 6. Package server jar (shared classes + server classes + server resources)
    echo "[build] packaging server jar..."
    jar cf "$OUT_DIR/bitdreamit-astm-e1381-transmission-server.jar" \
        -C "$OUT_DIR/shared" . \
        -C "$OUT_DIR/server" . \
        -C "$PROJECT_DIR/server/resources" .

    # 7. Package client jar (shared classes + client classes + client resources)
    echo "[build] packaging client jar..."
    jar cf "$OUT_DIR/bitdreamit-astm-e1381-transmission-client.jar" \
        -C "$OUT_DIR/shared" . \
        -C "$OUT_DIR/client" . \
        -C "$PROJECT_DIR/client/resources" .

    echo ""
    echo "[build] complete. Artifacts:"
    ls -la "$OUT_DIR"/*.jar
}

run_tests() {
    build
    echo "[test] running JUnit..."
    java -cp "$OUT_DIR/shared:$OUT_DIR/server:$OUT_DIR/test:$TEST_CP" \
        org.junit.runner.JUnitCore \
        com.bitdreamit.connect.plugins.transmission.astm.test.ASTME1381FrameTest \
        com.bitdreamit.connect.plugins.transmission.astm.test.ASTME1381RetryMetricsTest \
        com.bitdreamit.connect.plugins.transmission.astm.test.ASTME1381FrameCorruptionTest \
        com.bitdreamit.connect.plugins.transmission.astm.test.ASTME1381RoundTripTest
}

# --- Dispatch ---------------------------------------------------------------
case "${1:-build}" in
    clean)    clean ;;
    build)    build ;;
    test)     run_tests ;;
    rebuild)  clean && build ;;
    *) echo "Usage: $0 {clean|build|test|rebuild}"; exit 1 ;;
esac
