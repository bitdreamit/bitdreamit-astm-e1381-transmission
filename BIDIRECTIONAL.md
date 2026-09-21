# Bidirectional Support (Host Query / Order Download) — v1.4.0

This plugin version closes the last two gaps that prevented 100% ASTM
bidirectional operation. It was verified against four analyzer manuals end to
end and the produced frames are **byte-identical** to the manuals' examples:

| Analyzer | Manual | Verified frames |
|---|---|---|
| Bio-Rad D-10 | L20017702 (LIS1-A / LIS2-A) | query + answer turn framing |
| Horiba/ABX Pentra 400 | RAA023JEN §6.1–6.3 | checksums 6A/7C/06 (query), 47/14/38/4F/A1/09 (order) |
| Maccura i-800 | LIS Protocol V1.0.00.221210 | TSREQ^REAL / TSDWN^REAL |
| Erba Lachema XL | ASTM Host Interface Document v2.0 | 1024-char packed frames |

## What changed

### 1. `framePackingMode` property (new)
- `record` (default) — one ASTM record per frame, ETX per record frame.
  Matches the D-10 / Pentra 400 / i-800 host examples; accepted everywhere.
- `packed` — records packed into frames up to *Max Frame Content Length*
  (set 1024 for Erba XL), CR-separated inside the frame, ETX on the last
  frame — the Erba XL / i-800 TSDWN style.

### 2. Line-contention yield
If the instrument seizes the line with ENQ exactly while the host is about to
send the order, the handler now ACKs the instrument, absorbs its complete
query transmission (until EOT) and only then starts the host answer turn —
per ASTM E1381 first-sender-owns-the-line. Previously this situation caused a
guaranteed collision.

## Channel wiring for bidirectional (same as before)

1. TCP Listener source with transmission mode **ASTM E1381**, Server mode.
2. Source *Response* = the destination whose reply must go back on the same
   socket (or postprocessor output). Mirth hands those bytes to
   `ASTME1381StreamHandler.write()`, which performs the full host answer turn
   (drain → ENQ → frames → EOT).
3. Transformer: on a `Q|` record, look up the order (e.g.
   `SELECT ... FROM lis_dimension_orders WHERE barcode=? AND status='NEW'`),
   build the patient/order records, set `status='ACCEPTED'`.
4. Result records (`R|`) are parsed and upserted
   (`INSERT ... ON DUPLICATE KEY UPDATE` keyed by barcode+test).

## Analyzer settings cheat sheet

| Analyzer | Frame Packing Mode | Max Frame Content Length |
|---|---|---|
| Bio-Rad D-10 | record | 240 |
| Pentra 400 | record | 240 |
| i-800 | record (or packed) | 240 |
| Erba XL | packed | 1024 |

The low-level layer is deliberately free of business data — what to answer a
query with stays 100% channel/transformer territory.
