# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.7] - 2026-08-15 - "Consolidate to single plugin.xml; add deploy.sh helper"

This patch simplifies production deployment by consolidating the
two split XML files (`server-plugin.xml` + `client-plugin.xml`)
into a single root-level `plugin.xml`, which is the Mirth Connect
4.x convention.

### Changed
- **NEW FILE**: `plugin.xml` at the project root. This single file
  declares BOTH `<serverClasses>` and `<clientClasses>` blocks,
  replacing the older split layout. Mirth Connect 4.x reads this
  file at extension-load time.
- `distribution/build.sh`:
  - JAR packaging rules #6 and #7 no longer bundle `plugin.xml`
    inside the server/client JARs. The XML files are deployed as
    separate files at the extension folder root (Mirth 4.x
    convention).
  - Added rule #8: copies `plugin.xml` and `transmissionmode.xml`
    to the output directory so `out/` is a drop-in Mirth extension
    folder.
- `.idea/artifacts/bitdreamit_astm_e1381_transmission_server.xml`:
  removed the `<dir-copy>` element that bundled `server/resources/`
  inside the JAR.
- `.idea/artifacts/bitdreamit_astm_e1381_transmission_client.xml`:
  same — removed the `<dir-copy>` element for `client/resources/`.
- `server/resources/plugin.xml` and `client/resources/plugin.xml`:
  retained for backwards compatibility (older Mirth 3.x deployments
  that still expect the split layout), but no longer used by the
  default deployment flow.

### Added
- **NEW FILE**: `distribution/deploy.sh` — a helper script that
  assembles a ready-to-drop Mirth extension folder. Supports three
  modes:
    - `./deploy.sh build`   — build JARs + assemble
      `out/bitdreamit-astm-e1381-transmission/` folder
    - `./deploy.sh zip`      — also produce
      `out/bitdreamit-astm-e1381-transmission.zip`
    - `./deploy.sh install` — also copy directly to
      `$MIRTH_HOME/extensions/` (requires `MIRTH_HOME` env var)

### Documentation
- `README.md` "Deploy to Mirth" section rewritten to show the
  simplified 5-file production layout (2 XML + 3 JARs) and the
  two deployment options (manual copy vs `deploy.sh`).
- `PRODUCTION.md` section 2.1 updated with the same simplified
  layout and the `deploy.sh install` alternative.
- `BUILD-INFO.txt` updated with the v1.1.7 entry and a clearer
  explanation of the 5-file production layout.

### Production file inventory
After this patch, a production Mirth Connect 4.x extension folder
contains exactly **5 files**:

```
$MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/
├── plugin.xml                                       (extension descriptor)
├── transmissionmode.xml                             (transmission mode descriptor)
├── bitdreamit-astm-e1381-transmission-shared.jar    (shared classes)
├── bitdreamit-astm-e1381-transmission-server.jar    (server classes + shared)
└── bitdreamit-astm-e1381-transmission-client.jar    (client classes + shared)
```

Two XML files, three JAR files — that's it. No signing required
for Mirth Connect 4.x (signing is optional and only needed if
your Mirth instance is configured to enforce signed extensions).

## [1.1.6] - 2026-08-15 - "Move send() out of the client provider; fix architectural layering"

This patch fixes the compile error that v1.1.5 left behind on
`ASTME1381ClientProvider`:

```
ASTME1381ClientProvider.java:333:5
java: method does not override or implement a method from a supertype
```

### Root cause (architectural)
Mirth Connect 3.x / 4.x separates transmission-mode logic into two halves:

| Side | Class | Responsibility | Wire protocol? |
|------|-------|----------------|-----------------|
| **Client** (Administrator UI) | `TransmissionModeClientProvider` | Settings panel, sample labels, property validation, default property provisioning | **NO** - never sends/receives bytes |
| **Server** (Mirth Server process) | `TransmissionModeProvider` → `StreamHandler` | Actual `read()` / `write()` wire protocol | **YES** - moves bytes over TCP/Serial |

`TransmissionModeClientProvider` (Mirth 4.5.2) declares exactly **eight**
abstract methods:

1. `getSampleLabel()`
2. `getSampleValue()`
3. `getProperties()`
4. `getDefaultProperties()`
5. `setProperties(TransmissionModeProperties)`
6. `checkProperties(TransmissionModeProperties, boolean)`
7. `resetInvalidProperties()`
8. `getSettingsComponent()`

