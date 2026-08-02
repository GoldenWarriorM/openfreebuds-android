# Compose Multiplatform
-dontwarn org.jetbrains.compose.**
-keep class androidx.compose.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Activity result contracts / AndroidX
-keep class androidx.activity.** { *; }
-dontwarn androidx.activity.**
