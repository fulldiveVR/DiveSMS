#!/usr/bin/env python3
"""
Cleanup orphaned metadata entries.

This script removes metadata entries that no longer have corresponding
strings in the strings.xml file.
"""

import json
import yaml
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Dict, Set, List
from metadata_io import create_metadata_io


def get_strings_from_xml(xml_path: Path) -> Set[str]:
    """Extract all string keys from strings.xml."""
    if not xml_path.exists():
        print(f"✗ Strings file not found: {xml_path}")
        return set()

    tree = ET.parse(xml_path)
    root = tree.getroot()

    string_keys = set()
    for string_elem in root.findall('.//string'):
        key = string_elem.get('name')
        if key:
            string_keys.add(key)

    return string_keys


def find_orphaned_metadata(metadata_dir: Path, xml_path: Path) -> Dict[str, List[str]]:
    """
    Find metadata entries without corresponding strings.

    Returns:
        Dictionary mapping category to list of orphaned string keys
    """
    # Get all strings from XML
    xml_strings = get_strings_from_xml(xml_path)

    # Load metadata
    metadata_io = create_metadata_io(metadata_dir.parent, metadata_dir.name)

    orphaned_by_category = {}

    # Check each category
    index_path = metadata_dir / "index.json"
    with open(index_path, 'r', encoding='utf-8') as f:
        index = json.load(f)

    for category, string_keys in index.get('categories', {}).items():
        orphaned = []
        for string_key in string_keys:
            if string_key not in xml_strings:
                orphaned.append(string_key)

        if orphaned:
            orphaned_by_category[category] = orphaned

    return orphaned_by_category


def remove_orphaned_metadata(metadata_dir: Path, orphaned_by_category: Dict[str, List[str]], dry_run: bool = True):
    """
    Remove orphaned metadata from YAML files and update index.

    Args:
        metadata_dir: Path to metadata directory
        orphaned_by_category: Dictionary mapping category to orphaned keys
        dry_run: If True, only print what would be done
    """
    total_removed = 0

    for category, orphaned_keys in orphaned_by_category.items():
        category_path = metadata_dir / f"{category}.yaml"

        if not category_path.exists():
            print(f"⚠ Category file not found: {category_path}")
            continue

        # Load category file
        with open(category_path, 'r', encoding='utf-8') as f:
            category_data = yaml.safe_load(f) or {}

        # Remove orphaned keys
        removed_count = 0
        for key in orphaned_keys:
            if key in category_data:
                if dry_run:
                    print(f"  Would remove: {key}")
                else:
                    del category_data[key]
                    print(f"  ✓ Removed: {key}")
                removed_count += 1

        if removed_count > 0 and not dry_run:
            # Save updated category file
            with open(category_path, 'w', encoding='utf-8') as f:
                yaml.dump(category_data, f, default_flow_style=False, allow_unicode=True, sort_keys=True)
            print(f"✓ Updated {category}.yaml (removed {removed_count} entries)")

        total_removed += removed_count

    if not dry_run:
        # Update index.json
        index_path = metadata_dir / "index.json"
        with open(index_path, 'r', encoding='utf-8') as f:
            index = json.load(f)

        # Remove orphaned keys from index
        for category, orphaned_keys in orphaned_by_category.items():
            if category in index.get('categories', {}):
                index['categories'][category] = [
                    key for key in index['categories'][category]
                    if key not in orphaned_keys
                ]

                # Remove empty categories
                if not index['categories'][category]:
                    del index['categories'][category]

        # Update metadata
        if 'metadata' in index:
            index['metadata']['last_updated'] = str(Path(__file__).stat().st_mtime)

        # Save updated index
        with open(index_path, 'w', encoding='utf-8') as f:
            json.dump(index, f, indent=2, ensure_ascii=False)

        print(f"✓ Updated index.json")

    return total_removed


def main():
    import sys

    # Paths
    script_dir = Path(__file__).parent
    project_root = script_dir.parent.parent

    # Determine which metadata directory to clean
    metadata_subdir = "metadata_divesms"
    strings_file = project_root / 'presentation' / 'src' / 'main' / 'res' / 'values' / 'strings.xml'

    metadata_dir = script_dir / metadata_subdir

    # Check if --execute flag is provided
    dry_run = True
    if "--execute" in sys.argv:
        dry_run = False

    print(f"\n{'🔍 DRY RUN - ' if dry_run else ''}Cleaning orphaned metadata")
    print(f"Metadata directory: {metadata_subdir}")
    print(f"Strings file: {strings_file.relative_to(project_root)}")
    print("=" * 80)

    # Find orphaned metadata
    orphaned_by_category = find_orphaned_metadata(metadata_dir, strings_file)

    if not orphaned_by_category:
        print("\n✓ No orphaned metadata found!")
        return

    # Print summary
    total_orphaned = sum(len(keys) for keys in orphaned_by_category.values())
    print(f"\nFound {total_orphaned} orphaned metadata entries in {len(orphaned_by_category)} categories:\n")

    for category, orphaned_keys in sorted(orphaned_by_category.items()):
        print(f"📁 {category} ({len(orphaned_keys)} entries):")
        for key in sorted(orphaned_keys):
            print(f"  - {key}")
        print()

    # Remove orphaned metadata
    if dry_run:
        print("\n💡 This is a DRY RUN. No files were modified.")
        print("   To actually remove orphaned metadata, run with --execute flag:")
        print(f"   python3 cleanup_orphaned_metadata.py {'main ' if metadata_subdir == 'metadata_main' else ''}--execute")
    else:
        print("\n🗑️  Removing orphaned metadata...\n")
        removed = remove_orphaned_metadata(metadata_dir, orphaned_by_category, dry_run=False)
        print(f"\n✓ Cleanup complete! Removed {removed} orphaned metadata entries.")


if __name__ == "__main__":
    main()
