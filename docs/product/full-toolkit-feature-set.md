# Full Toolkit Feature Set and Gap Review

Date: 2026-07-31

## Decision

Build a privacy-first WiFi analyzer and network-diagnostics toolkit that is useful on an unmodified Android device. “Comprehensive” means the core workflows share one trustworthy scan model and explain their limits; it does not mean promising monitor-mode, silent network control, or unrestricted background scanning.

The first release should answer five questions well:

1. What WiFi networks and access points are visible?
2. How strong, crowded, and stable are they?
3. Which channel choices appear preferable, and how certain is that advice?
4. Is the current connection locally and externally healthy?
5. Can the user inspect or share the evidence without leaking network identifiers?

## Evidence boundary

This proposal is an independent product cut derived only from the reference feature inventory and UX audit. It treats unimplemented guide claims and features of a differently packaged current app as comparison ideas, not as evidence that the audited APK supports them.

## Current Android platform facts to carry into planning

- Android's current [WiFi scanning guidance](https://developer.android.com/develop/connectivity/wifi/wifi-scan) requires `ACCESS_WIFI_STATE`, fine location for apps targeting API 29+, and enabled system location for `getScanResults()`. Foreground `startScan()` calls remain limited to four scans per two minutes on Android 9 and later unless a developer-only test override is used.
- Android 17 makes broad LAN access permission-gated for apps targeting SDK 37+. The [local network permission guidance](https://developer.android.com/privacy-and-security/local-network-permission) favors system-mediated discovery pickers where possible and otherwise requires `ACCESS_LOCAL_NETWORK` at runtime.
- Modern connection/provisioning must use Android-owned flows. The [WiFi infrastructure overview](https://developer.android.com/develop/connectivity/wifi/wifi-infrastructure) distinguishes network suggestions, peer-device requests, and user-approved saved-network actions.
- Modern [`ScanResult`](https://developer.android.com/reference/android/net/wifi/ScanResult) data can expose structured security types, WiFi generation, and WiFi 7 Multi-Link Operation metadata when the OS and hardware provide it. Every such field therefore needs an explicit unavailable state.

## Feasibility labels

| Label | Meaning | Product treatment |
|---|---|---|
| **Stock** | Public Android APIs on an unmodified device | May ship in the main app, subject to permission and hardware checks. |
| **Constrained** | Public APIs exist, but OS throttling, redaction, hardware, network policy, or router behavior can prevent a complete result | Ship only with freshness, support, and uncertainty states; never imply guarantees. |
| **Optional** | Technically possible on stock Android but involves active traffic, external services, unusual permissions, or a separate workflow | Off by default and initiated explicitly. |
| **Privileged** | Requires root, device-owner/OEM authority, VPN interception, special hardware, or a companion | Isolate from the stock feature set and label the prerequisite before installation/use. |
| **Excluded** | Unsafe, deceptive, legally risky, or incompatible with the product boundary | Do not implement. |

## Non-negotiable product rules

- One scan repository owns permission state, scan requests, throttling detection, caching, and timestamped snapshots.
- A requested refresh interval is never presented as a guaranteed radio scan interval.
- Every derived score identifies its inputs, sample age, missing inputs, and confidence.
- Regulatory domain, DFS status, and channel-width assumptions are never silently guessed.
- Privacy redaction occurs before rendering, persistence, logging, export, or bug-report attachment by default; raw reports require explicit opt-in and exact-preview confirmation.
- Raw SSIDs, BSSIDs, IPs, and scan results never enter analytics, crash reports, or network requests.
- The app remains useful when permissions are denied; each unavailable feature explains why and offers the relevant system action.
- Accessibility semantics, 48 dp targets, color-independent encoding, scalable text, and reduced motion are release gates.
- Consequential actions use persistent confirmation; transient messages report completed actions only.
- The app does not save WiFi passwords or attempt to bypass Android-owned connection and security flows.

## MVP — ship in the first public release

### 1. Platform, permissions, and scan truth

| Feature | Feasibility | MVP decision and safety constraint |
|---|---|---|
| Capability check | **Stock** | Report WiFi state, supported bands, permission state, location-service dependency where applicable, and unavailable reasons before scanning. |
| Permission onboarding | **Stock** | Request only the OS-version-appropriate nearby-WiFi/location access after explaining local processing; no rating prompt during onboarding. |
| Unified live scan | **Constrained** | Merge scan results and current-connection data into one immutable, timestamped snapshot observed by every screen. |
| Scan freshness | **Stock** | Show last result time, requested cadence, actual observed cadence, stale state, and paused state. |
| Throttling handling | **Constrained** | Detect likely reused/stale results and explain Android limits; never direct users to weaken system protections as the default fix. |
| Band support | **Constrained** | Support 2.4, 5, and 6 GHz only when device APIs/results expose them; disable unsupported choices with an explanation. |
| Background behavior | **Constrained** | Stop active analysis when not visible except for an explicit, OS-compliant session; show that the data is no longer live. |
| Offline operation | **Stock** | Keep scanning, analysis, help, aliases, and export local; no account, ads, telemetry, or required cloud service. |

### 2. Nearby access points

| Feature | Feasibility | MVP decision and safety constraint |
|---|---|---|
| AP inventory | **Stock** | Show SSID/hidden state, BSSID, band, channel/frequency, width when exposed, RSSI, security capabilities, and snapshot age. |
| Connected AP | **Constrained** | Pin and label the connected BSSID; show local IP and negotiated link speed only when Android exposes them. |
| Modern security parsing | **Stock** | Distinguish open, enhanced-open where exposed, WEP, WPA/WPA2/WPA3 families, enterprise, and unknown without overstating security. |
| Search, sort, filter | **Stock** | Search by alias/SSID/BSSID; sort by strength, channel, band, or name; filter hidden, weak, insecure, or selected APs. |
| Same-SSID grouping | **Stock** | Group roaming candidates by SSID/security while keeping BSSID, band, and channel visible in details. |
| Aliases and favorites | **Stock** | Key locally by a per-install salted BSSID hash; never sync or log identifiers by default. |
| AP detail sheet | **Stock** | Move raw capability strings and secondary radio fields out of the dense list; include copy actions subject to Privacy Mode. |
| Open system WiFi UI | **Stock** | Let Android own joining and credential entry; do not recreate deprecated password/configuration flows. |
| Distance display | **Constrained** | Omit from the primary list; if shown in details, label it a rough RSSI/free-space estimate, never measured distance. |

### 3. WiFi analysis

| Feature | Feasibility | MVP decision and safety constraint |
|---|---|---|
| Channel graph | **Stock** | Plot channel footprint and width by band, with zoom and selection; use a synchronized searchable legend instead of labels on every curve. |
| Dense-network handling | **Stock** | Default to connected/favorited/strongest APs, support selecting two to four comparisons, and never rely on color alone. |
| Time graph | **Stock** | Plot selected BSSIDs against elapsed time/timestamps; show cadence, gaps, pause markers, session duration, and reset confirmation. |
| History bounds | **Stock** | Use explicit memory/session limits and virtualized selectors rather than silently dropping APs after a fixed series count. |
| Signal display | **Stock** | Make dBm primary; percentage is an optional clearly labelled mapping rather than a more accurate measurement. |
| Channel suitability | **Constrained** | Provide a small ordinal rating and one plain-language recommendation, not a pseudo-precise ten-star score. |
| Recommendation modes | **Constrained** | Separate “place a new AP” from “assess connected AP”; require intended width/band where it changes the result. |
| Recommendation explanation | **Constrained** | Disclose overlap, observed RSSI, width, sample count/age, connected-BSSID context, and unknown regulatory/DFS/utilization inputs. |
| Conservative channel set | **Constrained** | Recommend only channels the app can justify for the device/context; otherwise report “insufficient regulatory information.” |
| Pause/resume | **Stock** | Global, persistent, fully labelled control that does not silently resume merely because the user changes screens. |

### 4. Connection diagnostics

| Feature | Feasibility | MVP decision and safety constraint |
|---|---|---|
| Diagnostic summary | **Stock** | Summarize connected AP, signal class, security class, link speed, address availability, and WiFi interference evidence. |
| Signal guidance | **Constrained** | Use band-aware thresholds and trends; state that RSSI is not throughput, quality, or physical distance. |
| Internet state | **Constrained** | Prefer Android’s network capability/validation state and label captive or unvalidated states; do not silently call fixed third-party endpoints. |
| Gateway/DNS context | **Constrained** | Show gateway and configured DNS only when exposed; absence is “not available,” not a failed network. |
| Troubleshooting steps | **Stock** | Turn observations into reversible user actions such as moving, retrying, checking captive login, or opening system network settings. |
| Refresh and evidence | **Stock** | Diagnostics consumes the same snapshot and timestamp as all other views, preventing contradictory per-screen scan loops. |

### 5. Privacy-safe capture and sharing

| Feature | Feasibility | MVP decision and safety constraint |
|---|---|---|
| Global Privacy Mode | **Stock** | Alias SSIDs, mask BSSIDs/IPs, suppress exact timestamps/location-adjacent context, and visibly indicate redaction. |
| Export preview | **Stock** | Preview exactly what will leave the device and require an explicit override to include raw identifiers. |
| PNG export | **Stock** | Export a legible graph or full virtualized list with title, band, sample time, freshness, and limitations. |
| Structured export | **Stock** | Offer redacted CSV/JSON for current snapshot and diagnostics; use the system share sheet and app-private temporary files. |
| Metadata hygiene | **Stock** | Strip unnecessary image/file metadata and automatically expire temporary exports. |
| Local data controls | **Stock** | Provide view/delete-all actions for aliases, favorites, preferences, and any saved sessions; default to no history. |

### 6. Navigation, accessibility, and trust

| Feature | Feasibility | MVP decision and safety constraint |
|---|---|---|
| Labelled navigation | **Stock** | Use icon-plus-text destinations: Scan, Analyze, Diagnose, and Tools; make band selection a true segmented control. |
| Accessible controls | **Stock** | Give every action a name, role, selected/disabled state, tooltip, and 48 dp target. |
| Accessible live data | **Stock** | Announce scan, band, error, and pause state changes without speaking every RSSI update. |
| Visual accessibility | **Stock** | Support large text, high contrast, patterns/shapes, reduced motion, and non-haptic alternatives. |
| Clear empty/error states | **Stock** | Distinguish no APs, permission denial, WiFi off, unsupported band, scan throttling, and stale results. |
| Settings structure | **Stock** | Group Scan, Display, Privacy, Accessibility, and Advanced; include summaries, validation, and reset-to-default. |
| Local help | **Stock** | Explain permissions, throttling, RSSI, width, bands, hidden SSIDs, privacy, regulatory uncertainty, and active-versus-passive tests. |
| Open-source trust | **Stock** | Publish license/notices, reproducible build instructions, data-flow documentation, and a public threat model before release. |

## Post-MVP — valuable after the core is trustworthy

### Passive analysis and workflows

| Feature | Feasibility | Decision and constraint |
|---|---|---|
| Saved scan sessions | **Stock** | Explicit start/stop; encrypted/local-only storage; redacted export; retention policy and delete control. |
| Session comparison | **Stock** | Compare channel/RSSI snapshots by time or place label without claiming causal diagnosis. |
| Roaming view | **Constrained** | Compare BSSIDs for one SSID and flag likely candidates; do not claim client steering or handoff control. |
| Signal tracker | **Constrained** | Select by BSSID, show signal trend/lost state, and offer optional haptic/audio cues; call it a locator aid, not a distance meter. |
| Manual site survey | **Constrained** | Let users label rooms/points and record samples; location/floor-plan import is optional and stripped from exports by default. |
| Offline OUI labels | **Constrained** | Use an updateable offline database; mark locally administered/randomized BSSIDs and never equate vendor prefix with device identity. |
| Home-screen widget | **Constrained** | Show the last snapshot and its age; do not imply live background scanning. |
| Notifications | **Constrained** | User-created thresholds during an explicit session only; rate-limit and disclose OS background limits. |

### Active network tools

| Feature | Feasibility | Decision and constraint |
|---|---|---|
| DNS check | **Optional** | User starts a named lookup; show resolver, timing, answer, and failure stage without uploading scan inventory. |
| HTTP connectivity check | **Optional** | Use a documented, user-selectable endpoint; disclose destination and data use before the first request. |
| TCP reachability | **Optional** | Probe a user-entered host and port with timeouts; describe it as TCP connect, not ICMP ping. |
| Local throughput test | **Optional** | Prefer an explicit peer/self-hosted endpoint; label WiFi-path versus internet-path tests and the traffic volume. |
| Internet speed test | **Optional** | Require opt-in, server/data-use disclosure, metered-network warning, cancellation, and local history disabled by default. |
| Latency/jitter series | **Optional** | State protocol and endpoint; do not generalize one destination to total internet quality. |
| Route diagnostics | **Constrained** | Provide only protocols that work without raw-socket guarantees; label partial paths/timeouts instead of promising traceroute parity. |
| Local device discovery | **Optional** | Scan only the connected local subnet after explicit consent; rate-limit, honor isolation/permission failures, and save nothing by default. |
| mDNS/SSDP discovery | **Optional** | Separate passive listening from active queries and show which traffic will be sent. |
| User-scoped port check | **Optional** | Limit to user-selected devices/ports on the local network; no default broad or stealth scan. |
| Router admin shortcut | **Optional** | Open the detected gateway in the system browser after warning about HTTP/cert risk; never embed login, capture credentials, or auto-configure. |
| WiFi QR creation | **Optional** | Generate from credentials the user deliberately enters; never claim Android can reveal saved WiFi passwords. |
| Wake-on-LAN | **Optional** | User-configured local target only, with clear broadcast/network limitations and no cloud relay by default. |

## Optional or privileged modules — outside the stock promise

| Capability | Class | Boundary |
|---|---|---|
| WiFi RTT ranging | **Optional / hardware-gated** | Only for supported devices and RTT-capable APs with required permission; report uncertainty and never infer a person’s location. |
| Per-app traffic statistics | **Optional / special access** | Require Android’s user-granted usage access where available; explain scope and keep results on-device. |
| VPN-based traffic inspection | **Privileged by user consent** | Separate module using `VpnService`; disclose that it changes routing, conflicts with another VPN, and is not link-layer WiFi capture. |
| Device-owner fleet diagnostics | **Privileged** | Separate enterprise build/policy; never imply availability to ordinary installs. |
| Tethering/hotspot control | **Privileged / constrained** | Main app may open system settings; configuration or silent control belongs only in an authorized OEM/device-owner module. |
| Root packet capture | **Privileged** | Separate expert build with persistent warning, local-only defaults, capture limits, and no credential extraction features. |
| Monitor mode/raw 802.11 frames | **Privileged / hardware-dependent** | Requires compatible chipset/driver/root or an external adapter; never expose as a stock-device feature. |
| External radio companion | **Privileged / separate project** | Define a documented local protocol and consent model; keep device firmware/driver support out of the core APK. |

## Excluded — do not build

- Deauthentication, jamming, packet injection against third-party networks, evil-twin setup, or denial-of-service tooling.
- WPS cracking, password guessing, credential harvesting, saved-password extraction, or automated exploitation.
- Silent WiFi joins, hidden password storage, router credential capture, or automatic router reconfiguration.
- Internet-wide scanning, stealth scanning, unbounded subnet sweeps, or cloud-coordinated target discovery.
- Claims of continuous real-time background WiFi scanning on stock Android.
- Claims that RSSI estimates exact distance, indoor position, occupancy, identity, or safety.
- Claims that a scan-only score measures airtime utilization or guarantees the best legal router channel.
- A single hard-coded connectivity endpoint as the definition of “internet works.”
- Raw SSID/BSSID/IP uploads, public leaderboards, ad-tech SDKs, or analytics keyed to network identifiers.
- Automatic bug reports containing scan results or screenshots without a redacted preview and user confirmation.
- Treating a MAC/OUI prefix, SSID, or hostname as proof of a device vendor, owner, or threat.
- “AI security” labels or remediation claims without inspectable inputs, rules, confidence, and validation evidence.
- A generic vulnerability scanner in the consumer app; this materially expands safety, legal, and maintenance scope.

## Reference-to-toolkit gap decisions

| Reference behavior or gap | Toolkit decision |
|---|---|
| Four useful analysis mental models | Retain their functions, but reorganize them into labelled Scan, Analyze, Diagnose, and Tools navigation. |
| One cycling band icon | Replace with a true accessible segmented control and explicit unsupported states. |
| Per-screen receivers and scan loops | Replace with one shared scan repository and one timestamped source of truth. |
| Dense labels and color-only graph identity | Replace with selection, synchronized legend, compare limits, and non-color encodings. |
| Ten-star recommendation precision | Replace with a coarse rating, recommendation rationale, confidence, and regulatory unknowns. |
| Deprecated in-app connection flow | Remove; delegate joining and credentials to Android-owned UI. |
| Rough distance shown prominently | Demote to an optional, caveated estimate; tracker uses BSSID and signal trend. |
| Raw identifier export | Permit only as an explicit redaction-setting opt-out with sensitive-data warning, labelled exact preview, and per-share confirmation. |
| Fixed external reachability probes | Replace with OS validation for MVP and disclosed opt-in endpoints later. |
| Stale guide claims: discovery, speed test, router admin | Treat as post-MVP active tools with consent and network/privacy safeguards, not launch blockers. |
| External AI/widgets claims | Widget is post-MVP and stale-aware; unexplained AI analysis is excluded. |
| Missing accessibility names/small targets | Make semantic labels, state announcements, and minimum target sizing release-blocking. |

## MVP release gate

The MVP is ready only when all of the following are demonstrated on supported stock devices:

- Every screen renders the same snapshot ID, capture time, and freshness state for a given scan.
- Permission denied, location dependency, WiFi off, unsupported band, no results, throttled, stale, and paused states are separately testable.
- 2.4/5/6 GHz channel conversion and width rendering have unit tests, including unknown and malformed inputs.
- Security parsing has fixtures for modern, legacy, open, enterprise, and unknown capabilities.
- Recommendation fixtures expose every input and produce “insufficient information” when regulatory or sample evidence is inadequate.
- Export tests prove report redaction masks identifiers before text generation, raw output requires explicit selection, and temporary files expire.
- No raw network identifier appears in logs, analytics, crash payloads, or default persisted state.
- TalkBack can reach and identify every action; large text does not hide values or controls; graphs have equivalent text summaries.
- Active network traffic is absent from the MVP except Android/system behavior and the user’s explicit share action.
- The README, in-app help, threat model, permissions table, and limitations match observable behavior.

This cut is intentionally broad in passive WiFi analysis and deliberately narrow in active or elevated network operations. That boundary makes the first release both useful and credible on stock Android.
