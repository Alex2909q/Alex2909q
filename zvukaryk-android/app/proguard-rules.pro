# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class ua.zvukaryk.app.**$$serializer { *; }
-keepclassmembers class ua.zvukaryk.app.** { *** Companion; }
-keepclasseswithmembers class ua.zvukaryk.app.** { kotlinx.serialization.KSerializer serializer(...); }
