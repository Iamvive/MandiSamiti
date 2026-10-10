# Multi-AI Collaboration Protocol

← [Docs index](../MANDISAMITI-MASTER-SPEC.md)

When any AI assistant (Antigravity, Claude Code, Codex, OpenCode, Hermes) works on MandiSamiti:
1. **Rule §1:** Run `./gradlew test` (or `./gradlew jvmTest`) before making assertions. All 55 test suites must pass.
2. **Rule §2:** Strictly adhere to NGDL v1.2 tokens in `ui/theme/Color.kt` (No raw emojis in UI strings/icons; use Compose vector ImageVectors).
3. **Rule §3:** Record newly completed features in [feature-status.md](../product/feature-status.md) (the one home for status) and synchronize the Obsidian Second Brain (`vaults/Vivek-K/wiki/concepts/mandisamiti-product-review-2026-10-08.md` and `exports/context-pack.md`).
4. **Rule §4:** New docs get their own page under `product/`, `engineering/` or `superpowers/` and a row in the [docs index](../MANDISAMITI-MASTER-SPEC.md). Don't grow the index into a single long page again.
