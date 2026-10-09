package com.appwork.mandisamiti

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** BUG-1 regression: every screen and the sync engine must use the session's server shop id, never a literal. */
class NoDefaultShopIdTest {
    @Test
    fun productionSourcesNeverHardCodeShopDefault() {
        // jvmTest runs with the composeApp module dir as working directory.
        val roots = listOf(File("src/commonMain"), File("../core-data/src/commonMain"))
        roots.forEach { assertTrue(it.isDirectory, "missing source dir ${it.absolutePath}") }

        val offenders = roots.flatMap { root ->
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" && it.readText().contains("\"shop_default\"") }
                .map { it.path }
                .toList()
        }

        assertTrue(offenders.isEmpty(), "\"shop_default\" literal found in: $offenders")
    }
}
