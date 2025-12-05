#!/usr/bin/env python3
"""
String Metadata Management for Wize SMS (DiveSMS)

This script manages translation context metadata for all string resources.
It provides tools to add, update, query, and validate metadata for AI-powered translations.

Usage:
    # List all documented strings
    python manage_string_metadata.py list

    # Show metadata for a specific string
    python manage_string_metadata.py show flat_signin_message

    # Validate metadata consistency with strings.xml
    python manage_string_metadata.py validate

    # Export for AI translation to Chinese
    python manage_string_metadata.py export zh

    # Show coverage statistics
    python manage_string_metadata.py stats

    # Add new metadata (interactive)
    python manage_string_metadata.py add flat_new_string
"""

import json
import xml.etree.ElementTree as ET
import sys
import os
from pathlib import Path
from typing import Dict, List, Optional, Any
from datetime import datetime
from metadata_io import create_metadata_io, MetadataIO


class StringMetadata:
    """Represents metadata for a single string resource"""

    def __init__(self, key: str, data: Dict[str, Any]):
        self.key = key
        self.category = data.get('category', '')
        self.ui = data.get('ui', {})
        self.context = data.get('context', {})
        self.purpose = data.get('purpose', '')
        self.constraints = data.get('constraints', {})
        self.translation_guidance = data.get('translation_guidance', {})
        self.references = data.get('references', {})
        self.technical = data.get('technical', {})

    def to_dict(self) -> Dict[str, Any]:
        """Convert metadata to dictionary"""
        return {
            'category': self.category,
            'ui': self.ui,
            'context': self.context,
            'purpose': self.purpose,
            'constraints': self.constraints,
            'translation_guidance': self.translation_guidance,
            'references': self.references,
            'technical': self.technical
        }

    def get_short_description(self) -> str:
        """Get one-line description for listings"""
        element = self.ui.get('element', 'text')
        screen = self.ui.get('screen', 'Unknown')
        return f"[{self.category}] {element} in {screen}"


