# MandiSamiti — Go-To-Market (GTM), Sales Playbook & Data Protection Strategy

**Date:** 2026-10-10  
**Target Market:** Indian APMC Grain Mandis (Mathura, Orai, Aligarh, Hathras, Bundelkhand Hubs)  
**Target Customer:** Grain Commission Agents (*Aadhatis / कच्चा-पक्का आढ़ती*) and their Head Accountants (*Munim / मुनीम जी*)  
**Core Value Proposition:** *हिसाब पक्का, व्यापार सच्चा — 5 सेकंड में पर्चा, रात का सुकून, और 100% डेटा अमर (जीरो डेटा लॉस)*

---

## 1. Ground Reality & Customer Psychology

Mandi Aadhatis are **not** corporate SaaS clients. They do not click on Facebook/Google advertisements and are intensely skeptical of software that feels like compliance or surveillance.

### Three Primary Buying Drivers:
1. **साख और प्रतिष्ठा (Reputation & Social Proof):** Aadhatis buy when they see the adjacent merchant (*दुकान नं. 41*) using it comfortably without errors.
2. **शाम का सुकून (Evening Reconciled Drawer in 5 Minutes):** The auction rush ends by 5:00 PM; the Aadhati and Munim traditionally spend until 8:30–9:00 PM reconciling cash drawer, weighbridge cuts, and credit. MandiSamiti finishes this in under 5 minutes.
3. **किसान विश्वास (Farmer Transparency):** Clean, instant WhatsApp settlement slips eliminate bitter arguments over tare deductions (*काट/बारदाना*) and commission percentages.

---

## 2. The Number 1 Objection: "Accident & Data Loss" (The Fear Barrier)

### What the Aadhati Fears:
> *"अगर मोबाइल नाली में गिर गया, बच्चे ने रीसेट कर दिया, या मंडी में ट्रैक्टर के पहिए के नीचे दबकर चकनाचूर हो गया — तो मेरा लाखों का हिसाब उड़ जाएगा!"*

### The Hidden Truth (The Paper Danger vs Digital Safety):
The traditional Red Ledger (*लाल बहीखाता*) carries **₹50 लाख से ₹5 करोड़ की मौसमी उधारी** and is vulnerable to:
- Rain leaks from corrugated tin sheds in monsoon/unseasonal storms.
- Fire incidents in the market yard.
- Termites, moisture, and rat damage.
- Lost weighbridge slips (*कांटा पर्ची*) causing unrecoverable revenue leaks.

### Turning the Fear into our Strongest Closing Argument:
> *"लाला जी, भगवान न करे अगर आपकी दुकान में आग लग जाए या बारिश में टीन टपक जाए, तो क्या आपका लाल बहीखाता वापस आएगा? नहीं न? लाखों की उधारी हवा हो जाएगी!*  
> *मंडीसमिति में अगर आपका यह मोबाइल ट्रैक्टर के पहिए के नीचे आकर चकनाचूर भी हो जाए, तो भी आपकी एक चवन्नी का नुकसान नहीं होगा।*  
> *आप बाज़ार से कोई भी नया सस्ता फ़ोन उठाइए, अपना मोबाइल नंबर डालिए — 1 मिनट के अंदर आपके सारे किसानों का खाता, रोकड़ और पर्ची नए फ़ोन में वैसे ही खुल जाएगी जैसे कुछ हुआ ही न हो। कागज़ जल सकता है, भीग सकता है — लेकिन मंडीसमिति का हिसाब अमर है।"*

---

## 3. The Solution Architecture: "त्रिनेत्र सुरक्षा कवच" (Triple Lock Data Protection)

```
                                [ त्रिनेत्र सुरक्षा कवच ]
                                           │
         ┌─────────────────────────────────┼─────────────────────────────────┐
         │                                 │                                 │
  [ ताला 1: ऑटो क्लाउड तिजोरी ]   [ ताला 2: शाम 7 बजे WhatsApp बैकअप ]   [ ताला 3: 60-सेकंड नया फ़ोन रीस्टोर ]
  • जैसे ही 2G/4G/Wi-Fi मिले,       • शाम को गल्ला बंद होते ही पूरे      • नया ₹6,000 का फ़ोन लाओ,
    डेटा सुरक्षित सर्वर में सिंक     दिन का पक्का PDF लाला जी के          OTP डालो — 1 मिनट में
  • कोई बटन दबाने की ज़रूरत नहीं     पर्सनल WhatsApp पर चला जाए          पूरा हिसाब वापस!
```

