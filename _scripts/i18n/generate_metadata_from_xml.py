#!/usr/bin/env python3
"""
Generate enriched metadata for all strings in DiveSMS strings.xml

This script:
1. Parses strings.xml
2. Categorizes strings by prefix patterns
3. Generates basic metadata with technical analysis
4. Searches for code/layout references (optional)
5. Saves categorized YAML files ready for translation

Usage:
    python generate_metadata_from_xml.py              # Basic metadata only
    python generate_metadata_from_xml.py --enrich     # Include code search (slower)
    python generate_metadata_from_xml.py --dry-run    # Preview without saving
"""

import xml.etree.ElementTree as ET
import yaml
import re
import subprocess
from pathlib import Path
from collections import defaultdict
from typing import Dict, List, Optional
import sys

# Paths
PROJECT_ROOT = Path(__file__).parent.parent.parent
STRINGS_XML = PROJECT_ROOT / "presentation" / "src" / "main" / "res" / "values" / "strings.xml"
JAVA_SRC = PROJECT_ROOT / "presentation" / "src" / "main" / "java"
KOTLIN_SRC = PROJECT_ROOT / "presentation" / "src" / "main" / "kotlin"
RES_DIR = PROJECT_ROOT / "presentation" / "src" / "main" / "res"
LAYOUT_DIR = RES_DIR / "layout"
METADATA_DIR = Path(__file__).parent / "metadata_divesms"

# Category patterns for DiveSMS
CATEGORY_PATTERNS = {
    # Branding & About
    'branding': ['app_name', 'about_developer', 'about_source', 'about_contact'],

    # Main Screen & Inbox
    'main_screen': ['main_'],
    'conversations': ['inbox_', 'conversation', 'archived_'],

    # Compose & Messages
    'compose': ['compose_'],
    'messages': ['message_'],

    # Navigation
    'navigation': ['title_'],
    'drawer': ['drawer_'],
    'menus': ['menu_'],

    # Settings
    'settings': ['settings_'],

    # Notifications
    'notifications': ['notification_'],

    # QK Reply
    'qkreply': ['qkreply_'],

    # Conversation Info
    'conversation_info': ['info_'],

    # Blocking
    'blocking': ['blocking_', 'blocked_'],

    # Backup & Restore
    'backup': ['backup_'],

    # Scheduled Messages
    'scheduled': ['scheduled_'],

    # Premium Features
    'premium': ['qksms_plus_'],

    # Gallery
    'gallery': ['gallery_'],

    # Theme
    'theme': ['theme_'],

    # Shortcuts
    'shortcuts': ['shortcut_'],

    # Setup/Onboarding
    'setup': ['setup_'],

    # Widgets
    'widgets': ['widget_'],

    # Dialogs & Buttons
    'dialogs': ['dialog_'],
    'buttons': ['button_', 'rate_'],

    # Toasts & Messages
    'toasts': ['toast_'],

    # Changelog
    'changelog': ['changelog_'],

    # Cross-promotion
    'cross_promotion': ['install_browser', 'rate_us'],
}

# Category templates with tone/style guidance
CATEGORY_TEMPLATES = {
    'branding': {
        'tone': 'professional, branded',
        'style': 'proper noun',
        'domain': 'branding'
    },
    'main_screen': {
        'tone': 'friendly, neutral',
        'style': 'descriptive',
        'domain': 'messaging'
    },
    'conversations': {
        'tone': 'friendly, welcoming',
        'style': 'conversational',
        'domain': 'messaging'
    },
    'compose': {
        'tone': 'helpful, clear',
        'style': 'action-oriented',
        'domain': 'messaging'
    },
    'settings': {
        'tone': 'neutral, functional',
        'style': 'clear, instructional',
        'domain': 'settings'
    },
    'blocking': {
        'tone': 'neutral, security-focused',
        'style': 'clear, protective',
        'domain': 'privacy'
    },
    'premium': {
        'tone': 'positive, value-focused',
        'style': 'marketing, features',
        'domain': 'subscription'
    },
    'notifications': {
        'tone': 'informative, timely',
        'style': 'brief, actionable',
        'domain': 'notifications'
    },
    'dialogs': {
        'tone': 'clear, direct',
        'style': 'questioning, confirmatory',
        'domain': 'dialogs'
    },
    'buttons': {
        'tone': 'action-oriented',
        'style': 'imperative, brief',
        'domain': 'actions'
    },
}

def get_category(string_key: str) -> str:
    """Determine category for a string key"""
    key_lower = string_key.lower()

    # Check each category pattern
    for category, patterns in CATEGORY_PATTERNS.items():
        for pattern in patterns:
            if pattern in key_lower:
                return category

    return 'other'

def extract_emoji(text: str) -> tuple:
    """Extract emoji from string if present"""
    if not text:
        return None, None

    emoji_pattern = re.compile("["
        u"\U0001F600-\U0001F64F"  # emoticons
        u"\U0001F300-\U0001F5FF"  # symbols & pictographs
        u"\U0001F680-\U0001F6FF"  # transport & map symbols
        u"\U0001F1E0-\U0001F1FF"  # flags
        u"\U00002702-\U000027B0"
        u"\U000024C2-\U0001F251"
        "]+", flags=re.UNICODE)

    match = emoji_pattern.search(text)
    if match:
        emoji = match.group()
        if text.startswith(emoji):
            return emoji, 'prefix'
        elif text.endswith(emoji):
            return emoji, 'suffix'
        else:
            return emoji, 'inline'

    return None, None

