# WhatsApp direct send: design

Date: 2026-11-27. Approved by Vivek in chat. Base: `origin/main` at `3fac745`.

## Problem

Slip sharing never goes straight to the party.

- Buy and sell slips call `shareText(text, null)` (`App.kt`).
- The receipt preview passes `farmer.phone ?: buyer?.phone`, so a farmer with no number sends the slip to the buyer without saying so.
- Android ignores the number and targets only `com.whatsapp`. A phone with only WhatsApp Business gets the generic share sheet.
- iOS encodes only spaces and newlines, so `&`, `#` and Hindi text can break the link.

## Scope

In: buy slip, sell slip, receipt preview (older deal records).
Out: the day report keeps its picker. Slip text, print output and the number entry forms do not change.

## Recipient

| Screen | Recipient | Number used |
|---|---|---|
| Buy slip | farmer | party account, then the number stored on the slip |
| Sell slip | buyer | party account, then the number stored on the slip |
| Preview opened from a ledger | that ledger's party (buyer if `ledgerPartyId == buyer.id`, else farmer) | party account |
| Preview after saving a deal edit | farmer | party account |
| Any खाते slip | none | none |

The preview never falls back from one party to the other.

## Valid number

Optional spaces, `+`, `-`, brackets. Digits only otherwise. A leading `91` (12 digits) or `0` (11 digits) is dropped. The result must be 10 digits starting 6 to 9. Anything with letters is invalid, which also rejects old vehicle numbers stored in `vehicleNumber`.

## Screen

Valid number:

```
[        रामलाल को भेजें          ]   green send button
          98765 43210
          दूसरा कॉन्टैक्ट               text button
```

No valid number:

```
 रामलाल का सही मोबाइल नंबर नहीं है
[      कॉन्टैक्ट चुनकर भेजें         ]
```

Both apps installed, after either button:

```
किस ऐप से भेजें?
[ WhatsApp ]
[ WhatsApp Business ]
```

Each screen keeps its other buttons. On a buy slip with money due, "Pay Cash from Galla" stays first and primary, and the send button below it is drawn in the secondary style.

| Hindi | English |
|---|---|
| {name} को भेजें | Send to {name} |
| दूसरा कॉन्टैक्ट | Other contact |
| {name} का सही मोबाइल नंबर नहीं है | No valid mobile number for {name} |
| कॉन्टैक्ट चुनकर भेजें | Choose contact to send |
| किस ऐप से भेजें? | Send with which app? |
| भेजने से पहले संदेश देख लें: | Check the message before sending: |

## Sending

- Android direct: `ACTION_VIEW` on `https://wa.me/91XXXXXXXXXX?text=<encoded>`, with the package set to the chosen app. The user still taps Send in WhatsApp.
- Android other contact: `ACTION_SEND` text to the chosen app, as today.
- One app installed: use it without asking. Two: ask every time; store nothing. None, or the launch fails: system share sheet.
- Day report: passes no app, so it uses normal WhatsApp when installed (unchanged), otherwise Business.
- iOS: `https://wa.me/...` with every byte outside `A-Z a-z 0-9 - _ . ~` percent-encoded. iOS picks the app. No app sheet.
- `SLIP_SHARED` is tracked when a send is started. Dismissing the app sheet tracks nothing.

## Testing

- Per the MandiSamiti rule, unit tests are written after the flow passes on the device.
- Device checks use the staging build only, on the Redmi `d93c743`. Never tap Send inside WhatsApp.
- iOS has no app shell in this repo: compile check and unit tests of the shared code only.

## Not verified yet

- How much text a `wa.me` link carries into WhatsApp. Check with a long Hindi slip.
- WhatsApp Business handling of `wa.me` intents.
- Any iOS behaviour on a device.
