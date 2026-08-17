#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# check_extension.sh - diagnostic script for the bitdreamit-astm-e1381-transmission
# Mirth Connect extension.
# -----------------------------------------------------------------------------
#
# Verifies that:
#   1. The extension folder exists at $MIRTH_HOME/extensions/<ext-name>/
#   2. All 5 production files are present (3 JARs + 2 XMLs)
#   3. The plugin.xml is in the correct Mirth 4.5.2 format (<string> + <library>)
#   4. The ASTME1381TransmissionModeProperties class is listed in BOTH
#      <serverClasses> and <clientClasses>
#   5. The class is actually present inside each JAR
#   6. The extension folder name matches the `path` attribute in plugin.xml
#
# Usage:
#   MIRTH_HOME=/opt/mirth-connect ./check_extension.sh
#   MIRTH_HOME=/opt/mirth-connect ./check_extension.sh /path/to/extension/folder
#
# -----------------------------------------------------------------------------
set -e

EXT_NAME="bitdreamit-astm-e1381-transmission"
PROPS_CLASS="com/bitdreamit/connect/plugins/transmission/astm/shared/ASTME1381TransmissionModeProperties.class"
PROPS_FQCN="com.mirth.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties"

# --- Resolve the extension folder --------------------------------------------
if [ -n "${2:-}" ]; then
    EXT_DIR="$2"
elif [ -n "${MIRTH_HOME:-}" ]; then
    EXT_DIR="$MIRTH_HOME/extensions/$EXT_NAME"
else
    echo "ERROR: set MIRTH_HOME env var, or pass the extension folder as an argument."
    echo "  Example: MIRTH_HOME=/opt/mirth-connect $0"
    echo "  Example: $0 /opt/mirth-connect/extensions/$EXT_NAME"
    exit 2
fi

echo "============================================================"
echo "Extension diagnostic: $EXT_NAME"
echo "Extension folder:      $EXT_DIR"
echo "============================================================"
echo ""

# --- Check 1: extension folder exists ---------------------------------------
echo "[1/6] Checking extension folder exists..."
if [ ! -d "$EXT_DIR" ]; then
    echo "  FAIL: extension folder does not exist."
    echo "  Expected: $EXT_DIR"
    echo "  Fix: copy the 5 production files to this location, then restart Mirth."
    exit 1
fi
echo "  OK: folder exists."
echo ""

# --- Check 2: all 5 production files are present ----------------------------
echo "[2/6] Checking all 5 production files are present..."
EXPECTED_FILES=(
    "plugin.xml"
    "transmissionmode.xml"
    "bitdreamit-astm-e1381-transmission-shared.jar"
    "bitdreamit-astm-e1381-transmission-server.jar"
    "bitdreamit-astm-e1381-transmission-client.jar"
)
ALL_PRESENT=true
for f in "${EXPECTED_FILES[@]}"; do
    if [ -f "$EXT_DIR/$f" ]; then
        SIZE=$(stat -c%s "$EXT_DIR/$f" 2>/dev/null || stat -f%z "$EXT_DIR/$f" 2>/dev/null || echo "?")
        echo "  OK:   $f ($SIZE bytes)"
    else
        echo "  FAIL: $f MISSING"
        ALL_PRESENT=false
    fi
done
if [ "$ALL_PRESENT" = "false" ]; then
    echo ""
    echo "  Fix: copy the missing files from the build output to $EXT_DIR/"
    exit 1
fi
echo ""

# --- Check 3: plugin.xml uses the correct Mirth 4.5.2 format ----------------
echo "[3/6] Checking plugin.xml format..."
PLUGIN_XML="$EXT_DIR/plugin.xml"

# Check for <string> elements (not <serverClass> wrappers)
if grep -q "<serverClass\|<clientClass" "$PLUGIN_XML"; then
    echo "  FAIL: plugin.xml uses <serverClass>/<clientClass> wrapper elements."
    echo "  These are NOT supported in Mirth 4.5.2 (use <string> elements instead)."
    echo "  Fix: use v1.1.9+ of this extension."
    exit 1
fi
if ! grep -q "<serverClasses>" "$PLUGIN_XML"; then
    echo "  FAIL: plugin.xml is missing <serverClasses> element."
    exit 1
fi
if ! grep -q "<clientClasses>" "$PLUGIN_XML"; then
    echo "  FAIL: plugin.xml is missing <clientClasses> element."
    exit 1
fi
if ! grep -q "<library" "$PLUGIN_XML"; then
    echo "  FAIL: plugin.xml is missing top-level <library> elements."
    exit 1
fi
echo "  OK: plugin.xml uses the correct Mirth 4.5.2 format."
echo ""

# --- Check 4: Properties class is in BOTH <serverClasses> and <clientClasses>
echo "[4/6] Checking ASTME1381TransmissionModeProperties is registered for XStream..."
SERVER_CLASSES=$(sed -n '/<serverClasses>/,/<\/serverClasses>/p' "$PLUGIN_XML")
CLIENT_CLASSES=$(sed -n '/<clientClasses>/,/<\/clientClasses>/p' "$PLUGIN_XML")

if echo "$SERVER_CLASSES" | grep -q "$PROPS_FQCN"; then
    echo "  OK: Properties class is in <serverClasses> (server-side XStream)."
