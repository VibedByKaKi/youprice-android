# youprice-android

Android client that shows the youprice.fr customer dashboard on one page: profile, orders and lines, consumption, invoices and support tickets.

- Pure Kotlin, no AndroidX, no Compose, no third-party library. The UI uses platform views and the network layer uses `HttpURLConnection` and `org.json`.
- The only server contacted is `api.youprice.fr`. There is no analytics, no tracking and no Google Play Services.
- Email and password sign-in. youprice.fr sends a confirmation code the first time. The app then stores the trusted-device key it returns, so later sign-ins skip the code. The password is never stored.
- CI builds a minified release APK (`.github/workflows/build.yml`) and attaches it, as a plain `.apk` file, to the `latest` release: https://github.com/VibedByKaKi/youprice-android/releases/latest/download/youprice.apk. The APK is signed with the release key when the repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD` and `KEY_ALIAS` (default `soulbrowser`) are set, and with the debug key otherwise.

Invoice PDFs, payments and account changes from the web dashboard are not included. This app is read-only.

## Build

    ./gradlew assembleRelease

Requires JDK 17 and the Android SDK (API 35). The Android Gradle plugin is a build-time tool only. It adds nothing to the APK.
