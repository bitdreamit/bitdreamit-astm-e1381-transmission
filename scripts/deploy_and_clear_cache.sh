#!/usr/bin/env bash
# =============================================================================
# deploy_and_clear_cache.sh
# =============================================================================
# Full deployment + Mirth appdata cache clear for the ASTM E1381 extension.
#
# Use this script when:
#   - The extension was previously failing to load (ClassCastException,
#     ForbiddenClassException, etc.)
#   - You deployed a fix but the extension still doesn't appear in the
#     Mirth Administrator UI's extension list or in the channel editor's
#     Transmission Mode dropdown.
#   - A channel shows "invalid and cannot be edited".
#
# Why clearing the cache is necessary:
#   Mirth caches extension metadata and channel state in $MIRTH_HOME/appdata/.
#   When an extension fails to load, Mirth records the failure in the cache.
#   Even after deploying fixed files, Mirth may read the cached failure
#   state and skip the extension. Clearing the cache forces Mirth to
#   re-discover everything from scratch on next startup.
#
# USAGE:
#   sudo MIRTH_HOME=/opt/mirth-connect \
#       bash deploy_and_clear_cache.sh /path/to/bitdreamit-astm-e1381-transmission.zip
#
#   (or, if you have the unsign/ folder unpacked:)
#   sudo MIRTH_HOME=/opt/mirth-connect \
#       bash deploy_and_clear_cache.sh /path/to/unsign_folder
# =============================================================================
set -e

EXT_NAME="bitdreamit-astm-e1381-transmission"

# --- Resolve Mirth home ---
MIRTH_HOME="${MIRTH_HOME:-/opt/mirth-connect}"
if [ ! -d "$MIRTH_HOME" ]; then
    echo "ERROR: MIRTH_HOME not found at $MIRTH_HOME"
    echo "Set it explicitly:  sudo MIRTH_HOME=/your/mirth/path $0 <source>"
    exit 1
fi
EXT_DIR="$MIRTH_HOME/extensions/$EXT_NAME"
APPDATA="$MIRTH_HOME/appdata"

# --- Resolve source (zip or folder) ---
SOURCE="${1:-}"
if [ -z "$SOURCE" ]; then
    echo "ERROR: pass the source zip or folder as the first argument."
    echo "  Example: $0 /home/z/my-project/download/bitdreamit-astm-e1381-transmission.zip"
    echo "  Example: $0 /path/to/unsign_folder"
    exit 2
fi

# Stage the source files into a temp dir
STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT

if [ -f "$SOURCE" ] && [[ "$SOURCE" == *.zip ]]; then
    echo "[1/7] Extracting zip: $SOURCE"
    unzip -q "$SOURCE" -d "$STAGE"
    # Find the unsign/ folder inside the extracted zip
    UNSIGN=$(find "$STAGE" -type d -name "unsign" | head -1)
    if [ -z "$UNSIGN" ]; then
        echo "ERROR: no unsign/ folder found inside the zip."
        exit 3
    fi
    SRC_DIR="$UNSIGN"
elif [ -d "$SOURCE" ]; then
    echo "[1/7] Using source folder: $SOURCE"
    SRC_DIR="$SOURCE"
else
    echo "ERROR: source is neither a zip nor a folder: $SOURCE"
    exit 4
fi

echo "  Source files in: $SRC_DIR"
ls -la "$SRC_DIR"

# --- Step 2: Stop Mirth ---
echo ""
echo "[2/7] Stopping Mirth Connect..."
if command -v systemctl >/dev/null 2>&1; then
    sudo systemctl stop mirth-connect 2>/dev/null || true
    echo "  (sent systemctl stop)"
else
    echo "  systemctl not available - please stop Mirth manually before continuing."
    read -p "  Press Enter once Mirth is stopped... "
fi

# Verify Mirth is stopped
sleep 3

# --- Step 3: Back up and remove the old extension folder ---
echo ""
echo "[3/7] Backing up and removing old extension folder..."
if [ -d "$EXT_DIR" ]; then
    BACKUP="$HOME/${EXT_NAME}.bak.$(date +%s)"
    sudo mv "$EXT_DIR" "$BACKUP"
    echo "  Backed up to: $BACKUP"
else
    echo "  (no existing extension folder - nothing to back up)"
fi

# Also remove any stale .bak folders in the extensions dir
for d in "$MIRTH_HOME/extensions/${EXT_NAME}"*; do
    if [ -d "$d" ] && [ "$d" != "$EXT_DIR" ]; then
        echo "  WARN: removing stray folder: $d"
        sudo rm -rf "$d"
    fi
done

