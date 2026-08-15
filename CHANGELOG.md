# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
