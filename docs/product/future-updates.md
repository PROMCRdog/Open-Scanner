# Future Updates Backlog

Date: 2026-07-31

Implementation update: 2026-08-01

This document tracks agreed follow-on feature work and its implementation status. Each entry preserves the decision and constraints so completed and deferred updates remain traceable without re-deriving the rationale. Feasibility labels follow `full-toolkit-feature-set.md` (Stock / Constrained / Optional / Privileged / Excluded).

## 1. Channel-accurate band grouping

**Status: implemented.** Scan and Spectrum now use a shared channel-group model derived by validating each observation through `WifiChannelMapper`. The selector exposes 2.4 GHz, 5.2 GHz, 5.5 GHz / DFS, 5.8 GHz, and 6 GHz, and adds Unknown only when an unsupported frequency is actually observed. Spectrum axes and ticks are scoped to the selected group. Snapshot and session-log exports include the same group label.

Replace the current rough band grouping/labels derived from frequency ranges (for example "2.4G", "5.2G", "6G") with groups based on each AP's **validated Wi-Fi channel**. Keep the familiar high-level group names, but make their contents and labels channel-accurate.

Show APs in these top-level channel groups:

| Group | Channel membership |
|---|---|
| 2.4 GHz | Channels 1–14 |
| 5.2 GHz | 5 GHz low-band channels 34–64 |
| 5.5 GHz / DFS | 5 GHz mid-band channels 100–144 |
| 5.8 GHz | 5 GHz high-band channels 149–177 |
| 6 GHz | Valid mapped 6 GHz channels 1–233 |
| Unknown / unsupported frequency | Only when no valid channel mapping exists |

Frequency-to-group examples:

- 5180 MHz → 5.2 GHz, Channel 36
- 5240 MHz → 5.2 GHz, Channel 48
- 5745 MHz → 5.8 GHz, Channel 149
- 5805 MHz → 5.8 GHz, Channel 161
- 5865 MHz → 5.8 GHz, Channel 173

Constraints carried from the product rules:

- Membership is decided by the **validated channel mapping**, never by a raw frequency-range heuristic. An AP whose frequency does not map to a known channel goes to "Unknown / unsupported frequency" — it is never forced into the nearest group.
- The "5.5 GHz / DFS" label names a channel range, not a legal-usability claim. Regulatory domain and DFS usability remain unknowns per the existing rule that they are never silently guessed; UI copy must not imply these channels are usable or unusable for the user's router.
- Likely touch points when implemented: `core/domain/WifiChannelMapper.kt` (mapping/validation), scan-list grouping and band controls in `:app`, and the Spectrum tab's band organization.

## 2. Follow-on feature ideas

All are **Stock** — pure transformations of data the app already holds, with no new collection, permissions, or network traffic.

| Idea | Status | Summary | Rationale |
|---|---|---|---|
| AP stability / flapping indicator | **Implemented** | Derives a selected AP's RSSI range and absence share from up to 60 recent changed snapshots. Leading snapshots before first observation are excluded, and fewer than four assessed snapshots remain "Insufficient history." | Answers the roadmap's "how stable are they" question without presenting stability as throughput or connection quality. |
| Snapshot diff view | Deferred | Show what changed between refreshes or since app start: new APs, gone APs, channel changes. | Pure transformation of coordinator snapshots; useful for spotting router reboots or new hotspots. |
| Demo / fixture mode | Deferred | A synthetic scan-data source for screenshots, store listings, and CI UI tests. | Privacy-by-construction QA/marketing material (no real identifiers); makes throttle/stale/error states testable without hardware. |
| Localization | Deferred | Externalize strings and translate. | Not covered by any existing doc; cheapest to do before the string count grows. |
| Neighborhood posture summary | **Implemented** | A passive Tools summary counts observed APs by validated channel group, advertised security profile, and reported Wi-Fi generation. | Uses only already-parsed data and explicitly avoids identity, safety, or airtime claims. |
| 6 GHz PSC highlighting | Deferred | Mark PSC channels on the Spectrum tab. | PSC channels are a fixed standard set, not a regulatory guess, so this stays within the regulatory-uncertainty rule. |

The stability indicator uses explicit coarse thresholds: after at least four assessed snapshots and three present observations, an absence share of 25% or more is Flapping; otherwise any absence share above 10% or RSSI range above 10 dB is Variable; the remainder is Steady. The UI always shows the underlying present/assessed counts, absence share, and RSSI range and states that this is scan consistency rather than connection quality.

## 3. Wi-Fi session logging

**Status: implemented as a bounded, memory-only session workflow.** Tools now provides field selection, explicit start/stop, session statistics, clear/replace confirmation, an exact redacted preview, and text/JSON/CSV file export.

Privacy and lifecycle constraints:

- Record index and elapsed session time are always present; all other scan, AP, radio, security, generation, connection, link-speed, and local-address fields are individually selectable before start.
- The field set is fixed while a session is active. Raw SSIDs/BSSIDs are converted to salted, stable session aliases and masks; IP, gateway, and DNS values are masked; wall-clock start time is reduced to minute precision before the first record exists.
- Logging consumes scanner-state changes and samples the current foreground state at the requested refresh cadence so reused evidence remains explicit. It does not create another scan loop, background service, permission, or network request.
- A session stops at 500 state records or 25,000 AP rows. It is not a durable saved session and disappears with the app process unless the user explicitly exports it.
- Export uses a non-exported `FileProvider` with a temporary read grant. Cache files older than 24 hours are removed when another export is prepared.

### Priority guidance

- Finish the unfinished MVP items first (PNG export, saved sessions, aliases/favorites per the README known-limits list).
- Establish a trusted release channel (maintainer signing identity, reproducible builds) before expanding the feature set — the release build is deliberately unsigned today.
- The **Excluded** list in `full-toolkit-feature-set.md` remains binding. Also avoid hidden-SSID correlation and vendor inference from OUI prefixes; both conflict with the rule that identifiers are never treated as identity.
