#!/usr/bin/env bash
# =============================================================================
# nuclear_deploy.sh - Clean-slate deployment + diagnostic
# =============================================================================
# This script:
#   1. Stops Mirth
#   2. Removes the entire old extension folder
#   3. Removes ALL stray .bak folders
#   4. Deploys fresh files (3 jars + 2 XMLs) from the v1.3.4 zip
#   5. Clears the Mirth appdata cache (server-side)
#   6. Clears the Administrator UI local cache (client-side, if running locally)
#   7. Fixes file ownership
#   8. Starts Mirth
#   9. Tails the log for 30 seconds so we can see what happened
#
# USAGE:
#   sudo MIRTH_HOME=/opt/mirth-connect \
#       bash nuclear_deploy.sh /home/z/my-project/download/bitdreamit-astm-e1381-transmission.zip
# =============================================================================
set -e

EXT_NAME="bitdreamit-astm-e1381-transmission"

# --- Resolve Mirth home ---
MIRTH_HOME="${MIRTH_HOME:-/opt/mirth-connect}"
if [ ! -d "$MIRTH_HOME" ]; then
    echo "ERROR: MIRTH_HOME not found at $MIRTH_HOME"
    echo "Set it explicitly:  sudo MIRTH_HOME=/your/mirth/path $0 <zip>"
    exit 1
fi
EXT_DIR="$MIRTH_HOME/extensions/$EXT_NAME"
APPDATA="$MIRTH_HOME/appdata"

# --- Resolve source zip ---
SOURCE="${1:-}"
if [ -z "$SOURCE" ] || [ ! -f "$SOURCE" ]; then
    echo "ERROR: pass the source zip as the first argument."
    echo "  Example: $0 /home/z/my-project/download/bitdreamit-astm-e1381-transmission.zip"
    exit 2
fi

echo "============================================================"
echo "NUCLEAR DEPLOYMENT - clean slate"
echo "============================================================"
echo "Mirth home:       $MIRTH_HOME"
echo "Extension folder: $EXT_DIR"
echo "Appdata:          $APPDATA"
echo "Source zip:       $SOURCE"
echo ""

# --- Stage the source ---
STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT
echo "[1/9] Extracting zip to $STAGE..."
unzip -q "$SOURCE" -d "$STAGE"
UNSIGN=$(find "$STAGE" -type d -name "unsign" | head -1)
if [ -z "$UNSIGN" ]; then
    echo "  ERROR: no unsign/ folder found inside the zip."
    exit 3
fi
echo "  Source files in: $UNSIGN"
ls -la "$UNSIGN"

# --- Stop Mirth ---
echo ""
echo "[2/9] Stopping Mirth Connect..."
sudo systemctl stop mirth-connect 2>/dev/null || echo "  (systemctl stop failed - stop Mirth manually if needed)"
sleep 5

# --- Remove old extension folder + any stray folders ---
echo ""
echo "[3/9] Removing old extension folder and any stray .bak folders..."
for d in "$MIRTH_HOME/extensions/${EXT_NAME}"*; do
    if [ -d "$d" ]; then
        echo "  Removing: $d"
        sudo rm -rf "$d"
    fi
done

