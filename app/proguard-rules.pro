# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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

# ─────────────────────────────────────────────────────────────────────────────
# Production rules for Fruit OS Launcher (high perf, no crash in release)
# ─────────────────────────────────────────────────────────────────────────────

# DataStore / Preferences (serialization + internal)
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# Haze (Chris Banes) - reflection / internal for blur
-keep class dev.chrisbanes.haze.** { *; }
-keep class androidx.compose.ui.graphics.layer.** { *; }

# App widgets (AppWidgetHost, provider info, etc) - critical, host views use reflection
-keep class android.appwidget.** { *; }
-keep class com.stanleycx.fruitos.data.WidgetPlacement { *; }
-keep class com.stanleycx.fruitos.data.WidgetLayout { *; }
-keep class com.stanleycx.fruitos.ui.widget.** { *; }

# Our data models used in (de)serialization (JSON + toLayout)
-keep class com.stanleycx.fruitos.data.LauncherLayout { *; }
-keep class com.stanleycx.fruitos.data.LayoutItem { *; }
-keep class com.stanleycx.fruitos.data.LayoutItem$** { *; }
-keep class com.stanleycx.fruitos.data.AppInfo { *; }
-keep class com.stanleycx.fruitos.data.HomeItem { *; }
-keep class com.stanleycx.fruitos.data.HomeItem$** { *; }
-keep class com.stanleycx.fruitos.data.AppCategory { *; }

# Notification listener
-keep class com.stanleycx.fruitos.data.NotificationService { *; }

# Keep enums used in settings persistence (name())
-keepclassmembers enum com.stanleycx.fruitos.ui.components.GlassLevel { *; }
-keepclassmembers enum com.stanleycx.fruitos.ui.components.GlassTint { *; }
-keepclassmembers enum com.stanleycx.fruitos.ui.components.LoupeLevel { *; }
-keepclassmembers enum com.stanleycx.fruitos.ui.components.GlossLevel { *; }

# Compose + Kotlin reflection for stability / remember keys if any
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }
-keepclassmembers class **.Companion { *; }

# Coil (declared, even if not heavily used yet for safety)
-keep class coil.** { *; }

# For custom shapes / IconShape used in clip / path
-keep class com.stanleycx.fruitos.ui.components.IosIconShape { *; }

# Prevent removal of Haptics, important for UX
-keep class com.stanleycx.fruitos.ui.components.Haptics { *; }

# Parcel / Bundle if any widget config passes
-keep class android.os.Parcel { *; }

# Keep the contracts for widget bind/configure activity results
-keep class com.stanleycx.fruitos.data.BindWidgetContract { *; }
-keep class com.stanleycx.fruitos.data.ConfigureWidgetContract { *; }
-keep class com.stanleycx.fruitos.data.BindWidgetInput { *; }
-keep class com.stanleycx.fruitos.data.ConfigureWidgetInput { *; }