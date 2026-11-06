#!/usr/bin/env bash
set -euo pipefail

input_apk=""
output_apk=""
keystore=""
ks_type="PKCS12"
ks_alias=""
ks_pass=""
key_pass=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --out)
            output_apk="$2"
            shift 2
            ;;
        --ks)
            keystore="$2"
            shift 2
            ;;
        --ks-type)
            ks_type="$2"
            shift 2
            ;;
        --ks-alias)
            ks_alias="$2"
            shift 2
            ;;
        --ks-pass)
            ks_pass="$2"
            shift 2
            ;;
        --key-pass)
            key_pass="$2"
            shift 2
            ;;
        *)
            if [[ -z "$input_apk" ]]; then
                input_apk="$1"
                shift
            else
                shift
            fi
            ;;
    esac
done

if [[ -z "$input_apk" || -z "$output_apk" || -z "$keystore" ]]; then
    echo "Usage: $0 <input.apk> --out <output.apk> --ks <keystore> --ks-alias <alias> --ks-pass <pass> --key-pass <pass>"
    exit 1
fi

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
mapfile -t apksigners < <(find "$sdk_root/build-tools" -type f -name apksigner -print | sort -V)
if [[ ${#apksigners[@]} -eq 0 ]]; then
    echo "Error: apksigner not found"
    exit 1
fi
apksigner="${apksigners[${#apksigners[@]}-1]}"

cp "$input_apk" "$output_apk"

"$apksigner" sign \
    --ks "$keystore" \
    --ks-type "$ks_type" \
    --ks-key-alias "$ks_alias" \
    --ks-pass "pass:$ks_pass" \
    --key-pass "pass:$key_pass" \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    "$output_apk"

echo "Successfully signed APK: $output_apk"
exit 0
