# Project-specific R8 rules.
#
# The com.lbs.schoolhelper package is intentionally not kept: application
# classes remain eligible for shrinking, optimization, and obfuscation.
# Android components referenced by the manifest/resources and library rules
# supplied by their artifacts are handled by the Android Gradle Plugin/R8.

# Gson-backed local cache models use field names as their persisted JSON keys.
# Keep those field names stable across app updates while still allowing the
# model classes themselves to be obfuscated.
-keepclassmembers,allowoptimization class com.lbs.schoolhelper.data.model.** {
    <fields>;
}
-keepclassmembers,allowoptimization class com.lbs.schoolhelper.data.profile.** {
    <fields>;
}

# Retrofit/Gson response DTOs are populated by reflection. Keep their JSON
# field names and members while allowing the DTO classes themselves to rename.
-keepclassmembers,allowoptimization class com.lbs.schoolhelper.data.remote.dto.** {
    <fields>;
}

# The notice service returns these types through a suspend Retrofit method.
# Keep the concrete response types so R8 full mode cannot erase the generic
# response shape used by Retrofit's converter.
-keep,allowoptimization class com.lbs.schoolhelper.data.remote.dto.NoticeListResponseDto { *; }
-keep,allowoptimization class com.lbs.schoolhelper.data.remote.dto.NoticeSummaryDto { *; }

# Keep generic signatures and Gson/Retrofit annotations used by reflective
# JSON conversion. Gson 2.13.2 and Retrofit 3.0.0 also ship their own
# consumer rules; these attributes cover the app's generic DTO usage.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
