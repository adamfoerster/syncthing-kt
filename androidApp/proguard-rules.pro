# R8 rules for release builds. Keep readable stack traces via the mapping file uploaded to the stores.
-keepattributes SourceFile,LineNumberTable,*Annotation*,InnerClasses,EnclosingMethod,Signature
-renamesourcefileattribute SourceFile

# kotlinx.serialization: the REST DTOs are decoded reflectively through generated serializers.
-keepclassmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.adamfoerster.meshsync.**$$serializer { *; }

# Ktor logs through SLF4J, which is not shipped on Android.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
