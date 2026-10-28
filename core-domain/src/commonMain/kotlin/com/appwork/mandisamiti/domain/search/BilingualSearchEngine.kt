package com.appwork.mandisamiti.domain.search

/**
 * High-performance, offline bilingual search engine for Indian Mandi applications.
 *
 * Supports cross-script transliteration (Devanagari <-> Latin/English) and phonetic matching,
 * allowing mandi traders to type in either Hindi or English (or Hinglish) and match records
 * stored in either script.
 *
 * Examples:
 * - Farmer saved as "Bhagvan Singh" -> Matches search "भगवान", "bhagwan", "singh"
 * - Farmer saved as "RadheyShyam" -> Matches search "राधे", "राधेश्याम", "radhe"
 * - Village saved as "Kheda" -> Matches search "खेड़ा", "keda"
 * - Commodity "गेहूं" -> Matches search "wheat", "gehun", "gehu"
 * - Devanagari numerals "१४" -> Matches slip number "14"
 */
object BilingualSearchEngine {

    private val VOWELS = mapOf(
        '\u0905' to "a", '\u0906' to "a", '\u0907' to "i", '\u0908' to "i",
        '\u0909' to "u", '\u090A' to "u", '\u090B' to "ri", '\u090F' to "e",
        '\u0910' to "ai", '\u0913' to "o", '\u0914' to "au"
    )

    private val CONSONANTS = mapOf(
        '\u0915' to "k", '\u0916' to "kh", '\u0917' to "g", '\u0918' to "gh", '\u0919' to "ng",
        '\u091A' to "ch", '\u091B' to "chh", '\u091C' to "j", '\u091D' to "jh", '\u091E' to "ny",
        '\u091F' to "t", '\u0920' to "th", '\u0921' to "d", '\u0922' to "dh", '\u0923' to "n",
        '\u0924' to "t", '\u0925' to "th", '\u0926' to "d", '\u0927' to "dh", '\u0928' to "n",
        '\u092A' to "p", '\u092B' to "ph", '\u092C' to "b", '\u092D' to "bh", '\u092E' to "m",
        '\u092F' to "y", '\u0930' to "r", '\u0932' to "l", '\u0935' to "v",
        '\u0936' to "sh", '\u0937' to "sh", '\u0938' to "s", '\u0939' to "h",
        '\u0958' to "k", '\u0959' to "kh", '\u095A' to "gh", '\u095B' to "z",
        '\u095C' to "d", '\u095D' to "dh", '\u095E' to "f", '\u095F' to "y"
    )

    private val MATRAS = mapOf(
        '\u093E' to "a", '\u093F' to "i", '\u0940' to "i", '\u0941' to "u",
        '\u0942' to "u", '\u0943' to "ri", '\u0947' to "e", '\u0948' to "ai",
        '\u094B' to "o", '\u094C' to "au"
    )

    private val DIGITS = mapOf(
        '\u0966' to '0', '\u0967' to '1', '\u0968' to '2', '\u0969' to '3', '\u096A' to '4',
        '\u096B' to '5', '\u096C' to '6', '\u096D' to '7', '\u096E' to '8', '\u096F' to '9'
    )

    private const val HALANT = '\u094D'
    private const val NUKTA = '\u093C'
    private const val VISARGA = '\u0903'

    private val COMMODITY_SYNONYMS: List<Set<String>> = listOf(
        setOf("wheat", "gehu", "gehun", "गेहूं", "गेंहू", "कनक", "kanak"),
        setOf("mustard", "sarson", "sarso", "सरसों", "सरसो", "राई", "rai", "toria", "तोरी"),
        setOf("chana", "gram", "channa", "चना", "चना दाल", "chickpea", "चने"),
        setOf("matar", "peas", "pea", "मटर"),
        setOf("paddy", "dhan", "rice", "धान", "चावल", "chawal"),
        setOf("maize", "makka", "corn", "मक्का"),
        setOf("bajra", "pearl millet", "बाजरा"),
        setOf("urad", "mash", "उड़द", "उरद"),
        setOf("moong", "mung", "मूंग", "मूँग"),
        setOf("soybean", "soyabean", "soya", "सोयाबीन", "सोया"),
        setOf("masoor", "lentil", "मसूर"),
        setOf("til", "sesame", "तिल"),
        setOf("jau", "barley", "जौ"),
        setOf("potato", "aloo", "alu", "आलू"),
        setOf("onion", "pyaz", "kanda", "प्याज", "कांदा"),
        setOf("garlic", "lahsun", "लहसुन")
    )

