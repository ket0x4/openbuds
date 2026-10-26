# Material 3 Expressive Design System Guidelines for OpenBuds

This document defines the **Material 3 Expressive** (`androidx.compose.material3:material3:1.5.0-alpha15`) design language architecture, token scales, physics-based motion principles, and component usage guidelines for **OpenBuds**.

---

## 1. Core Principles & Expressive Tactics

Material 3 Expressive expands Material Design 3 to create interfaces with stronger visual hierarchy, tactile responsiveness, and emotional engagement while preserving usability and accessibility:

1. **Hero Moments**: Key hardware status surfaces—such as the Dashboard **Hero Hardware Stage** and three-pod **Battery Section**—use generous container curves (`extraLargeIncreased` = `36.dp`), reactive tonal glows, and expressive typography to anchor user attention immediately.
2. **Emphasized Containment**: Related settings and controls are grouped into intentional surface containers (`surfaceContainerLow` through `surfaceContainerHighest`) and connected list groups (`SegmentedListItem`) rather than flat divider-heavy lists.
3. **Interactive Shape Morphing**: Interactive components (`Button`, `FilledTonalIconButton`, `TonalToggleButton`, `SegmentedListItem`, and `ExpressiveStyleableTile`) dynamically morph their corner radii on press and selection.
4. **Connected Group Dynamics**: Multi-option selectors (`ButtonGroup` + `TonalToggleButton`) fuse options into a cohesive group where the pressed/active item expands horizontally via `ButtonGroupScope.animateWidth()` while neighboring options compress with spring physics.
5. **Physics-Driven Motion**: Every spatial and visual transition is driven by `MotionScheme.expressive()` spring specifications rather than fixed-duration easing curves.

---

## 2. Color System & Tonal Hierarchy

OpenBuds uses Material You dynamic color (`dynamicDarkColorScheme` / `dynamicLightColorScheme`) on Android 12+ and falls back to the custom **Glacier, Mint & Lilac** M3 Expressive palette defined in [`Color.kt`](../app/src/main/java/com/openbuds/app/ui/theme/Color.kt).

| Surface / Role Token | Usage in OpenBuds |
| :--- | :--- |
| `background` / `surface` | Root screen canvas behind scrollable content |
| `surfaceContainerLowest` | Recessed wells inside cards (e.g., `ButtonGroup` wells, metric pills) |
| `surfaceContainerLow` | Hero Hardware Stage base surface and inactive battery pods |
| `surfaceContainer` | Disconnected Dual Connect host cards |
| `surfaceContainerHigh` | Primary feature cards (`ExpressiveStyleableCard`), active Dual Connect host cards, and `SegmentedListItem` surfaces |
| `surfaceContainerHighest` | Floating toolbar (`HorizontalFloatingToolbar`), pressed tile states, and elevated dialogs |
| `primary` / `primaryContainer` | Active ANC mode/strength, active codec (`LDAC`), in-ear pod highlight, and primary CTAs |
| `secondary` / `secondaryContainer` | Secondary status badges, charging pod containers, and system Bluetooth hints |
| `tertiary` / `tertiaryContainer` | Awareness mode accent, Low Latency gaming mode, and Primary Host star badge |
| `ChargingGreen`, `BatteryWarning`, `BatteryLow` | Semantic hardware telemetry tokens for battery level, charging LEDs, and active playback |

### Contrast Rules
Always pair `on*` tokens with their matching container token (`onPrimary` on `primary`, `onPrimaryContainer` on `primaryContainer`, `onSecondaryContainer` on `secondaryContainer`, `onTertiaryContainer` on `tertiaryContainer`, `onErrorContainer` on `errorContainer`).

---

## 3. Typography & Emphasized Scale

In Material 3 Expressive, `Typography` provides **30 text styles**: 15 baseline styles (`displayLarge`..`labelSmall`) and **15 Emphasized styles** (`displayLargeEmphasized`..`labelSmallEmphasized`) configured in [`Type.kt`](../app/src/main/java/com/openbuds/app/ui/theme/Type.kt).

| Role | Baseline Style | Emphasized Style (`*Emphasized`) | OpenBuds Application |
| :--- | :--- | :--- | :--- |
| **Display** | `displayMedium` (`36sp`, Bold) | `displayMediumEmphasized` (`36sp`, Black, `tnum`) | Large hero numerical readouts |
| **Headline** | `headlineMedium` (`24sp`, Bold) | `headlineMediumEmphasized` (`24sp`, Black, `tnum`) | Screen titles (`FreeBuds 5i`, `Audio`, `Devices`, `Controls`) and pod battery `%` |
| **Title Large** | `titleLarge` (`21sp`, SemiBold) | `titleLargeEmphasized` (`21sp`, Black) | Modal & confirmation dialog titles |
| **Title Medium** | `titleMedium` (`17sp`, SemiBold) | `titleMediumEmphasized` (`17sp`, ExtraBold) | Section card headers (`Noise Control`, `Equalizer`, `Tap Shortcuts`) |
| **Title Small** | `titleSmall` (`14sp`, SemiBold) | `titleSmallEmphasized` (`14sp`, ExtraBold) | Tile titles, EQ preset names, and setting row titles |
| **Body** | `bodyLarge` / `bodyMedium` / `bodySmall` | `bodyLargeEmphasized` / `bodyMediumEmphasized` / `bodySmallEmphasized` | Supporting descriptions, selected bottom-sheet rows, and MAC addresses |
| **Label** | `labelLarge` / `labelMedium` / `labelSmall` | `labelLargeEmphasized` / `labelMediumEmphasized` / `labelSmallEmphasized` | Selected `TonalToggleButton` labels, status pills, and telemetry chips |

