-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    static ** INSTANCE;
}
-keep class com.cardvault.app.data.** { *; }
-keep class com.cardvault.app.**$$serializer { *; }
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn org.bouncycastle.**
-dontwarn com.google.errorprone.annotations.**

