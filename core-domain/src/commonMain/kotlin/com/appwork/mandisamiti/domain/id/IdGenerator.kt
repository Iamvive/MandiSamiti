package com.appwork.mandisamiti.domain.id

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Globally unique entry ids, so entries made offline on different phones never collide when synced. */
object IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    fun newId(): String = Uuid.random().toString()
}
