# MandiSamiti — Docs Index
<!-- Conforms to NGDL v1.2, Cross-AI One-Brain Protocol, and VoltAgent/awesome-design-md -->

**Product:** MandiSamiti (मंडी समिति — आढ़त बही-खाता)  
**Package:** `com.appwork.mandisamiti`  
**Platforms:** Android (Primary), Desktop (JVM), iOS (KMP Framework), Web (FastAPI Backend)  
**Target Demographic:** Indian Agricultural Traders (Aadhatis) & Farmers (Age 25 – 65)  
**Design Standard:** NoGravity Design Language (NGDL v1.2)  
**Last Updated:** 2026-10-10  

This page is an index. Each topic lives on its own page; open the one you need.

## Product
| Page | What it covers |
| :--- | :--- |
| [Feature status](product/feature-status.md) | **The one home for status:** every feature, its status and phase, with links to its spec and plan |
| [Vision & accessibility](product/vision-and-accessibility.md) | Who the app is for and the 25–65 ergonomics rules |
| [Information architecture](product/information-architecture.md) | The 4-hub layout: Dashboard, Khata, Galla, Settings |
| [QuickBooks adaptation](product/quickbooks-adaptation.md) | Phase 7 backlog: aging chips, close & lock, expense chips |
| [Pilot plan](product/pilot-plan.md) | Running the 21-shop pilot, ending access, and what happens to shop data |
| [Pilot cost](product/pilot-cost.md) | Cost breakdown per month, day, shop and bill |

## Engineering
| Page | What it covers |
| :--- | :--- |
| [Mandi math](engineering/mandi-math.md) | Exact paisa formulas for weight, crop value, payable and receivable |
| [Audit integrity](engineering/audit-integrity.md) | Void, correction and append-only revision rules |
| [AI collaboration](engineering/ai-collaboration.md) | Rules every AI follows when working on this project |
| [Backend README](../backend/README.md) | FastAPI server: run, test, deploy |

## Design specs (`superpowers/specs/`)
| Date | Spec |
| :--- | :--- |
| 2026-09-20 | [Aadhat ledger — product & technical design](superpowers/specs/2026-09-20-mandisamiti-aadhat-ledger-design.md) |
| 2026-10-08 | [Accounts & cloud sync](superpowers/specs/2026-10-08-mandisamiti-accounts-and-sync-design.md) |
| 2026-10-10 | [Phase 4 — staff roles](superpowers/specs/2026-10-10-mandisamiti-phase4-staff-roles-design.md) |

## Implementation plans (`superpowers/plans/`)
| Date | Plan |
| :--- | :--- |
| 2026-09-20 | [KMP + CMP rebuild](superpowers/plans/2026-09-20-mandisamiti-kmp-rebuild.md) |
| 2026-10-07 | [Phase 1 — 4-tab navigation & language](superpowers/plans/2026-10-07-mandisamiti-phase1-navigation-and-language.md) |
| 2026-10-08 | [Phase 1A — trustworthy ledger](superpowers/plans/2026-10-08-mandisamiti-1a-trustworthy-ledger.md) |
| 2026-10-08 | [Phase 1B — backend hardening](superpowers/plans/2026-10-08-mandisamiti-1b-backend-hardening.md) |
| 2026-10-08 | [Accounts](superpowers/plans/2026-10-08-mandisamiti-accounts.md) |
| 2026-10-09 | [Sync](superpowers/plans/2026-10-09-mandisamiti-sync.md) |

## Other
- [Android launch modes masterclass](android-launch-modes-masterclass.html) — learning material.
- Interview starter branches: [spec](superpowers/specs/2026-09-13-interview-starter-branches-design.md) · [plan](superpowers/plans/2026-09-13-interview-starter-branches.md) — not product work.
- Vault (decisions, costs, reviews): `vaults/Vivek-K/wiki/entities/mandisamiti.md` in the Vivek-K hub.

## Adding docs
New topic → new page under `product/` or `engineering/`, plus one row here. New feature → a row in [feature status](product/feature-status.md) linking its spec and plan. Older plans that say "update §2 of this file" now mean [feature status](product/feature-status.md).
