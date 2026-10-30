# MandiSamiti — AI Operating Contract

## 1. Feature Velocity & Testing Standard (Vivek's Product Rule)
- **Feature first, tests on finalization:** While discovering and building product features, focus on getting the feature UI/UX and behavior working first.
- **Do not block on strict TDD:** Do not enforce upfront Test-Driven Development during active feature iterations.
- **Test upon feature finalization:** Write unit and regression tests once the feature flow and design are final.
- Transition to strict TDD will occur later as the product matures.
- **Silent Testing & Voice Guardrail:** Never run voice tests, soundbox TTS playback, or trigger audio output during testing unless Vivek explicitly requests it. No AI should make sound. In production, sound and voice announcements must strictly be driven by user preference (settings/toggle).

## 2. Architectural Guardrails
- **Integer Math:** All currency in `Long` paisa (1 INR = 100 paisa) and weights in `Long` grams (1 Qtl = 100,000 g). Zero float arithmetic.
- **NGDL v1.2 Tokens:** Use design tokens from `ui/theme/Color.kt`. High contrast (≥ 4.5:1), vector icons (`ImageVector`), zero unicode emojis in UI.
- **Offline-First:** All writes commit locally to SQLite/SQLDelight first, with append-only revisions and asynchronous sync outbox.

## 3. Verification
- Run `./gradlew test` (or `./gradlew jvmTest`) to verify test suites after finalizing code.

