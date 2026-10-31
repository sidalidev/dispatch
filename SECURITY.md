# Security policy

## Reporting a vulnerability

Please write to **contact@sidali.dev** with "Dispatch security issue" in the subject, or use GitHub's private vulnerability reporting on this repository, and do not post the issue anywhere public. You will get an answer within a week.

Include what an attacker can do, the steps to reproduce, and the version or commit. The app's About screen has a "Report a security issue" button that opens the same address.

## Scope

In scope: anything that lets another app read Dispatch's copies or settings, make it send content to an app the user did not pick, read a file through Dispatch that it was not given access to, make it use the network, or survive "Erase everything".

Out of scope: the clipboard itself (the focused app and the keyboard can read it; that is inherent to copying), and what the target app does with what you send it.

## Design notes

- No permission at all. The expected list is `docs/permissions.txt` (empty); CI checks the merged release manifest against it.
- No `QUERY_ALL_PACKAGES`: package visibility comes from `<queries>` for `SEND` and `SEND_MULTIPLE` intents.
- `ShareActivity` forwards only what it received, and only to the activity the user tapped, with read access to the shared URIs (never write access it was not given).
- `ClipProvider` is not exported. It serves only names matching `clip-<digits>.<png|jpg|webp|gif|heic|avif>` from the app's no-backup directory, read-only.
- Backups and device-to-device transfers of app data are disabled.
- Threat model: [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md). Reviews: [docs/security](docs/security).

## Release signing

Releases are signed with this certificate:

```
SHA-256: CD:9C:CF:A3:54:0D:49:A6:87:63:B7:B6:76:80:E8:5B:97:79:B6:26:ED:32:31:54:97:29:90:B4:48:E5:A1:D6
Owner:   CN=Sid Ali Bentifraouine, O=sidali.dev, C=FR
```

Check a downloaded APK with `apksigner verify --print-certs dispatch-<version>.apk`: the "certificate SHA-256 digest" line must match the fingerprint above (without the colons). Obtainium and Android refuse an update signed with another key.

The About screen reads the certificate of the installed APK from the system and shows its SHA-256 (tap it to see all of it). Compare it with the one above. Builds you make yourself are signed with your own debug key: their fingerprint will not match, and they should not be trusted as releases.
