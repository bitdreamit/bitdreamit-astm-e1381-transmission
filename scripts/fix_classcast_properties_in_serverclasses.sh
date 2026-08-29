#!/usr/bin/env bash
# =============================================================================
# fix_classcast_properties_in_serverclasses.sh
# =============================================================================
# Diagnose & fix the ClassCastException:
#   class com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties
#   cannot be cast to class com.mirth.connect.plugins.ServerPlugin
#       at DefaultExtensionController.initPlugins(DefaultExtensionController.java:211)
#
# ROOT CAUSE (always one of these three):
#   (A) A pre-v1.2.5 plugin.xml is on disk in the Mirth extensions folder and
#       still lists ASTME1381TransmissionModeProperties inside <serverClasses>
#       or <clientClasses>. Our v1.2.5 fix REMOVED it from those lists because
#       it extends TransmissionModeProperties -> Purgable, NOT ServerPlugin.
#   (B) An older .jar file is on the classpath that embeds a stale plugin.xml
#       with the bad listing (jars built before v1.2.5 contained their own
#       plugin.xml inside META-INF/ or root).
#   (C) A backup folder like bitdreamit-astm-e1381-transmission.bak/ sits
#       next to the live extension and Mirth's extension loader picks up
#       its plugin.xml too.
#
# This script:
#   1. Locates the Mirth extension folder.
#   2. Reports any plugin.xml that lists ASTME1381TransmissionModeProperties.
#   3. Backs up the offending files and replaces them with a clean v1.3.2
#      plugin.xml that has the bad entry stripped.
#   4. Reports any stale .bak folders.
#   5. Tells you to restart Mirth.
# =============================================================================
set -e

EXT_NAME="bitdreamit-astm-e1381-transmission"
PROPS_FQCN="com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties"

# --- Locate Mirth ---
MIRTH_HOME="${MIRTH_HOME:-/opt/mirth-connect}"
if [ ! -d "$MIRTH_HOME" ]; then
    echo "MIRTH_HOME not found at $MIRTH_HOME"
    echo "Set it explicitly:  sudo MIRTH_HOME=/your/mirth/path $0"
    exit 1
fi
EXT_DIR="$MIRTH_HOME/extensions/$EXT_NAME"

echo "============================================================"
echo "Mirth home:       $MIRTH_HOME"
echo "Extension folder: $EXT_DIR"
echo "============================================================"

# --- Look for stray extension folders (e.g. .bak) ---
echo ""
echo "[1/4] Looking for stray copies of the extension folder..."
stray_count=0
for d in "$MIRTH_HOME/extensions/${EXT_NAME}"*; do
    if [ -d "$d" ] && [ "$d" != "$EXT_DIR" ]; then
        echo "  WARN: $d exists alongside the live extension"
        stray_count=$((stray_count+1))
    fi
done
if [ $stray_count -eq 0 ]; then
    echo "  OK: no stray folders"
else
    echo "  -> RENAME these out of the way, e.g.:"
    echo "       sudo mv $MIRTH_HOME/extensions/${EXT_NAME}.bak $HOME/${EXT_NAME}.bak.$(date +%s)"
fi

# --- Check the loose plugin.xml on disk ---
echo ""
echo "[2/4] Checking loose plugin.xml on disk..."
if [ ! -f "$EXT_DIR/plugin.xml" ]; then
    echo "  FAIL: $EXT_DIR/plugin.xml does not exist"
    echo "  -> Re-deploy the v1.3.2 plugin.xml from this ZIP to that location."
    exit 2
fi

if grep -q "$PROPS_FQCN" "$EXT_DIR/plugin.xml" 2>/dev/null; then
    # Check whether it's inside a comment or actually in <serverClasses>/<clientClasses>
    if python3 -c "
import re, sys
with open('$EXT_DIR/plugin.xml') as f:
    c = f.read()
