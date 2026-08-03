# Keep Bluetooth HID reflection / callbacks
-keep class android.bluetooth.** { *; }
-keepclassmembers class * extends android.bluetooth.BluetoothHidDevice$Callback { *; }

# App models used by Gson-less JSON (manual) — keep Parcelables if added
-keepclassmembers class com.tapboard.app.** {
    <fields>;
}

-dontwarn javax.annotation.**