def has_format_specifiers(text: str) -> tuple:
    """Check if string has format specifiers and extract details"""
    if not text:
        return False, []

    specifiers = []

    # Pattern for %s, %d, %1$s, etc.
    pattern = r'(%(?:\d+\$)?[sdifgeoxX])'
    matches = re.finditer(pattern, text)

    for i, match in enumerate(matches, 1):
        specifiers.append({
            'placeholder': match.group(0),
            'position': i
        })

    return len(specifiers) > 0, specifiers

def has_html(text: str) -> bool:
    """Check if string has HTML formatting"""
    if not text:
        return False
    return bool(re.search(r'<[^>]+>', text))

def estimate_max_length(string_key: str, string_value: str) -> Optional[int]:
    """Estimate reasonable max length based on string type"""
    if not string_value:
        return None

    current_len = len(string_value)

    # Very short strings (labels, buttons)
    if current_len <= 15:
        return current_len + 10

    # Button text, menu items
    if 'button' in string_key or 'menu_' in string_key:
        return current_len + 15

    # Titles, headers
    if 'title' in string_key:
        return current_len + 20

    # Hints, placeholders
    if 'hint' in string_key:
        return current_len + 25

    # Messages, descriptions (more flexible)
    if 'message' in string_key or 'summary' in string_key:
        return None  # No strict limit

    # Default: add 30% buffer
    return int(current_len * 1.3)

def search_in_code(string_key: str) -> List[str]:
    """Search for string usage in Java/Kotlin code (optional enrichment)"""
    pattern = f"R\\.string\\.{string_key}"
    refs = []

    for src_dir in [JAVA_SRC, KOTLIN_SRC]:
        if not src_dir.exists():
            continue

        try:
            result = subprocess.run(
                ["grep", "-r", "-l", pattern, str(src_dir)],
                capture_output=True,
                text=True,
                timeout=3
            )

            if result.returncode == 0 and result.stdout:
                for line in result.stdout.strip().split('\n'):
                    if line:
                        try:
                            rel_path = Path(line).relative_to(PROJECT_ROOT)
                            refs.append(str(rel_path))
                        except ValueError:
                            pass
        except (subprocess.TimeoutExpired, Exception):
            pass

    return refs[:3]  # Limit to first 3 references

def search_in_layouts(string_key: str) -> List[str]:
    """Search for string usage in layout XML files (optional enrichment)"""
    if not LAYOUT_DIR.exists():
        return []

    pattern = f"@string/{string_key}"
    refs = []

    try:
        result = subprocess.run(
            ["grep", "-r", "-l", pattern, str(LAYOUT_DIR)],
            capture_output=True,
            text=True,
            timeout=3
        )

        if result.returncode == 0 and result.stdout:
            for line in result.stdout.strip().split('\n'):
                if line:
                    try:
                        rel_path = Path(line).relative_to(PROJECT_ROOT)
                        refs.append(str(rel_path))
                    except ValueError:
                        pass
    except (subprocess.TimeoutExpired, Exception):
        pass

    return refs[:2]  # Limit to first 2 references

def infer_ui_element(string_key: str, code_refs: List[str], layout_refs: List[str]) -> str:
    """Infer UI element type from context"""
    key_lower = string_key.lower()

    if 'button' in key_lower:
        return 'button'
    elif 'title' in key_lower:
        return 'title'
    elif 'hint' in key_lower:
        return 'hint'
    elif 'error' in key_lower:
        return 'error_message'
    elif 'message' in key_lower:
        return 'text'
    elif 'summary' in key_lower:
        return 'description'
    elif 'label' in key_lower:
        return 'label'
    elif layout_refs:
        return 'text'
    else:
        return 'text'

def infer_screen(code_refs: List[str]) -> Optional[str]:
    """Infer screen name from code references"""
    if not code_refs:
        return None

    first_ref = code_refs[0]

    if 'Fragment' in first_ref:
        screen_name = Path(first_ref).stem.replace('Fragment', '').replace('_', ' ').title()
        return f"{screen_name}"
    elif 'Activity' in first_ref:
        screen_name = Path(first_ref).stem.replace('Activity', '').replace('_', ' ').title()
        return f"{screen_name}"

    return None