c = re.sub(r'<!--.*?-->', '', c, flags=re.DOTALL)
m = re.search(r'<(?:serverClasses|clientClasses)>(?:[^<]|<(?!!--))*?$PROPS_FQCN', c)
sys.exit(0 if m else 1)
"; then
        echo "  FAIL: $EXT_DIR/plugin.xml still lists $PROPS_FQCN"
        echo "         inside <serverClasses> or <clientClasses>."
        echo "  -> This is the cause of your ClassCastException."
        echo "  -> Backing it up and replacing with the v1.3.2 clean version."
        # Find the new plugin.xml from the source zip if available
        SRC="/home/z/my-project/workspace/bitdreamit-astm-e1381-transmission/plugin.xml"
        if [ -f "$SRC" ]; then
            sudo cp "$EXT_DIR/plugin.xml" "$EXT_DIR/plugin.xml.bak.$(date +%s)"
            sudo cp "$SRC" "$EXT_DIR/plugin.xml"
            echo "  OK: replaced. Backup saved as $EXT_DIR/plugin.xml.bak.*"
        else
            echo "  -> Source plugin.xml not found at $SRC"
            echo "  -> Manual fix: edit $EXT_DIR/plugin.xml and REMOVE the line"
            echo "       <string>$PROPS_FQCN</string>"
            echo "     from BOTH <serverClasses> and <clientClasses>."
        fi
    else
        echo "  OK: $PROPS_FQCN appears only in a comment (harmless)"
    fi
else
    echo "  OK: $EXT_DIR/plugin.xml does NOT list $PROPS_FQCN"
fi

# --- Check for stale plugin.xml inside any jar ---
echo ""
echo "[3/4] Checking for stale plugin.xml embedded inside JARs..."
which unzip >/dev/null 2>&1 || { echo "  SKIP: 'unzip' not installed"; }
for jar in "$EXT_DIR"/*.jar; do
    [ -f "$jar" ] || continue
    if unzip -p "$jar" plugin.xml 2>/dev/null | grep -q "$PROPS_FQCN"; then
        # Check whether the match is inside serverClasses/clientClasses (real)
        if unzip -p "$jar" plugin.xml 2>/dev/null | python3 -c "
import re, sys
c = sys.stdin.read()
c = re.sub(r'<!--.*?-->', '', c, flags=re.DOTALL)
m = re.search(r'<(?:serverClasses|clientClasses)>(?:[^<]|<(?!!--))*?$PROPS_FQCN', c)
sys.exit(0 if m else 1)
"; then
            echo "  FAIL: $jar embeds a plugin.xml that still lists $PROPS_FQCN"
            echo "  -> This JAR was built before v1.2.5. Replace it with the"
            echo "     v1.3.2 JAR from this ZIP's unsign/ folder."
        else
            echo "  OK: $jar embeds a plugin.xml but $PROPS_FQCN is only in a comment"
        fi
    else
        echo "  OK: $jar does not embed $PROPS_FQCN"
    fi
done

# --- Check for stale extension cache ---
echo ""
echo "[4/4] Checking for stale Mirth extension cache..."
APPDATA="$MIRTH_HOME/appdata"
if [ -d "$APPDATA" ]; then
    echo "  Appdata folder: $APPDATA"
    echo "  -> If the channel still fails after steps 1-3, clear the cache:"
    echo "       sudo systemctl stop mirth-connect"
    echo "       sudo rm -rf $APPDATA/.mirth $APPDATA/.tmp"
    echo "       sudo systemctl start mirth-connect"
else
    echo "  No appdata folder found at $APPDATA (skip)"
fi

echo ""
echo "============================================================"
echo "DONE. Restart Mirth Connect now:"
echo "  sudo systemctl restart mirth-connect"
echo ""
echo "Then watch the log for the extension to load successfully:"
echo "  sudo tail -f $MIRTH_HOME/logs/mirth.log | grep -A2 'ASTM E1381'"
echo "============================================================"
