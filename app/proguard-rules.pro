# KanjiLens ProGuard Rules (R8 Full Mode)

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_** { *; }

# Keep Retrofit interfaces and annotations
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.internal.platform.**

# Keep Room entities and DAOs
-keep class com.jworks.kanjilens.data.local.entities.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.internal.codegen.**
-keepclassmembers class * {
    @dagger.* <fields>;
    @javax.inject.* <fields>;
}

# Google Play Billing
-keep class com.android.vending.billing.** { *; }

# Kotlinx Serialization
-keepattributes RuntimeVisibleAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor (used by Supabase)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Supabase
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# Kuromoji
-keep class com.atilika.kuromoji.** { *; }
-keepclassmembers class com.atilika.kuromoji.** { *; }

# SLF4J (optional logging backend - used by Kuromoji)
-dontwarn org.slf4j.**

# R8 Full Mode: keep default constructors for classes used in reflection
-keepclassmembers class * {
    public <init>();
}

# Coroutines
-dontwarn kotlinx.coroutines.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Suppress warnings for missing classes in dependencies
-dontwarn java.lang.invoke.StringConcatFactory
