---
name: emandi-form6
description: Fill Form 6 / 6R (प्रपत्र-6, Voucher for Seller) on the UP eMandi trader portal from a typed Hindi or English message, then stop so Vivek can enter the captcha and press Save. Use for "form 6 bharo", "6R kaato", "प्रपत्र-6 भरो", "fill form 6", "voucher for seller". DRAFT: dry runs only until Vivek signs off.
---

# eMandi Form 6 (6R)

Portal: https://emandi.up.gov.in (Hindi only, no language switch).
Runtime: any AI with a browser tool that can read the page, set field values, click and take screenshots (Claude in Chrome, Playwright or Chrome DevTools MCP, Gemini/Antigravity browser, Codex or OpenCode browser tools). Steps below say what to do, not which tool call to use. Use your runtime's own tab, read-page, fill/select and click actions. The user stays logged in in that same browser.
Pages: `/Traders/Dashboard`, `/Traders/add_six_r` (fill), `/Traders/generated_6R` (verify), `/Traders/SixREditTrader` (correction, never automatic).

## Hard rules
1. **Never click संरक्षित करें (Save).** Vivek presses it himself. Only if `.env` has `EMANDI_CLAUDE_SAVES=1` AND Vivek replies "confirm / हाँ भेजो" in the same turn may Save be clicked. While this skill is DRAFT, ignore that setting.
2. **Login is Vivek's.** Never type, read or store a username, password, OTP or captcha, and never read them from files. If the page is the login page or redirects there: stop, send `error` ("login needed"), wait for "continue", then restart the voucher from step 1 (reload clears the form).
3. **Issuer is always व्यापारी (Vyapari, radio value `t`).** Never आढ़ती (`d`), even if asked. Refuse and say why.
4. Never print or write licence numbers; use `<LICENCE_NO>`. Never commit or push from this skill.
5. Set values with the browser tool's fill/select action and blur a field by clicking a blank area. Do not press Tab in the rate field (a portal alert can freeze the tab; if the tab stops responding, ask Vivek to dismiss the alert).

## Inputs (one typed message, Hindi, English or Hinglish)
Ask for anything missing, one question at a time:
| Input | हिंदी | Notes |
|---|---|---|
| commodity | कृषि उत्पाद | map via aliases below |
| variety | किस्म | never default; ask and list the 5 likeliest (wheat: Local, Dara, Sharbati, Lokwan, PBW-343; "Other" exists) |
| grade | ग्रेड | FAQ / Non-FAQ / Grade A; no default |
| quantity (qtl) | तौल/मात्रा (कुंतल) | number |
| rate per qtl | दर (प्रति कुंतल) | number |
| seller name + district | विक्रेता फर्म/किसान का नाम व जिला | one text box |
| seller mobile | मोबाइल नंबर | 10 digits |
| entry slip no (optional) | प्रवेश पर्ची संख्या | blank if not given |
| sale site (optional) | प्रधान स्थल/विक्रय स्थल का नाम | default "Mathura Mandi" |

Not needed: net amount (auto = qty x rate), fee, cess, total, buyer licence and buyer firm (auto-filled when व्यापारी is chosen), date (locked to today), book/serial no, export (stays "For Non Export").

## Steps
1. Open `/Traders/add_six_r` in a browser tab you control. Login page means rule 2.
2. Select **व्यापारी** first. Verify buyer licence and buyer firm boxes are now filled (never print the licence). If empty, click व्यापारी again.
3. Sale site: keep "Mathura Mandi" unless the message names another.
4. Seller name+district and mobile (check 10 digits).
5. Commodity (select by option value, see aliases), wait 2 s, then variety (value like `OT/00031`), then grade (FAQ 4, Non-FAQ 9, Grade A 10).
6. Read the red hint under the row: `(न्यूनतम दर : MIN अधिकतम दर : MAX)`. If rate < MIN or > MAX: **stop**, tell Vivek the range, wait for a new rate. Never adjust it. (The portal alerts and clears the rate otherwise; a JS alert can freeze the tab.)
7. Quantity, then rate; click a blank area to blur. Wait 1 s.
8. Verify: net = qty x rate; fee = 1% of net; cess = 0.5% of net; total = fee + cess. Mismatch: report, do not continue.
9. Entry slip if given. Leave export as is.
10. Summary card in Vivek's language (Hindi field names as on screen): all values, fee, cess, total, seller mobile; never licence or captcha. Send `ready` notification.
11. **PAUSE.** Say: "Captcha daalo aur संरक्षित करें dabao. Ho jaye to 'saved' likho." Do nothing else.
12. On "saved": open `/Traders/generated_6R`, set dates to today, click खोजें, match crop, quantity, rate. Send `saved` with serial no and totals. If no match, send `error`, never retry Save.
13. Any error text from the portal: quote it as shown (Hindi), send `error`, stop.

## Commodity aliases (confirm with Vivek, option value in brackets)
गेहूं/गेहूँ/gehun/wheat [6] | धान/dhan/paddy [1] | चावल/chawal/rice [2] | बाजरा/bajra [4] | मक्का/makka/maize [5] | जौ/jau/barley [7] | चना/chana [12] | सरसों/sarson/mustard "लाही तथा सरसो *2" [23] | सोयाबीन [17] | मूँगफली/moongfali [21] | ग्वार/guar [19] | अरहर/arhar [14] | मूंग/moong [11] | उर्द/urad [10] | मसूर/masoor [15] | तिल/til [22] | ज्वार/jowar [3] | मटर/matar [13].
Anything else: select by Hindi name from the live dropdown; if two matches (e.g. सुपारी twice) ask.

## Notifications
`python3 projects/MandiSamiti/automations/emandi-form6/emandi_notify.py <ready|saved|error> "<message>"` (stages: ready = captcha+Save needed, saved = record, error = fix needed). `.env` keys: `EMANDI_TEST_MODE` (default 1: personal chat, `[TEST]` tag), `EMANDI_CHANNEL_ID` (admin channel, used when test mode is 0), `EMANDI_CLAUDE_SAVES` (default 0). Bot token stays in Keychain. Full details go to the channel, never licence, captcha or token.

## Manual tests (no real voucher)
1. English dry fill: "fill form 6: wheat, Local, FAQ, 10 qtl @ <rate in range>, seller TEST, Mathura, mobile <dummy>". Expect Vyapari selected, buyer auto-filled, totals correct, summary card, `[TEST]` message, stop before Save. Press Reset.
2. Hindi dry fill: "गेहूं 10 क्विंटल, लोकल, FAQ, रेट <सही>, किसान TEST मथुरा". Same checks.
3. Guards: rate below MIN stops with the range; missing grade or variety asks; "आढ़ती चुनो" refused; "save kar do" refused in draft; qty x rate mismatch flagged.
4. Login: open the login page in the tab; skill must stop with "login needed".
5. Leak check: `grep -rnE "[0-9]{10}|L/20" .agents/skills/emandi-form6 projects/MandiSamiti/automations/emandi-form6/emandi_notify.py` finds nothing real; `git status` shows no `.env`.
6. Go-live (after sign-off): one real voucher, Vivek saves, skill verifies in `/Traders/generated_6R`, `saved` message arrives. Then `/project-update`.
