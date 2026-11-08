# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class ua.zvukaryk.app.**$$serializer { *; }
-keepclassmembers class ua.zvukaryk.app.** { *** Companion; }
-keepclasseswithmembers class ua.zvukaryk.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Anthropic Java SDK + Jackson (рефлексія для (де)серіалізації)
-keep class com.anthropic.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class kotlin.Metadata { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, RuntimeVisibleAnnotations
-keepclassmembers class * { @com.fasterxml.jackson.annotation.JsonCreator *; @com.fasterxml.jackson.annotation.JsonProperty *; }
-dontwarn com.fasterxml.jackson.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn java.beans.**
-dontwarn javax.annotation.**
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn java.lang.reflect.AnnotatedParameterizedType
-dontwarn java.lang.reflect.AnnotatedType
