# ADR 0003: UI readability overhaul

Date: 2026-07-31
Status: accepted

## Decision

Adopt the tokenized design system documented in [docs/architecture/ui-design-system.md](../architecture/ui-design-system.md) as the native UI reference. Colors, type, and spacing are consumed as tokens from `OpenScannerTheme.kt`; shared components and charts come from `ui/components/Common.kt` and `ui/components/Charts.kt`. The overhaul stays within ADR 0001's Field Console direction: dark-first, fixed palette, 48 dp touch targets, and state encoded by more than color.

Charts are required to carry axis titles with units, computed ticks aligned to gridlines at human-readable increments, and legends that identify every series by text and shape, not color alone. 11sp (`labelSmall`) is the text floor; nothing renders smaller.

The native implementation and current captures supersede early exploratory prototypes as the visual source of truth. Those prototypes are not part of the public source distribution.

## Consequences

- Future UI work follows the design-system document; new screens and components use its tokens and shared components rather than ad-hoc values.
- Chart changes must preserve the labeling rules: axis titles with units, computed gridline-aligned ticks, and text-plus-shape legends.
- Current native captures live under `docs/assets/`; only those captures represent the shipped UI.
