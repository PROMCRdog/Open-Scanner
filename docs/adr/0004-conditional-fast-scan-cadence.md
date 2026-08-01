# ADR 0004: Conditional foreground five-second scan requests

Date: 2026-08-02
Status: accepted

## Context

Android normally limits foreground Wi-Fi scan requests. A developer can disable the platform's Wi-Fi scan-throttling switch for local testing, but that changes only the request quota: it does not promise that the radio, driver, or OEM framework will complete a new physical scan every five seconds. `WifiManager.startScan()` is asynchronous, and Android may reject a request, deliver a late callback, or expose cached results.

Open Scanner therefore needs to distinguish the cadence at which it asks Android for a scan from the age and identity of the results Android actually supplies. A faster timer must not manufacture history points or present cached observations as live measurements.

## Decision

- Support the exact requested intervals 5, 10, 15, 30, and 60 seconds. The default and invalid-value fallback remain 30 seconds; version 0.2.0 does not add a one-second mode.
- Offer the five-second option only while the platform capability has been discovered and explicitly reports that Wi-Fi scan throttling is off. An enabled or unavailable setting, including API 26–28 where Open Scanner cannot read it, leaves the option disabled with an accessible explanation.
- Do not reset a saved five-second preference while capability discovery is still in progress. After discovery, if throttling is enabled or unavailable, persist 30 seconds and reschedule immediately.
- Use one monotonic foreground scheduler with at most one scan request in flight. A request is targeted from the previous request start plus the selected interval. If a callback takes longer than the interval, the next request may start immediately after completion, but requests never overlap. Manual refreshes during an in-flight request are coalesced. A 15-second timeout releases a stalled request.
- Stop the scheduler when scanning is paused or the activity is not foreground-visible. This feature does not introduce a service, alarm, wake lock, background task, or second scan loop.
- Advance the snapshot sequence, capture timestamp, charts, and history only after a successful updated-results broadcast whose platform result timestamps advance, or after a successful newly empty result. Rejected requests, explicitly non-updated callbacks, unchanged timestamps, cached reads, and timeouts retain the prior observation identity and timestamps while updating request/connection evidence.
- Treat `likelyThrottled` as evidence about the latest request cycle, not a permanent diagnosis. Keep the configured Android throttle state separate from observed reuse or rejection.
- Label the setting **5 s request mode** and warn that it can increase battery use and that actual result cadence remains hardware/OEM-dependent.

## Consequences

- “5 s” describes a best-effort request cadence, never a guaranteed five-second radio measurement.
- The UI can show request state, actual source age, and stale/cached evidence without inventing samples.
- Five-second mode remains an explicit developer-oriented foreground option. Ordinary users retain the 30-second default and existing slower choices.
- No permission changes are required. The app continues to omit `INTERNET`, background location, storage, notification, VPN, and local-network permissions, and it sends no app-originated network traffic.
- Physical-device validation remains essential because scan completion latency and Developer Options behavior vary by Android version, chipset, and OEM.
