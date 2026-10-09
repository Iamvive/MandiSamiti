package com.appwork.mandisamiti.platform

/** Desktop is a dev target: `MANDI_API_BASE_URL` if set, else a local backend. Never prod by default. */
actual val apiBaseUrl: String =
    System.getenv("MANDI_API_BASE_URL")?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: "http://localhost:8000"
