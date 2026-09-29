# Keep data models
-keep class com.israadev.nuxlauncher.core.models.** { *; }
-keep class com.oracle.dalvik.** { *; }
-keep class com.movtery.zalithlauncher.game.input.CriticalNativeTest { *; }
-keep class org.lwjgl.glfw.CallbackBridge { *; }

# SDL Keep rules (JNI native methods and callbacks)
-keep class org.libsdl.app.** { *; }
-keep class com.movtery.zalithlauncher.game.sdl.** { *; }