# --- Step 4: Deploy the new files ---
echo ""
echo "[4/7] Deploying new extension files to $EXT_DIR..."
sudo mkdir -p "$EXT_DIR"
sudo cp "$SRC_DIR"/*.jar "$EXT_DIR/"
if [ -f "$SRC_DIR/plugin.xml" ]; then
    sudo cp "$SRC_DIR/plugin.xml" "$EXT_DIR/"
fi
if [ -f "$SRC_DIR/transmissionmode.xml" ]; then
    sudo cp "$SRC_DIR/transmissionmode.xml" "$EXT_DIR/"
fi
echo "  Deployed files:"
ls -la "$EXT_DIR"

# --- Step 5: Clear the Mirth appdata cache ---
# This is the step most people miss. The cache stores extension metadata
# and channel state from previous runs. If Mirth previously failed to load
# the extension, the cache records that failure and Mirth won't retry on
# the next startup unless the cache is cleared.
echo ""
echo "[5/7] Clearing Mirth appdata cache..."
if [ -d "$APPDATA" ]; then
    echo "  Appdata folder: $APPDATA"
    # Back up the appdata folder first (in case we need to restore)
    APPDATA_BAK="$HOME/mirth-appdata.bak.$(date +%s)"
    sudo cp -r "$APPDATA" "$APPDATA_BAK" 2>/dev/null || true
    echo "  Appdata backed up to: $APPDATA_BAK"

    # Clear the specific cache subfolders that affect extension loading
    for sub in .mirth .tmp extension-manager; do
        if [ -d "$APPDATA/$sub" ]; then
            echo "  Clearing $APPDATA/$sub"
            sudo rm -rf "$APPDATA/$sub"
        fi
    done
    echo "  Cache cleared."
else
    echo "  No appdata folder at $APPDATA (skip)"
fi

# --- Step 6: Fix ownership ---
echo ""
echo "[6/7] Fixing file ownership..."
# Common Mirth user/group - adjust if your installation uses different
MIRTH_USER=$(stat -c '%U' "$MIRTH_HOME" 2>/dev/null || echo "mirth")
MIRTH_GROUP=$(stat -c '%G' "$MIRTH_HOME" 2>/dev/null || echo "mirth")
echo "  Setting ownership to $MIRTH_USER:$MIRTH_GROUP"
sudo chown -R "$MIRTH_USER:$MIRTH_GROUP" "$EXT_DIR" 2>/dev/null || true
if [ -d "$APPDATA" ]; then
    sudo chown -R "$MIRTH_USER:$MIRTH_GROUP" "$APPDATA" 2>/dev/null || true
fi

# --- Step 7: Start Mirth ---
echo ""
echo "[7/7] Starting Mirth Connect..."
if command -v systemctl >/dev/null 2>&1; then
    sudo systemctl start mirth-connect
    echo "  (sent systemctl start)"
    echo ""
    echo "  Waiting 15 seconds for Mirth to start up..."
    sleep 15
    echo ""
    echo "  Mirth status:"
    sudo systemctl status mirth-connect --no-pager 2>/dev/null | head -10 || true
else
    echo "  systemctl not available - please start Mirth manually."
fi

echo ""
echo "============================================================"
echo "DEPLOYMENT COMPLETE."
echo "============================================================"
echo ""
echo "What was done:"
echo "  1. Stopped Mirth Connect"
echo "  2. Backed up old extension folder to $HOME/${EXT_NAME}.bak.*"
echo "  3. Removed any stray .bak folders in the extensions dir"
echo "  4. Deployed fresh extension files (3 jars + 2 XMLs)"
echo "  5. Cleared the Mirth appdata cache (.mirth, .tmp, extension-manager)"
echo "  6. Fixed file ownership"
echo "  7. Started Mirth Connect"
echo ""
echo "VERIFY:"
echo "  1. Check the Mirth log for errors:"
echo "       sudo tail -100 $MIRTH_HOME/logs/mirth.log | grep -i 'ASTM\|error\|exception'"
echo ""
echo "  2. Open the Mirth Administrator UI:"
echo "       - Go to Extensions -> Extension Manager"
echo "       - 'ASTM E1381 Transmission Mode' should be in the list"
echo ""
echo "  3. Open (or create) a channel:"
echo "       - Source: TCP Listener"
echo "       - Transmission Mode dropdown should include 'ASTM E1381'"
echo ""
echo "IF THE EXTENSION STILL DOESN'T LOAD:"
echo "  The Mirth log will have the actual error. Run:"
echo "    sudo grep -A5 'ASTM\|ClassCast\|ForbiddenClass\|AbstractMethod\|NoSuchMethod' $MIRTH_HOME/logs/mirth.log | tail -50"
echo "  and share the output for further diagnosis."
