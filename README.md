# AndroidFileManager

Android file manager with directory browser + embedded Python terminal (via Chaquopy).

## Features
- Browse device storage (with runtime permissions)
- Navigate folders, view text files
- Simple Python terminal that executes code using embedded CPython

## Build with GitHub Actions
Every push to `main` triggers a workflow that builds a **debug APK** and uploads it as an artifact.

1. Go to the **Actions** tab of this repository
2. Select the **Build APK** workflow
3. Download the APK artifact when the run finishes

You can also build locally:

```bash
# Open in Android Studio (recommended)
# or from command line after installing Android SDK + JDK 17+:
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions
The app requests:
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` (legacy)
- `MANAGE_EXTERNAL_STORAGE` (Android 11+) for broader access
- Storage Access Framework is used as a fallback

On modern Android, full unrestricted access is limited by the system unless the user grants the special "All files access" permission.

## Notes
- Chaquopy embeds Python. The free tier is sufficient for basic use.
- For release builds / Play Store you will need a signing key and to adjust the workflow.
- This is a starting point — expand the UI, add more file operations, and package extra Python libraries as needed.
