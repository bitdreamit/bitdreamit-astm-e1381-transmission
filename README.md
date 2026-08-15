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
   ../mirth-libs/test/junit-4.13.2.jar
   ../mirth-libs/test/hamcrest-core-1.3.jar
   ```

2. In IntelliJ, declare three project-level libraries:
   - `mirth-server` = `mirth-server.jar` + `donkey-server.jar` + `mirth-client-core.jar`
   - `mirth-client` = `mirth-client.jar` + `mirth-client-core.jar`
   - `junit-4`      = `junit-4.13.2.jar` + `hamcrest-core-1.3.jar`

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

## Build & Deploy

### Option A - shell script

```bash
cd distribution
chmod +x build.sh
./build.sh            # builds all three jars in ../out/
./build.sh clean      # remove ../out/
./build.sh test       # build + run JUnit tests
```

### Option B - Maven (optional)

```bash
mvn -Dmirth.libs=$HOME/mirth-libs clean package
```
The `pom.xml` declares Mirth jars as `system`-scope dependencies under
`${mirth.libs}` so you don't need to install them to a local Maven repo.

### Deploy to Mirth

```bash
MIRTH_HOME=/opt/mirth-connect

mkdir -p $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission
cp out/bitdreamit-astm-e1381-transmission-shared.jar  $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/
cp out/bitdreamit-astm-e1381-transmission-server.jar $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/
cp out/bitdreamit-astm-e1381-transmission-client.jar $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/
cp transmissionmode.xml                              $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/
cp server/resources/plugin.xml                       $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/server-plugin.xml
cp client/resources/plugin.xml                       $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/client-plugin.xml

# Restart Mirth service
sudo systemctl restart mirth-connect
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
