# MandiSamiti Design System

## Foundation

MandiSamiti follows the NoGravity Design Language in `../../docs/design-system/DESIGN-SYSTEM.md`. Compose colours come only from `../../packages/ngdl-compose`; feature code uses the existing `Mandi*` aliases and does not copy token values.

## Interface character

- Dense, readable account-book screens for traders using the app in daylight and dark environments.
- Primary actions stay obvious; warnings remain visible without competing with transaction amounts.
- Hindi and English labels must fit the same layouts without truncating core data.

## Components

- Cards and panels use the NGDL card surface, border, and elevation.
- Nested status tiles use the elevated surface token.
- Warning badges use the warning token family with a vector icon and text.
- Icons use Compose vector assets. Emoji are not interface icons.

## Layout and accessibility

- Text contrast is at least 4.5:1 in light and dark themes.
- Screens must not scroll horizontally at 390px width.
- Header and action rows wrap or reflow on narrow screens.
- Empty, suspicious, and incomplete data states are labelled honestly.

## Verification

UI changes are checked in light and dark themes at desktop and 390px-equivalent phone width, controls are exercised on test data, and `tests/ngdl-tokens` passes before handoff. Anything not checked is listed in the review note.
