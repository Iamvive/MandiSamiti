package com.appwork.mandisamiti.domain.model

/** Slip number for display; falls back to the short id for rows created before slip numbers existed. */
fun slipLabel(slipNo: Long?, id: String): String = slipNo?.toString() ?: id.takeLast(6)
