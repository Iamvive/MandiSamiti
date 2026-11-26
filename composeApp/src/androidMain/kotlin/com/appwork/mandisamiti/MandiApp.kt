package com.appwork.mandisamiti

import android.app.Application
import com.appwork.mandisamiti.platform.PiiScrubber
import io.sentry.android.core.SentryAndroid

/** Starts crash reporting only when a DSN is built in. No user, no request bodies, no UI-text breadcrumbs. */
class MandiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.SENTRY_DSN.isBlank()) return
        SentryAndroid.init(this) { o ->
            o.dsn = BuildConfig.SENTRY_DSN
            o.environment = if (BuildConfig.DEBUG) "${BuildConfig.SENTRY_ENV}-debug" else BuildConfig.SENTRY_ENV
            o.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}"
            o.isSendDefaultPii = false
            o.tracesSampleRate = 0.0
            o.isEnableUserInteractionBreadcrumbs = false
            o.isEnableUserInteractionTracing = false
            o.isAttachScreenshot = false
            o.isAttachViewHierarchy = false
            o.setBeforeBreadcrumb { b, _ -> b.apply { message = message?.let(PiiScrubber::scrub) } }
            o.setBeforeSend { event, _ ->
                event.apply {
                    message?.let { m -> m.message = m.message?.let(PiiScrubber::scrub) }
                    exceptions?.forEach { it.value = it.value?.let(PiiScrubber::scrub) }
                }
            }
        }
    }
}
