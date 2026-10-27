# R8 rules for OpenBuds release builds.
#
# Most libraries used here ship consumer rules (Compose, Coroutines, Glance,
# AndroidX). The rules below cover the app's own entry points that the platform
# instantiates by name and the reflection used against hidden Bluetooth APIs.

# Components declared in AndroidManifest.xml are instantiated by the framework.
# AGP generates rules for these, but keeping them explicit protects against
# manifest merges that only the release variant produces.
-keep class com.openbuds.app.MainActivity { *; }
-keep class com.openbuds.app.service.** { *; }
-keep class com.openbuds.app.receiver.** { *; }
-keep class com.openbuds.app.widget.** { *; }

# Glance resolves AppWidget receivers and their state definitions at runtime.
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }

# The Bluetooth codec helpers call hidden framework methods through reflection
# (setCodecConfig, getCodecStatus, createRfcommSocket, ...). Those targets live in
# the platform, so this only keeps the Kotlin wrappers that hold the calls.
-keep class com.openbuds.app.bluetooth.** { *; }

# Kotlin metadata and annotations are read reflectively by serializers and by
# Compose tooling in release builds.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
