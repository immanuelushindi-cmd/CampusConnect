# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room entities
-keep class com.campusconnect.app.data.local.entity.** { *; }

# Domain models (used in Firestore deserialization)
-keep class com.campusconnect.app.domain.model.** { *; }

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class com.squareup.okhttp3.** { *; }

# Coil
-keep class coil.** { *; }