class MetadataManager:
    """Manages string metadata storage and operations"""

    def __init__(self, metadata_file: Path, strings_file: Path):
        self.metadata_file = metadata_file  # Legacy path (for backward compatibility)
        self.strings_file = strings_file
        self.metadata: Dict[str, StringMetadata] = {}
        self.file_data: Dict[str, Any] = {}

        # Initialize metadata_io for split format
        self.metadata_io: Optional[MetadataIO] = None
        i18n_dir = metadata_file.parent
        if (i18n_dir / "metadata_divesms").exists():
            self.metadata_io = create_metadata_io(i18n_dir, "metadata_divesms")

        self.load()

    def load(self):
        """Load metadata from split YAML files or legacy JSON file"""
        # Try split format first
        if self.metadata_io and self.metadata_io.is_split_format():
            try:
                strings_data = self.metadata_io.get_all_metadata()
                for key, data in strings_data.items():
                    self.metadata[key] = StringMetadata(key, data)

                # Load project info from index
                self.file_data = self.metadata_io.get_project_info()
                self.file_data['total_strings'] = 0  # Will be updated by validate
                self.file_data['documented_strings'] = len(self.metadata)

                print(f"✓ Loaded {len(self.metadata)} string metadata entries (split format)")
                return
            except Exception as e:
                print(f"⚠ Error loading split format: {e}")
                print(f"  Falling back to legacy format...")

        # Fall back to legacy JSON file
        if self.metadata_file.exists():
            with open(self.metadata_file, 'r', encoding='utf-8') as f:
                self.file_data = json.load(f)
                strings_data = self.file_data.get('strings', {})
                for key, data in strings_data.items():
                    self.metadata[key] = StringMetadata(key, data)
            print(f"✓ Loaded {len(self.metadata)} string metadata entries (legacy format)")
        else:
            print(f"✗ Metadata file not found: {self.metadata_file}")
            self.file_data = {
                "metadata_version": "1.0",
                "project": "Wize SMS (DiveSMS)",
                "default_locale": "en",
                "last_updated": datetime.now().strftime("%Y-%m-%d"),
                "total_strings": 0,
                "documented_strings": 0,
                "strings": {}
            }

    def save(self):
        """Save metadata to split YAML files or legacy JSON file"""
        # Save to split format if available
        if self.metadata_io and self.metadata_io.is_split_format():
            try:
                # Group by category
                from collections import defaultdict
                by_category = defaultdict(dict)

                for key, meta in self.metadata.items():
                    category = meta.category or 'uncategorized'
                    by_category[category][key] = meta.to_dict()

                # Save each category file
                for category, strings in by_category.items():
                    self.metadata_io.save_category(category, strings)

                # Update index.json with counts
                index_data = self.metadata_io._load_index()
                index_data['documented_strings'] = len(self.metadata)
                index_data['total_strings'] = self.file_data.get('total_strings', 0)
                index_data['categories'] = {cat: sorted(strings.keys()) for cat, strings in by_category.items()}
                index_data['files'] = {cat: f"metadata_divesms/{cat}.yaml" for cat in by_category.keys()}
                self.metadata_io.save_index(index_data)

                print(f"✓ Saved {len(self.metadata)} metadata entries to split format ({len(by_category)} categories)")
                return
            except Exception as e:
                print(f"⚠ Error saving split format: {e}")
                print(f"  Falling back to legacy format...")

        # Fall back to legacy JSON format
        self.file_data['last_updated'] = datetime.now().strftime("%Y-%m-%d")
        self.file_data['documented_strings'] = len(self.metadata)

        # Convert metadata objects to dicts
        strings_dict = {}
        for key, meta in self.metadata.items():
            strings_dict[key] = meta.to_dict()
        self.file_data['strings'] = strings_dict

        # Write to file with nice formatting
        with open(self.metadata_file, 'w', encoding='utf-8') as f:
            json.dump(self.file_data, f, indent=2, ensure_ascii=False)

        print(f"✓ Saved {len(self.metadata)} metadata entries to legacy format: {self.metadata_file}")

    def get_metadata(self, key: str) -> Optional[StringMetadata]:
        """Get metadata for a string key"""
        return self.metadata.get(key)

    def add_metadata(self, key: str, data: Dict[str, Any]):
        """Add or update metadata for a string"""
        self.metadata[key] = StringMetadata(key, data)

    def remove_metadata(self, key: str):
        """Remove metadata for a string"""
        if key in self.metadata:
            del self.metadata[key]

    def list_all(self) -> List[str]:
        """Get list of all documented string keys"""
        return sorted(self.metadata.keys())

    def get_strings_from_xml(self) -> Dict[str, str]:
        """Parse strings.xml and return dict of {name: value}"""
        if not self.strings_file.exists():
            print(f"✗ Strings file not found: {self.strings_file}")
            return {}

        tree = ET.parse(self.strings_file)
        root = tree.getroot()
        strings = {}

        for string_elem in root.findall('.//string'):
            name = string_elem.get('name')
            if name:
                text = string_elem.text or ''
                strings[name] = text

        return strings

    def validate_consistency(self) -> List[str]:
        """Validate metadata is in sync with strings.xml"""
        issues = []

        xml_strings = self.get_strings_from_xml()
        xml_keys = set(xml_strings.keys())
        meta_keys = set(self.metadata.keys())

        # Update total strings count
        self.file_data['total_strings'] = len(xml_keys)

        # Find strings without metadata
        missing_metadata = xml_keys - meta_keys
        if missing_metadata:
            issues.append(f"\n⚠ {len(missing_metadata)} strings without metadata:")
            for key in sorted(list(missing_metadata)[:10]):  # Show first 10
                issues.append(f"  - {key}: {xml_strings[key][:50]}...")
            if len(missing_metadata) > 10:
                issues.append(f"  ... and {len(missing_metadata) - 10} more")

        # Find metadata without strings
        orphaned_metadata = meta_keys - xml_keys
        if orphaned_metadata:
            issues.append(f"\n⚠ {len(orphaned_metadata)} metadata entries without strings:")
            for key in sorted(list(orphaned_metadata)):
                issues.append(f"  - {key}")

        if not issues:
            issues.append("✓ All metadata is in sync with strings.xml")

        return issues

    def export_for_ai_translation(self, target_locale: str) -> Dict[str, Any]:
        """Export metadata in AI-friendly format for translation services"""
        xml_strings = self.get_strings_from_xml()

        export_data = {
            "project": "Wize SMS (DiveSMS)",
            "source_locale": "en",
            "target_locale": target_locale,
            "export_date": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "total_strings": len(xml_strings),
            "documented_strings": len(self.metadata),
            "strings": []
        }

        for key, meta in self.metadata.items():
            source_text = xml_strings.get(key, '')

            # Build context description
            ui_location = f"{meta.ui.get('screen', 'Unknown')} > {meta.ui.get('element', 'text')}"
            if meta.ui.get('section'):
                ui_location = f"{meta.ui.get('screen')} > {meta.ui.get('section')} > {meta.ui.get('element')}"

            surrounding = []
            if meta.context.get('surrounding_elements'):
                surrounding = meta.context['surrounding_elements']

            string_data = {
                "key": key,
                "source_text": source_text,
                "context": {
                    "ui_location": ui_location,
                    "purpose": meta.purpose,
                    "shown_when": meta.context.get('shown_when', ''),
                    "surrounding_elements": surrounding,
                    "position": meta.ui.get('position', '')
                },
                "constraints": {
                    "max_length": meta.constraints.get('max_length'),
                    "reason": meta.constraints.get('reason', ''),
                    "tone": meta.translation_guidance.get('tone', 'neutral'),
                    "style": meta.translation_guidance.get('style', 'descriptive')
                },
                "technical": {
                    "format_specifiers": meta.technical.get('format_specifiers', False),
                    "specifier_info": meta.technical.get('specifier_info', []),
                    "html_formatting": meta.technical.get('html_formatting', False),
                    "contains_emoji": meta.technical.get('contains_emoji', False),
                    "emoji_character": meta.technical.get('emoji_character', '')
                },
                "terminology": meta.translation_guidance.get('terminology', {}),
                "cultural_notes": meta.translation_guidance.get('cultural_notes', ''),
                "category": meta.category
            }
            export_data["strings"].append(string_data)

        return export_data

    def get_statistics(self) -> Dict[str, Any]:
        """Get statistics about metadata coverage"""
        xml_strings = self.get_strings_from_xml()
        total = len(xml_strings)
        documented = len(self.metadata)

        # Count by category
        categories = {}
        for meta in self.metadata.values():
            cat = meta.category
            categories[cat] = categories.get(cat, 0) + 1

        # Count technical features
        with_format_specs = sum(1 for m in self.metadata.values()
                               if m.technical.get('format_specifiers', False))
        with_emoji = sum(1 for m in self.metadata.values()
                        if m.technical.get('contains_emoji', False))
        with_html = sum(1 for m in self.metadata.values()
                       if m.technical.get('html_formatting', False))

        return {
            "total_strings": total,
            "documented_strings": documented,
            "undocumented_strings": total - documented,
            "coverage_percent": (documented / total * 100) if total > 0 else 0,
            "categories": categories,
            "technical_features": {
                "format_specifiers": with_format_specs,
                "emoji": with_emoji,
                "html": with_html
            }
        }


