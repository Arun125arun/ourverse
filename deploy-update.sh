#!/usr/bin/env bash
# Builds the app and publishes it to https://ourverse-98c44.web.app so both
# phones can download the update remotely (Settings → Check for updates).
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$HOME/.local/jdk}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"

./gradlew assembleRelease -x uploadCrashlyticsMappingFileRelease

VC=$(grep -oP 'versionCode = \K[0-9]+' app/build.gradle.kts)
VN=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)

REPO="Arun125arun/ourverse-releases"
APK_URL="https://github.com/$REPO/releases/latest/download/ourverse.apk"
GHUB="$HOME/.local/bin/ghub"

# APK lives on GitHub Releases (Firebase's free plan forbids hosting APKs).
cp app/build/outputs/apk/release/app-release.apk /tmp/ourverse.apk
if "$GHUB" release view "v$VN" -R "$REPO" >/dev/null 2>&1; then
    "$GHUB" release upload "v$VN" /tmp/ourverse.apk -R "$REPO" --clobber
else
    "$GHUB" release create "v$VN" /tmp/ourverse.apk -R "$REPO" \
        --title "OurVerse $VN" --notes "OurVerse version $VN"
fi

# version.json + download page live on Firebase Hosting.
rm -f hosting/ourverse.apk
cat > hosting/version.json <<EOF
{"versionCode": $VC, "versionName": "$VN", "url": "$APK_URL"}
EOF

npx -y firebase-tools deploy --only hosting --project ourverse-98c44
echo
echo "Published version $VN (code $VC)"
echo "  Download page:  https://ourverse-98c44.web.app"
echo "  Direct APK:     $APK_URL"
