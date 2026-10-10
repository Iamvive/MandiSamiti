# Pilot Cost Breakdown (21 shops × 1 month)

← [Docs index](../MANDISAMITI-MASTER-SPEC.md) · Plan: [pilot plan](pilot-plan.md)

Costed 2026-10-10. Load assumed: 21 shops × at most 50 bills/day × 30 days.

## Summary
| | Per month | Per day | Per shop per day |
| :--- | ---: | ---: | ---: |
| **Recommended** (2 GB droplet + daily backups) | **₹1,880** | **₹63** | **₹3** |
| With 20% buffer | ₹2,255 | ₹75 | ₹3.60 |
| Current droplet already has ≥2 GB free (backups + OTP only) | ₹470 | ₹16 | ₹0.75 |

Per bill at full load: **~₹0.06**. Each bill costs almost nothing extra. The app is offline-first, voice (TTS) runs on the phone, and WhatsApp sharing uses the shop's own WhatsApp. The only network calls go to our backend, so no paid API is called per bill.

## Load
- **Bills:** 21 × 50 × 30 = **31,500 bills/month** at most.
- **Peak:** if a shop's 50 bills fall in a 3-hour rush, ~350 bills/hour, about 0.1 request/second.
- **Storage:** ~100 MB/month.
- **Bandwidth:** ~1–2 GB/month, against the 2 TB the droplet includes.
- **Bottleneck:** RAM, not CPU. Prod, staging, Postgres, Redis and the finance bot share one droplet, so it needs ≥2 GB.

## Line items (monthly)
| Item | Cost | Notes |
| :--- | ---: | :--- |
| Droplet Basic 2 GB / 1 vCPU / 50 GB ($12) | ₹1,407 | $12 + 18% GST + ~3.5% card forex. ₹0 if the current droplet already has ≥2 GB and headroom |
| Daily backups (+30% of droplet = $3.60) | ₹422 | Must-have: the server is the book of record. Weekly (+20%) is ₹281 |
| Off-site `pg_dump` (Backblaze B2, free 10 GB) | ₹0 | Plus one restore drill before go-live |
| WhatsApp OTP | ~₹14 (budget ₹50) | 21 users × ~5 OTPs × ₹0.115 + 18% GST, via Meta Cloud API direct. A BSP adds 10–25% |
| Monitoring (UptimeRobot / Sentry / Grafana free tiers) | ₹0 | Fits the free tiers at this scale |
| Domain `appworx.co.in` | ₹0 | Already owned |
| Play Console | ₹0 | Already owned; distribute through the internal-testing track |
| Onboarding | ₹0 | Vivek and his father onboard the shops in person; the in-app Hindi manual replaces printed cards |
| **Total** | **~₹1,880** | ~₹89 per shop |
| Buffer 20% | ~₹375 | **~₹2,255 with buffer** |
| *Optional:* Bluetooth 58 mm thermal printers | ₹1,500–2,500 each (₹31–52k) | Only if shops must print bills. The app has no print feature yet |

## After the pilot ends
| Item | Cost | Notes |
| :--- | ---: | :--- |
| Read-only grace period (30 days) | ₹0 extra | View and export run on the phone; the server only has to stay up for shops that continue |
| Keeping a final snapshot until deletion (≤90 days) | ~₹15/month | DigitalOcean snapshot at ~$0.06/GB-month for 1–2 GB |

## Ways to lower it
- **GST:** add a GSTIN to the DigitalOcean team. That drops the 18% GST from the invoice (reverse charge applies), saving ~₹280/month.
- **Droplet:** if the current droplet already has ≥2 GB free, skip the ₹1,407 line. Check with `free -h` on the VPS.
- **Short pilot:** DigitalOcean bills hourly up to the monthly cap. A 10-day pilot costs about ₹630.

## Assumptions and sources
- **Exchange rate:** $1 = ₹96 (September 2026 readings were 94.8–96.0).
- **Card forex fee:** ~3.5%, varies by card.
- **Prices to re-check before buying:**
  - [DigitalOcean backups pricing](https://docs.digitalocean.com/products/backups/details/pricing/) (doc verified 2026-03-09)
  - [DigitalOcean India tax](https://docs.digitalocean.com/platform/billing/taxes/ind/) (18% GST)
  - [Meta WhatsApp pricing](https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing.md)
  - ₹0.115 per authentication message, as cited by third-party India guides
- **Droplet size:** unknown, not recorded in the infra inventory.
