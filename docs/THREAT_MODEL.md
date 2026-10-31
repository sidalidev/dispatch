# Threat model

What Dispatch protects:

- What you share. It passes only to the app you tap, with the access Dispatch was given and no more (read access stays read access).
- Your choices (pinned and hidden apps, one switch). Other apps cannot read them: private storage, no exported provider, no backup, no device transfer.
- The private copies made by "Copy image". Only the app you paste into gets a one-off read grant, through the clipboard.
- Your network. Dispatch has no internet permission, so even a bug in it cannot send anything online.

Against whom: other apps on the phone, cloud backups, and a curious developer (there is no server, no account and nothing to hand over).

What it does not protect:

- The clipboard. The app in front and the keyboard can read what you copy. That is how copying works.
- What the target app does with what you send it.
- A phone that is unlocked in someone else's hands, or rooted, or running a hostile system.
- Data at rest is not encrypted by Dispatch beyond Android's file-based encryption. It holds no secret worth a key of its own.
- Erasing deletes files; flash storage may keep old blocks until they are reused. Android's file-based encryption makes them unreadable once the user's keys are gone (factory reset).

Updates: Dispatch cannot update itself, by design. Updates come from Obtainium or by hand, checked against the certificate fingerprint in SECURITY.md.

Cyber Resilience Act: Dispatch is free, non-commercial software with no paid tier, so it is outside the CRA's scope today. To revisit if that changes.