# --- Deploy fresh files ---
echo ""
echo "[4/9] Deploying fresh extension files to $EXT_DIR..."
sudo mkdir -p "$EXT_DIR"
sudo cp "$UNSIGN"/*.jar "$EXT_DIR/"
sudo cp "$UNSIGN/plugin.xml" "$EXT_DIR/"
sudo cp "$UNSIGN/transmissionmode.xml" "$EXT_DIR/"
echo "  Deployed files:"
ls -la "$EXT_DIR"

# --- Verify the deployed files ---
echo ""
echo "[5/9] Verifying deployed files..."

# Check 1: plugin.xml has XML declaration
if head -1 "$EXT_DIR/plugin.xml" | grep -q '<?xml'; then
    echo "  OK: plugin.xml has XML declaration"
else
    echo "  FAIL: plugin.xml missing XML declaration!"
fi

# Check 2: transmissionmode.xml has <sharedClassName>
if grep -q "<sharedClassName>" "$EXT_DIR/transmissionmode.xml"; then
    echo "  OK: transmissionmode.xml has <sharedClassName>"
    grep "<sharedClassName>" "$EXT_DIR/transmissionmode.xml" | sed 's/^/    /'
else
    echo "  FAIL: transmissionmode.xml missing <sharedClassName>!"
fi

# Check 3: plugin.xml doesn't list Properties in serverClasses/clientClasses
if python3 -c "
import re
with open('$EXT_DIR/plugin.xml') as f:
    c = f.read()
c_nc = re.sub(r'<!--.*?-->', '', c, flags=re.DOTALL)
for block in ['serverClasses', 'clientClasses']:
    m = re.search(r'<' + block + r'>(.*?)</' + block + r'>', c_nc, re.DOTALL)
    if m:
        for s in re.findall(r'<string>([^<]+)</string>', m.group(1)):
            if 'Properties' in s:
                print(f'  FAIL: {block} lists {s}')
                exit(1)
" 2>/dev/null; then
    echo "  OK: plugin.xml doesn't list Properties in serverClasses/clientClasses"
fi

# Check 4: jars contain the expected classes
JDK=$(which jar 2>/dev/null || echo "/home/z/my-project/jdk/jdk-17.0.20+8/bin/jar")
for jar in "$EXT_DIR"/*.jar; do
    cnt=$("$JDK" tf "$jar" 2>/dev/null | grep -c "\.class$")
    echo "  $(basename $jar): $cnt classes"
done

# --- Clear caches ---
echo ""
echo "[6/9] Clearing ALL Mirth caches..."

# Server-side appdata cache
if [ -d "$APPDATA" ]; then
    for sub in .mirth .tmp extension-manager; do
        if [ -d "$APPDATA/$sub" ]; then
            echo "  Clearing $APPDATA/$sub"
            sudo rm -rf "$APPDATA/$sub"
        fi
    done
    # Also clear the Administrator UI's local settings (where channel cache lives)
    for sub in .mirth/.conf; do
        if [ -d "$APPDATA/$sub" ]; then
            echo "  Clearing $APPDATA/$sub"
            sudo rm -rf "$APPDATA/$sub"
        fi
    done
fi

# Java Web Start cache (if the Administrator UI is launched via Web Start)
for ws_cache in "$HOME/.java/deployment/cache" "$HOME/.cache/icedtea-web" "$HOME/.config/icedtea-web"; do
    if [ -d "$ws_cache" ]; then
        echo "  Clearing Java Web Start cache: $ws_cache"
        rm -rf "$ws_cache"
    fi
done

# Mirth Administrator UI local cache (mirth-appdata in user home)
for ui_cache in "$HOME/.mirth" "$HOME/mirth-appdata"; do
    if [ -d "$ui_cache" ]; then
        echo "  Clearing Administrator UI cache: $ui_cache"
        rm -rf "$ui_cache"
    fi
done

echo "  Caches cleared."

# --- Fix ownership ---
echo ""
echo "[7/9] Fixing file ownership..."
MIRTH_USER=$(stat -c '%U' "$MIRTH_HOME" 2>/dev/null || echo "mirth")
MIRTH_GROUP=$(stat -c '%G' "$MIRTH_HOME" 2>/dev/null || echo "mirth")
echo "  Setting ownership to $MIRTH_USER:$MIRTH_GROUP"
sudo chown -R "$MIRTH_USER:$MIRTH_GROUP" "$EXT_DIR" 2>/dev/null || true
[ -d "$APPDATA" ] && sudo chown -R "$MIRTH_USER:$MIRTH_GROUP" "$APPDATA" 2>/dev/null || true

# --- Start Mirth ---
echo ""
echo "[8/9] Starting Mirth Connect..."
sudo systemctl start mirth-connect
echo "  (started - waiting 20 seconds for Mirth to load extensions...)"
sleep 20

# --- Tail the log ---
echo ""
echo "[9/9] Mirth log - last 100 lines (look for ASTM / error / exception):"
echo "============================================================"
LOG="$MIRTH_HOME/logs/mirth.log"
if [ -f "$LOG" ]; then
    sudo tail -100 "$LOG" | grep -iE 'ASTM|error|exception|loaded plugin|transmission' | tail -40
    echo ""
    echo "============================================================"
    echo "FULL EXTENSION LOAD SEQUENCE (last 200 lines, filtered):"
    echo "============================================================"
    sudo tail -200 "$LOG" | grep -iE 'ASTM|extension|plugin|transmission|ClassCast|ForbiddenClass|AbstractMethod|NoSuchMethod' | tail -30
else
    echo "  Log file not found at $LOG"
fi

echo ""
echo "============================================================"
echo "DEPLOYMENT COMPLETE."
echo "============================================================"
echo ""
echo "WHAT TO LOOK FOR IN THE LOG ABOVE:"
echo ""
echo "SUCCESS indicators:"
echo "  - 'Loaded plugin: ASTM E1381 Transmission Mode'"
echo "  - No ClassCastException"
echo "  - No ForbiddenClassException"
echo "  - No AbstractMethodError / NoSuchMethodError"
echo ""
echo "FAILURE indicators (and what they mean):"
echo "  - 'ClassCastException: ...Properties cannot be cast to ServerPlugin'"
echo "    -> plugin.xml still lists Properties in <serverClasses> or <clientClasses>"
echo "  - 'ForbiddenClassException'"
echo "    -> transmissionmode.xml missing or not being read"
echo "  - 'AbstractMethodError' or 'NoSuchMethodError'"
echo "    -> the plugin class is missing a method that real Mirth 4.5.2 requires"
echo "  - 'ClassNotFoundException: com.mirth.connect.plugins.TransmissionModeProvider'"
echo "    -> Mirth jars not on classpath (shouldn't happen for a deployed extension)"
echo ""
echo "NEXT STEPS:"
echo "  1. Open the Mirth Administrator UI"
echo "  2. Go to Extensions -> Extension Manager"
echo "  3. Check if 'ASTM E1381 Transmission Mode' is in the list"
echo "  4. If yes, open/create a channel and check the Transmission Mode dropdown"
echo ""
echo "  5. If the extension is NOT in the list, share the FULL log lines above"
echo "     (from 'FULL EXTENSION LOAD SEQUENCE' downward) for further diagnosis."
echo ""
echo "  6. For the broken 'Test_astm-tcp' channel:"
echo "     - If the extension loaded successfully, the channel will become editable"
echo "     - If not, you may need to delete it via REST API and recreate it"
