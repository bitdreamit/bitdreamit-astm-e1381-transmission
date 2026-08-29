#!/usr/bin/env bash
# =============================================================================
# rebuild_from_real_mirth.sh
# =============================================================================
# Rebuilds the ASTM E1381 extension jars against the REAL Mirth Connect jars
# from your Mirth installation. This fixes the ForbiddenClassException
# caused by the pre-built jars being compiled against stub Mirth classes.
#
# PREREQUISITES:
#   - JDK 8+ installed (check: java -version)
#   - Mirth Connect 4.5.x installed
#
# USAGE:
#   sudo MIRTH_HOME=/opt/mirth-connect bash rebuild_from_real_mirth.sh
#
#   Then deploy the rebuilt jars:
#   sudo cp /tmp/astm-rebuilt/*.jar /opt/mirth-connect/extensions/bitdreamit-astm-e1381-transmission/
#   sudo systemctl restart mirth-connect
# =============================================================================
set -e

MIRTH_HOME="${MIRTH_HOME:-/opt/mirth-connect}"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="/tmp/astm-rebuilt"

if [ ! -d "$MIRTH_HOME" ]; then
    echo "ERROR: MIRTH_HOME not found at $MIRTH_HOME"
    echo "Set it: sudo MIRTH_HOME=/opt/mirth-connect bash $0"
    exit 1
fi

echo "============================================================"
echo "Rebuilding ASTM E1381 extension against REAL Mirth jars"
echo "============================================================"
echo "Mirth home:  $MIRTH_HOME"
echo "Project dir: $PROJECT_DIR"
echo "Output dir:  $OUT_DIR"
echo ""

# --- Step 1: Locate the REAL Mirth jars ---
echo "[1/6] Locating real Mirth jars..."

MIRTH_LIBS_DIR="/tmp/astm-mirth-libs"
rm -rf "$MIRTH_LIBS_DIR"
mkdir -p "$MIRTH_LIBS_DIR/server" "$MIRTH_LIBS_DIR/client" "$MIRTH_LIBS_DIR/test"

# Find Mirth jars - they could be in $MIRTH_HOME/lib/ or $MIRTH_HOME/lib/extensions/
find_mirth_jar() {
    local name="$1"
    local found
    found=$(find "$MIRTH_HOME" -name "$name" -type f 2>/dev/null | head -1)
    echo "$found"
}

# Server jars
SERVER_JARS=""
for jar_name in mirth-server.jar donkey-server.jar mirth-client-core.jar; do
    found=$(find "$MIRTH_HOME" -name "$jar_name" -type f 2>/dev/null | head -1)
    if [ -n "$found" ]; then
        cp "$found" "$MIRTH_LIBS_DIR/server/"
        SERVER_JARS="$SERVER_JARS:$MIRTH_LIBS_DIR/server/$jar_name"
        echo "  Found: $jar_name -> $found"
    else
        echo "  WARN: $jar_name not found in $MIRTH_HOME"
    fi
done

# Client jars
CLIENT_JARS=""
for jar_name in mirth-client.jar mirth-client-core.jar; do
    found=$(find "$MIRTH_HOME" -name "$jar_name" -type f 2>/dev/null | head -1)
    if [ -n "$found" ]; then
        cp "$found" "$MIRTH_LIBS_DIR/client/"
        CLIENT_JARS="$CLIENT_JARS:$MIRTH_LIBS_DIR/client/$jar_name"
        echo "  Found: $jar_name -> $found"
    else
        echo "  WARN: $jar_name not found in $MIRTH_HOME"
    fi
done

# Find log4j
LOG4J_JAR=$(find "$MIRTH_HOME" -name "log4j-1.2-api*.jar" -type f 2>/dev/null | head -1)
if [ -z "$LOG4J_JAR" ]; then
    LOG4J_JAR=$(find "$MIRTH_HOME" -name "log4j*.jar" -type f 2>/dev/null | head -1)
fi
if [ -n "$LOG4J_JAR" ]; then
    cp "$LOG4J_JAR" "$MIRTH_LIBS_DIR/server/"
    SERVER_JARS="$SERVER_JARS:$LOG4J_JAR"
    echo "  Found: log4j -> $LOG4J_JAR"
else
    echo "  WARN: log4j jar not found"
fi

# Find miglayout (for client UI)
MIG_CORE=$(find "$MIRTH_HOME" -name "miglayout-core*.jar" -type f 2>/dev/null | head -1)
MIG_SWING=$(find "$MIRTH_HOME" -name "miglayout-swing*.jar" -type f 2>/dev/null | head -1)
if [ -n "$MIG_CORE" ]; then
    cp "$MIG_CORE" "$MIRTH_LIBS_DIR/client/"
    CLIENT_JARS="$CLIENT_JARS:$MIG_CORE"
    echo "  Found: miglayout-core -> $MIG_CORE"
