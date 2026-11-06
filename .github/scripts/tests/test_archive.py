#!/usr/bin/env python3
"""
Unit tests for release archive handling.
"""
import unittest
import zipfile
import io

class TestReleaseArchive(unittest.TestCase):
    def test_zip_creation_and_integrity(self):
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:
            zf.writestr("test.txt", "Accessible Camera and Gallery")

        buf.seek(0)
        with zipfile.ZipFile(buf, "r") as zf:
            self.assertIn("test.txt", zf.namelist())
            content = zf.read("test.txt").decode("utf-8")
            self.assertEqual("Accessible Camera and Gallery", content)

    def test_zip_stored_integrity(self):
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w", zipfile.ZIP_STORED) as zf:
            zf.writestr("open-fire-release-unsigned.apk", b"PK00APKBYTES")

        buf.seek(0)
        with zipfile.ZipFile(buf, "r") as zf:
            info = zf.getinfo("open-fire-release-unsigned.apk")
            self.assertEqual(zipfile.ZIP_STORED, info.compress_type)

if __name__ == "__main__":
    unittest.main()
