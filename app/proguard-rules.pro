# Add project specific ProGuard rules here.
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-keep class org.opencv.** { *; }
-keepattributes *Annotation*
-keep class com.docscan.app.model.** { *; }