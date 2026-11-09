# VibedByKaKi/youprice-android

Lightweight pure-Kotlin Android client that shows the youprice.fr customer dashboard on one page: profile, orders and lines, consumption, invoices and support tickets.

| [![](.github/assets/get-it-on-github.png)](https://github.com/VibedByKaKi/youprice-android/releases/tag/latest) | [![](.github/assets/badge_obtainium.png)](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%257B%2522id%2522%253A%2520%2522net.kaki87.youprice%2522%252C%2520%2522url%2522%253A%2520%2522https%253A%252F%252Fgithub.com%252FVibedByKaKi%252Fyouprice-android%2522%252C%2520%2522author%2522%253A%2520%2522VibedByKaKi%2522%252C%2520%2522name%2522%253A%2520%2522YouPrice%2522%252C%2520%2522preferredApkIndex%2522%253A%25200%252C%2520%2522additionalSettings%2522%253A%2520%2522%257B%255C%2522includePrereleases%255C%2522%253A%2520true%252C%2520%255C%2522fallbackToOlderReleases%255C%2522%253A%2520true%252C%2520%255C%2522apkFilterRegEx%255C%2522%253A%2520%255C%2522.%252A%255C%255C%255C%255C.apk%2524%255C%2522%257D%2522%257D) |
| --- | --- |

The APK is a rolling GitHub prerelease on the [`latest`](https://github.com/VibedByKaKi/youprice-android/releases/tag/latest) tag. Direct download: [youprice.apk](https://github.com/VibedByKaKi/youprice-android/releases/download/latest/youprice.apk). In Obtainium, keep **Include pre-releases** on if you add the repo by hand.

## What this app does

- **No dependencies.** Pure Kotlin with no AndroidX, no Compose and no third-party library. The UI uses platform views, and the network layer uses `HttpURLConnection` and `org.json`.
- **One server.** The app contacts only `api.youprice.fr`. There is no analytics, no tracking and no Google Play Services.
- **Sign-in.** Email and password. youprice.fr sends a confirmation code the first time, then the app stores the trusted-device key it returns, so later sign-ins skip the code.
- **Remember password.** The password is stored only if the box is ticked, encrypted with an Android Keystore key.
- **Read-only.** Invoice PDFs, payments and account changes from the web dashboard are not included.

## Build

    ./gradlew assembleRelease

Requires JDK 17 and the Android SDK (API 35). The Android Gradle plugin is a build-time tool only and adds nothing to the APK.

## CI

- **Build** runs on every push to `main`, on pull requests and on manual dispatch. It builds a minified release APK.
- On `main`, it replaces the `latest` release with `youprice.apk`.
- The APK is signed with the release key when the repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD` and `KEY_ALIAS` (default `soulbrowser`) are set, and with the debug key otherwise.
