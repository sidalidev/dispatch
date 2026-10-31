#!/usr/bin/env bash
# Repository checks, run by CI after the release build. Run locally the same way: bash .github/checks.sh
set -euo pipefail
cd "$(dirname "$0")/.."
fail=0

# (a) The merged release manifest asks only for permissions listed in docs/permissions.txt.
manifests=$(find app/build/intermediates/merged_manifest/release -name AndroidManifest.xml 2>/dev/null || true)
if [ -z "$manifests" ]; then echo "No merged release manifest: run :app:assembleRelease first"; exit 1; fi
allowed=$(sed -e '/^#/d' -e '/^[[:space:]]*$/d' docs/permissions.txt | sort -u)
for m in $manifests; do
  asked=$(grep -oE '<uses-permission[^>]*android:name="[^"]+"' "$m" | sed -E 's/.*android:name="([^"]+)"/\1/' | sort -u || true)
  extra=$(comm -23 <(echo "$asked" | sed '/^$/d') <(echo "$allowed" | sed '/^$/d'))
  if [ -n "$extra" ]; then echo "Permission not in docs/permissions.txt ($m):"; echo "$extra"; fail=1; fi
done

# (b) No long or short dashes and no personal paths, in sources or docs.
dashes=$(printf '\xe2\x80\x93|\xe2\x80\x94')
if git grep -nIE "$dashes" -- . ; then echo "Use commas or periods instead of long dashes"; fail=1; fi
if git grep -nIE '/[U]sers/' -- . ; then echo "Remove personal paths"; fail=1; fi

# (c) The latest security review is less than 6 months old.
latest=$(ls docs/security | grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}\.md$' | sort | tail -1 | sed 's/\.md$//')
if [ -z "$latest" ]; then echo "No security review in docs/security"; fail=1
else
  limit=$(date -u -d '6 months ago' +%F 2>/dev/null || date -u -v-6m +%F)
  if [[ "$latest" < "$limit" ]]; then echo "Last security review $latest is older than 6 months"; fail=1; fi
fi

# (d) CHANGELOG.md has an entry for the app's versionName.
version=$(sed -nE 's/^[[:space:]]*versionName = "([^"]+)".*/\1/p' app/build.gradle.kts)
if ! grep -qE "^## ${version//./\\.}([[:space:]]|$)" CHANGELOG.md; then echo "CHANGELOG.md has no entry for $version"; fail=1; fi

[ "$fail" = 0 ] && echo "Repository checks passed (version $version, last review $latest)"
exit "$fail"