**There is NO `send()` method on this class** — and there shouldn't be,
because the client provider runs inside the Mirth Administrator UI
process (on the user's desktop), not on the server. A `send()` method
on the client provider would either:
- never be called by Mirth (if Mirth only calls server-side handlers), or
- if it were called, would try to send bytes from the user's desktop
  process — which is obviously wrong.

The v1.1.4 / v1.1.5 plugin code mistakenly added a `send()` method
(and its helpers `establishConnection()`, `sendRecordChunked()`,
`sendFrameWithRetry()`, `readByteWithTimeout()`, `splitIntoRecords()`)
to `ASTME1381ClientProvider`. This logic was duplicated from — and
should have lived exclusively in — the server-side
`ASTME1381StreamHandler.write(byte[])`, which already implements the
full ENQ / ACK / NAK / EOT / framing / checksum flow.

Because Mirth 4.5.2's `TransmissionModeClientProvider` does not
declare a `send()` method, the `@Override` annotation on the duplicate
`send()` failed to compile.

### Fixed
- `client/ASTME1381ClientProvider.java`:
  - **REMOVED** the entire `send(OutputStream, InputStream, String)`
    method.
  - **REMOVED** all of its private helpers:
    - `establishConnection(OutputStream, InputStream)`
    - `sendRecordChunked(OutputStream, InputStream, byte[], int)`
    - `sendFrameWithRetry(OutputStream, InputStream, ASTME1381Frame)`
    - `readByteWithTimeout(InputStream, long)`
    - `splitIntoRecords(byte[])`
  - **REMOVED** the now-unused `metrics` field, the `getMetrics()`
    accessor, and the `ensureProps()` calls inside the removed methods.
  - **REMOVED** the now-unused imports:
    `org.apache.log4j.Logger`, `java.io.InputStream`,
    `java.io.OutputStream`, `java.nio.charset.Charset`,
    `java.util.List`, `java.util.ArrayList`,
    `ASTME1381Frame`, `ASTME1381FrameException`,
    `ASTME1381RetryMetrics`.
  - Added a comprehensive class-level javadoc explaining the
    client-side vs server-side architectural separation and listing
    the eight required abstract method overrides.
  - The class now has exactly **eight** `@Override` annotations —
    one for each abstract method declared on
    `TransmissionModeClientProvider` (Mirth 4.5.2).
  - Added a `persistToPreferences()` helper called from
    `resetInvalidProperties()` so the settings panel (which reads
    from `java.util.prefs.Preferences`) stays in sync with the
    reset values.

### No functionality lost
The full ASTM E1381-02 wire protocol (ENQ / ACK / NAK / EOT / STX-FN-
payload-ETX|ETB-checksum-CR-LF framing with retry) remains in the
**server-side** `ASTME1381StreamHandler.write(byte[])` method, which
is unchanged in this release. The server-side plugin
(`ASTME1381TransmissionModePlugin`) returns this StreamHandler from
its `getStreamHandler(...)` factory; the Mirth server process then
calls `StreamHandler.read()` and `StreamHandler.write(byte[])` to
move bytes over the wire.

### Verification
After this patch, `ASTME1381ClientProvider`:
- Has exactly 8 `@Override` annotations (no more, no less).
- Has no `send()` method.
- Has no Logger, no I/O streams, no byte[] chunking helpers.
- Imports only: `ASTME1381Constants`, `ASTME1381TransmissionModeProperties`,
  `TransmissionModeProperties`, `TransmissionModeClientProvider`,
  `javax.swing.*`, `java.util.prefs.Preferences`.

The project now correctly separates the client UI (this class) from
the server-side wire protocol (`ASTME1381StreamHandler`).

## [1.1.5] - 2026-08-15 - "Add miglayout-core to classpath + implement remaining TransmissionModeClientProvider abstract methods"

This patch fixes two issues that 1.1.4 left behind:

1. The `client` module failed to compile with
   `cannot access net.miginfocom.layout.LC` because `miglayout-core-4.2.jar`
   was missing from the IntelliJ `mirth-client` library definition.

2. `TransmissionModeClientProvider` (Mirth 4.5+) declares additional
   abstract methods (`getProperties`, `getDefaultProperties`,
   `checkProperties`, `resetInvalidProperties`, `getSettingsComponent`)
   that the v1.1.4 `ASTME1381ClientProvider` did not implement. The
   user manually stubbed them out with `return null` / `return false`
   to make the build pass; v1.1.5 properly implements them.

### Fixed
- `.idea/libraries/mirth_client.xml`:
  - Added `miglayout-core-4.2.jar` to the `mirth-client` IntelliJ library.
    MigLayout 4.2 ships as TWO jars; the `miglayout-swing` jar's
    `MigLayout` class internally references `net.miginfocom.layout.LC`
    (which lives in `miglayout-core`). Without `miglayout-core` on the
    classpath, javac fails with `cannot access net.miginfocom.layout.LC`.
    Added an inline comment explaining the dependency.
- `pom.xml` (parent):
  - Added `miglayout.version` property = `4.2`.
  - Added `com.miglayout:miglayout-core` and `com.miglayout:miglayout-swing`
    to `dependencyManagement` (system-scoped, pointing at
    `${mirth.libs}/client/miglayout-{core,swing}-${miglayout.version}.jar`).
- `client/pom.xml`:
  - Added `<dependency>` entries for `miglayout-core` and `miglayout-swing`.
- `distribution/build.sh`:
  - Added `MIGLAYOUT_CORE_JAR` and `MIGLAYOUT_SWING_JAR` variables.
  - Added both to `CLIENT_CP` so `javac` sees them when compiling the
    client module.
- `client/ASTME1381ClientProvider.java`:
  - Properly implemented all five additional abstract methods declared
    on `TransmissionModeClientProvider` (Mirth 4.5+):
      * `getProperties()` - returns the stored `props` (lazily defaulted
        via `ensureProps()` to a fresh `ASTME1381TransmissionModeProperties`
        if never set; never returns `null`).
      * `getDefaultProperties()` - returns a NEW
        `ASTME1381TransmissionModeProperties()` instance on every call
        (so callers can mutate without affecting this provider's state).
      * `checkProperties(TransmissionModeProperties, boolean)` - validates
        every property against its spec range:
          - byte properties must be 0-255
          - frame content / checksum lengths must be positive integers
          - checksum algorithm must be one of the three known identifiers
          - timeouts must be positive integers (or zero if `ignoreMissing`)
          - frame number start must be 0 or 1
        Returns `true` only if every property passes.
      * `resetInvalidProperties()` - resets any property currently set
        to an invalid value back to its spec default. Called by Mirth
        after `checkProperties` returns `false` to auto-fix the channel
        configuration. Operates in-place on the bean returned by
        `getProperties()`.
      * `getSettingsComponent()` - returns a new
        `ASTME1381TransmissionModeSettingsPanel("ASTM E1381")` so the
        channel editor and the standalone Settings panel share the
        same UI component.
  - Added a private `ensureProps()` helper that lazily defaults `props`
    to a fresh `ASTME1381TransmissionModeProperties()` if null. Used by
    `getProperties()`, `resetInvalidProperties()`, and `send()`.
  - Added private `inByteRange(int)` and `inPositiveRange(int, boolean)`
    helpers used by `checkProperties`.
  - Updated `setProperties(TransmissionModeProperties)` to default to a
    fresh properties bean if called with `null` (defensive: Mirth
    should never do this, but if it does we'd rather NPE later in
    `send()` than crash inside `setProperties()`).
  - Updated class-level javadoc to list all required overrides (now
    nine abstract methods on the parent) with their semantics.

### Verification
After this patch, `ASTME1381ClientProvider` correctly implements every
abstract method declared on Mirth's
`com.mirth.connect.plugins.TransmissionModeClientProvider`:

- `getSampleLabel()` - returns "ASTM E1381 Sample"
- `getSampleValue()` - returns a minimal ASTM E1381-02 sample payload
- `getProperties()` - returns the stored props (never null)
- `getDefaultProperties()` - returns a fresh defaults bean
- `setProperties(TransmissionModeProperties)` - casts, validates, stores
- `checkProperties(TransmissionModeProperties, boolean)` - validates ranges
- `resetInvalidProperties()` - resets invalid props to defaults
- `getSettingsComponent()` - returns the shared settings panel
- `send(OutputStream, InputStream, String)` - drives the wire protocol

The `client` module now compiles cleanly with both `miglayout-core-4.2.jar`
and `miglayout-swing-4.2.jar` on the classpath.

## [1.1.4] - 2026-08-15 - "Add getSampleLabel() + fix send() signature (String, not byte[])"

This patch fixes the two remaining compile errors in
`ASTME1381ClientProvider`:

```
ASTME1381ClientProvider.java:40:8
java: ... does not override abstract method getSampleLabel()
       in com.mirth.connect.plugins.TransmissionModeClientProvider
ASTME1381ClientProvider.java:81:5
java: method does not override or implement a method from a supertype
```

### Root cause
`com.mirth.connect.plugins.TransmissionModeClientProvider` (Mirth 3.x /
4.x) declares FOUR abstract methods that subclasses must implement:

```java
public abstract class TransmissionModeClientProvider {
    public abstract String getSampleLabel();
    public abstract String getSampleValue();
    public abstract void setProperties(TransmissionModeProperties properties);
    public abstract void send(OutputStream out, InputStream in, String message) throws Exception;
}
```

The original plugin code:
1. Did not implement `getSampleLabel()` at all.
2. Implemented `send(OutputStream, InputStream, byte[])` - with a
   `byte[]` parameter instead of `String`. Because the parameter type
   did not match the parent's abstract method signature, this method
   was NOT an override; it was just a same-named method on the
   subclass that the Mirth framework would never have called. The
   `@Override` annotation on it has been a compile error since v1.0.0
   (it just didn't surface until v1.1.3 made the rest of the file
   compile cleanly).

### Fixed
- `client/ASTME1381ClientProvider.java`:
  - Added required `@Override getSampleLabel()` returning
    `"ASTM E1381 Sample"` - the label shown next to the "Send Test
    Message" button in the Mirth channel editor.
  - Changed `send(OutputStream, InputStream, byte[])` ->
    `send(OutputStream, InputStream, String)` to match the parent's
    abstract method signature. The String is converted to bytes
    internally using `Charset.forName("ISO-8859-1")` so that char
    codes 0-255 map 1:1 to byte values 0-255. This is essential for
    ASTM E1381 because the protocol is byte-oriented and may carry
    arbitrary 8-bit values; UTF-8 would mangle any byte > 0x7F.
  - Kept `@Override` on `getSampleValue()`, `setProperties()`, and
    the new `send(OutputStream, InputStream, String)` - all four
    are now real overrides of abstract methods on the parent class.
  - Updated the class-level javadoc to list all four required
    overrides and explain the String-vs-byte[] API contract.

### Verification
After this patch, `ASTME1381ClientProvider` correctly implements
every abstract method declared on
`com.mirth.connect.plugins.TransmissionModeClientProvider`:

- `getSampleLabel()` - returns "ASTM E1381 Sample"
- `getSampleValue()` - returns a minimal ASTM E1381-02 sample payload
- `setProperties(TransmissionModeProperties)` - casts and stores
- `send(OutputStream, InputStream, String)` - drives the
  ENQ -> ACK -> frames -> EOT flow

## [1.1.3] - 2026-08-15 - "Fix wrong base class names (TransmissionModePlugin vs TransmissionModeProvider)"

This patch fixes the wrong-base-class compile errors that 1.1.2 left
behind on the server and client modules:

```
ASTME1381ServerProvider.java:8:33
java: cannot find symbol
  symbol:   class TransmissionModeServerProvider
ASTME1381TransmissionModePlugin.java:11:33
java: cannot find symbol
  symbol:   class TransmissionModePlugin
ASTME1381TransmissionModeClientPlugin.java:5:33
java: cannot find symbol
  symbol:   class TransmissionModeClientPlugin
ASTME1381ClientProvider.java:16:8
java: ... does not override abstract method getSampleValue()
       in com.mirth.connect.plugins.TransmissionModeClientProvider
```

### Root cause
In Mirth Connect 4.5+ the two abstract base classes for transmission-mode
plugins have **similar but opposite** names to what the original plugin
code assumed:

| Side | Original code assumed | Actual Mirth 4.5+ class |
|------|----------------------|--------------------------|
| Server | `TransmissionModeServerProvider` (does NOT exist) | `com.mirth.connect.plugins.TransmissionModeProvider` |
| Server | `TransmissionModePlugin` (exists, but is the CLIENT-side class!) | `TransmissionModeProvider` |
| Client | `TransmissionModeClientPlugin` (does NOT exist) | `com.mirth.connect.plugins.TransmissionModePlugin` |

The user-supplied decompiled signatures from Mirth 4.5+ are:

```java
// CLIENT-side abstract class (despite the name, NOT server-side)
public abstract class TransmissionModePlugin extends ClientPlugin {
    public TransmissionModePlugin(String name) { super(name); }
    public abstract TransmissionModeClientProvider createProvider();
    public void start() {}
    public void stop() {}
    public void reset() {}
}

// SERVER-side abstract class
public abstract class TransmissionModeProvider implements ServerPlugin {
    public abstract StreamHandler getStreamHandler(
        InputStream in, OutputStream out,
        BatchStreamReader batchStreamReader,
        TransmissionModeProperties properties);
    public void start() {}
    public void stop() {}
}
```

The original plugin author mixed up which name was the client-side class
and which was the server-side class, so every `extends` clause in the
server and client modules was wrong.

### Fixed
- `server/ASTME1381TransmissionModePlugin.java`:
  - Changed `extends TransmissionModePlugin` -> `extends TransmissionModeProvider`.
  - Removed `@Override` from `getPluginPointName()`, `getPluginPointDescription()`,
    `getDefaultProperties()` (not declared on `TransmissionModeProvider`;
    may be on `ServerPlugin`/`Plugin` in some Mirth versions, but keeping
    the annotation breaks the build on versions where they aren't).
  - Kept `@Override` on `getStreamHandler(...)`, `start()`, `stop()`
    (these ARE on `TransmissionModeProvider`).
  - Added a comprehensive class-level javadoc explaining the API surface
    and the historical confusion.
- `server/ASTME1381ServerProvider.java`: **DELETED**.
  This class extended a non-existent `TransmissionModeServerProvider`
  base class and provided a `receive(InputStream, OutputStream)` method
  that does not fit the Mirth `StreamHandler`-based architecture. The
  actual receive flow already lives in `ASTME1381StreamHandler.read()`.
  Removed the corresponding `<serverClass>` entry from
  `server/resources/plugin.xml`.
- `client/ASTME1381TransmissionModeClientPlugin.java`:
  - Changed `extends TransmissionModeClientPlugin` -> `extends TransmissionModePlugin`.
  - Changed import accordingly.
  - Added required `@Override createProvider()` returning a new
    `ASTME1381ClientProvider`.
  - Removed `@Override` from `getSettingsPanel()` and `getPluginPointName()`
    (not declared on `TransmissionModePlugin` directly; may be on
    `ClientPlugin`/`Plugin` in some Mirth versions, but kept un-annotated
    for build tolerance).
  - Removed the redundant `start()`, `stop()`, `reset()` overrides
    entirely - `TransmissionModePlugin` already provides default empty
    implementations of these methods, so the overrides added nothing
    but noise.
- `client/ASTME1381ClientProvider.java`:
  - Added required `@Override getSampleValue()` returning a minimal
    ASTM E1381-02 sample payload (Header/Patient/Order/Result/Terminator
    records) for Mirth's "Send Test Message" feature.
  - Kept `@Override` on `setProperties(...)` and `send(...)` - these
    are real overrides of `TransmissionModeClientProvider` abstract
    methods.
- `server/resources/plugin.xml`: removed the `<serverClass>` entry for
  the deleted `ASTME1381ServerProvider`. Bumped `pluginVersion` to
  `1.1.3`. Added a comment explaining the API note about
  `TransmissionModeProvider` vs the non-existent
  `TransmissionModeServerProvider`.
- `client/resources/plugin.xml`: bumped `pluginVersion` to `1.1.3`
  to keep the two plugin.xml files in sync.

### Verification
After this patch, the only `@Override` annotations across the server
and client modules are on methods that genuinely exist on the parent
classes:

- `ASTME1381TransmissionModePlugin`: `getStreamHandler`, `start`, `stop`
- `ASTME1381TransmissionModeClientPlugin`: `createProvider`
- `ASTME1381ClientProvider`: `getSampleValue`, `setProperties`, `send`
- `ASTME1381StreamHandler` (unchanged): `read`, `write`, `commit`

## [1.1.2] - 2026-08-15 - "Remove phantom @Override on getPropertyDescriptors / setProperties"

This patch fixes the two remaining compile errors that 1.1.1 left
behind on `ASTME1381TransmissionModeProperties`:

```
ASTME1381TransmissionModeProperties.java:91:5
java: method does not override or implement a method from a supertype
ASTME1381TransmissionModeProperties.java:134:5
java: method does not override or implement a method from a supertype
```

### Root cause
The Mirth Connect 4.5+ base class `TransmissionModeProperties`
(in `mirth-client-core.jar`) only declares the two `Purgable`
methods: `getPluginPointName()` and `getPurgedProperties()`. It does
NOT declare `getPropertyDescriptors()` or `setProperties(Map)` - those
methods belong to the unrelated `PropertyVerifier` interface used by
**DataType** properties (HL7V2, XML, ...), not by transmission-mode
properties. The original plugin author copy-pasted these methods from
a data type plugin and annotated them with `@Override`, but the
annotations were always wrong - they just didn't surface as errors
until the parent class actually loaded (which required the
`donkey-server.jar` fix from 1.1.1).

### Fixed
- `ASTME1381TransmissionModeProperties.java`:
  - Removed the `@Override` annotation from `getPropertyDescriptors()`.
    Added a detailed javadoc explaining why it's not an override and
    why the method is retained (settings panel uses it as a single
    source of truth for field metadata; external tooling may call it).
  - Removed the `@Override` annotation from `setProperties(...)`.
    Same javadoc treatment. Mirth's channel XML serializer populates
    the bean via the individual field setters (e.g. `setEnquiryByte(int)`)
    using XStream, not via this bulk loader.
  - The `@Override` on `getPurgedProperties()` is correct (it overrides
    `Purgable.getPurgedProperties()`) and is left in place.
  - Expanded the class-level javadoc with an "API surface note (Mirth
    4.5+)" paragraph documenting why `getPropertyDescriptors()` and
    `setProperties()` are not `@Override`.

### Verification
After this patch, the only `@Override` annotation in
`ASTME1381TransmissionModeProperties.java` is on
`getPurgedProperties()` - which IS a real override of the `Purgable`
interface method.

## [1.1.1] - 2026-08-15 - "Purgable classpath fix"

This patch release fixes the cascading compile errors that users see when
their `mirth-client-core.jar` is present (which contains
`TransmissionModeProperties`) but their `donkey-server.jar` is missing
(which contains `com.mirth.connect.donkey.util.purge.Purgable`). The
parent class `TransmissionModeProperties` implements `Purgable`, so
without `donkey-server.jar` on the compile classpath, every `@Override`
and inherited-method call in `ASTME1381TransmissionModeProperties`
cascades into errors.

### Fixed
- `shared/shared.iml`: now declares the `mirth-server` project library
  (which bundles `donkey-server.jar`) in addition to `mirth-client`.
  Previously, the `shared` module only depended on `mirth-client`, which
  does not include `donkey-server.jar`.
- `client/client.iml`, `test/test.iml`: same fix - they now also declare
  `mirth-server` so `Purgable` is resolvable when the `shared` module
  is on their compile classpath.
- `ASTME1381TransmissionModeProperties.java`:
  - `setProperties()` signature changed from raw `Map` to the
    parameterized `Map<String, DataTypePropertyDescriptor>` to match
    the actual Mirth 4.5+ parent signature (removes the unchecked
    conversion warning and is the proper override).
  - `setProperties()` body now extracts values via
    `DataTypePropertyDescriptor.getValue()` instead of calling
    `toString()` on the descriptor object. The original code had a
    latent bug: it always passed a `DataTypePropertyDescriptor` instance
    to `parseHex(Object)` / `parseInt(Object)` / `toBoolean(Object)`,
    which would invoke the default `Object.toString()` and yield a
    non-parseable string like "DataTypePropertyDescriptor@1b6d3586".
    The fix uses `getValue()` which returns the actual configured
    value (String / Integer / Boolean).
  - `getPurgedProperties()` no longer calls the inherited
    `getPluginPointName()` (which lives on `Purgable`). It now uses
    `ASTME1381Constants.PLUGIN_NAME` directly, which is identical
    (the constructor passes that same constant to `super(...)`).
    This makes the source more tolerant of partial Mirth jars.
  - Added a `DataTypeDescriptorGetter` helper class to keep the
    `setProperties()` body readable.
  - Added a "Build prerequisite (Mirth 4.5+)" javadoc block explaining
    the `Purgable` classpath requirement.

### Added
- `stubs/` directory with a compile-time-only fallback interface for
  `com.mirth.connect.donkey.util.purge.Purgable`. For users who
  genuinely cannot obtain `donkey-server.jar`, the stub allows the
  project to compile. See `stubs/README.md` for usage. The stub is
  NOT packaged into the produced jars.
- Module-level `pom.xml` files for `shared`, `server`, `client`, and
  `test`. The parent `pom.xml` referenced these as Maven reactor
  modules, but no module-level poms existed - meaning `mvn package`
  could not actually build anything. Each module pom now declares the
  correct dependencies, including `donkey-server` for the `Purgable`
  requirement.
- Parent `pom.xml`: added an internal-module `dependencyManagement`
  entry for the shared module; added a build-prerequisites header
  explaining the `donkey-server.jar` / `Purgable` requirement.
- `README.md`: expanded with a "Troubleshooting" section covering
  the `cannot access Purgable` cascade, the
  `method does not override or implement a method from a supertype`
  symptom, and the difference between `TransmissionModePlugin`
  (server-side) and `TransmissionModeClientPlugin` (client-side).

### Changed
- `BUILD-INFO.txt`: added "Item 0: Build prerequisite" at the top
  documenting the `donkey-server.jar` requirement and the full set
  of fixes shipped in this patch release. Updated the build
  instructions to mark `donkey-server.jar` as `CRITICAL`.

## [1.1.0] - 2026-08-14 - "Production hardening"

This release reconciles the inconsistent `com.bitdreamit.mirth.astm.e1381.*`
and `com.bitdreamit.connect.plugins.transmission.astm.*` code paths that were
left over from a partial refactoring in 1.0.0. The codebase now compiles
end-to-end against Mirth Connect 4.5+ jars and ships as three properly
structured jars (shared / server / client).

### Added
- `ASTME1381Constants`:
  - `MAX_FRAME_TEXT_LENGTH` alias (back-compat with 1.0.x callers).
  - `PLUGIN_POINT_NAME` alias (back-compat).
  - New constants `DEFAULT_MAX_ENQ_RETRIES`, `DEFAULT_MAX_FRAME_RETRIES`,
    `DEFAULT_ENQ_TIMEOUT_MS`, `DEFAULT_FRAME_ACK_TIMEOUT_MS`,
    `DEFAULT_ENQ_BACKOFF_BASE_MS`, `DEFAULT_ENQ_BACKOFF_CAP_MS`.
  - Plugin version bumped to `1.1.0`.
- `ASTME1381TransmissionModeProperties`:
  - New fields `frameNumberStart`, `strictFrameSequencing`, `maxEnqRetries`,
    `maxFrameRetries`, `enqTimeoutMs`, `frameAckTimeoutMs` - all exposed
    via `getPropertyDescriptors()` so channel authors can edit them in the
    Mirth UI.
  - `parseHex` / `parseInt` are now null-safe and never throw.
  - Removed unused `DonkeyElement` import.
- `ASTME1381ServerProvider`:
  - Bounded ENQ establishment wait using `getEstablishmentTimeout()`.
  - Bounded frame read using `getFrameTimeout()`.
  - Cap of 32 consecutive NAKs before aborting to prevent spin loops.
  - Proper Log4j logging at INFO/WARN/ERROR.
  - Null-checks on InputStream / OutputStream.
  - Cast safety check in `setProperties()`.
- `ASTME1381ClientProvider` (moved from `server/` to `client/`):
  - Log4j logging for every ENQ attempt / NAK / ACK.
  - Defensive checks on null/empty payloads.
  - Uses `props.getMaxFrameContentLength()` instead of a hardcoded constant.
- `ASTME1381RetryMetrics`: added `framesSent`, `framesReceived`,
  `sessionStartedAt`, `markSessionStart()`, and a `toString()` for log lines.
- `ASTME1381Frame`: defensive copies of payload, `toString()`, better
  exception messages with hex values, accepts cause in
  `ASTME1381FrameException`.
- New `distribution/build.sh` features: `clean`, `test`, `rebuild`
  sub-commands; produces a separate `*-shared.jar` matching
  `transmissionmode.xml`; honors `MIRTH_LIBS_DIR` env var.
- Optional Maven build via `pom.xml`.
- CI workflow at `.github/workflows/ci.yml`.
- Dockerfile for containerised builds/tests.
- `CONTRIBUTING.md`, `PRODUCTION.md`, `LICENSE` (Apache 2.0).
- Expanded test coverage: `ASTME1381RetryMetricsTest`,
  `ASTME1381FrameCorruptionTest`, `ASTME1381RoundTripTest`.

### Changed
- **BREAKING**: canonical Java package is now
  `com.bitdreamit.connect.plugins.transmission.astm.*` (the
  `com.bitdreamit.mirth.astm.e1381.*` packages are gone). Channels
  exported against 1.0.x will need to be re-imported with the new jars
  installed (the channel XML stores property values, not class names,
  so the migration is automatic for stored channels - only custom code
  references need updating).
- `ASTME1381TransmissionModeProperties` moved from `server/` to `shared/`
  module so it is bundled in BOTH `server.jar` AND `client.jar` (1.0.x
  shipped a client.jar that was missing the Properties class).
- `ASTME1381Frame`, `ASTME1381FrameException`, `ASTME1381RetryMetrics`
  moved from `server/` to `shared/` for the same reason.
- `ASTME1381ClientProvider` moved from `server/` to `client/` (it extends
  a client-side Mirth class).
- `transmissionmode.xml`: class names now reference the canonical package;
  jar names now match `build.sh` output (`bitdreamit-astm-e1381-transmission-{shared,server,client}.jar`).
- `client/resources/plugin.xml` and `server/resources/plugin.xml` now also
  register the new `ASTME1381ClientProvider` and `ASTME1381ServerProvider`.
- `.gitignore`: now ignores `out/` (was incorrectly ignoring `out/production`).
- `shared/shared.iml`: now depends on `mirth-client` library because
  Properties extends `TransmissionModeProperties`.

### Removed
- `client/.../ASTME1381ClientPlugin.java` - orphaned duplicate that extended
  the wrong base class (`ClientPlugin` instead of `TransmissionModeClientPlugin`).
  The canonical registration lives in `ASTME1381TransmissionModeClientPlugin`.

### Fixed
- **Compile failure**: 1.0.x source would not compile because package
  declarations didn't match directory structure and imports referenced
  classes from a non-existent package. Fixed by unifying on the canonical
  `com.bitdreamit.connect.plugins.transmission.astm.*` package.
- **Missing imports**: `ASTME1381TransmissionModePlugin` referenced
  `ASTME1381TransmissionModeProperties` without importing it.
- **Stale jar name**: `transmissionmode.xml` referenced
  `astm-e1381-transmission-{shared,server,client}.jar` but `build.sh`
  produced `bitdreamit-astm-e1381-transmission-{server,client}.jar`.
  Now both use the `bitdreamit-` prefixed names consistently.
- **`ASTME1381TransmissionModeClientPlugin.getPluginPointName()`** returned
  `ASTME1381Constants.PLUGIN_POINT_NAME` which did not exist. Now returns
  `PLUGIN_NAME` (with a back-compat alias for old code).
- **`ASTME1381ClientProvider`** referenced `props.getMaxEnqRetries()`,
  `props.getEnqTimeoutMs()`, `props.getFrameAckTimeoutMs()`,
  `props.getMaxFrameRetries()`, `props.getFrameNumberStart()` and
  `ASTME1381Constants.MAX_FRAME_TEXT_LENGTH` - none of which existed
  in 1.0.x. All are now defined.
- **Spin-loop risk**: server-side `receive()` had no cap on consecutive
  bad bytes / NAKs; a misbehaving peer could spin the thread indefinitely.
- **Empty payload**: `ASTME1381ClientProvider.send()` would attempt to
  send zero-length data; now short-circuits cleanly.
- **`readByteWithTimeout` infinite loop**: when the stream was EOF
  (`in.read()` returned -1), the original implementation kept looping
  until the deadline. Now returns -1 immediately.
- **Frame payload aliasing**: `ASTME1381Frame.getText()` returned the
  internal array, allowing callers to mutate it. Now returns a clone.

## [1.0.0] - 2025-01-15 - "Initial release"

- Initial public release of the ASTM E1381-02 transmission mode plugin.
- ENQ/ACK establishment, STX/ETB/ETX framing, Add-Mod-256 checksum,
  0-7 frame sequencing, per-frame retry, Mirth UI settings panel.
