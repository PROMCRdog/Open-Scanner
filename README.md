# Open Scanner

English | [简体中文](README.zh-CN.md)

[![CI](https://github.com/PROMCRdog/Open-Scanner/actions/workflows/ci.yml/badge.svg)](https://github.com/PROMCRdog/Open-Scanner/actions/workflows/ci.yml)

Open Scanner is a local-first, open-source Wi-Fi analysis toolkit for Android. It turns Android's passive scan results into a clear five-tab workflow: **Scan**, **Track**, **Spectrum**, **Tools**, and **Settings**.

Version 0.1.0 is an early public release: its automated privacy/build gates, permanent signature, exact-artifact verification, and bounded physical-device smoke test passed. It is not yet broad API 26–36 device or manual accessibility/performance certification. The v0.2.0 development line adds a truthful conditional five-second request mode; its implementation and device validation must pass the separate [v0.2.0 checklist](docs/release/v0.2.0-checklist.md) before release. Assurance policy remains defined by [ADR 0003](docs/adr/0003-early-public-release-policy.md).

The interface follows the selected dark **Field Console** direction. It prioritizes legibility, labelled controls, explicit freshness, and honest unavailable states over maximum data density. The channel spectrum from the alternate design is preserved as a dedicated top-level tab.

![Track interface with labelled axes, units, and a text-and-shape legend](docs/assets/track-screen.png)

## Current development scope

- One application-scoped scan coordinator; screens never create competing scan loops.
- Nearby access-point inventory with channel-validated 2.4, 5.2, 5.5/DFS, 5.8, 6 GHz, and unsupported-frequency groups.
- SSID, BSSID, signal, channel, frequency, width, security, Wi-Fi generation, and a highlighted current-system Wi-Fi marker when Android exposes connection evidence.
- Search, strength-first sorting, explicit refresh, cached/throttled-result warning, read-only Android scan-throttle status, and distinct permission/Wi-Fi/Location/device error states.
- Requested intervals of 5, 10, 15, 30, and 60 seconds. **5 s request mode** is foreground-only and available only when Android explicitly reports scan throttling off; it shows actual source age and never turns cached data into a new sample.
- A selected-AP tracker with a timestamp-scaled, bounded 60-sample, memory-only signal history and a visible gap when the latest evidence is no longer current.
- A recent-snapshot stability indicator that reports RSSI range and observed absence share instead of requiring users to infer flapping from the graph.
- A dedicated Canvas spectrum graph showing up to four emphasized networks and an accessible text equivalent; unknown channel widths stay visibly unknown instead of becoming invented 20 MHz footprints.
- Observed co-channel/overlap analysis that does not pretend to know legal router channels or airtime utilization.
- Android-provided physical Wi-Fi connection validation, captive-portal, link-speed, IP, gateway, and DNS evidence without mixing in a cellular default route or probing an external server.
- A passive neighborhood posture summary with observed counts by channel group, advertised security profile, and reported Wi-Fi generation.
- Global on-screen Privacy Mode persisted through DataStore.
- Explicit start/stop Wi-Fi session logging with selectable fields and a report-redaction choice frozen when each session starts.
- Redacted-by-default snapshot and text/JSON/CSV log export, with an explicit warning before enabling raw reports and an exact confirmation preview before every share.
- Temporary exports use Android's URI-grant flow and are deleted after one hour while the app remains open or on a later app start/export.
- No account, ads, telemetry, cloud service, analytics SDK, or `INTERNET` permission.

## Safety and privacy boundary

Version 0.2 remains passive. It does not join networks, collect passwords, probe local devices, run speed tests, scan ports, or contact internet endpoints. Android owns network joining and protected settings. Faster requests add no background service, permission, or `INTERNET` access and stop when the app is not foreground-visible.

Nearby Wi-Fi scans can reveal location context, so Android requires precise Location permission and, on many versions, the system Location switch. Open Scanner does not request GPS coordinates. Raw scan history stays in memory and disappears with the process. Session logs are also memory-only and bounded. Report redaction is on by default: redacted sessions transform SSIDs, BSSIDs, exact wall-clock times, and local addresses before a log record exists. Users can explicitly allow unredacted reports; new log sessions then retain only the selected raw fields in memory. Every export is labelled and previewed exactly before a temporary file is shared.

See [the threat model](docs/security/threat-model.md) and [feature boundary](docs/product/full-toolkit-feature-set.md).

## Build

Requirements:

- JDK 17
- Android SDK Platform 36.1 and Build Tools 36.0.0 or newer compatible 36.x tools
- ADB for physical-device testing

The wrapper and dependency versions are pinned. From the repository root:

Configure JDK 17 and Android SDK Platform 36.1 through Android Studio or the
standard `JAVA_HOME` and `ANDROID_HOME` environment variables, then run:

```bash
./gradlew --dependency-verification=strict test :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew --dependency-verification=strict :app:lintRelease :app:assembleRelease
```

Install the debug APK only on a device you control:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Some Xiaomi/HyperOS devices independently require the developer option **Install via USB**. Open Scanner's build does not bypass that device-side safeguard.

Source builds produce an unsigned release candidate. Official release artifacts, when published, are signed through a private maintainer process. Use debug APKs only for local device QA; never distribute them as production releases.

## Project layout

| Module | Responsibility |
|---|---|
| `:app` | Compose UI, activity, state mapping, and manual application graph |
| `:core:model` | Immutable scan, connection, capability, and preference models |
| `:core:domain` | Channel mapping/grouping, signal and stability classes, posture aggregation, security fallback parsing, freshness, and overlap analysis |
| `:core:privacy` | Identifier masking and privacy transformations |
| `:core:export` | Redacted/raw snapshot encoders, bounded Wi-Fi log recorder, and text/JSON/CSV log encoders |
| `:data:wifi-android` | Android API adapter and the single scan coordinator |
| `:data:settings` | DataStore-backed local preferences |
| `docs/architecture/ui-design-system.md` | Tokenized UI design system: color/type/spacing tokens, shared components, and chart conventions |

Architecture details are in [docs/architecture/native-app.md](docs/architecture/native-app.md).

## Known limits

- Android can throttle, reject, delay, or reuse Wi-Fi scan results. Even with Developer Options scan throttling disabled, **5 s** is a request cadence—not a guaranteed five-second radio measurement—and can increase battery use.
- Five-second mode automatically resets to the 30-second default if Android later reports throttling enabled or its state is unavailable. Version 0.2.0 has no one-second mode and no background scanning.
- RSSI is not distance, speed, occupancy, identity, or safety.
- Passive overlap is not airtime utilization and cannot guarantee a legal or optimal router channel.
- Hardware, OS version, permissions, access-point beacons, and OEM behavior determine which fields are available.
- Session logs are not durable saved sessions: they end with the app process and are capped at 500 state records or 25,000 AP rows.
- PNG export, encrypted saved-session history/comparison, aliases/favorites, snapshot diff, demo mode, 6 GHz PSC highlighting, active DNS/HTTP tests, LAN discovery, and throughput tests remain future work.

## Contributing and security

Read [CONTRIBUTING.md](CONTRIBUTING.md), [GOVERNANCE.md](GOVERNANCE.md), [SUPPORT.md](SUPPORT.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and [SECURITY.md](SECURITY.md) before opening work publicly. Never attach raw SSIDs, BSSIDs, IP addresses, or unredacted screenshots to a bug report.

## License

Apache License 2.0. See [LICENSE](LICENSE).
