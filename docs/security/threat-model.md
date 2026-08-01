# Threat model

## Assets

Raw and displayed SSIDs, BSSIDs, IP addresses, gateway/DNS addresses, scan timestamps, connection metadata, preferences, in-memory session logs, screenshots, and exports may reveal location or household/network context.

## In-scope threats

- Accidental sharing of identifiers through exports, screenshots, accessibility semantics, logs, crashes, clipboard, or backups.
- Stale or cached scans being presented as live measurements.
- A new dependency or permission silently expanding data collection or network access.
- Export encoders or UI state bypassing Privacy Mode.
- Unbounded histories, scan loops, or chart work causing battery/performance harm.
- A long logging session causing unbounded memory use or retaining identifiers after stop.
- An exported Android component exposing internal state.

## Current controls

- No `INTERNET`, storage, background-location, VPN, local-network, notification, or usage-access permission.
- `allowBackup=false`, restrictive data-extraction rules, and cleartext disabled. The launcher activity is the only app-defined exported component; AndroidX's exported profile-installer receiver is protected by the signature-level `android.permission.DUMP` permission.
- The scan coordinator is application-scoped and foreground-visible only.
- Sensitive model `toString()` methods redact identifiers.
- Production code emits no scan results to Android system logs, crash reporting, telemetry, or analytics.
- Privacy Mode transforms identifiers before Compose UI models.
- JSON/CSV encoders always redact internally and coarsen timestamps before preview.
- The explicit session logger fixes its selectable field set at start and creates immutable records only after applying salted stable SSID/BSSID session aliases, local-address masking, and minute-precision wall-clock reduction. It stores no raw SSID, BSSID, or local address value.
- Session logs are RAM-only and stop at 500 state records or 25,000 AP rows. Clear/replace actions are explicit; process exit clears the session.
- Approved exports are written under the app cache only after exact preview, shared through a non-exported `FileProvider` with a temporary read grant, and files older than 24 hours are removed on a later export.
- Histories are RAM-only and globally bounded to 128 networks, 60 points per network, and 30 minutes; process exit clears them.
- Android owns joining, credentials, captive-portal login, and Wi-Fi settings.
- Tests cover representative channel conversion, legacy and unknown security parsing, proportional spectrum-width truthfulness, paused/stale history windows and pruning, scan lifecycle serialization, deterministic connection selection, privacy transformation, and export leakage.

## Out of scope

A rooted or compromised OS, physical access to an unlocked device, radio-level anonymity, malicious recipients retaining a user-approved share, and screenshots taken while Privacy Mode is intentionally off.

## Future-feature gate

Any active DNS/HTTP/TCP test, LAN discovery, throughput test, VPN mode, external radio, durable saved session, raw export, or background session needs a separate threat-model update, permission review, explicit consent flow, and release evidence before implementation.
