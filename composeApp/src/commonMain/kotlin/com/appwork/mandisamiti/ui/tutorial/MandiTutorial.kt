package com.appwork.mandisamiti.ui.tutorial

/**
 * High-touch vernacular tutorial definitions for MandiSamiti.
 * Built specifically for Indian APMC Mandi traders & Aadhatis (Age 25–65).
 * Each tutorial is strictly 30–45 seconds with offline 3-step fallbacks.
 */
data class MandiTutorial(
    val id: String,
    val titleHindi: String,
    val titleEnglish: String,
    val durationTextHindi: String,
    val durationTextEnglish: String,
    val youtubeVideoId: String,
    val descriptionHindi: String,
    val descriptionEnglish: String,
    val fallbackStepsHindi: List<String>,
    val fallbackStepsEnglish: List<String>
) {
    fun title(isEnglish: Boolean): String = if (isEnglish) titleEnglish else titleHindi
    fun duration(isEnglish: Boolean): String = if (isEnglish) durationTextEnglish else durationTextHindi
    fun description(isEnglish: Boolean): String = if (isEnglish) descriptionEnglish else descriptionHindi
    fun fallbackSteps(isEnglish: Boolean): List<String> = if (isEnglish) fallbackStepsEnglish else fallbackStepsHindi
    fun videoUrl(): String = "https://www.youtube.com/watch?v=$youtubeVideoId"

    companion object {
        val DEAL_ENTRY = MandiTutorial(
            id = "deal_entry",
            titleHindi = "सौदा दर्ज करना सीखें",
            titleEnglish = "Learn Deal Entry",
            durationTextHindi = "40 सेकंड",
            durationTextEnglish = "40s",
            youtubeVideoId = "dQw4w9WgXcQ", // Template ID, replaceable via remote config
            descriptionHindi = "किसान का नाम, बोरी, वजन और भाव दर्ज करके 5 सेकंड में पक्का सौदा बनाएं।",
            descriptionEnglish = "Record farmer produce, bags, net weight, and rate in under 5 seconds.",
            fallbackStepsHindi = listOf(
                "किसान / व्यापारी का नाम चुनें या नया जोड़ें",
                "बोरी की गिनती, कुल वजन (क्विंटल) और भाव दर्ज करें",
                "हरा 'सौदा सुरक्षित करें' बटन दबाएं — साउंडबॉक्स तुरंत पुष्टि बोलेगा"
            ),
            fallbackStepsEnglish = listOf(
                "Select or add the farmer / buyer party",
                "Enter bag count, net weight (Quintal), and auction rate",
                "Tap green 'Save Deal' — voice soundbox confirms immediately"
            )
        )

        val GALLA_REGISTER = MandiTutorial(
            id = "galla_register",
            titleHindi = "दैनिक गल्ला व रोकड़ मिलाना",
            titleEnglish = "Cash Drawer & Balance",
            durationTextHindi = "40 सेकंड",
            durationTextEnglish = "40s",
            youtubeVideoId = "dQw4w9WgXcQ",
            descriptionHindi = "दुकान में नकद जमा, हम्माली व मंडी खर्च का हिसाब एक जगह रखें।",
            descriptionEnglish = "Track daily cash collections, labor payments, and mandi expenses.",
            fallbackStepsHindi = listOf(
                "नकद प्राप्ति के लिए हरा 'नकद जमा' बटन दबाएं",
                "हम्माली, किराया या चाय-खर्च के लिए लाल 'नकद निकासी' बटन दबाएं",
                "स्क्रीन के शीर्ष पर दिन का शुद्ध रोकड़ शेष (Cash in Hand) देखें"
            ),
            fallbackStepsEnglish = listOf(
                "Tap green 'Cash In' for incoming cash payments",
                "Tap red 'Cash Out' for hammali, freight, or shop expenses",
                "Check live Cash in Hand at the top KPI tile"
            )
        )

        val KHATA_STATEMENT = MandiTutorial(
            id = "khata_statement",
            titleHindi = "बहीखाता व WhatsApp पर्चा",
            titleEnglish = "Ledger & WhatsApp Slip",
            durationTextHindi = "35 सेकंड",
            durationTextEnglish = "35s",
            youtubeVideoId = "dQw4w9WgXcQ",
            descriptionHindi = "किसी भी पार्टी का बकाया देखें और 1 टैप में WhatsApp पर पर्चा भेजें।",
            descriptionEnglish = "View party ledger balances and send WhatsApp statements with 1 tap.",
            fallbackStepsHindi = listOf(
                "खाता सूची में पार्टी का नाम खोजें और उस पर टैप करें",
                "लेना (बाकी) या देना का कुल हिसाब देखें",
                "'WhatsApp पर्चा भेजें' बटन दबाकर सीधे पार्टी को हिसाब शेयर करें"
            ),
            fallbackStepsEnglish = listOf(
                "Search and select the party from the Khata ledger list",
                "View net receivable or payable transaction breakdown",
                "Tap 'Share on WhatsApp' to send an instant formal slip"
            )
        )

        val DAY_CLOSING = MandiTutorial(
            id = "day_closing",
            titleHindi = "शाम का हिसाब (Day Close)",
            titleEnglish = "Evening Day Close",
            durationTextHindi = "45 सेकंड",
            durationTextEnglish = "45s",
            youtubeVideoId = "dQw4w9WgXcQ",
            descriptionHindi = "शाम 6 बजे गल्ले की नकदी गिनकर हिसाब मिलाएं और दिन बंद करें।",
            descriptionEnglish = "Count physical drawer cash at 6 PM, check discrepancy, and lock the day.",
            fallbackStepsHindi = listOf(
                "गल्ला स्क्रीन पर 'शाम का हिसाब मिलाएं' बटन दबाएं",
                "दुकान में मौजूद कुल नकद राशि (हाथ में रोकड़) दर्ज करें",
                "ऐप बताएगा 'हिसाब बराबर', 'फालतू' या 'कमी' — पुष्टि करके सुरक्षित करें"
            ),
            fallbackStepsEnglish = listOf(
                "Tap 'Day Closing Reconciliation' in the Galla tab",
                "Enter physical cash counted in your shop drawer",
                "App automatically verifies exact match, surplus, or deficit"
            )
        )

        val SOUNDBOX_SETTINGS = MandiTutorial(
            id = "soundbox_settings",
            titleHindi = "बोलने वाला साउंडबॉक्स",
            titleEnglish = "Voice Soundbox Audio",
            durationTextHindi = "30 सेकंड",
            durationTextEnglish = "30s",
            youtubeVideoId = "dQw4w9WgXcQ",
            descriptionHindi = "हर सौदे और जमा पर अपनी भाषा में आवाज़ से पुष्टि सुनें।",
            descriptionEnglish = "Hear immediate audio voice announcements on every deal and payment.",
            fallbackStepsHindi = listOf(
                "ऊपर के बार में स्पीकर बटन से आवाज़ ऑन या म्यूट करें",
                "हर सौदा सहेजने पर ऐप बोलेगा: 'रामकुमार जी से 25000 का सौदा दर्ज हुआ'",
                "सेटिंग्स में जाकर अपनी पसंदीदा भाषा (हिन्दी / अंग्रेजी) चुनें"
            ),
            fallbackStepsEnglish = listOf(
                "Toggle audio on/off using the speaker pill in the top bar",
                "Listen to voice announcements after saving any transaction",
                "Customize audio dialect or volume inside the Settings tab"
            )
        )

        val ALL_TUTORIALS = listOf(
            DEAL_ENTRY,
            GALLA_REGISTER,
            KHATA_STATEMENT,
            DAY_CLOSING,
            SOUNDBOX_SETTINGS
        )

        fun fromId(id: String): MandiTutorial? = ALL_TUTORIALS.firstOrNull { it.id == id }
    }
}