def cmd_list(manager: MetadataManager):
    """List all documented strings"""
    print(f"\n📋 Documented Strings ({len(manager.metadata)}):")
    print("=" * 80)

    for key in manager.list_all():
        meta = manager.get_metadata(key)
        if meta:
            desc = meta.get_short_description()
            print(f"  {key}")
            print(f"    {desc}")
            print()


def cmd_show(manager: MetadataManager, key: str):
    """Show detailed metadata for a string"""
    meta = manager.get_metadata(key)
    if not meta:
        print(f"✗ No metadata found for: {key}")
        return

    xml_strings = manager.get_strings_from_xml()
    source_text = xml_strings.get(key, '(not found in strings.xml)')

    print(f"\n📝 Metadata for: {key}")
    print("=" * 80)
    print(f"\nSource Text: {source_text}")
    print(f"\nCategory: {meta.category}")
    print(f"\nUI Context:")
    print(f"  Screen: {meta.ui.get('screen', 'N/A')}")
    print(f"  Section: {meta.ui.get('section', 'N/A')}")
    print(f"  Element: {meta.ui.get('element', 'N/A')}")
    print(f"  Position: {meta.ui.get('position', 'N/A')}")
    print(f"\nPurpose: {meta.purpose}")

    if meta.constraints:
        print(f"\nConstraints:")
        print(f"  Max Length: {meta.constraints.get('max_length', 'None')}")
        print(f"  Reason: {meta.constraints.get('reason', 'N/A')}")

    if meta.translation_guidance:
        print(f"\nTranslation Guidance:")
        print(f"  Tone: {meta.translation_guidance.get('tone', 'N/A')}")
        print(f"  Style: {meta.translation_guidance.get('style', 'N/A')}")

        terms = meta.translation_guidance.get('terminology', {})
        if terms.get('preferred'):
            print(f"  Preferred Terms: {', '.join(terms['preferred'])}")
        if terms.get('avoid'):
            print(f"  Avoid Terms: {', '.join(terms['avoid'])}")

    if meta.technical:
        print(f"\nTechnical:")
        print(f"  Format Specifiers: {meta.technical.get('format_specifiers', False)}")
        print(f"  Contains Emoji: {meta.technical.get('contains_emoji', False)}")
        if meta.technical.get('emoji_character'):
            print(f"  Emoji: {meta.technical['emoji_character']}")


