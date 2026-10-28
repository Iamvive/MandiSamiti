package com.appwork.mandisamiti.domain.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BilingualSearchEngineTest {

    @Test
    fun testEnglishFarmerMatchesHindiSearch() {
        // Farmer saved in English as "Bhagvan Singh"
        assertTrue(BilingualSearchEngine.matches("Bhagvan Singh", "भगवान"))
        assertTrue(BilingualSearchEngine.matches("Bhagvan Singh", "bhagwan"))
        assertTrue(BilingualSearchEngine.matches("Bhagvan Singh", "सिंह"))
        assertTrue(BilingualSearchEngine.matches("Bhagvan Singh", "singh"))

        // Farmer saved as "RadheyShyam"
        assertTrue(BilingualSearchEngine.matches("RadheyShyam", "राधे"))
        assertTrue(BilingualSearchEngine.matches("RadheyShyam", "राधेश्याम"))
        assertTrue(BilingualSearchEngine.matches("RadheyShyam", "radhe"))
        assertTrue(BilingualSearchEngine.matches("RadheyShyam", "shyam"))
    }

    @Test
    fun testHindiFarmerMatchesEnglishSearch() {
        // Farmer saved in Hindi
        assertTrue(BilingualSearchEngine.matches("सुरेश कुमार", "suresh"))
        assertTrue(BilingualSearchEngine.matches("सुरेश कुमार", "kumar"))
        assertTrue(BilingualSearchEngine.matches("रामेश्वर प्रसाद", "rameshwar"))
        assertTrue(BilingualSearchEngine.matches("रामेश्वर प्रसाद", "ramesh"))
    }

    @Test
    fun testVillageNameBilingualMatching() {
        // Villages saved in English or Hindi
        assertTrue(BilingualSearchEngine.matches("Nagla", "नगला"))
        assertTrue(BilingualSearchEngine.matches("नगला", "nagla"))
        assertTrue(BilingualSearchEngine.matches("Kheda", "खेड़ा"))
        assertTrue(BilingualSearchEngine.matches("खेड़ा", "kheda"))
        assertTrue(BilingualSearchEngine.matches("खेड़ा", "keda"))
    }

    @Test
    fun testCommoditySynonymsMatching() {
        // Wheat / Gehun synonyms
        assertTrue(BilingualSearchEngine.matches("गेहूं", "wheat"))
        assertTrue(BilingualSearchEngine.matches("गेहूं", "gehu"))
        assertTrue(BilingualSearchEngine.matches("गेहूं", "gehun"))
        assertTrue(BilingualSearchEngine.matches("Wheat", "गेहूं"))
        assertTrue(BilingualSearchEngine.matches("Wheat", "gehu"))

        // Mustard / Sarson synonyms
        assertTrue(BilingualSearchEngine.matches("सरसों", "mustard"))
        assertTrue(BilingualSearchEngine.matches("सरसों", "sarso"))
        assertTrue(BilingualSearchEngine.matches("Mustard", "सरसों"))

        // Chana synonyms
        assertTrue(BilingualSearchEngine.matches("चना", "gram"))
        assertTrue(BilingualSearchEngine.matches("चना", "chickpea"))
        assertTrue(BilingualSearchEngine.matches("Gram", "चना"))
    }

    @Test
    fun testDevanagariNumeralsMatching() {
        assertTrue(BilingualSearchEngine.matches("14", "१४"))
        assertTrue(BilingualSearchEngine.matches("Slip 104", "१०४"))
        assertTrue(BilingualSearchEngine.matches("9876543210", "९८७६५४३२१०"))
    }

    @Test
    fun testMatchesAnyAcrossMultipleFields() {
        val farmer = "Bhagvan Singh"
        val village = "Nagla"
        val slip = "104"
        val crop = "गेहूं"

        assertTrue(BilingualSearchEngine.matchesAny("भगवान", farmer, village, slip, crop))
        assertTrue(BilingualSearchEngine.matchesAny("nagla", farmer, village, slip, crop))
        assertTrue(BilingualSearchEngine.matchesAny("१०४", farmer, village, slip, crop))
        assertTrue(BilingualSearchEngine.matchesAny("wheat", farmer, village, slip, crop))
        assertFalse(BilingualSearchEngine.matchesAny("सोयाबीन", farmer, village, slip, crop))
    }
}
