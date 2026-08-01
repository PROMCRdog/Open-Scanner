# Selected Design Direction

## Decision

The first release uses the user-selected **Option 2: Field Console** as its primary visual language, with **Option 1's channel spectrum** retained as a dedicated `Spectrum` tab.

## Visual source of truth

- Primary Track screen: [the current native capture](../assets/track-screen.png) and [`TrackScreen.kt`](../../app/src/main/java/org/openscanner/app/ui/screens/TrackScreen.kt).
- Spectrum behavior and hierarchy: [`SpectrumScreen.kt`](../../app/src/main/java/org/openscanner/app/ui/screens/SpectrumScreen.kt) and the [UI design system](../architecture/ui-design-system.md).

Public visual evidence uses synthetic network data only. The implementation and its tests must never reuse identifiers from private device captures.

## Product rules

1. Default to a dark, high-contrast field interface with clear live, paused, and freshness states.
2. Keep the selected signal and its meaning dominant; detailed evidence remains visible without competing with the primary reading.
3. Use a dedicated Spectrum tab for 2.4, 5, and 6 GHz overlap analysis.
4. Keep Scan, Track, Spectrum, Tools, and Settings reachable through a persistent bottom navigation.
5. Use semantic labels, non-color status cues, 48 dp targets, whole-number measurements, and privacy-safe synthetic/demo data.
