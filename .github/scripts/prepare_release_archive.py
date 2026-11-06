#!/usr/bin/env python3
"""
Canonicalizes and packages APK release archives for CI distribution.
"""
import argparse
import os
import shutil
import subprocess
import sys
import zipfile

def normalize_apk(zipalign_bin, apks):
    for apk in apks:
        if not os.path.exists(apk):
            continue
        temp_apk = apk + ".tmp"
        if zipalign_bin and os.path.exists(zipalign_bin):
            cmd = [zipalign_bin, "-p", "-f", "4", apk, temp_apk]
            subprocess.run(cmd, check=True)
            shutil.move(temp_apk, apk)
        print(f"Normalized: {apk}")

def package_apks(output_zip, apks):
    with zipfile.ZipFile(output_zip, "w", zipfile.ZIP_STORED) as zf:
        for apk in apks:
            arcname = os.path.basename(apk)
            zf.write(apk, arcname=arcname)
            print(f"Packaged: {arcname} into {output_zip}")

def verify_zip(zip_path, expected_apks):
    if not os.path.exists(zip_path):
        print(f"Error: {zip_path} not found", file=sys.stderr)
        sys.exit(1)

    with zipfile.ZipFile(zip_path, "r") as zf:
        namelist = zf.namelist()
        for expected in expected_apks:
            base = os.path.basename(expected)
            if base not in namelist:
                print(f"Error: {base} not found in {zip_path}", file=sys.stderr)
                sys.exit(1)
            # Verify file can be read
            zf.read(base)
    print(f"Verified {zip_path} contains all {len(expected_apks)} expected APKs successfully.")

def main():
    parser = argparse.ArgumentParser(description="Release archive tool")
    subparsers = parser.add_subparsers(dest="command")

    norm_parser = subparsers.add_parser("normalize")
    norm_parser.add_argument("--zipalign", help="Path to zipalign binary")
    norm_parser.add_argument("apks", nargs="+", help="APKs to normalize")

    pkg_parser = subparsers.add_parser("package")
    pkg_parser.add_argument("--output", required=True, help="Output ZIP path")
    pkg_parser.add_argument("apks", nargs="+", help="APKs to include")

    ver_parser = subparsers.add_parser("verify")
    ver_parser.add_argument("zip_path", help="Archive to verify")
    ver_parser.add_argument("--expected-apk", action="append", default=[], help="Expected APK in zip")

    args = parser.parse_args()

    if args.command == "normalize":
        normalize_apk(args.zipalign, args.apks)
    elif args.command == "package":
        package_apks(args.output, args.apks)
    elif args.command == "verify":
        verify_zip(args.zip_path, args.expected_apk)
    else:
        parser.print_help()
        sys.exit(1)

if __name__ == "__main__":
    main()
