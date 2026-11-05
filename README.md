# youprice-android

Android client that shows the youprice.fr customer dashboard on one page: profile, orders and lines, consumption, invoices and support tickets.

- Pure Kotlin, no AndroidX, no Compose, no third-party library. The UI uses platform views and the network layer uses `HttpURLConnection` and `org.json`.
- The only server contacted is `api.youprice.fr`. There is no analytics, no tracking and no Google Play Services.
- Email and password sign-in. youprice.fr sends a confirmation code the first time. The app then stores the trusted-device key it returns, so later sign-ins skip the code. The password is never stored.
- CI builds a minified release APK (`.github/workflows/build.yml`) and uploads it as the `youprice-release-apk` artifact. The APK is signed with the debug key.

Invoice PDFs, payments and account changes from the web dashboard are not included. This app is read-only.

## Build

    ./gradlew assembleRelease

Requires JDK 17 and the Android SDK (API 35). The Android Gradle plugin is a build-time tool only. It adds nothing to the APK.
