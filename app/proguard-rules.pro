# ── Khwab Aura Unity JNI bridge ──────────────────────────────────────────────
#
# These classes are referenced by their fully-qualified names from Unity C#
# via AndroidJavaClass / CallStatic.  They must NEVER be renamed, moved, or
# removed by R8/ProGuard.
#
# Unity calls (from AuraAndroidBridge.cs):
#   new AndroidJavaClass("com.toblad.khwab.aura.UnityAuraBridgeCallback")
#   .CallStatic("onUnityReady")
#   .CallStatic("onAuraActivated")
#   .CallStatic("onAuraDeactivated")
#   .CallStatic("onUnityHeartbeat", ...)

-keep class com.toblad.khwab.aura.UnityAuraBridgeCallback {
    public static *;
}

-keep class com.toblad.khwab.aura.UnityAuraBridge {
    public static *;
}
