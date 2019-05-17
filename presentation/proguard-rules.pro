-dontobfuscate

# android-smsmms
# -keep class android.net.** { *; }
-dontwarn android.net.ConnectivityManager
-dontwarn android.net.LinkProperties

# autodispose
-dontwarn com.uber.autodispose.**

# ez-vcard
-dontwarn ezvcard.**
-dontwarn org.apache.log.**
-dontwarn org.apache.log4j.**
-dontwarn org.python.core.**

# okio
-dontwarn okio.**

# umeng
-keep class com.umeng.** {*;}
-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep public class org.goodev.rms.R$*{
public static final int *;
}