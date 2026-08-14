# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
