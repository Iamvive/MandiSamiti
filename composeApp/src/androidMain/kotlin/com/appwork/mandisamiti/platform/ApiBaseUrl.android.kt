package com.appwork.mandisamiti.platform

import com.appwork.mandisamiti.BuildConfig

/** Release: HTTPS production; debug: `mandi.apiBaseUrl` from local.properties, else the emulator host. */
actual val apiBaseUrl: String = BuildConfig.API_BASE_URL
