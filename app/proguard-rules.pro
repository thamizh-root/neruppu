# SLF4J
-dontwarn org.slf4j.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, EnclosingMethod
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# Room
-dontwarn androidx.room.paging.LimitOffsetPagingSource

# Hilt
-keep class dagger.hilt.android.internal.** { *; }
