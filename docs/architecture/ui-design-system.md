# UI design system — Field Console

The shared foundation for every screen. Dark-first, fixed palette (ADR 0001), Jetpack
Compose Material 3 only — no additional UI libraries. Source of truth:
`app/src/main/java/org/openscanner/app/ui/theme/OpenScannerTheme.kt`,
`.../ui/components/Common.kt`, `.../ui/components/Charts.kt`.

## Color tokens

Fixed Scanner palette; **no dynamic color**. Import from
`org.openscanner.app.ui.theme`, never inline hex in screens.

| Token | Role |
| --- | --- |
| `ScannerBackground` | App/root background |
| `ScannerSurface` | Cards, charts, banners |
| `ScannerSurfaceRaised` | Elevated surfaces on top of cards |
| `ScannerBorder` | Outlines, dividers, chart gridlines |
| `ScannerText` / `ScannerMuted` | Primary / secondary text |
| `ScannerCyan` | Primary accent: live state, selected items, primary series |
| `ScannerAmber` | Attention + the one primary action per view |
| `ScannerGreen` | Positive/ok state |
| `ScannerOrange` | Warning/stale state (always paired with text) |
| `ScannerPurple` | 4th chart series only |
| `ScannerOnCyan` / `ScannerOnAmber` | Text/icons printed on cyan/amber fills |
| `ScannerIconWell` | Recessed circle behind cyan instrument icons |
| `ScannerPositiveSurface` / `ScannerPositiveBorder` | Positive banner pair |

## Type scale

Use `MaterialTheme.typography` (provided by `OpenScannerTheme`, same values in
`ScannerTypography`). Never scatter ad-hoc `sp` values.

- `displayMedium` 40 / `displaySmall` 32 — hero instrument readouts (e.g. current dBm)
- `headlineMedium` 26 — app header title; `headlineSmall` 22 — empty/unavailable state titles
- `titleLarge` 18 / `titleMedium` 16 / `titleSmall` 14 — section headers, row titles
- `bodyLarge` 16 / `bodyMedium` 14 / `bodySmall` 12 — body copy
- `labelLarge` 14 — buttons, segmented controls; `labelMedium` 12 — badges, eyebrows, legend;
  `labelSmall` 11 — **floor**: chart tick labels and the densest captions. Nothing below 11sp.

## Spacing tokens

`ScannerSpacing`: `Xs` 4, `Sm` 8, `Md` 12, `Lg` 16, `Xl` 24, `Xxl` 32 dp.
`ScannerSpacing.MinTouchTarget` = 48 dp — minimum for every interactive control.
Shapes come from `MaterialTheme.shapes` (4/6/12/20 dp corners).

## Shared components (`ui/components/Common.kt`)

```kotlin
@Composable fun AppHeader(
    phase: ScannerPhase,
    freshness: Freshness?,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
)
```
Top of every tab screen; includes `StatusBadge`. Pass a short `eyebrow` for screen context.

```kotlin
@Composable fun StatusBadge(phase: ScannerPhase, freshness: Freshness?, modifier: Modifier = Modifier)
```
LIVE/PAUSED/AGING/STALE/CHECK/OFFLINE instrument tag. Use standalone when a view needs
freshness state away from a header.

```kotlin
@Composable fun ChannelGroupSelector(groups: List<ChannelGroupUiModel>, selectedGroup: WifiChannelGroup, onSelect: (WifiChannelGroup) -> Unit, modifier: Modifier = Modifier)
```
Horizontally scrollable 48 dp segmented radio control for channel-validated 2.4/5.2/5.5-DFS/5.8/6 GHz groups, plus Unknown only when observed. Handles disabled groups and semantics itself.

```kotlin
@Composable fun PrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)
```
The single amber call-to-action of a view. At most one per screen region.

```kotlin
@Composable fun SignalGlyph(signalDbm: Int, modifier: Modifier = Modifier)
```
Decorative wifi-bars icon in `ScannerIconWell`. Informational only — always pair with the
dBm text next to it (the glyph alone must never carry meaning).

```kotlin
@Composable fun InformationBanner(icon: ImageVector, text: String, modifier: Modifier = Modifier, positive: Boolean = false)
```
Explanatory/notice strip; `positive = true` for ok/confirmed messages.

```kotlin
@Composable fun ScannerUnavailable(
    phase: ScannerPhase, safeErrorCode: String?,
    onRequestPermission: () -> Unit, onOpenWifiSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit, onRetry: () -> Unit,
    modifier: Modifier = Modifier,
)
```
Full-width empty/blocked state for permission/wifi/location/error phases.

## Charts (`ui/components/Charts.kt`)

```kotlin
@Composable fun SignalHistoryChart(history: List<SignalSample>, latestDbm: Int, nowElapsedMs: Long, modifier: Modifier = Modifier)
@Composable fun SpectrumChart(channelGroup: WifiChannelGroup, networks: List<NetworkUiModel>, modifier: Modifier = Modifier)
```

Chart conventions — any new chart must follow these:

- **Axis titles with units**: Y "Signal (dBm)" top-left; X title centered below the plot
  ("Time" for history; "Channel" or "Frequency (GHz)" per band for spectrum).
- **Ticks are computed, never hardcoded**: tick values come from the pure functions in
  `SignalHistoryGeometry.kt` / `SpectrumGeometry.kt` (`signalHistoryYAxis`,
  `signalHistoryTimeTicks`, `spectrumYAxis`, `spectrumXTicks`) and gridlines are drawn at
  exactly the tick positions. dBm axes snap to 10/20 dB steps inside the honest RSSI
  window; time ticks use 15s/30s/1m/2m/5m/10m/15m/30m/1h steps; channel ticks are
  meaningful per selected channel group (1/6/11/14 on 2.4 GHz, group-scoped recognizable channels on 5 GHz, and round GHz values on 6 GHz).
- **Tick labels ≥ 11sp** (`labelSmall`), drawn on the canvas aligned to their gridlines.
- **Legend identifies every encoding by more than color**: swatch (line / dot / patch /
  bare-line) + text label. Selected series = thicker swatch + brighter text + "· selected".
  Unknown channel width = bare vertical line glyph + "· width unknown". A shaded stale
  region always gets a "Stale gap" legend entry and a text marker on the chart.
- **Text equivalent**: every chart carries a `contentDescription` summarizing axis range,
  latest/min/max, sample count, and stale state. Keep it in sync when changing visuals.
- **Honest states are visible**: stale/throttled data keeps the orange shaded tail;
  unknown widths stay a bare vertical line — never invent a footprint.
- dBm is primary; percentages may only appear as a labelled secondary mapping.
  Never imply RSSI = distance.

## Accessibility rules (release gates)

- Every interactive control ≥ 48 dp and exposes role + contentDescription (and
  selected/disabled state where applicable).
- State is never encoded by color alone — always pair with text, shape, or line style.
- Status/freshness changes are announced via semantics; charts are summarized, not
  read point-by-point.

## Notes for the screen rewrite (phase 2)

- `0xFF18333A` icon circles → `ScannerIconWell`; `0xFF071013`/`0xFF151515` on-accent text
  → `ScannerOnCyan`/`ScannerOnAmber`; `0xFF16231B`/`0xFF31523D` → positive banner tokens.
- Replace inline `fontSize = N.sp` with the nearest `MaterialTheme.typography` slot
  (25sp title → `headlineMedium`, 16sp row title → `titleMedium`, 13sp body → `bodyMedium`,
  11sp caption → `labelSmall`, …) and inline dp paddings with `ScannerSpacing`.
- All existing `Scanner*` color vals remain valid; the chart and component signatures
  above are stable call targets.
