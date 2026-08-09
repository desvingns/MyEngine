# The Android shell starts the sandbox through concrete Kotlin entry points and loads the
# versioned content pack from APK assets. Keep the simulation/content packages stable while R8
# removes unused third-party code around them.
-keep class dev.myengine.android.** { *; }
-keep class dev.myengine.content.** { *; }
-keep class dev.myengine.core.** { *; }
-keep class dev.myengine.entities.** { *; }
-keep class dev.myengine.world.** { *; }
-keep class dev.myengine.ai.** { *; }
-keep class dev.myengine.logistics.** { *; }
-keep class dev.myengine.defense.** { *; }
-keep class dev.myengine.storyteller.** { *; }
-keep class dev.myengine.render.** { *; }
-keep class dev.myengine.games.sandbox.** { *; }
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,InnerClasses,EnclosingMethod