1. **ताला 1: ऑटो क्लाउड तिजोरी (Background Sync):**
   - 100% offline-first operation throughout the day.
   - The moment any connection (2G, 4G, or evening Wi-Fi) is detected, SQLite writes synchronize incrementally to the cloud VPS with zero manual intervention.
2. **ताला 2: शाम 7 बजे की WhatsApp तिजोरी (Off-Device Daily PDF Digest):**
   - At Day-Closing (*दैनिक गल्ला मिलान*), a 1-tap encrypted PDF ledger summary is pushed to the owner's personal WhatsApp.
   - Even if the shop phone is lost or damaged overnight, today's closing figures exist on the owner's personal device.
3. **ताला 3: 60-सेकंड नया फ़ोन रीस्टोर (`FirstSyncScreen`):**
   - Any replacement phone running MandiSamiti restores all parties, balances, transactions, and trade slips within 60 seconds of OTP verification.

---

## 4. Ground Sales Playbook (The "Gaddi Demo" Script)

### Timing:
Best visiting window is **2:00 PM to 4:30 PM** (post-auction lull, before evening settlement). Never visit during morning auction rush (8:00 AM – 1:00 PM).

### The 3-Minute Live Gaddi Demonstration:
1. **The Hook:** Place the phone and portable Soundbox on the Aadhati's counter desk (*गद्दी*).
2. **The 5-Second Input:**
   - Tap `[▶ सौदा दर्ज करना सीखें • 40 सेकंड]` pill to show how easy help is.
   - Select farmer (*रमेश कुमार*), type `50` bags, `25.5` quintal gross weight.
   - Switch to Stage 2, type rate `2450`.
3. **The Soundbox Announcement:**
   - Soundbox loudly speaks in clear Hindi: *"श्री गणेश ट्रेडिंग... रमेश कुमार जी से पचास बोरी का सौदा दर्ज हुआ।"*
   - Nearby shop owners and farmers turn around to listen.
4. **The WhatsApp Slip Landing:**
   - Tap WhatsApp button — the structured bill slip lands on the Aadhati's personal phone.
5. **The Closing Pitch:**
   - *"लाला जी, मुनीम जी को हटाना नहीं है — मुनीम जी का काम 4 गुना आसान करना है ताकि शाम 6:30 बजे गल्ला मिलकर आप दोनों घर जा सकें।"*

---

## 5. Monetization Models

| Tier | Package | Price | Target Customer |
| :--- | :--- | :--- | :--- |
| **Option A (Recommended)** | 30-Day Free Harvest Trial + Annual License | ₹2,999 / year per shop (≈₹250/mo) | Standard Aadhath shops (1-2 counters) |
| **Option B (Hardware Bundle)** | Physical 4G Soundbox + 1-Year License | ₹4,999 one-time | Premium shops wanting high counter presence |
| **Option C (Multi-Counter Enterprise)** | Up to 3 Linked Devices (Owner + 2 Munims) | ₹4,499 / year | High-volume traders with multiple auction sheds |

---

## 6. Distribution & Viral Flywheel

```
                                [ 1 Active Aadhati ]
                                         │
                   Generates 30 to 80 Settlement Slips per day
                                         │
                                         ▼
                 [ WhatsApp Settlement Slip with Brand Footer ]
                 "Powered by मंडीसमिति • मुफ़्त स्मार्ट आढ़त ऐप"
                                         │
                    ┌────────────────────┴────────────────────┐
                    ▼                                         ▼
            [ 1,500+ Farmers/Month ]                 [ 50+ Buyer Merchants ]
         Receive transparent slips             See digital accounting standard
                    │                                         │
                    └────────────────────┬────────────────────┘
                                         ▼
                           Adjacent Mandi Shops Inquire
                             "लाला जी, ये पर्चा कहां से बना?"
```

---

## 7. Immediate 14-Day Action Roadmap

1. **Phase 1: Pilot 10 Shops (Orai & Mathura Mandi)**
   - Personally onboard 10 trusted Aadhatis with pre-configured mandi rates (1.5% commission, ₹8/bag labour).
2. **Phase 2: Video Shorts Distribution**
   - Circulate YouTube Short #1 and local video testimonials in regional Vyapar Mandal WhatsApp groups.
3. **Phase 3: Visual In-App Trust Badging**
   - Add the `[ 🛡️ बैकअप सुरक्षित ]` shield badge on the Top Bar to provide constant psychological reassurance of zero data loss.
