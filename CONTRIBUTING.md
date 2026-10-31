# Contributing to Dispatch

Thanks for helping. Dispatch does one thing; contributions that keep it small, private and fast are the most welcome.

## Ground rules

- **No permission, ever.** A pull request that adds any permission, directly or through a dependency, will not be merged. INTERNET and QUERY_ALL_PACKAGES especially. CI fails if the merged manifest asks for one.
- **No usage statistics.** Dispatch does not count shares or rank apps by use. The only automatic ordering is the opt-in "Remember last used".
- **No new dependency** without a strong reason. Today the app uses only AndroidX Compose and Activity.
- Keep it simple. If a feature needs a settings screen of its own, it probably belongs in another app.

## Getting started

1. JDK 21, Android SDK with platform 36.
2. `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :sharecheck:assembleDebug`
3. Install both APKs on a device or emulator, then share to Dispatch:
   `adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "https://example.com/?utm_source=x&id=3" -n dev.sidali.dispatch/.ShareActivity`
   Pin ShareCheck, tap it, and read `adb logcat -s ShareCheck`. For images, share a picture from the Files app.

## Pull requests

- One topic per pull request, with a short description of what changes for the user.
- Add or update unit tests in `app/src/test` for any change to `Links.kt` or `Order.kt`.
- Interface text is English only and goes in `values/strings.xml`. Sentence case, one verb per button.
- Update `CHANGELOG.md` under "Unreleased".

## Reporting bugs

Open an issue with your phone model, Android version (and ROM, e.g. GrapheneOS), what you shared from which app, and which target.
