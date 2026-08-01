# Physical-device install note

Date: 2026-07-31
Device: Xiaomi 25019PNF3C, Android 16 / HyperOS 3.0.306.0.WOACNXM

The debug APK built successfully, but the standard authorized command

```text
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

returned:

```text
INSTALL_FAILED_USER_RESTRICTED: Install canceled by user
```

Read-only checks showed developer options enabled, unknown-source install enabled, and no effective device-policy install restriction for the primary user. The remaining likely gate is Xiaomi/HyperOS's separate **Install via USB** developer setting or an equivalent device-side confirmation.

No security setting was changed or bypassed. Native physical UI/runtime verification remains pending until the device owner enables that standard install path. Private device captures remain under the ignored `research/private-captures/` directory.
