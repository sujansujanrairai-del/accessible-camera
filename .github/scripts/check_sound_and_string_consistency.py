#!/usr/bin/env python3
"""
Validates string consistency and accessibility resource integrity.
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

def main():
    strings_file = Path("app/src/main/res/values/strings.xml")
    if not strings_file.exists():
        print(f"Error: {strings_file} not found", file=sys.stderr)
        return 1

    try:
        tree = ET.parse(strings_file)
        root = tree.getroot()
    except Exception as e:
        print(f"Error parsing {strings_file}: {e}", file=sys.stderr)
        return 1

    required_keys = ["app_name", "app_description", "developer_credit", "welcome_start_button"]
    found_keys = set()
    for child in root.findall("string"):
        name = child.attrib.get("name")
        if name:
            found_keys.add(name)
            text = child.text or ""
            # Verify no emoji in string values
            for char in text:
                if ord(char) > 0x1F000:
                    print(f"Error: Emoji found in string '{name}': {text}", file=sys.stderr)
                    return 1

    missing = [k for k in required_keys if k not in found_keys]
    if missing:
        print(f"Error: Missing required string resources: {missing}", file=sys.stderr)
        return 1

    print(f"Resource consistency check passed: {len(found_keys)} valid strings verified.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
