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

# Gson TypeToken anonymous subclasses need their generic signature preserved,
# otherwise `object : TypeToken<List<X>>() {}` crashes at runtime under R8.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# App models (used with Gson)
-keep class ir.sospans.lalastories.model.** { *; }
-keep class ir.sospans.lalastories.remote.** { *; }

# InteractiveStoryRepository's private raw Gson DTOs (not in model/remote, so not
# covered by the rules above) - without this, R8 renames/merges their fields and
# Gson silently fails to populate them, since it matches JSON keys by field name.
-keep class ir.sospans.lalastories.repository.InteractiveStoryRepository$Raw* { *; }

# Coil
-keep class coil.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
