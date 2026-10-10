package com.appwork.mandisamiti.ui.tutorial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MandiTutorialTest {

    @Test
    fun allTutorialsCatalogIntegrity() {
        val tutorials = MandiTutorial.ALL_TUTORIALS
        assertEquals(5, tutorials.size, "Must contain exactly 5 core tutorials")

        val ids = tutorials.map { it.id }
        assertEquals(ids.distinct().size, ids.size, "All tutorial IDs must be unique")

        tutorials.forEach { tutorial ->
            assertTrue(tutorial.id.isNotBlank(), "Tutorial ID must not be blank")
            assertTrue(tutorial.titleHindi.isNotBlank(), "Hindi title must not be blank for ${tutorial.id}")
            assertTrue(tutorial.titleEnglish.isNotBlank(), "English title must not be blank for ${tutorial.id}")
            assertTrue(tutorial.durationTextHindi.isNotBlank(), "Hindi duration must not be blank for ${tutorial.id}")
            assertTrue(tutorial.durationTextEnglish.isNotBlank(), "English duration must not be blank for ${tutorial.id}")
            assertTrue(tutorial.descriptionHindi.isNotBlank(), "Hindi description must not be blank for ${tutorial.id}")
            assertTrue(tutorial.descriptionEnglish.isNotBlank(), "English description must not be blank for ${tutorial.id}")

            // Offline resilience check: at least 3 fallback steps
            assertTrue(tutorial.fallbackStepsHindi.size >= 3, "Must have at least 3 Hindi fallback steps for ${tutorial.id}")
            assertTrue(tutorial.fallbackStepsEnglish.size >= 3, "Must have at least 3 English fallback steps for ${tutorial.id}")

            assertTrue(tutorial.videoUrl().startsWith("https://www.youtube.com/watch?v="), "Video URL must be a valid YouTube URL for ${tutorial.id}")
        }
    }

    @Test
    fun fromIdLookupWorks() {
        val dealTutorial = MandiTutorial.fromId("deal_entry")
        assertNotNull(dealTutorial)
        assertEquals(MandiTutorial.DEAL_ENTRY, dealTutorial)

        val gallaTutorial = MandiTutorial.fromId("galla_register")
        assertNotNull(gallaTutorial)
        assertEquals(MandiTutorial.GALLA_REGISTER, gallaTutorial)

        val unknown = MandiTutorial.fromId("non_existent_id")
        assertNull(unknown)
    }

    @Test
    fun localizedAccessorsWork() {
        val tutorial = MandiTutorial.DEAL_ENTRY

        assertEquals(tutorial.titleEnglish, tutorial.title(isEnglish = true))
        assertEquals(tutorial.titleHindi, tutorial.title(isEnglish = false))

        assertEquals(tutorial.durationTextEnglish, tutorial.duration(isEnglish = true))
        assertEquals(tutorial.durationTextHindi, tutorial.duration(isEnglish = false))

        assertEquals(tutorial.descriptionEnglish, tutorial.description(isEnglish = true))
        assertEquals(tutorial.descriptionHindi, tutorial.description(isEnglish = false))

        assertEquals(tutorial.fallbackStepsEnglish, tutorial.fallbackSteps(isEnglish = true))
        assertEquals(tutorial.fallbackStepsHindi, tutorial.fallbackSteps(isEnglish = false))
    }
}