    /**
     * Transliterates Devanagari text into Latin phonetic characters.
     * Also normalizes Devanagari numerals to standard ASCII digits.
     */
    fun devanagariToLatin(text: String): String {
        val result = StringBuilder(text.length * 2)
        var i = 0
        val chars = text.toCharArray()
        val n = chars.size

        while (i < n) {
            val c = chars[i]
            val digit = DIGITS[c]
            if (digit != null) {
                result.append(digit)
                i++
                continue
            }

            val vowel = VOWELS[c]
            if (vowel != null) {
                result.append(vowel)
                i++
                continue
            }

            val consonant = CONSONANTS[c]
            if (consonant != null) {
                // Check look-ahead for nukta, halant or matras
                var nextIndex = i + 1
                if (nextIndex < n && chars[nextIndex] == NUKTA) {
                    nextIndex++
                }

                if (nextIndex < n && chars[nextIndex] == HALANT) {
                    // Halant kills the inherent vowel
                    result.append(consonant)
                    i = nextIndex + 1
                    continue
                } else if (nextIndex < n && MATRAS.containsKey(chars[nextIndex])) {
                    result.append(consonant).append(MATRAS[chars[nextIndex]])
                    i = nextIndex + 1
                    continue
                } else if (nextIndex < n && (CONSONANTS.containsKey(chars[nextIndex]) || VOWELS.containsKey(chars[nextIndex]))) {
                    // Consonant followed by another consonant gets inherent 'a'
                    result.append(consonant).append('a')
                    i = nextIndex
                    continue
                } else {
                    // Word boundary or end of token
                    result.append(consonant)
                    i = nextIndex
                    continue
                }
            }

            if (c == '\u0901' || c == '\u0902') {
                result.append('n')
            } else if (c == VISARGA) {
                result.append('h')
            } else if (c != NUKTA && c != HALANT) {
                result.append(c)
            }
            i++
        }

        return result.toString()
    }

    /**
     * Computes a normalized phonetic key for text across Latin and Devanagari.
     * Collapses aspirated consonants (bh->b, dh->d), w/v, sh/s, ee/i, oo/u,
     * and collapses repeated adjacent characters.
     */
    fun phoneticKey(text: String): String {
        var s = devanagariToLatin(text).lowercase()

        val replacements = listOf(
            "singh" to "sin", "sinh" to "sin", "sing" to "sin",
            "ph" to "f", "w" to "v", "ee" to "i", "oo" to "u",
            "sh" to "s", "kh" to "k", "gh" to "g", "ch" to "c",
            "chh" to "c", "jh" to "j", "th" to "t", "dh" to "d",
            "bh" to "b", "z" to "j", "q" to "k", "x" to "ks",
            "ey" to "e", "aa" to "a", "ai" to "e", "au" to "o",
            "ng" to "n", "nh" to "n"
        )
        for ((old, new) in replacements) {
            s = s.replace(old, new)
        }

        // Collapse duplicate adjacent letters
        val collapsed = StringBuilder(s.length)
        for (ch in s) {
            if (collapsed.isEmpty() || collapsed.last() != ch || !ch.isLetter()) {
                collapsed.append(ch)
            }
        }
        return collapsed.toString()
    }

    /**
     * Extracts the consonant skeleton (preserves the first letter and drops internal vowels).
     * This handles variations in vowel representations (e.g. "Bhagvan" vs "Bhagavan", "Nagla" vs "Nagala").
     */
    fun consonantSkeleton(text: String): String {
        val key = phoneticKey(text)
        val words = key.split(' ', '\t', '\n', '-', '_')
        val result = StringBuilder()

        for (w in words) {
            if (w.isEmpty()) continue
            if (result.isNotEmpty()) result.append(' ')
            result.append(w.first())
            for (j in 1 until w.length) {
                val ch = w[j]
                if (ch !in "aeiou" || !ch.isLetter()) {
                    result.append(ch)
                }
            }
        }
        return result.toString()
    }

    /**
     * Determines if [target] matches [query] using:
     * 1. Direct case-insensitive substring match
     * 2. Devanagari-normalized phonetic substring match
     * 3. Consonant skeleton substring match
     * 4. Mandi commodity synonym group match
     */
    fun matches(target: String?, query: String): Boolean {
        if (target.isNullOrBlank()) return false
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return true

        val tRaw = target.lowercase()
        val qRaw = cleanQuery.lowercase()

        // 1. Exact raw match
        if (tRaw.contains(qRaw)) return true

        // 2. Direct Devanagari numeral translation
        val qTrans = devanagariToLatin(qRaw)
        val tTrans = devanagariToLatin(tRaw)
        if (tTrans.contains(qTrans)) return true

        // 3. Phonetic key match
        val qKey = phoneticKey(cleanQuery)
        val tKey = phoneticKey(target)
        if (qKey.isNotEmpty() && tKey.contains(qKey)) return true

        // 4. Consonant skeleton match (for words with length >= 2)
        if (qKey.length >= 2) {
            val qSkeleton = consonantSkeleton(cleanQuery)
            val tSkeleton = consonantSkeleton(target)
            if (qSkeleton.isNotEmpty() && tSkeleton.contains(qSkeleton)) return true
        }

        // 5. Commodity synonym matching
        for (group in COMMODITY_SYNONYMS) {
            val queryInGroup = group.any { qRaw.contains(it) || qTrans.contains(it) || phoneticKey(it).contains(qKey) }
            if (queryInGroup) {
                val targetInGroup = group.any { tRaw.contains(it) || tTrans.contains(it) || phoneticKey(it).contains(tKey) }
                if (targetInGroup) return true
            }
        }

        return false
    }

    /**
     * Matches [query] against multiple candidate fields (e.g. farmer name, village, slip number, crop).
     * Returns true if any field matches.
     */
    fun matchesAny(query: String, vararg targets: String?): Boolean {
        if (query.isBlank()) return true
        return targets.any { matches(it, query) }
    }
}
