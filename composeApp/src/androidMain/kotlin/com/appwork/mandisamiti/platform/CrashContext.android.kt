package com.appwork.mandisamiti.platform

import io.sentry.Sentry

actual object CrashContext {
    actual fun setScreen(name: String) {
        Sentry.setTag("screen", name)
    }
}
