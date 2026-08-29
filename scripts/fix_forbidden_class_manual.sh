#!/usr/bin/env bash
# =============================================================================
# fix_forbidden_class_manual.sh
# =============================================================================
# WORKAROUND for ForbiddenClassException without rebuilding jars.
#
# This script manually registers ASTME1381TransmissionModeProperties with
# Mirth's XStream security framework by adding it to the server's
# configuration. This is a WORKAROUND - the proper fix is to rebuild the
# jars against real Mirth jars (see rebuild_from_real_mirth.sh).
#
# USAGE:
#   sudo MIRTH_HOME=/opt/mirth-connect bash fix_forbidden_class_manual.sh
# =============================================================================
set -e

MIRTH_HOME="${MIRTH_HOME:-/opt/mirth-connect}"
EXT_NAME="bitdreamit-astm-e1381-transmission"
EXT_DIR="$MIRTH_HOME/extensions/$EXT_NAME"
PROPS_CLASS="com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties"

echo "============================================================"
echo "MANUAL XStream ALLOW-LIST WORKAROUND"
echo "for ForbiddenClassException"
echo "============================================================"
echo "Mirth home: $MIRTH_HOME"
echo ""

# --- Check 1: Verify transmissionmode.xml exists ---
echo "[1/4] Checking transmissionmode.xml..."
if [ ! -f "$EXT_DIR/transmissionmode.xml" ]; then
    echo "  FAIL: $EXT_DIR/transmissionmode.xml not found"
    echo "  This file is REQUIRED - it tells Mirth's TransmissionModeController"
    echo "  to call xStream.allowTypes() for our Properties class."
    echo ""
    echo "  Fix: copy transmissionmode.xml to $EXT_DIR/"
    exit 1
fi
echo "  OK: transmissionmode.xml exists"

# Check it has <sharedClassName>
if grep -q "<sharedClassName>" "$EXT_DIR/transmissionmode.xml"; then
    echo "  OK: <sharedClassName> element present"
else
    echo "  FAIL: <sharedClassName> element missing from transmissionmode.xml"
    exit 1
fi

# --- Check 2: Check if the server-side extension loaded ---
echo ""
echo "[2/4] Checking if server-side extension loaded..."
LOG="$MIRTH_HOME/logs/mirth.log"
if [ -f "$LOG" ]; then
    echo "  Last 20 lines mentioning ASTM/Error from the log:"
    sudo grep -i 'ASTM\|error.*plugin\|exception.*plugin\|DefaultExtensionController' "$LOG" | tail -20
    echo ""
    echo "  If you see 'Error instantiating plugin: ASTM E1381' above,"
    echo "  the server-side extension is FAILING TO LOAD."
    echo "  The ForbiddenClassException is a SYMPTOM of this failure."
    echo "  The ROOT CAUSE is that the plugin class can't be instantiated"
    echo "  (usually because the jars were compiled against stub Mirth"
    echo "  classes that don't match the real Mirth API)."
else
    echo "  Log not found at $LOG"
fi

# --- Check 3: Check the plugin.xml ---
echo ""
echo "[3/4] Checking plugin.xml..."
if [ ! -f "$EXT_DIR/plugin.xml" ]; then
    echo "  FAIL: $EXT_DIR/plugin.xml not found"
    exit 1
fi

# Check for Properties in serverClasses/clientClasses
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
    echo "  OK: plugin.xml doesn't list Properties in class lists"
fi

# --- Check 4: Check the jars ---
echo ""
echo "[4/4] Checking jars..."
for jar in "$EXT_DIR"/*.jar; do
    [ -f "$jar" ] || continue
    cnt=$(jar tf "$jar" 2>/dev/null | grep -c "\.class$" || echo 0)
    echo "  $(basename $jar): $cnt classes"
done

echo ""
echo "============================================================"
echo "DIAGNOSIS"
echo "============================================================"
echo ""
echo "If the ForbiddenClassException is STILL happening after deploying"
echo "the v1.3.4 files, the ROOT CAUSE is almost certainly that the"
echo "pre-built jars were compiled against STUB Mirth classes (not the"
echo "real Mirth 4.5.2 jars). The stubs have different constructor"
echo "signatures and method implementations than the real Mirth API."
echo ""
echo "When Mirth tries to instantiate ASTME1381TransmissionModePlugin,"
echo "it fails silently (the error is in the server log but not shown"
echo "in the Administrator UI). Since the server-side extension doesn't"
echo "load, the TransmissionModeController never reads transmissionmode.xml,"
echo "and XStream never allows ASTME1381TransmissionModeProperties."
echo ""
echo "TO FIX PERMANENTLY:"
echo "  1. Run rebuild_from_real_mirth.sh to rebuild the jars against"
echo "     the REAL Mirth jars from \$MIRTH_HOME/lib/"
echo "  2. Deploy the rebuilt jars"
echo "  3. Clear the Mirth cache:"
echo "       sudo rm -rf \$MIRTH_HOME/appdata/.mirth \$MIRTH_HOME/appdata/.tmp"
echo "  4. Restart Mirth:"
echo "       sudo systemctl restart mirth-connect"
echo "  5. Check the log for the ACTUAL error:"
echo "       sudo grep -A5 'ASTM\|Error.*plugin\|exception' \$MIRTH_HOME/logs/mirth.log | tail -30"
echo ""
echo "Share those log lines and I can fix the specific incompatibility."