def cmd_validate(manager: MetadataManager):
    """Validate metadata consistency"""
    print("\n🔍 Validating metadata consistency...")
    print("=" * 80)

    issues = manager.validate_consistency()
    for issue in issues:
        print(issue)


def cmd_export(manager: MetadataManager, target_locale: str):
    """Export metadata for AI translation"""
    print(f"\n📤 Exporting metadata for {target_locale} translation...")

    export_data = manager.export_for_ai_translation(target_locale)
    output_file = f"translation_context_{target_locale}.json"

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(export_data, f, indent=2, ensure_ascii=False)

    print(f"✓ Exported {len(export_data['strings'])} strings to {output_file}")
    print(f"\nExport Summary:")
    print(f"  Total strings in project: {export_data['total_strings']}")
    print(f"  Documented strings: {export_data['documented_strings']}")
    print(f"  Coverage: {export_data['documented_strings'] / export_data['total_strings'] * 100:.1f}%")


def cmd_stats(manager: MetadataManager):
    """Show statistics"""
    print("\n📊 Metadata Statistics")
    print("=" * 80)

    stats = manager.get_statistics()

    print(f"\nOverall Coverage:")
    print(f"  Total strings: {stats['total_strings']}")
    print(f"  Documented: {stats['documented_strings']}")
    print(f"  Undocumented: {stats['undocumented_strings']}")
    print(f"  Coverage: {stats['coverage_percent']:.1f}%")

    print(f"\nBy Category:")
    for cat, count in sorted(stats['categories'].items(), key=lambda x: -x[1]):
        print(f"  {cat}: {count} strings")

    print(f"\nTechnical Features:")
    tech = stats['technical_features']
    print(f"  With format specifiers: {tech['format_specifiers']}")
    print(f"  With emoji: {tech['emoji']}")
    print(f"  With HTML: {tech['html']}")


def print_usage():
    """Print usage information"""
    print("""
String Metadata Management Tool

Usage:
    python manage_string_metadata.py <command> [arguments]

Commands:
    list                List all documented strings
    show <key>          Show detailed metadata for a string
    validate            Validate metadata consistency with strings.xml
    export <locale>     Export metadata for AI translation (e.g., 'zh', 'es', 'ja')
    stats               Show coverage statistics
    help                Show this help message

Examples:
    python manage_string_metadata.py list
    python manage_string_metadata.py show flat_signin_message
    python manage_string_metadata.py validate
    python manage_string_metadata.py export zh
    python manage_string_metadata.py stats
""")


def main():
    # File paths
    script_dir = Path(__file__).parent  # _scripts/i18n/
    project_root = script_dir.parent.parent  # project root
    metadata_file = script_dir / 'strings_metadata.json'
    strings_file = project_root / 'presentation' / 'src' / 'main' / 'res' / 'values' / 'strings.xml'

    # Check command
    if len(sys.argv) < 2:
        print_usage()
        return

    command = sys.argv[1].lower()

    if command == 'help':
        print_usage()
        return

    # Initialize manager
    manager = MetadataManager(metadata_file, strings_file)

    # Execute command
    if command == 'list':
        cmd_list(manager)

    elif command == 'show':
        if len(sys.argv) < 3:
            print("✗ Error: Please provide a string key")
            print("Usage: python manage_string_metadata.py show <key>")
            return
        key = sys.argv[2]
        cmd_show(manager, key)

    elif command == 'validate':
        cmd_validate(manager)
        manager.save()  # Save updated counts

    elif command == 'export':
        if len(sys.argv) < 3:
            print("✗ Error: Please provide a target locale")
            print("Usage: python manage_string_metadata.py export <locale>")
            print("Examples: zh, es, ja, fr, de")
            return
        locale = sys.argv[2]
        cmd_export(manager, locale)

    elif command == 'stats':
        cmd_stats(manager)

    else:
        print(f"✗ Unknown command: {command}")
        print_usage()


if __name__ == "__main__":
    main()
