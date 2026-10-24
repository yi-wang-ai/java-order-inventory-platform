#!/usr/bin/env python3
"""Fast source/package sanity checks without external dependencies."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parent.parent
required=["README.md","pom.xml","compose.yaml","Dockerfile",".env.example",".gitignore","LICENSE","SECURITY.md","CHANGELOG.md",
          "src/main/resources/application.yml","src/main/resources/schema.sql","src/main/resources/static/index.html",
          ".github/workflows/ci.yml",".github/workflows/integration.yml","scripts/demo.sh","scripts/test-core.sh","docs/TEST_REPORT.md"]
for item in required:
    assert (root/item).is_file(),f"Missing {item}"
ET.parse(root/'pom.xml')
readme=(root/'README.md').read_text()
for phrase in ["The problem","The solution","Quick start","Testing"]:
    assert phrase in readme,f"README lacks {phrase}"
assert not (root/'.env').exists(),"Release must not contain local secrets (.env)"
print('PASS: required files, POM XML, README sections and secret-file check')