else
    echo "  FAIL: Properties class is NOT in <serverClasses>."
    echo "  This will cause ForbiddenClassException on the server side."
    echo "  Fix: use v1.2.0+ of this extension."
    exit 1
fi

if echo "$CLIENT_CLASSES" | grep -q "$PROPS_FQCN"; then
    echo "  OK: Properties class is in <clientClasses> (client-side XStream)."
else
    echo "  FAIL: Properties class is NOT in <clientClasses>."
    echo "  This will cause ForbiddenClassException on the client side."
    echo "  Fix: use v1.2.0+ of this extension."
    exit 1
fi
echo ""

# --- Check 5: Properties class is actually present inside each JAR -----------
echo "[5/6] Checking JAR contents for ASTME1381TransmissionModeProperties.class..."
JARS=(
    "bitdreamit-astm-e1381-transmission-shared.jar"
    "bitdreamit-astm-e1381-transmission-server.jar"
    "bitdreamit-astm-e1381-transmission-client.jar"
)
ALL_JARS_OK=true
for jar in "${JARS[@]}"; do
    if jar tf "$EXT_DIR/$jar" 2>/dev/null | grep -q "$PROPS_CLASS"; then
        echo "  OK:   $jar contains ASTME1381TransmissionModeProperties.class"
    elif unzip -l "$EXT_DIR/$jar" 2>/dev/null | grep -q "$PROPS_CLASS"; then
        echo "  OK:   $jar contains ASTME1381TransmissionModeProperties.class"
    else
        echo "  FAIL: $jar does NOT contain ASTME1381TransmissionModeProperties.class"
        ALL_JARS_OK=false
    fi
done
if [ "$ALL_JARS_OK" = "false" ]; then
    echo ""
    echo "  The Properties class is missing from one or more JARs."
    echo "  This means the JAR was built from an incomplete or stale build."
    echo "  Fix: rebuild the JARs using:"
    echo "    cd distribution && ./build.sh clean && ./build.sh"
    echo "  Or via IntelliJ IDEA: Build -> Build Artifacts -> All Artifacts -> Build"
    echo "  Then copy the new JARs to $EXT_DIR/"
    exit 1
fi
echo ""

# --- Check 6: extension folder name matches plugin.xml path attribute -------
echo "[6/6] Checking extension folder name matches plugin.xml path attribute..."
PLUGIN_PATH=$(grep -oP '(?<=path=")[^"]+' "$PLUGIN_XML" | head -1)
ACTUAL_DIR_NAME=$(basename "$EXT_DIR")
if [ "$PLUGIN_PATH" = "$ACTUAL_DIR_NAME" ]; then
    echo "  OK: folder name '$ACTUAL_DIR_NAME' matches plugin.xml path='$PLUGIN_PATH'"
else
    echo "  FAIL: folder name '$ACTUAL_DIR_NAME' does NOT match plugin.xml path='$PLUGIN_PATH'"
    echo "  Mirth uses the path attribute to locate the extension folder."
    echo "  Fix: rename the extension folder to match the path attribute."
    exit 1
fi
echo ""

# --- Summary ----------------------------------------------------------------
echo "============================================================"
echo "ALL CHECKS PASSED"
echo "============================================================"
echo ""
echo "Extension folder: $EXT_DIR"
echo "Plugin version:   $(grep -oP '(?<=<pluginVersion>)[^<]+' "$PLUGIN_XML")"
echo "Mirth version:    $(grep -oP '(?<=<mirthVersion>)[^<]+' "$PLUGIN_XML")"
echo ""
echo "If you are STILL getting ForbiddenClassException when opening a"
echo "channel, the problem is NOT with the files on disk - they are correct."
echo "The problem is that the Mirth Administrator UI has a STALE cache of"
echo "the old extension's class registration. You MUST do a FULL clean"
echo "reinstall:"
echo ""
echo "  STEP 1: Uninstall the old extension in the Administrator UI:"
echo "    Extensions -> Extension Manager -> select the extension -> Uninstall"
echo ""
echo "  STEP 2: Restart the Mirth Server:"
echo "    sudo systemctl restart mirth-connect"
echo ""
echo "  STEP 3: FULLY CLOSE the Mirth Administrator UI:"
echo "    - Do NOT just disconnect. Close the entire application."
echo "    - On Windows: File -> Exit (or close all windows)."
echo "    - On Linux: File -> Exit."
echo "    - Verify the Java process is gone (Task Manager / ps aux | grep mirth)"
echo ""
echo "  STEP 4: Reopen the Mirth Administrator UI and log in."
echo "    (The UI will re-download extension metadata from the server.)"
echo ""
echo "  STEP 5: Install the new extension via Extension Manager:"
echo "    Extensions -> Extension Manager -> Install -> select the .zip"
echo ""
echo "  STEP 6: Restart the Mirth Server AGAIN (so the new extension loads):"
echo "    sudo systemctl restart mirth-connect"
echo ""
echo "  STEP 7: FULLY CLOSE the Administrator UI AGAIN, then reopen."
echo "    (The UI needs to re-download the new extension's class list"
echo "    and re-register the Properties class with its XStream instance.)"
echo ""
echo "  STEP 8: Open your channel - the ForbiddenClassException should be gone."
echo ""
echo "If you STILL get the error after following all 8 steps, the channel"
echo "XML may have stale data from a very old extension version. Delete"
echo "the channel and create a new one."
echo ""
