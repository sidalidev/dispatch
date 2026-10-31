# Changelog

## 0.4.0

- Dispatch has its own color now: lavender, the same on the icon and in the app, in light and dark. It no longer takes its palette from the wallpaper. Pages are cream in light and pine night in dark, the family colors of sidali.dev apps.
- New icon: a rounded paper plane on flat lavender, with a themed version that follows the system colors.
- New type: Bricolage Grotesque for titles, Figtree for text, both embedded and both open source (SIL Open Font License). Your system text size still applies.
- Contrast checked line by line: every text passes WCAG AA in light and dark. Links and actions use a deeper lavender where the brand color would be too light.
- The illustration on the home screen shows your app in honey, next to Dispatch in lavender.
- The favorites card keeps its size while the list of apps loads, so the page no longer jumps.
- First release, signed with the release key published in SECURITY.md. Updates come through Obtainium from the GitHub releases: About says so, since Dispatch has no internet access to update itself.

## 0.3.1

- Permissions screen, from About: Dispatch asks for none, and says so line by line. Permissions, notifications and battery show their real state, read again each time you come back, with why Dispatch does not need them.
- Tap a line to open the exact Settings page, to turn it on or off: the app's page, its notifications, its battery usage. No reminders, no repeated requests.

## 0.3.0

- About screen: our principles, the version and what's new, the date of the last security review, the signing certificate of the installed app (to compare with SECURITY.md), a way to report a security issue, the privacy policy and a contact address.
- "What leaves your device": nothing. Dispatch has no internet access; it only hands what you share to the app you pick.
- Erase everything: resets favorites and settings and deletes the private copies of copied images, after you type ERASE.
- The home screen links to About instead of its old privacy line.

## 0.2.0

- Dispatch now wears the colors of your phone: the palette comes from your wallpaper (Material You) in light and dark, on Android 12 and newer. Android 11 gets an indigo palette of its own. No fixed blue anymore.
- The home screen illustration (Share, Dispatch, your app) is drawn in the same palette.
- Themed icon: the monochrome layer is redrawn to sit like the system's own themed icons.
- Predictive back: swipe back and the share sheet follows your finger, then closes, or settles back if you change your mind.
- Switches stay green, tuned to the palette; step numbers, buttons and capsules use the palette's tones.
- The launch window matches the first frame in every palette, so there is no flash.

## 0.1.0

- First version: a share target that shows your favorite apps in your order, then every other app that can receive what you share.
- Targets are exact share screens (Instagram Messages, not all of Instagram); pin, hide, reorder by touch and hold or with Move up and Move down.
- Copy (links cleaned of tracking parameters), Copy image, Save.
- "Remember last used", off by default.
- No permission at all. App data excluded from backups and device transfers. Reproducible builds.
