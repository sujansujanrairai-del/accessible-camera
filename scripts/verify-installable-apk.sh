#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <path-to-apk>"
    exit 1
fi

apk="$1"
if [[ ! -f "$apk" ]]; then
    echo "Error: APK file does not exist: $apk"
    exit 1
fi

echo "Verifying APK integrity: $apk"
unzip -tq "$apk"

# Verify mandatory entries
for entry in "AndroidManifest.xml" "classes.dex" "resources.arsc"; do
    if ! unzip -l "$apk" | grep -F "$entry" >/dev/null 2>&1; then
        echo "Error: Missing required entry '$entry' in $apk"
        exit 1
    fi
done

echo "APK verification successful: $apk"
exit 0
