# Keep WebView JavaScript interface
-keepclassmembers class com.islamic.dashboard.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
