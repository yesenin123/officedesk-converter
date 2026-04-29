# POI / iText большие — оставляем большую часть
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn com.itextpdf.**
-dontwarn nl.siegmann.epublib.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**

-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class com.itextpdf.** { *; }
-keep class nl.siegmann.epublib.** { *; }
-keep class com.vladsch.flexmark.** { *; }

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
