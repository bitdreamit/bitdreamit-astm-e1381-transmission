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
   ../mirth-libs/server/donkey-server.jar
   ../mirth-libs/client/mirth-client.jar
   ../mirth-libs/client/mirth-client-core.jar      # shared model classes
   ../mirth-libs/test/junit-4.13.2.jar
   ../mirth-libs/test/hamcrest-core-1.3.jar
   ```

2. In IntelliJ, declare two project-level libraries:
   - `mirth-server` = `mirth-server.jar` + `donkey-server.jar` + `mirth-client-core.jar`
   - `mirth-client` = `mirth-client.jar` + `mirth-client-core.jar`
   - `junit-4`      = `junit-4.13.2.jar` + `hamcrest-core-1.3.jar`

3. Open this folder in IntelliJ IDEA (`File → Open`).

4. Modules `shared`, `server`, `client`, `test` load automatically.

5. `Build → Build Project` (Ctrl+F9).

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
