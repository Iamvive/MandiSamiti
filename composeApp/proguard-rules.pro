# MandiSamiti R8 / ProGuard rules

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.appwork.mandisamiti.**$$serializer { *; }
-keepclassmembers class com.appwork.mandisamiti.** {
    *** Companion;
}
-keepclasseswithmembers class com.appwork.mandisamiti.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor / OkHttp
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**
-keep class io.ktor.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
