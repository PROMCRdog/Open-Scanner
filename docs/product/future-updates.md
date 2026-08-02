# Future Updates Backlog

Date: 2026-07-31

Implementation update: 2026-08-02

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
| Localization | **Implemented** | User-facing Compose copy is externalized with English and Simplified Chinese (`zh-CN`) resources; Settings can select System default, English, or Simplified Chinese, and the README is maintained in both languages. | System default remains the clean-install/reset value and follows later device-language changes; Android's per-app locale APIs own persistence and Android 13+ system-setting synchronization. |
| Neighborhood posture summary | **Implemented** | A passive Tools summary counts observed APs by validated channel group, advertised security profile, and reported Wi-Fi generation. | Uses only already-parsed data and explicitly avoids identity, safety, or airtime claims. |
| 6 GHz PSC highlighting | Deferred | Mark PSC channels on the Spectrum tab. | PSC channels are a fixed standard set, not a regulatory guess, so this stays within the regulatory-uncertainty rule. |

The stability indicator uses explicit coarse thresholds: after at least four assessed snapshots and three present observations, an absence share of 25% or more is Flapping; otherwise any absence share above 10% or RSSI range above 10 dB is Variable; the remainder is Steady. The UI always shows the underlying present/assessed counts, absence share, and RSSI range and states that this is scan consistency rather than connection quality.

## 2.1 Spectrum display scaling

**Status: implemented for the v0.2.1 candidate.** Spectrum no longer truncates a selected channel group to four curves. Every observed AP in the group is displayed by default. A per-group, in-memory multi-select filter lets the user show or hide individual curves, restore all curves, or keep only the focused AP. A separate radio control in the same selector chooses exactly one focused AP; changing focus never changes Android's Wi-Fi connection. The selector makes that single-choice rule explicit with a `FOCUS · ONE` heading, a cyan-highlighted focused row and badge, and muted but still selectable alternative radio controls. The focused AP remains pinned so the chart, summary, and overlap analysis do not silently refer to different networks.

The chart uses scalable visual roles rather than assigning an unlimited set of unrelated colors: cyan identifies focus, green identifies Android's current Wi-Fi connection, and the remaining displayed APs share a subdued purple role. If focus and connection refer to the same AP, a green peak marker preserves the independent connection status. The exact `CURRENT WI-FI` badge is also carried into the Spectrum selector and Track picker. For bonded widths, the footprint remains centered on Android's reported segment center while a dashed marker and text identify the primary channel. Curve visibility does not change the full-snapshot overlap analysis, and the selector says so explicitly.

## 3. Wi-Fi session logging

**Status: implemented as a bounded, memory-only session workflow.** Tools provides field selection, explicit start/stop, session statistics, clear/replace confirmation, and text/JSON/CSV export with an exact redacted-by-default preview.

Privacy and lifecycle constraints:

- Record index and elapsed session time are always present; all other scan, AP, radio, security, generation, connection, link-speed, and local-address fields are individually selectable before start.
- The field set and redaction choice are fixed while a session is active. Redacted sessions convert SSIDs/BSSIDs to salted, stable session aliases, mask local addresses, and reduce wall-clock start time to minute precision before the first record exists. Explicitly unredacted sessions retain selected raw values in memory.
- Logging consumes scanner-state changes and samples the current foreground state at the requested refresh cadence so reused evidence remains explicit. It does not create another scan loop, background service, permission, or network request.
- A session stops at 500 state records or 25,000 AP rows. It is not a durable saved session and disappears with the app process unless the user explicitly exports it.
- Export uses a non-exported `FileProvider` with a temporary read grant. Unredacted output is labelled in its payload and filename and requires an exact warning preview. Cache files are removed after one hour while the app remains open or on a later app start/export.

### Priority guidance

- Finish the unfinished MVP items first (PNG export, saved sessions, aliases/favorites per the README known-limits list).
- Maintain the trusted release channel and permanent maintainer signing identity before expanding the feature set.
- The **Excluded** list in `full-toolkit-feature-set.md` remains binding. Also avoid hidden-SSID correlation and vendor inference from OUI prefixes; both conflict with the rule that identifiers are never treated as identity.

## 4. Conditional five-second foreground refresh

**Status: accepted for the v0.2.0 candidate; implementation and release verification are tracked in the [v0.2.0 checklist](../release/v0.2.0-checklist.md).**

Add a **5 s request mode** alongside 10, 15, 30, and 60 seconds. It is available only after Android explicitly reports that Wi-Fi scan throttling is off; enabled or unavailable capability states disable it. If a persisted five-second choice later becomes ineligible, the app waits for capability discovery to finish, then persists and schedules the 30-second default.

The mode remains foreground-only and uses the existing single scan coordinator. It permits one in-flight request, coalesces manual refresh, times stalled requests out after 15 seconds, and advances histories only for fresh platform timestamps or a successful newly empty result. It does not add a one-second mode, permission, `INTERNET` access, background service, or second scan loop. Five seconds is a best-effort request cadence, not a guarantee of fresh hardware measurements; UI copy must expose actual source age and warn about battery and OEM limits. The full decision is recorded in [ADR 0004](../adr/0004-conditional-fast-scan-cadence.md).
