# Production Deployment Guide

This document covers operational concerns when running the
`bitdreamit-astm-e1381-transmission` plugin in a production Mirth Connect
cluster.

## 1. Sizing

| Resource            | Minimum    | Recommended |
|---------------------|------------|-------------|
| Mirth Connect       | 4.5.0      | 4.5.2+      |
| JVM heap            | 2 GB       | 4-8 GB      |
| JVM version         | 8          | 17 (LTS)    |
| CPU cores           | 2          | 4+          |
| Disk (logs + spool) | 10 GB      | 100 GB SSD  |

## 2. Deployment

### 2.1 Standalone Mirth

```bash
# 1. Stop Mirth
sudo systemctl stop mirth-connect

# 2. Back up the existing extension (if upgrading)
EXT_DIR=/opt/mirth-connect/extensions/bitdreamit-astm-e1381-transmission
[ -d "$EXT_DIR.bak" ] && rm -rf "$EXT_DIR.bak"
[ -d "$EXT_DIR" ] && mv "$EXT_DIR" "$EXT_DIR.bak"

# 3. Install the new jars and metadata
mkdir -p "$EXT_DIR"
cp out/bitdreamit-astm-e1381-transmission-shared.jar  "$EXT_DIR/"
cp out/bitdreamit-astm-e1381-transmission-server.jar "$EXT_DIR/"
cp out/bitdreamit-astm-e1381-transmission-client.jar "$EXT_DIR/"
cp transmissionmode.xml                              "$EXT_DIR/"
cp server/resources/plugin.xml                       "$EXT_DIR/server-plugin.xml"
cp client/resources/plugin.xml                       "$EXT_DIR/client-plugin.xml"

# 4. Start Mirth
sudo systemctl start mirth-connect

# 5. Verify the extension loaded
sudo grep -A2 "ASTM E1381" /opt/mirth-connect/logs/mirth.log
```

### 2.2 Cluster (Mirth Connect Server Cluster)

Repeat the standalone procedure on every node. The extension is
stateless at the cluster level (no shared state in the plugin itself),
so a rolling restart is safe.

For an HA pair, the recommended upgrade sequence is:

1. Upgrade the **passive** node first.
2. Fail over so the passive node becomes active.
3. Verify the new node handles ASTM traffic correctly.
4. Upgrade the (now-passive) former active node.
5. Fail back if desired.

## 3. Channel configuration

### 3.1 TCP Listener source (incoming ASTM)

1. Create a new channel.
2. Source connector: **TCP Listener**.
3. Set the listening port (e.g. `5050`).
4. Transmission Mode: **ASTM E1381**.
5. Configure the settings panel:
   - Frame settings: leave defaults unless your instrument requires
     non-standard control bytes.
   - Validation → **Validate Frame Number** = true,
     **Strict Frame Sequencing** = true (instruments that follow the
     spec strictly), false (lenient mode for instruments that restart
     sequencing at 1 instead of 0).
   - Connection → **Establishment Timeout (ms)** = `15000` default,
     raise to `30000` for slow serial links.
   - Mode → **Server Mode** = true (we are the listener).

### 3.2 TCP Sender destination (outgoing ASTM)

1. Add a **TCP Sender** destination connector.
2. Set the target host:port (e.g. `instrument.local:5050`).
3. Transmission Mode: **ASTM E1381**.
4. Mode → **Server Mode** = false (we are the sender).
5. The other settings mirror the listener's defaults.

## 4. Monitoring

### 4.1 Log levels

By default the plugin logs at `INFO`. To increase verbosity for
troubleshooting, edit `$MIRTH_HOME/conf/log4j.properties`:

```properties
log4j.logger.com.bitdreamit.connect.plugins.transmission.astm=DEBUG
```

Restart Mirth for the change to take effect.

### 4.2 Operational counters

Each provider exposes a `RetryMetrics` object via `getMetrics()`:

| Counter          | Meaning                                                        |
|------------------|----------------------------------------------------------------|
| `framesSent`     | Number of frames the client successfully ACKed                 |
| `framesReceived` | Number of frames the server successfully decoded               |
| `frameRetries`   | Frames retransmitted due to NAK / timeout                       |
| `nakCount`       | Total NAKs sent or received                                    |
| `enqRetries`     | Number of ENQ retransmissions before establishment succeeded   |
| `sessionStartedAt` | Wall-clock time the current session started (epoch ms)       |

These counters reset at the start of each session. To expose them in
Prometheus, write a small Mirth channel that periodically calls
`channel.getConnector(...).getTransmissionModeProperties()` and
publishes the values via a Prometheus exporter.

### 4.3 Health checks

The simplest health check is a periodic TCP connect to the listener
port and sending an ENQ byte (`0x05`). The plugin should respond with
ACK (`0x06`). If no ACK is received within 5 seconds, the listener
is unhealthy.

## 5. Troubleshooting

| Symptom                                      | Likely cause                                 | Resolution                                                                 |
|----------------------------------------------|----------------------------------------------|----------------------------------------------------------------------------|
| `Frame missing STX envelope` errors in log   | Peer sends raw bytes without STX             | Verify the peer speaks ASTM E1381, not raw TCP                            |
| `Checksum mismatch` errors                   | Peer uses XOR / None checksum                | Change **Checksum Algorithm** to match the peer                           |
| `Out-of-sequence frame` warnings             | Peer starts sequencing at 0, plugin expects 1| Set **Frame Number Start** to 0, or disable **Strict Frame Sequencing**  |
| Listener hangs after ENQ                     | Peer never sends first frame                 | Lower **Frame Timeout (ms)** to fail faster                                |
| Sender fails with "No ACK to ENQ"            | Peer not listening, or NAK on ENQ            | Check peer is up; check peer's logs for the ENQ reception                  |
| High `frameRetries` count                    | Noisy serial line / network packet loss      | Check cabling / baud rate; reduce `Max Frame Retries` to fail faster      |
| Plugin not appearing in Mirth UI             | Extension metadata not copied                | Verify `transmissionmode.xml` and `plugin.xml` are in the extension dir   |

## 6. Backup

The plugin is stateless; the only persistent state is the channel
configuration that uses the plugin (exported as part of the Mirth
channel XML). No special backup procedure is required for the plugin
itself beyond keeping a copy of the deployed jars.

## 7. Upgrade path from 1.0.x

1. Review [CHANGELOG.md](CHANGELOG.md) for breaking changes.
2. Stop Mirth.
3. Back up the existing extension folder.
4. Replace the jars and metadata files.
5. Start Mirth.
6. Open each channel that uses ASTM E1381 and re-save it (this
   re-serialises the channel XML against the new Properties schema).
7. Deploy the updated channels.

Existing channels will continue to work because the property keys are
backwards-compatible. The only manual action needed is re-saving them
to pick up new defaults like `frameNumberStart` and `maxEnqRetries`.
