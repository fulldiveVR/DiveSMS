#!/usr/bin/env python3
"""
Update index.json with all categories from metadata YAML files

Usage:
    python update_index.py
"""

import json
import yaml
from pathlib import Path
from datetime import datetime

METADATA_DIR = Path(__file__).parent / "metadata_divesms"
INDEX_FILE = METADATA_DIR / "index.json"

def main():
    print("=" * 80)
    print("📇 Updating index.json")
    print("=" * 80)
    print()

    # Load existing index
    if INDEX_FILE.exists():
        with open(INDEX_FILE, 'r', encoding='utf-8') as f:
            index = json.load(f)
    else:
        index = {
            "version": "1.0",
            "project": "Wize SMS (DiveSMS)",
            "default_locale": "en",
            "description": "String metadata for Wize SMS, organized by category for efficient AI-powered translation",
            "notes": {
                "format": "Split YAML format with category-based organization",
                "benefits": "Token-efficient loading, easier maintenance, scalable architecture"
            }
        }

    # Scan all YAML files
    categories = {}
    files = {}
    total_strings = 0

    yaml_files = sorted(METADATA_DIR.glob("*.yaml"))

    print(f"📂 Scanning {len(yaml_files)} category files...")
    print()

    for yaml_file in yaml_files:
        category = yaml_file.stem

        with open(yaml_file, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f) or {}

        string_keys = sorted(data.keys())
        categories[category] = string_keys
        files[category] = f"metadata_divesms/{yaml_file.name}"
        total_strings += len(string_keys)

        print(f"  ✓ {category:20s}: {len(string_keys):3d} strings")

    # Update index
    index["categories"] = categories
    index["files"] = files
    index["total_strings"] = total_strings
    index["documented_strings"] = total_strings
    index["last_updated"] = datetime.now().strftime("%Y-%m-%d")

    # Save index
    with open(INDEX_FILE, 'w', encoding='utf-8') as f:
        json.dump(index, f, indent=2, ensure_ascii=False)

    print()
    print(f"💾 Saved index.json")
    print()
    print("=" * 80)
    print(f"✅ Index updated!")
    print(f"   Categories: {len(categories)}")
    print(f"   Total strings: {total_strings}")
    print("=" * 80)

if __name__ == "__main__":
    main()
