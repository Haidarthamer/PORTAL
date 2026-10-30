# PORTAL

**Offline-first file intelligence for Android.** PORTAL v0.1 is the foundation for inspecting Windows PE files, Android APKs and iOS IPA archives locally on-device.

> This release is an inspection prototype, not a Windows compatibility runtime, decompiler, or APK/IPA converter.

## v0.1
- Native Kotlin + Jetpack Compose interface.
- Import a file using Android's system document picker (Storage Access Framework).
- Identify PE, APK/ZIP and IPA containers using signatures and bounded reads.
- PE machine / optional-header inspection (x86, x86-64, ARM64).
- APK ZIP inventory and native-library ABI discovery.
- IPA app bundle and Mach-O architecture discovery.
- No account, API key, network access, or root required for scanning.

## Build
Open the project in Android Studio, or run the **Android CI** workflow in GitHub Actions. The workflow builds a debug APK and uploads it as an artifact. The first workflow run is the actual build verification; this repository does not claim a successful build until Actions reports success.

Requirements: JDK 17, Android SDK platform 35, Gradle 8.11.1.

## Important technical boundaries
- Box64 executes Linux x86-64 ELF programs on ARM64; it does not directly execute Windows PE files. A Windows runtime generally needs Wine plus Box64 in a compatible Linux userspace.
- APK analysis cannot magically convert 32-bit native libraries to 64-bit. Native code needs a matching ABI build or source rebuild.
- IPA files use Apple formats/frameworks and cannot be directly converted into Android APKs.
- Exporting/re-signing an APK does not make it equivalent to the original signed application.
- Runtime execution, dynamic debugging, patching, decompilation and APK export are not implemented in v0.1.

Only inspect or modify software you own or are authorized to analyze. PORTAL does not provide DRM or access-control bypass features.

## Project layout
- `app/` — Android application and local inspection UI.
- `.github/workflows/android.yml` — cloud debug-APK build.
- `docs/ROADMAP.md` — staged engineering plan.

## License
MIT. Third-party runtime components, if added later, will retain their own licenses and notices.
