# Troubleshooting

Common deployment-time errors and their fixes.

---

## `ClassCastException: ASTME1381TransmissionModeProperties cannot be cast to ServerPlugin`

```
ERROR (DefaultExtensionController:304): Error instantiating plugin: ASTM E1381 Transmission Mode (bitdreamit)
java.lang.ClassCastException: class com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties
    cannot be cast to class com.mirth.connect.plugins.ServerPlugin
    at DefaultExtensionController.initPlugins(DefaultExtensionController.java:211)
    at Mirth.startup(Mirth.java:334)
```

### What it means
Your `plugin.xml` (the loose file in
`$MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/plugin.xml`)
still lists `ASTME1381TransmissionModeProperties` inside `<serverClasses>`
or `<clientClasses>`. Mirth's `initPlugins()` walks those two lists and
casts every entry to `ServerPlugin` / `ClientPlugin` respectively.
`ASTME1381TransmissionModeProperties` extends `TransmissionModeProperties`
(which implements `Purgable`), NOT `ServerPlugin`, so the cast fails and
Mirth refuses to load the extension.

### Why it's happening even though v1.2.5 fixed it in source
The v1.2.5 fix removed `ASTME1381TransmissionModeProperties` from the
`<serverClasses>` and `<clientClasses>` lists in our `plugin.xml`. But
**you almost certainly have an OLD `plugin.xml` on disk** in your Mirth
extensions folder. The most common reasons:

1. **You copied the new `.jar` files but kept the old `plugin.xml`.**
   Mirth reads the LOOSE `plugin.xml` from the extension folder, not the
   one inside the jar. If you only updated the jars, the old XML is still
   active.
2. **You deployed a previous ZIP** (pre-v1.2.5: v1.2.0 / v1.2.1 / v1.2.2 /
   v1.2.3 / v1.2.4) that still had the bad listing.
3. **A backup folder** `$MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission.bak/`
   sits next to the live extension and Mirth's extension loader picks up
   its `plugin.xml` too.
4. **A stale jar** built before v1.2.5 is on the classpath. Old jars
   embedded their own `plugin.xml` inside the jar root; Mirth may load
   that one instead of the loose file.

### How to fix
Run the diagnostic script we ship:

```bash
sudo MIRTH_HOME=/opt/mirth-connect \
    bash /path/to/this/zip/scripts/fix_classcast_properties_in_serverclasses.sh
```

It will:
- Locate every `plugin.xml` in your Mirth extension folder AND inside
  every jar.
- Report any that still list `ASTME1381TransmissionModeProperties` in
  `<serverClasses>` / `<clientClasses>`.
- Back up the offending file and replace it with a clean v1.3.2
  `plugin.xml`.
- Warn about stray `.bak` folders.

Or fix manually:

```bash
EXT_DIR=/opt/mirth-connect/extensions/bitdreamit-astm-e1381-transmission
sudo systemctl stop mirth-connect

# 1. Remove the bad entry from plugin.xml
sudo sed -i '/<string>com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties<\/string>/d' \
    "$EXT_DIR/plugin.xml"

# 2. Replace any old jars with the v1.3.2 jars from this ZIP's unsign/ folder
sudo cp unsign/*.jar "$EXT_DIR/"

# 3. Move any .bak folders OUT of the extensions dir
sudo mv "$EXT_DIR.bak" "$HOME/astm-ext-backup-$(date +%s)" 2>/dev/null || true

# 4. Restart Mirth
sudo systemctl start mirth-connect
```

### Verification
After Mirth restarts, the log should show:

```
INFO  (DefaultExtensionController:XXX): Loaded plugin: ASTM E1381 Transmission Mode (bitdreamit)
```

with NO `ClassCastException` and NO `Error instantiating plugin` line.

---

## `ForbiddenClassException: ASTME1381TransmissionModeProperties`

```
Channel "Test_astm-tcp" is invalid and cannot be edited. Original cause:
com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties
com.thoughtworks.xstream.security.ForbiddenClassException:
  com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties
    at com.thoughtworks.xstream.security.NoTypePermission.allows(NoTypePermission.java:26)
    at com.thoughtworks.xstream.mapper.SecurityMapper.realClass(SecurityMapper.java:74)
    ...
    at com.mirth.connect.model.converters.ObjectXMLSerializer.deserializeList(ObjectXMLSerializer.java:421)
```

### What it means
Mirth's XStream security framework refuses to deserialize the
`ASTME1381TransmissionModeProperties` class. The channel was saved
successfully (the server-side XStream had the class allowed), but when
the Administrator UI tries to read the channel back, the client-side
XStream rejects it. The channel becomes "invalid" and cannot be edited.