def generate_metadata(string_key: str, string_value: str, category: str, enrich: bool = False) -> Dict:
    """Generate metadata structure for a string"""

    emoji, emoji_pos = extract_emoji(string_value)
    has_format, specifier_info = has_format_specifiers(string_value)
    has_html_tags = has_html(string_value)
    max_length = estimate_max_length(string_key, string_value)

    # Basic metadata
    metadata = {
        'category': category,
        'purpose': f"{string_value[:80]}{'...' if len(string_value) > 80 else ''}",
        'technical': {
            'format_specifiers': has_format,
            'html_formatting': has_html_tags,
            'contains_emoji': emoji is not None,
            'plurals': False
        },
        'constraints': {},
        'translation_guidance': {
            'tone': 'neutral',
            'style': 'descriptive'
        },
        'ui': {
            'element': infer_ui_element(string_key, [], []),
            'screen': 'Unknown'
        },
        'references': {}
    }

    # Add format specifier details
    if has_format and specifier_info:
        metadata['technical']['specifier_info'] = specifier_info

    # Add emoji details
    if emoji:
        metadata['technical']['emoji_character'] = emoji
        metadata['technical']['emoji_position'] = emoji_pos

    # Add max length constraint
    if max_length:
        metadata['constraints']['max_length'] = max_length
        metadata['constraints']['reason'] = 'Estimated UI space limitation'

    # Apply category template
    if category in CATEGORY_TEMPLATES:
        template = CATEGORY_TEMPLATES[category]
        metadata['translation_guidance']['tone'] = template['tone']
        metadata['translation_guidance']['style'] = template['style']
        metadata['translation_guidance']['terminology'] = {
            'domain': template['domain']
        }

    # Optional enrichment with code search
    if enrich:
        code_refs = search_in_code(string_key)
        layout_refs = search_in_layouts(string_key)

        if code_refs:
            metadata['references']['code_files'] = code_refs
            screen = infer_screen(code_refs)
            if screen:
                metadata['ui']['screen'] = screen

        if layout_refs:
            metadata['references']['layouts'] = layout_refs

        # Re-infer UI element with enriched data
        metadata['ui']['element'] = infer_ui_element(string_key, code_refs, layout_refs)

    return metadata

def parse_strings_xml() -> Dict[str, str]:
    """Parse strings.xml and return all strings"""
    strings = {}

    if not STRINGS_XML.exists():
        print(f"❌ Error: {STRINGS_XML} not found")
        return strings

    tree = ET.parse(STRINGS_XML)
    root = tree.getroot()

    for string_elem in root.findall('string'):
        name = string_elem.get('name')
        value = string_elem.text or ""
        if name:
            strings[name] = value

    return strings

def main():
    import argparse

    parser = argparse.ArgumentParser(description='Generate metadata from strings.xml')
    parser.add_argument('--enrich', action='store_true', help='Include code/layout search (slower)')
    parser.add_argument('--dry-run', action='store_true', help='Preview without saving')
    args = parser.parse_args()

    print("=" * 80)
    print("📋 DiveSMS Metadata Generator")
    print("=" * 80)
    print()
    print(f"Source: {STRINGS_XML}")
    print(f"Target: {METADATA_DIR}")
    if args.enrich:
        print("Mode: ENRICHED (with code search)")
    else:
        print("Mode: BASIC (fast)")
    if args.dry_run:
        print("⚠️  DRY RUN - No files will be saved")
    print()

    # Parse all strings
    print("📖 Parsing strings.xml...")
    all_strings = parse_strings_xml()
    print(f"✓ Found {len(all_strings)} strings")
    print()

    # Categorize and generate metadata
    print("🏷️  Categorizing and generating metadata...")
    categorized = defaultdict(dict)

    for i, (string_key, string_value) in enumerate(all_strings.items(), 1):
        category = get_category(string_key)
        metadata = generate_metadata(string_key, string_value, category, enrich=args.enrich)
        categorized[category][string_key] = metadata

        # Progress indicator
        if i % 50 == 0:
            print(f"  Processed {i}/{len(all_strings)} strings...")

    print(f"✓ Generated metadata for {len(all_strings)} strings")
    print()

    # Print statistics
    print("📊 Categorization statistics:")
    for category, strings in sorted(categorized.items(), key=lambda x: len(x[1]), reverse=True):
        print(f"  {category:20s}: {len(strings):3d} strings")
    print()

    if args.dry_run:
        print("⚠️  DRY RUN - Files not saved")
        print()
        print("Run without --dry-run to save metadata:")
        print(f"  python {Path(__file__).name}")
        return

    # Create metadata directory
    METADATA_DIR.mkdir(parents=True, exist_ok=True)

    # Save to YAML files
    print(f"💾 Saving metadata to {METADATA_DIR}...")
    for category, strings in categorized.items():
        output_file = METADATA_DIR / f"{category}.yaml"

        with open(output_file, 'w', encoding='utf-8') as f:
            yaml.dump(strings, f, allow_unicode=True, sort_keys=True, default_flow_style=False)

        print(f"  ✓ Saved {len(strings)} strings to {output_file.name}")

    print()
    print("=" * 80)
    print(f"✅ Complete! Generated metadata for {len(all_strings)} strings across {len(categorized)} categories")
    print("=" * 80)
    print()
    print("Next steps:")
    print("  1. Review generated YAML files in metadata_divesms/")
    print("  2. Update index.json with new categories")
    print("  3. Run: poetry run python manage_string_metadata.py validate")
    print("  4. Run: poetry run python manage_string_metadata.py stats")

if __name__ == "__main__":
    main()