> **Guideline**: Prefer `MaterialTheme.typography.<role>Emphasized` for focal headers, active toggle states, and key metrics instead of ad-hoc `.copy(fontWeight = FontWeight.Black)` overrides. All numeric readouts use `fontFeatureSettings = "tnum"` to prevent layout jitter during live battery or bitrate updates.

---

## 4. Expanded Shape Scale & `MaterialShapes` Polygons

### 4.1 Expanded Corner Token Scale (`OpenBudsShapes`)
Defined in [`Shape.kt`](../app/src/main/java/com/openbuds/app/ui/theme/Shape.kt) and provided to `MaterialExpressiveTheme`:

- `extraSmall`: `4.dp` — compact inner indicators
- `small`: `8.dp` — inner segmented seams (`SegmentedListItem` middle corners)
- `medium`: `14.dp` — pressed tile corners & compact badges
- `large`: `20.dp` — highlighted/active hardware pods & inner cards
- `largeIncreased`: `24.dp` — interactive tiles & outer `SegmentedListItem` corners
- `extraLarge`: `28.dp` — primary feature cards (`ExpressiveStyleableCard`) and bottom sheets
- `extraLargeIncreased`: `36.dp` — Hero Hardware Stage bottom curvature
- `extraExtraLarge`: `48.dp` — full pill containers

### 4.2 Expressive Polygon Badges (`MaterialShapes`)
M3 Expressive introduces `RoundedPolygon` geometry in `MaterialShapes`, converted to Compose `Shape` via `.toShape()`. OpenBuds uses `ExpressiveIconBadge` with distinct geometric silhouettes to give each functional section an unmistakable visual identity:

| Polygon Token | Visual Form | OpenBuds Usage |
| :--- | :--- | :--- |
| `MaterialShapes.SoftBurst` | Soft multi-point burst | Bluetooth pairing & Codec section icon badges |
| `MaterialShapes.Cookie9Sided` | 9-sided scalloped cookie | Noise Control / ANC section icon badge & Press-and-Hold badge |
| `MaterialShapes.Sunny` | Warm radial sun polygon | Equalizer section icon badge |
| `MaterialShapes.Clover4Leaf` | 4-leaf rounded clover | Low Latency gaming & Tap Shortcuts icon badges |
| `MaterialShapes.Pill` / `Bun` | Organic capsule geometry | Hardware & Firmware info icon badges |

---

## 5. Physics-Based Motion (`MotionScheme.expressive()`)

All animations in OpenBuds retrieve their spring specifications from `MaterialTheme.motionScheme` (`MotionScheme.expressive()`):

1. **Spatial Springs** (`fastSpatialSpec()`, `defaultSpatialSpec()`, `slowSpatialSpec()`):
   - Used for **geometry, position, scale, corner radius morphing, and segment width** (`animateWidth`).
   - Under-damped to allow subtle, natural physical bounce/overshoot.
2. **Effects Springs** (`fastEffectsSpec()`, `defaultEffectsSpec()`, `slowEffectsSpec()`):
   - Used for **color, alpha, border tint, and elevation**.
   - Critically damped so colors and opacity never overshoot or flash.

---

## 6. Component Architecture & Mapping

| UI Pattern | M3 Expressive Component | Key APIs Used |
| :--- | :--- | :--- |
| **Bottom Navigation Dock** | `HorizontalFloatingToolbar` + `ButtonGroup` | `FloatingToolbarDefaults.standardFloatingToolbarColors()`, `ButtonGroupDefaults.connected*ButtonShapes()`, `Modifier.animateWidth()` |
| **Mode & Strength Selectors** | `ButtonGroup` + `TonalToggleButton` / `ToggleButton` | `ButtonGroupDefaults.ConnectedSpaceBetween`, `connectedLeadingButtonShapes()`, `connectedMiddleButtonShapes()`, `connectedTrailingButtonShapes()` |
| **Grouped Settings & Sheets** | `SegmentedListItem` | `ListItemDefaults.segmentedShapes(index, count)`, `ListItemDefaults.segmentedColors()`, `ListItemDefaults.SegmentedGap` |
| **Battery Charging Bar** | `LinearWavyProgressIndicator` | Animated wave (`WavyProgressIndicatorDefaults.indicatorAmplitude`) when charging; calm determinate line (`amplitude = { 0f }`) when discharging |
| **Bluetooth Connecting State** | `ContainedLoadingIndicator` | Morphing polygon indicator inside a tonal container while establishing RFCOMM/A2DP link |
| **Header Action Buttons** | `FilledTonalIconButton` | `shapes = IconButtonDefaults.shapes()` for round-to-square press morphing |
| **Primary / Secondary Actions** | `Button` / `FilledTonalButton` | `shapes = ButtonDefaults.shapes()` for pill-to-squircle press morphing |
