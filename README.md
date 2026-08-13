# bitdreamit-astm-e1381-transmission

ASTM E1381-02 low-level transmission mode plugin for Mirth Connect / BridgeLink.

## IntelliJ IDEA Setup

1. Copy Mirth jars to sibling `mirth-libs/` folder:
   ```
   ../mirth-libs/server/mirth-server.jar
   ../mirth-libs/server/donkey-server.jar
   ../mirth-libs/server/mirth-core.jar
   ../mirth-libs/client/mirth-client.jar
   ../mirth-libs/client/mirth-core.jar
   ../mirth-libs/test/junit-4.13.2.jar
   ../mirth-libs/test/hamcrest-core-1.3.jar
   ```

2. Open this folder in IntelliJ IDEA (`File → Open`).

3. Modules `shared`, `server`, `client`, `test` load automatically.

4. `Build → Build Project` (Ctrl+F9).

## Build & Deploy

```bash
cd distribution
chmod +x build.sh
./build.sh
```

Copy to Mirth extensions:
```bash
cp out/bitdreamit-astm-e1381-transmission-server.jar $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/
cp server/resources/plugin.xml $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/
cp out/bitdreamit-astm-e1381-transmission-client.jar $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/
cp client/resources/plugin.xml $MIRTH_HOME/extensions/bitdreamit-astm-e1381-transmission/
```

Restart Mirth service.

## Features
- ENQ/ACK/NAK establishment phase
- STX..ETB/ETX..checksum..CRLF transfer phase
- Frame numbering 0-7 with configurable start value (0 or 1)
- Per-frame retry with exponential backoff
- Strict vs. lenient frame sequencing mode
- Retry/NAK metrics hook for operational dashboards
- Transport-agnostic (Serial, TCP, any InputStream/OutputStream)

## Test
Run `ASTME1381FrameTest` via IntelliJ JUnit runner or:
```bash
cd test
javac -cp ../out/shared:../out/server:$JUNIT_JAR src/com/bitdreamit/connect/plugins/transmission/astm/test/*.java
java -cp ../out/shared:../out/server:$JUNIT_JAR:src org.junit.runner.JUnitCore com.bitdreamit.connect.plugins.transmission.astm.test.ASTME1381FrameTest
```
