#!/usr/bin/env python3
"""
Summarizes unit test failures from JUnit XML reports.
"""
import glob
import os
import xml.etree.ElementTree as ET

def main():
    report_files = glob.glob("app/build/test-results/testDebugUnitTest/*.xml")
    failures = []
    for file in report_files:
        try:
            tree = ET.parse(file)
            root = tree.getroot()
            for testcase in root.iter("testcase"):
                failure = testcase.find("failure")
                if failure is not None:
                    name = testcase.attrib.get("name", "unknown")
                    classname = testcase.attrib.get("classname", "unknown")
                    msg = failure.attrib.get("message", failure.text or "")
                    failures.append(f"{classname} > {name}: {msg.strip()}")
        except Exception:
            pass

    if failures:
        print("\n".join(failures))
    else:
        print("No test failures found in JUnit reports.")

if __name__ == "__main__":
    main()
