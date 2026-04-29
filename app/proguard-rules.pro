# POI / iText большие — оставляем большую часть
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn com.itextpdf.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**

# AWT/Swing/ImageIO не нужны на Android
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.imageio.**
-dontwarn com.sun.**
-dontwarn org.osgi.**

# JSoup и Log4j annotations
-dontwarn javax.annotation.**
-dontwarn aQute.bnd.annotation.spi.**

-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class com.itextpdf.** { *; }
-keep class com.vladsch.flexmark.** { *; }

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