### Why MLLP doesn't have this problem but we do
Mirth's built-in MLLP plugin's `MLLPModeProperties` class lives inside
Mirth's core jars (`mirth-server.jar` / `mirth-client-core.jar`), which
are added to XStream's allow-list during Mirth's bootstrap. Our
`ASTME1381TransmissionModeProperties` lives inside the extension's
`bitdreamit-astm-e1381-transmission-shared.jar`, which is NOT on the
default allow-list.

### The fix: `transmissionmode.xml` with `<sharedClassName>`
Mirth's `TransmissionModeController` (both server and client versions)
scans every `$MIRTH_HOME/extensions/*/transmissionmode.xml` file at
startup and calls `xStream.allowTypes(...)` for each `<sharedClassName>`
declared there. That's how extension transmission modes get their
Properties class onto XStream's allow-list.

Our `transmissionmode.xml` ships with this element:

```xml
<transmissionMode>
    ...
    <sharedClassName>com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties</sharedClassName>
    ...
</transmissionMode>
```

If you're seeing `ForbiddenClassException`, **the file is missing or
stale on your Mirth server**. Re-deploy it:

```bash
sudo systemctl stop mirth-connect
EXT_DIR=/opt/mirth-connect/extensions/bitdreamit-astm-e1381-transmission
sudo cp /path/to/this/zip/transmissionmode.xml $EXT_DIR/transmissionmode.xml
sudo systemctl start mirth-connect
```

After Mirth restarts, both the server-side and client-side XStream
instances will have the Properties class allowed, and the channel
becomes editable again.

### How to clean up the invalid channel
Once the fix above is deployed, the invalid channel will become
editable. You don't need to delete it — just open it in the channel
editor, make a trivial change (e.g. rename then rename back), and Save.
This re-serializes the channel XML with the now-allowed Properties class.

---

## `Cannot resolve class: ASTM E1381` in channel editor

The transmission mode dropdown shows the option but selecting it produces
an error.

### Cause
The extension loaded but the `TransmissionModeProvider` registry wasn't
populated. Usually a sign that `ASTME1381TransmissionModePlugin.start()`
threw silently.

### Fix
Check `$MIRTH_HOME/logs/mirth.log` for a stack trace right after the
"Loaded plugin" line. Most common cause is a missing dependency jar
(`log4j-1.2-api-2.17.2.jar` or `donkey-server.jar`).

---

## Settings panel does not appear ("Frame Settings" link greyed out)

### Cause
v1.2.6 removed `ASTME1381ClientProvider` from `<clientClasses>` because
it extends `TransmissionModeClientProvider`, NOT `ClientPlugin`. Mirth's
client-side extension loader silently cast-failed and aborted the
client-side init, leaving the settings panel unregistered.

### Fix
Make sure the `plugin.xml` you deployed is from v1.2.6 or later. The
`<clientClasses>` block should contain ONLY
`ASTME1381TransmissionModeClientPlugin`.

---

## "No data received" on a serial-port ASTM channel

### Cause
This is the bug the v1.3.2 `patches/serialsourceconnector-astm-no-data-fix.patch`
fixes. The serial connector was reading bytes with `readBytes()` AND
creating a `StreamHandler` over `getInputStream()` (a second reader on
the same port). The handler never saw the bytes the connector had
already consumed.

### Fix
Apply the companion patch to the **separate**
`bitdreamit-mirth-labextensions` repository (NOT this repo):

```bash
cd /path/to/bitdreamit-mirth-labextensions
git am /path/to/this/zip/patches/serialsourceconnector-astm-no-data-fix.patch
# OR if you don't use git-am:
patch -p1 < /path/to/this/zip/patches/serialsourceconnector-astm-no-data-fix.patch
```

Then rebuild and redeploy the serial-connector extension.

---

## Compile errors when rebuilding from source

See `stubs/README.md` for the most common ones (missing `Purgable`
interface = missing `donkey-server.jar`; missing
`net.miginfocom.layout.LC` = missing `miglayout-core-4.2.jar`; missing
`org.apache.log4j.Logger` = missing `log4j-1.2-api-2.17.2.jar`).

See `BUILD-INFO.txt` for the complete list of required Mirth jars and
their expected locations under `../mirth-libs/`.

---

## Still stuck?

1. Run `distribution/check_extension.sh` (shipped in this ZIP) — it
   verifies the deployed extension's structure.
2. Check `CHANGELOG.md` for the version history — your error may have
   been fixed in a specific version.
3. Run `distribution/build.sh test` to confirm the source compiles
   and the JUnit tests pass on your machine.