fi
if [ -n "$MIG_SWING" ]; then
    cp "$MIG_SWING" "$MIRTH_LIBS_DIR/client/"
    CLIENT_JARS="$CLIENT_JARS:$MIG_SWING"
    echo "  Found: miglayout-swing -> $MIG_SWING"
fi

echo ""
echo "  Server classpath: $SERVER_JARS"
echo "  Client classpath: $CLIENT_JARS"

# --- Step 2: Clean and prepare output ---
echo ""
echo "[2/6] Cleaning output directory..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR/shared" "$OUT_DIR/server" "$OUT_DIR/client"

# --- Step 3: Compile shared module ---
echo ""
echo "[3/6] Compiling shared module (Constants + Properties + Frame + ...)..."
SHARED_CP="$MIRTH_LIBS_DIR/client/mirth-client-core.jar:$MIRTH_LIBS_DIR/server/donkey-server.jar"
javac -d "$OUT_DIR/shared" \
    -cp "$SHARED_CP" \
    -sourcepath "$PROJECT_DIR/shared/src" \
    $(find "$PROJECT_DIR/shared/src" -name "*.java") 2>&1
echo "  Shared: $(find "$OUT_DIR/shared" -name '*.class' | wc -l) classes"

# --- Step 4: Compile server module ---
echo ""
echo "[4/6] Compiling server module (Plugin + StreamHandler)..."
javac -cp "$OUT_DIR/shared:$SERVER_JARS" \
    -d "$OUT_DIR/server" \
    -sourcepath "$PROJECT_DIR/server/src" \
    $(find "$PROJECT_DIR/server/src" -name "*.java") 2>&1
echo "  Server: $(find "$OUT_DIR/server" -name '*.class' | wc -l) classes"

# --- Step 5: Compile client module ---
echo ""
echo "[5/6] Compiling client module (ClientPlugin + Provider + SettingsPanel + SettingsDialog)..."
javac -cp "$OUT_DIR/shared:$CLIENT_JARS" \
    -d "$OUT_DIR/client" \
    -sourcepath "$PROJECT_DIR/client/src" \
    $(find "$PROJECT_DIR/client/src" -name "*.java") 2>&1
echo "  Client: $(find "$OUT_DIR/client" -name '*.class' | wc -l) classes"

# --- Step 6: Package jars ---
echo ""
echo "[6/6] Packaging jars..."

# Shared jar
jar cf "$OUT_DIR/bitdreamit-astm-e1381-transmission-shared.jar" \
    -C "$OUT_DIR/shared" .

# Server jar (shared + server classes)
mkdir -p "$OUT_DIR/server-jar"
cp -r "$OUT_DIR/shared/." "$OUT_DIR/server-jar/"
cp -r "$OUT_DIR/server/." "$OUT_DIR/server-jar/"
jar cf "$OUT_DIR/bitdreamit-astm-e1381-transmission-server.jar" \
    -C "$OUT_DIR/server-jar" .

# Client jar (shared + client classes)
mkdir -p "$OUT_DIR/client-jar"
cp -r "$OUT_DIR/shared/." "$OUT_DIR/client-jar/"
cp -r "$OUT_DIR/client/." "$OUT_DIR/client-jar/"
jar cf "$OUT_DIR/bitdreamit-astm-e1381-transmission-client.jar" \
    -C "$OUT_DIR/client-jar" .

# Copy XML files
cp "$PROJECT_DIR/plugin.xml" "$OUT_DIR/"
cp "$PROJECT_DIR/transmissionmode.xml" "$OUT_DIR/"

echo ""
echo "============================================================"
echo "BUILD COMPLETE - jars compiled against REAL Mirth jars"
echo "============================================================"
echo ""
echo "Output files in $OUT_DIR:"
ls -la "$OUT_DIR"/*.jar "$OUT_DIR"/*.xml
echo ""
echo "DEPLOY:"
echo "  EXT_DIR=$MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission"
echo "  sudo systemctl stop mirth-connect"
echo "  sudo cp $OUT_DIR/*.jar $OUT_DIR/*.xml \$EXT_DIR/"
echo "  sudo rm -rf $MIRTH_HOME/appdata/.mirth $MIRTH_HOME/appdata/.tmp"
echo "  sudo systemctl start mirth-connect"
echo ""
echo "Then check the log:"
echo "  sudo grep -i 'ASTM\|error\|exception' $MIRTH_HOME/logs/mirth.log | tail -20"
