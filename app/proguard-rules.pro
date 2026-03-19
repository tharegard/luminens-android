# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Supabase serialization
-keep class com.luminens.android.data.model.** { *; }
-keepclassmembers class com.luminens.android.data.model.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# GPUImage
-keep class jp.co.cyberagent.android.gpuimage.** { *; }

# uCrop
-keep class com.yalantis.ucrop.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Ktor
-dontwarn io.ktor.**

# Coil
-dontwarn coil.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
