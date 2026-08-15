# bitdreamit-astm-e1381-transmission

A production-ready **ASTM E1381-02 lower-layer transmission mode** plugin for
[Mirth® Connect](https://www.nextgen.com/solutions/health-data-platform/mirth-connect)
4.5+ and BridgeLink. It can be used in conjunction with the **TCP Listener / TCP
Sender** connectors, or the **Serial Connector** extension.

The plugin implements the full E1381-02 state machine: ENQ/ACK/NAK establishment,
STX / FN / payload / ETB|ETX / checksum / CR / LF framing, Add-Mod-256 (or XOR / None)
checksums, frame-number sequencing (0-7 with strict or lenient mode), per-frame
retry with exponential ENQ backoff, per-phase timeouts, and a thread-safe
`RetryMetrics` counter for operational dashboards.

> Protocol reference: <https://docs.nextgen.com/en-US/mirthc2ae-connect-user-guide-3299192/astm-e1381-transmission-mode-12617>

---

## Project layout

```
bitdreamit-astm-e1381-transmission/
├── shared/        # Constants, Properties, Frame, FrameException, RetryMetrics
│   └── src/.../shared/
├── server/        # TransmissionModePlugin, StreamHandler, ServerProvider
│   ├── resources/plugin.xml
│   └── src/.../server/
├── client/        # TransmissionModeClientPlugin, ClientProvider, SettingsPanel
│   ├── resources/plugin.xml
│   └── src/.../client/
├── test/          # JUnit tests for the shared module
├── distribution/  # build.sh - produces the three extension jars
├── transmissionmode.xml   # Mirth extension transmission-mode definition
├── pom.xml        # Optional Maven build (system-scoped deps to mirth-libs/)
├── Dockerfile     # Container image used for repeatable builds/tests
└── .github/workflows/ci.yml   # GitHub Actions CI
```

## Module dependency graph

```
            ┌─────────────────────────────┐
            │           shared            │  (Constants + Properties +
            │  (no Mirth server/client    │   Frame + FrameException +
            │   deps - only model classes) │   RetryMetrics)
            └──────────────┬──────────────┘
                  ┌────────┴────────┐
                  ▼                 ▼
            ┌──────────┐      ┌──────────┐
            │  server  │      │  client  │
            │ StreamH. │      │ Client   │
            │ Server   │      │ Provider │
            │ Plugin   │      │ UI Panel │
            └──────────┘      └──────────┘
                  ▲                 ▲
                  └────────┬────────┘
                           ▼
                        ┌─────┐
                        │ test│  (JUnit)
                        └─────┘
```

## IntelliJ IDEA Setup

1. Copy Mirth jars to a sibling `mirth-libs/` folder:
   ```
   ../mirth-libs/server/mirth-server.jar
   ../mirth-libs/server/donkey-server.jar        # REQUIRED - provides com.mirth.connect.donkey.util.purge.Purgable
   ../mirth-libs/client/mirth-client.jar
   ../mirth-libs/client/mirth-client-core.jar      # shared model classes (incl. TransmissionModeProperties)
   ../mirth-libs/client/miglayout-core-4.2.jar      # REQUIRED by the client SettingsPanel - provides net.miginfocom.layout.LC
   ../mirth-libs/client/miglayout-swing-4.2.jar     # provides net.miginfocom.swing.MigLayout
   ../mirth-libs/test/junit-4.13.2.jar
   ../mirth-libs/test/hamcrest-core-1.3.jar
   ```

2. In IntelliJ, declare three project-level libraries:
   - `mirth-server` = `mirth-server.jar` + `donkey-server.jar` + `mirth-client-core.jar`
   - `mirth-client` = `mirth-client.jar` + `mirth-client-core.jar` + `miglayout-core-4.2.jar` + `miglayout-swing-4.2.jar`
   - `junit-4`      = `junit-4.13.2.jar` + `hamcrest-core-1.3.jar`

   > **Critical:** BOTH MigLayout jars are required.
   > `miglayout-swing`'s `MigLayout` class internally references
   > `net.miginfocom.layout.LC` (which lives in `miglayout-core`).
   > Without `miglayout-core` on the classpath, javac fails with:
   > `cannot access net.miginfocom.layout.LC`.
   >
   > **Critical:** `donkey-server.jar` MUST be in the `mirth-server` library.
   > Without it, the `shared` module fails to compile with cascading
   > `cannot access com.mirth.connect.donkey.util.purge.Purgable` errors.
   > See **Troubleshooting** below.

3. Open this folder in IntelliJ IDEA (`File → Open`).

4. Modules `shared`, `server`, `client`, `test` load automatically.
   Each module's `.iml` file already declares the correct library dependencies:
   - `shared` → `mirth-server` + `mirth-client`
   - `server` → `shared` + `mirth-server`
   - `client` → `shared` + `mirth-client` + `mirth-server`
   - `test` → `shared` + `mirth-server` + `junit-4`

5. `Build → Build Project` (Ctrl+F9).

## Troubleshooting

### `CannotResolveClassException` at extension-install time (HTTP 500)

When installing the extension via the Mirth Administrator UI's
**Extensions → Extension Manager → Install** dialog, you may see:

```
Unable to install extension: Method failed: HTTP/1.1 500 Internal Server Error
Caused by: ... CannotResolveClassException:
  com.bitdreamit.connect.plugins.transmission.astm.server.ASTME1381TransmissionModePlugin
path: /pluginMetaData/serverClasses/serverClass
converter-type: com.mirth.connect.model.converters.FilterTransformerElementsConverter
```

**Root cause:** the `<serverClass>` and `<clientClass>` elements in
`plugin.xml` were using a `class="..."` attribute to specify the
plugin class name. Mirth's XStream-based `PluginMetaDataConverter`
maps the `name` attribute (not `class`) to the `PluginClass.name`
String field. The `class` attribute is XStream's built-in reserved
attribute for specifying the actual Java type to instantiate — so
when XStream encounters `class="com.bitdreamit....ASTME1381TransmissionModePlugin"`,
it tries to *resolve and instantiate* that Java class at
extension-install time, before the JARs are on the classpath.
Result: `CannotResolveClassException`.

**Fix:** make sure you are using v1.1.8+ of this plugin, which
changed all three `plugin.xml` files to use `name="..."` instead of
`class="..."`:

```xml
<!-- WRONG (v1.0.0 - v1.1.7) -->
<serverClass class="com.bitdreamit.connect.plugins.transmission.astm.server.ASTME1381TransmissionModePlugin">

<!-- CORRECT (v1.1.8+) -->
<serverClass name="com.bitdreamit.connect.plugins.transmission.astm.server.ASTME1381TransmissionModePlugin">
```

After the fix, XStream parses `plugin.xml` correctly: the `name="..."`
attribute is mapped to the `PluginClass.name` String field, no class
instantiation is attempted at install time, and the extension's JARs
are loaded onto the server-side and client-side classpaths. The
`ASTME1381TransmissionModePlugin` class is then resolvable when
Mirth's connector framework needs it at runtime.

### `cannot access net.miginfocom.layout.LC`

The `client` module's `ASTME1381TransmissionModeSettingsPanel` imports
`net.miginfocom.swing.MigLayout` (from `miglayout-swing-4.2.jar`).
MigLayout 4.2 ships as **two** jars:

- `miglayout-core-4.2.jar` — contains `net.miginfocom.layout.*`
  (the `LC`, `AC`, `CC` constraint classes)
- `miglayout-swing-4.2.jar` — contains `net.miginfocom.swing.MigLayout`

The swing jar's `MigLayout` class internally references
`net.miginfocom.layout.LC` (from the core jar) at construction time,
so BOTH jars must be on the compile classpath. If `miglayout-core` is
missing, javac fails with:

```
java: cannot access net.miginfocom.layout.LC
  class file for net.miginfocom.layout.LC not found
```

**Fix:** locate `miglayout-core-4.2.jar` in your Mirth Connect
installation (`$MIRTH_HOME/client/lib/miglayout-core-4.2.jar` or
similar) and copy it to `../mirth-libs/client/miglayout-core-4.2.jar`.
Then in IntelliJ: File → Project Structure → Libraries → `mirth-client`
→ click `+` → attach the jar.

The IntelliJ project library definition in `.idea/libraries/mirth_client.xml`
already references both jars; you just need the actual files on disk.

### `cannot access com.mirth.connect.donkey.util.purge.Purgable`

This is the most common build failure. It happens when `donkey-server.jar`
is missing from the compile classpath. The parent class
`TransmissionModeProperties` (in `mirth-client-core.jar`) implements
`Purgable` (in `donkey-server.jar`), so both jars must be available.

**Symptoms:**
```
ASTME1381TransmissionModeProperties.java:33:8
java: cannot access com.mirth.connect.donkey.util.purge.Purgable
  class file for com.mirth.connect.donkey.util.purge.Purgable not found
ASTME1381TransmissionModeProperties.java:83:5
java: method does not override or implement a method from a supertype
ASTME1381TransmissionModeProperties.java:132:63
java: cannot find symbol
  symbol:   variable this
  location: class com.bitdreamit.connect.plugins.transmission.astm.shared.ASTME1381TransmissionModeProperties
...
ASTME1381TransmissionModeProperties.java:264:39
java: cannot find symbol
  symbol:   method getPluginPointName()
```

**Fix (preferred):** locate `donkey-server.jar` in your Mirth Connect
installation (`$MIRTH_HOME/lib/donkey-server.jar` or
`$MIRTH_HOME/lib/extensions/server/donkey-server.jar`) and copy it to
`../mirth-libs/server/donkey-server.jar`. Then in IntelliJ:
File → Project Structure → Libraries → `mirth-server` → click `+` →
attach the jar.

**Fix (fallback):** if you genuinely cannot obtain `donkey-server.jar`,
add the `stubs/` directory to your compile source roots. See
[`stubs/README.md`](stubs/README.md) for details. The stub interface
lets you compile, but the real Mirth Connect server still needs the
real `Purgable` class at runtime.

### `method does not override or implement a method from a supertype`

There are TWO different root causes for this error in this project:

1. **On `getPurgedProperties()`** (or all three methods at once) -
   always a downstream symptom of the `Purgable` issue above. Once
   `donkey-server.jar` is on the classpath, these errors disappear.

2. **On `getPropertyDescriptors()` and/or `setProperties(Map)` only** -
   the Mirth Connect 4.5+ `TransmissionModeProperties` base class does
   NOT declare these methods. They belong to the unrelated
   `PropertyVerifier` interface used by **DataType** properties
   (HL7V2, XML, ...), not by transmission-mode properties. The original
   plugin author copy-pasted them from a data type plugin and annotated
   them with `@Override`, but the annotations were always wrong.

   **Fix:** make sure you are using v1.1.2+ of this plugin, which
   removes the `@Override` annotations from both methods. The methods
   themselves are retained (the settings panel uses
   `getPropertyDescriptors()` as a single source of truth for field
   metadata; external tooling may call `setProperties(Map)` to bulk-load
   the bean). Mirth's channel XML serializer does NOT call either
   method - it populates the bean via the individual field setters
   (e.g. `setEnquiryByte(int)`) using XStream.

### `TransmissionModePlugin` vs `TransmissionModeClientPlugin` vs `TransmissionModeProvider`

In Mirth Connect 4.5+ the abstract base classes for transmission-mode plugins
have **similar but opposite** names to what the original v1.0.x / v1.1.x
plugin code assumed:

| Side | Original code assumed | Actual Mirth 4.5+ class |
|------|----------------------|--------------------------|
| Server | `TransmissionModeServerProvider` (does NOT exist) | `com.mirth.connect.plugins.TransmissionModeProvider` |
| Server | `TransmissionModePlugin` (exists, but is the CLIENT-side class!) | `TransmissionModeProvider` |
| Client | `TransmissionModeClientPlugin` (does NOT exist) | `com.mirth.connect.plugins.TransmissionModePlugin` |

So:
- `com.mirth.connect.plugins.TransmissionModePlugin` is the **CLIENT-side**
  abstract class. It extends `ClientPlugin` and declares the abstract method
  `createProvider()` which returns a `TransmissionModeClientProvider`.
- `com.mirth.connect.plugins.TransmissionModeProvider` is the **SERVER-side**
  abstract class. It implements `ServerPlugin` and declares the abstract
  method `getStreamHandler(InputStream, OutputStream, BatchStreamReader,
  TransmissionModeProperties)`.
- `com.mirth.connect.plugins.TransmissionModeClientProvider` is the abstract
  base class for client-side provider instances (the things that actually
  drive the `send(OutputStream, InputStream, byte[])` flow). It declares
  the abstract method `getSampleValue()`.

v1.1.3 of this plugin fixes all three mismatches:
- `ASTME1381TransmissionModePlugin` (server) now extends
  `TransmissionModeProvider`.
- `ASTME1381TransmissionModeClientPlugin` (client) now extends
  `TransmissionModePlugin` and overrides `createProvider()`.
- `ASTME1381ClientProvider` now overrides the required `getSampleValue()`
  abstract method.
- `ASTME1381ServerProvider` (which extended a non-existent
  `TransmissionModeServerProvider`) has been DELETED. The receive flow
  already lives in `ASTME1381StreamHandler.read()`.

### `cannot find symbol: class TransmissionModeServerProvider`

The original v1.0.x / v1.1.x plugin code referenced a class named
`TransmissionModeServerProvider` in the `com.mirth.connect.plugins`
package. That class does NOT exist in any released Mirth Connect
version. The fix is to delete the file `ASTME1381ServerProvider.java`
(which extends the non-existent class) and rely on the existing
`ASTME1381StreamHandler.read()` for the receive flow. v1.1.3 does this
automatically.

### `does not override abstract method getSampleValue() in TransmissionModeClientProvider`

`com.mirth.connect.plugins.TransmissionModeClientProvider` declares
the abstract method `getSampleValue()` (which returns a `String`).
The original `ASTME1381ClientProvider` did not override it, so the
class would not compile. v1.1.3 adds the missing override, returning a
minimal ASTM E1381-02 sample payload (Header / Patient / Order /
Result / Terminator records) for Mirth's "Send Test Message" feature.

### `does not override abstract method getSampleLabel() in TransmissionModeClientProvider`

In addition to `getSampleValue()`, the parent class also declares
`getSampleLabel()` (also returning a `String`). This is the label
shown next to the "Send Test Message" button in the Mirth channel
editor. v1.1.4 adds the missing override, returning
`"ASTM E1381 Sample"`.

### `method does not override or implement a method from a supertype` on `send()`

In Mirth Connect 3.x / 4.x, **`TransmissionModeClientProvider` does NOT
declare a `send()` method**. The wire protocol is the **server side's**
responsibility:

| Side | Class | Has `send()`? |
|------|-------|---------------|
| Client (Administrator UI) | `TransmissionModeClientProvider` | **NO** |
| Server (Mirth Server process) | `ASTME1381StreamHandler extends StreamHandler` | **YES** — via `write(byte[])` |

The original v1.1.4 / v1.1.5 plugin code mistakenly put a `send()`
method on `ASTME1381ClientProvider`, mirroring logic that already lived
in `ASTME1381StreamHandler.write(byte[])`. Because Mirth 4.5.2's
`TransmissionModeClientProvider` does not declare a `send()` method,
the `@Override` annotation on the duplicate `send()` failed to compile.

**Fix:** make sure you are using v1.1.6+ of this plugin, which removes
the entire `send()` method (and all its helpers) from
`ASTME1381ClientProvider`. The full ASTM E1381-02 wire protocol
remains in `ASTME1381StreamHandler.write(byte[])` (server side) — no
functionality is lost.

### Architectural separation: client UI vs. server wire protocol

Mirth Connect 3.x / 4.x separates transmission-mode logic into two
halves that run in different JVMs:

- **Client side** (runs inside the Mirth Administrator UI process on
  the user's desktop): `ASTME1381TransmissionModeClientPlugin` →
  `ASTME1381ClientProvider extends TransmissionModeClientProvider`.
  Handles: settings panel UI, sample message buttons, property
  validation, default property provisioning. **Never sends or
  receives bytes over the wire.**

- **Server side** (runs inside the Mirth Server process):
  `ASTME1381TransmissionModePlugin extends TransmissionModeProvider`
  returns an `ASTME1381StreamHandler extends StreamHandler` from its
  `getStreamHandler(...)` factory. The Mirth server process then calls
  `StreamHandler.read()` and `StreamHandler.write(byte[])` to move
  bytes over TCP/Serial. All ENQ / ACK / NAK / EOT / framing /
  checksum logic lives here.

Do NOT put wire-protocol code in the client provider. If you find
yourself adding a `send()` method to a class that extends
`TransmissionModeClientProvider`, you are mixing the two halves —
move the code to the server-side `StreamHandler` instead.

## Build & Deploy

### Option A - IntelliJ IDEA Build Artifacts (recommended for development)

The project ships three pre-configured **Artifact** definitions in
`.idea/artifacts/`. Each produces one of the three extension JARs
that Mirth Connect expects:

| Artifact name | Output JAR | Contents |
|---------------|------------|----------|
| `bitdreamit-astm-e1381-transmission-shared` | `out/artifacts/bitdreamit_astm_e1381_transmission_shared/bitdreamit-astm-e1381-transmission-shared.jar` | shared module classes |
| `bitdreamit-astm-e1381-transmission-server` | `out/artifacts/bitdreamit_astm_e1381_transmission_server/bitdreamit-astm-e1381-transmission-server.jar` | shared + server classes + `server/resources/` |
| `bitdreamit-astm-e1381-transmission-client` | `out/artifacts/bitdreamit_astm_e1381_transmission_client/bitdreamit-astm-e1381-transmission-client.jar` | shared + client classes + `client/resources/` |

**To build all three JARs:**

1. In IntelliJ IDEA, open the project (`File → Open`).
2. Make sure all four modules compile cleanly:
   `Build → Rebuild Project` (Ctrl+Shift+F9).
3. From the menu: `Build → Build Artifacts...`
4. In the popup, pick **All Artifacts → Build** (or build each
   artifact individually: `Build`, then repeat for the other two).
5. The three JARs appear under `out/artifacts/`:

   ```
   out/artifacts/bitdreamit_astm_e1381_transmission_shared/bitdreamit-astm-e1381-transmission-shared.jar
   out/artifacts/bitdreamit_astm_e1381_transmission_server/bitdreamit-astm-e1381-transmission-server.jar
   out/artifacts/bitdreamit_astm_e1381_transmission_client/bitdreamit-astm-e1381-transmission-client.jar
   ```

**To rebuild a single artifact on every `Make Project`:**
right-click the artifact in `File → Project Structure → Artifacts`
and check **"Build on make"**, or set `build-on-make="true"` in the
artifact's XML file. Off by default so that `Make Project` does not
re-zip the JARs every time you save a source file.

**Editing the artifact definitions:**
`File → Project Structure → Artifacts` opens a visual editor. The
underlying XML files live at `.idea/artifacts/*.xml` and are committed
to version control so the whole team uses the same definitions.

### Option B - shell script (CI / headless builds)

```bash
cd distribution
chmod +x build.sh
./build.sh            # builds all three jars in ../out/
./build.sh clean      # remove ../out/
./build.sh test       # build + run JUnit tests
```

The shell script produces byte-identical JARs to the IntelliJ IDEA
Build Artifacts approach (same module outputs, same resource
directories, same JAR names). Use the IDE for development, use the
shell script for CI / Docker / headless builds.

### Option C - Maven (optional)

```bash
mvn -Dmirth.libs=$HOME/mirth-libs clean package
```
The `pom.xml` declares Mirth jars as `system`-scope dependencies under
`${mirth.libs}` so you don't need to install them to a local Maven repo.

> **Note:** Maven produces JARs at `<module>/target/*.jar` rather than
> `out/artifacts/`. The downstream Mirth deploy step is the same —
> just adjust the source path when copying to `$MIRTH_HOME/extensions/`.

### Deploy to Mirth

The production extension folder needs exactly **5 files**:

```
$MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/
├── plugin.xml                                       (extension descriptor)
├── transmissionmode.xml                             (transmission mode descriptor)
├── bitdreamit-astm-e1381-transmission-shared.jar    (shared classes)
├── bitdreamit-astm-e1381-transmission-server.jar    (server classes + shared)
└── bitdreamit-astm-e1381-transmission-client.jar    (client classes + shared)
```

**Two XML files, three JAR files — that's it.** The split `server-plugin.xml` +
`client-plugin.xml` layout used in older versions is no longer needed; Mirth
Connect 4.x reads a single consolidated `plugin.xml` that declares both
`<serverClasses>` and `<clientClasses>` blocks.

#### Option A - manual copy

```bash
MIRTH_HOME=/opt/mirth-connect
EXT_DIR=$MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission

mkdir -p $EXT_DIR
cp out/artifacts/bitdreamit_astm_e1381_transmission_shared/bitdreamit-astm-e1381-transmission-shared.jar  $EXT_DIR/
cp out/artifacts/bitdreamit_astm_e1381_transmission_server/bitdreamit-astm-e1381-transmission-server.jar $EXT_DIR/
cp out/artifacts/bitdreamit_astm_e1381_transmission_client/bitdreamit-astm-e1381-transmission-client.jar $EXT_DIR/
cp plugin.xml             $EXT_DIR/
cp transmissionmode.xml   $EXT_DIR/

sudo systemctl restart mirth-connect
```

#### Option B - deploy.sh helper (assembles + optionally zips/installs)

```bash
cd distribution
./deploy.sh             # build JARs + assemble out/bitdreamit-astm-e1381-transmission/
./deploy.sh zip         # also produce out/bitdreamit-astm-e1381-transmission.zip
./deploy.sh install     # also copy directly to $MIRTH_HOME/extensions/
                        #   (requires MIRTH_HOME env var)
```

After Mirth restarts, the new **ASTM E1381** transmission mode will appear in
the channel editor under **TCP Listener → Transmission Mode** or
**TCP Sender → Transmission Mode**.

## Features

### Protocol
- **ENQ/ACK/NAK establishment phase** with bounded timeout.
- **STX / FN / payload / ETB|ETX / checksum / CR / LF** transfer phase.
- **Frame numbering 0-7** with configurable start value (0 or 1) and
  strict (NAK on out-of-sequence) or lenient (accept any valid frame) modes.
- **Three checksum algorithms**: Add-Mod-256 (default per spec), XOR, None.
- **Per-frame retry** with exponential backoff on NAK / timeout.
- **EOT session termination**.
- **Transport-agnostic** - works with any `InputStream` / `OutputStream`
  (TCP, Serial RS-232, etc.).

### Production hardening (added in 1.1.0)
- Bounded ENQ establishment timeout (no infinite wait).
- Cap on consecutive NAKs to avoid spin loops on misbehaving peers.
- Thread-safe `RetryMetrics` with sent/received/retries/naks/enqRetries counters.
- Per-phase configurable timeouts (establishment, frame, response, ENQ, frame ACK).
- Log4j structured logging at INFO/WARN/ERROR for every state transition.
- Null-safe property parsing (never throws, never silently 0's defaults).
- Defensive copies of frame payloads to prevent aliasing bugs.
- Comprehensive JUnit coverage (frame round-trip, bad checksum, missing
  envelope, max-length, retry-metrics counters, etc.).
- CI workflow + Dockerfile for repeatable builds.

## Test

Run from IntelliJ (right-click `ASTME1381FrameTest` → Run) or via shell:

```bash
cd distribution
./build.sh test
```

Or manually:
```bash
JUNIT_JAR=../mirth-libs/test/junit-4.13.2.jar:../mirth-libs/test/hamcrest-core-1.3.jar
cd test
javac -cp ../out/shared:$JUNIT_JAR src/com/bitdreamit/connect/plugins/transmission/astm/test/*.java
java -cp ../out/shared:$JUNIT_JAR:src \
    org.junit.runner.JUnitCore \
    com.bitdreamit.connect.plugins.transmission.astm.test.ASTME1381FrameTest
```

## Configuration

All configurable properties are exposed via the Mirth channel editor's
"ASTM E1381" settings panel. Defaults follow the ASTM E1381-02 standard.

| Group       | Property                  | Default     | Description                                       |
|-------------|---------------------------|-------------|---------------------------------------------------|
| Frame       | ENQ                       | 0x05        | Enquiry byte                                      |
|             | STX                       | 0x02        | Start-of-frame byte                               |
|             | Max Frame Content Length  | 240         | Max payload bytes per frame                       |
|             | ETB                       | 0x17        | Intermediate-frame terminator                     |
|             | ETX                       | 0x03        | Final-frame terminator                            |
|             | Checksum Byte Length      | 2           | Bytes used to encode the checksum                 |
|             | Frame Terminator          | 0x0D0A      | Trailer after checksum (CR + LF)                  |
|             | EOT                       | 0x04        | End-of-transmission byte                          |
| Validation  | Validate Frame Number     | true        | Verify frame sequence numbers 0-7                 |
|             | Strict Frame Sequencing   | true        | NAK on out-of-sequence frames                     |
|             | Frame Number Start        | 1           | First frame number (0 or 1)                       |
|             | Ignore Server-Side Cancel | false       | Ignore EOT from sender mid-transfer              |
|             | Use Checksum              | true        | Enable frame checksum validation                  |
|             | Use Strict Validation     | false       | Enforce strict ASTM E1381 compliance              |
|             | Checksum Algorithm        | Add Mod 256 | Add Mod 256 / XOR / None                          |
|             | Bidirectional             | true        | Enable bidirectional communication                |
|             | ACK                       | 0x06        | Positive acknowledge byte                         |
|             | NAK                       | 0x15        | Negative acknowledge byte                         |
| Connection  | Max Transfer Attempts     | 6           | Maximum retry attempts per frame                  |
|             | Max ENQ Retries           | 6           | Maximum ENQ establishment retries                 |
|             | Max Frame Retries         | 6           | Per-frame ACK retry count                         |
|             | Establishment Timeout (ms)| 15000       | Timeout for connection establishment             |
|             | Contention Timeout (ms)  | 20000       | Timeout for line contention resolution            |
|             | Frame Timeout (ms)        | 30000       | Timeout waiting for complete frame                |
|             | Response Timeout (ms)     | 15000       | Timeout waiting for ACK/NAK response              |
|             | ENQ ACK Timeout (ms)      | 15000       | Timeout waiting for ACK to ENQ                    |
|             | Frame ACK Timeout (ms)    | 15000       | Timeout waiting for ACK to a data frame           |
| Mode        | Server Mode               | true        | true=Server (listener), false=Client (sender)     |

## Versioning

This project follows [Semantic Versioning](https://semver.org/). See
[CHANGELOG.md](CHANGELOG.md) for the history of releases.

## License

Apache License 2.0 - see [LICENSE](LICENSE).

## Contributing

Pull requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) first.
