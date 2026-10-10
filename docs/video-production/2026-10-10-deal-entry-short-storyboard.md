# MandiSamiti — Production Video Storyboard #1
<!-- Format: Vertical 9:16 (YouTube Shorts / Reels / WhatsApp Status) -->
<!-- Target Audience: Indian APMC Mandi Aadhatis & Grain Traders (Age 25–65) -->
<!-- Topic: 10 सेकंड में पक्का सौदा दर्ज करें (Lightning Deal Entry) -->

---

## 📋 Production Metadata
- **Video Title (Hindi):** 10 सेकंड में किसान का पक्का सौदा दर्ज करें | मंडी समिति ऐप
- **Video Title (English):** Fast Deal Entry in 10 Seconds | MandiSamiti App
- **Aspect Ratio:** 9:16 (1080 × 1920, Vertical Portrait)
- **Target Duration:** 35 Seconds
- **Voiceover Character:** 32-38 year old North Indian Mandi trader (warm, crisp, confident, respectful Devanagari Hindi)
- **Background Music:** Subtle acoustic Dholak & Flute pulse (low volume: -18dB) under voiceover

---

## 🎬 Second-by-Second Storyboard & Cue Sheet

| Timestamp | Visual Cue (Camera / Screen) | Voiceover Script (Hindi) | On-Screen Text / Caption | Audio & Soundbox Cue |
| :---: | :--- | :--- | :--- | :--- |
| **00:00 – 00:04** | **Hook Shot:** Close-up of a chaotic wooden mandi desk with torn paper slips, calculator, and brass weights. Fast camera push-in. | *"नीलामी की आपाधापी में कांटा पर्ची खोने का डर रहता है?"* | **पर्ची खोने का डर? ❌** | Rustling paper SFX, busy auction murmur in background. |
| **00:04 – 00:08** | **Solution Transition:** Clean cut to MandiSamiti app running on a real smartphone held in one hand. Top bar displays *"माँ शारदा ट्रेडर्स"*. | *"अब पुराने रजिस्टर छोड़िए, मिलिए मंडी समिति ऐप से!"* | **मंडी समिति ऐप ✅** | Clean UI whoosh sound. |
| **00:08 – 00:15** | **Step 1 — Party & Produce:** Thumb taps on farmer name *"रामअवतार (बरौली)"*, produce auto-selected as *"गेहूं (Wheat)"*. | *"बस किसान का नाम चुना, बोरी और कुल वजन डाला..."* | **1. किसान चुना 🌾<br>2. बोरी व वजन** | Subtle keypad click sounds. |
| **00:15 – 00:22** | **Step 2 — Rate & Auto-Math:** Thumb types rate `₹2,450`. App instantly displays Gross Value, Commission (2.5%), and Net Payable without mental math. | *"भाव लिखते ही पूरा हिसाब चुटकियों में तैयार — बिना किसी गलती के!"* | **सटीक हिसाब • 0% गलती ⚡** | Coin count chime sound. |
| **00:22 – 00:28** | **Step 3 — Soundbox & Save:** Big green **"सौदा सुरक्षित करें"** button pressed. Screen shows double-tick sync. | *(Pause for Soundbox)* <br><br> **Soundbox Voice:** *"रामअवतार जी से 50 बोरी का सौदा दर्ज हुआ।"* | **📢 साउंडबॉक्स पुष्टि!** | **Mandi Voice Soundbox** speaks loudly and clearly. |
| **00:28 – 00:32** | **Payoff — WhatsApp Slip:** Thumb taps **"WhatsApp पर्चा भेजें"**. Formatted Hindi account receipt pops up on WhatsApp. | *"और 1 टैप में किसान भाई के WhatsApp पर पक्की पर्ची पहुंच गई!"* | **सीधा WhatsApp पर्चा 📲** | WhatsApp message sent chime. |
| **00:32 – 00:35** | **Outro / CTA:** App logo, Google Play badge, and helpline number. | *"मंडी समिति — हिसाब पक्का, व्यापार सच्चा। अभी डाउनलोड करें।"* | **हिसाब पक्का, व्यापार सच्चा 🤝** | Uplifting acoustic resolve chord. |

