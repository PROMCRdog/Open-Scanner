# ADR 0001: Field Console visual direction

Date: 2026-07-31
Status: accepted

## Decision

Use the second proposed direction, **Field Console**, as the native product shell: a dark-first, high-contrast instrument UI with strong signal hierarchy, persistent freshness and pause state, compact evidence cards, and labelled bottom navigation.

The user explicitly requested that the spectrum visualization from option 1 remain available as its own top-level **Spectrum** tab. The resulting phone navigation is Scan, Track, Spectrum, Tools, and Settings.

## Consequences

- UI clarity and one-handed discoverability take priority over maximizing data density.
- Signal state is encoded by text and shape as well as color.
- All controls use at least 48 dp touch targets and labelled accessibility semantics.
- A fixed teal/lime/amber palette is used instead of dynamic color so chart identities stay stable.
- The native UI, current capture under `docs/assets/`, and UI design-system document are the maintained visual references.
