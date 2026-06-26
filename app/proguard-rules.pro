# Suppress missing optional SDK classes from Adivery's dependencies
-dontwarn com.mbridge.msdk.nativex.view.MBMediaView
-dontwarn com.mbridge.msdk.video.bt.module.orglistener.g

# AdTrace
-keep class io.adtrace.sdk.** { *; }
-keep interface io.adtrace.sdk.** { *; }

# Adivery
-keep class com.adivery.sdk.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# App models (used with Gson)
-keep class ir.sospans.lalastories.model.** { *; }

# Coil
-keep class coil.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