---

## 🤖 Gemini Prompt for Photorealistic B-Roll Generation (Google Veo / Imagen 3)

### Prompt A — Intro Hook Shot (Mandi Desk)
```text
Photorealistic 9:16 vertical video shot, 4k 60fps. Close-up view of a traditional Indian grain mandi merchant's wooden desk (आढ़त की गद्दी). Jute burlap sacks full of golden wheat and mustard seeds in soft-focus background. On the desk: an authentic metal balance scale (तराजू), brass weights, traditional ledger notebooks with red cloth binding (बहीखाता), and handwritten slips. Golden morning sunlight filtering through corrugated metal shed roof with floating dust motes. Atmospheric, documentary style, authentic Indian agricultural market.
```

### Prompt B — Hand Holding Device in Mandi Setting
```text
Photorealistic 9:16 vertical video shot, 4k 60fps. A 40-year-old Indian grain trader's hand holding a modern black smartphone with a clean, high-contrast accounting app interface on screen. The merchant is standing inside a bustling North Indian agricultural mandi auction yard. In the background: farmers, wooden carts, and stacks of burlap crop bags under warm daylight. Clean cinematic depth of field, sharp focus on the phone screen and fingers.
```

---

## 🎙️ Cloud Text-to-Speech Voice Synthesis Configuration

### SSML Markup for Voice Track:
```xml
<speak>
  <p>
    <s>नीलामी की आपाधापी में कांटा पर्ची खोने का डर रहता है?</s>
    <break time="250ms"/>
    <s>अब पुराने रजिस्टर छोड़िए, मिलिए मंडी समिति ऐप से!</s>
    <break time="300ms"/>
    <s>बस किसान का नाम चुना, बोरी और कुल वजन डाला...</s>
    <s>भाव लिखते ही पूरा हिसाब चुटकियों में तैयार — बिना किसी गलती के!</s>
    <break time="500ms"/>
  </p>
  <p>
    <!-- Soundbox announcement simulation -->
    <prosody rate="95%" pitch="-1st">
      <s>"रामअवतार जी से पचास बोरी का सौदा दर्ज हुआ।"</s>
    </prosody>
    <break time="400ms"/>
  </p>
  <p>
    <s>और एक टैप में किसान भाई के व्हाट्सएप पर पक्की पर्ची पहुंच गई!</s>
    <break time="300ms"/>
    <s>मंडी समिति — हिसाब पक्का, व्यापार सच्चा। अभी डाउनलोड करें।</s>
  </p>
</speak>
```

- **Recommended Google Cloud TTS Voice:** `hi-IN-Neural2-B` (Male, Natural conversational tone) or `hi-IN-Journey-D`.
- **Speaking Rate:** `1.05x` (engaging, brisk pacing for Shorts).
- **Pitch:** `-0.5st` (authoritative, warm Indian merchant timbre).

---

## 🚀 Execution Steps via ADB & Local Terminal

1. **Record the live App UI from Redmi Note 7:**
   ```bash
   adb shell screenrecord --size 1080x2160 --bit-rate 12000000 /sdcard/mandi_demo.mp4
   # Perform the Deal Entry on phone, then hit Ctrl+C
   adb pull /sdcard/mandi_demo.mp4 ./projects/MandiSamiti/assets/videos/deal_raw.mp4
   ```

2. **Generate the Hindi Voiceover via Google Cloud TTS or Gemini API:**
   - Synthesize the SSML text above to `deal_voiceover.mp3`.

3. **Composite & Overlay:**
   - Place `deal_raw.mp4` inside the phone bezel overlay.
   - Attach intro B-Roll (0–4s) and outro card (32–35s).
   - Sync voice track and Soundbox audio chime at 22s.
