# Keep data models and serialization
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @org.jetbrains.kotlinx.serialization.Serializable <fields>;
}
-keep class com.step2ck.scanquiz.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

