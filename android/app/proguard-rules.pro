# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# kotlinx.serialization: keep generated serializers
-keepattributes *Annotation*
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class com.duoplan.app.data.remote.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.duoplan.app.data.remote.**$$serializer { *; }
