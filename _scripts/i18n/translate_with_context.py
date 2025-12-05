#!/usr/bin/env python3
"""
AI Translation with Context - Universal String Translation System for DiveSMS

Multi-language translation using structured output - translates one string
to all 21 languages in a single API call for efficiency.

Usage:
    # Translate all documented strings to all languages
    python translate_with_context.py --all-languages --output

    # Translate specific string to all languages
    python translate_with_context.py --string app_name --all-languages

    # Translate to specific languages only
    python translate_with_context.py --languages ru,fr,es --output

    # Dry run (show prompts without calling AI)
    python translate_with_context.py --all-languages --dry-run

Requirements:
    - Export OPENAI_API_KEY="your-key" or ANTHROPIC_API_KEY="your-key"
    - Install: pip install openai anthropic pyyaml
"""

import json
import sys
import os
import re
import xml.etree.ElementTree as ET
import logging
import traceback
from pathlib import Path
from typing import Dict, List, Optional
from datetime import datetime
from metadata_io import create_metadata_io, MetadataIO

# Setup logging
log_file = Path(__file__).parent / f"translation_errors_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.FileHandler(log_file, encoding='utf-8'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


# Profile Configuration
# Maps profile names to their metadata directory and strings.xml path
PROFILES = {
    'divesms': {
        'metadata_dir': 'metadata_divesms',
        'strings_path': 'presentation/src/main/res/values/strings.xml',
        'description': 'Wize SMS/DiveSMS strings'
    }
}

DEFAULT_PROFILE = 'divesms'


# AI Provider Configuration
AI_PROVIDER = os.getenv('AI_TRANSLATION_PROVIDER', 'openai')
OPENAI_MODEL = 'gpt-4o'
ANTHROPIC_MODEL = 'claude-3-opus-20240229'

# Supported languages
ALL_LANGUAGES = [
    "en", "zh-CN", "zh-TW", "hi", "es", "ar", "pt-BR", "id",
    "bn", "ru", "ja", "de", "fr", "ko", "tr", "vi", "it", "th", "pl", "uk"
]

# Locale code mapping (API format → Android format)
LOCALE_MAPPING = {
    "en": "en",
    "zh-CN": "zh-rCN",      # Simplified Chinese
    "zh-TW": "zh-rTW",      # Traditional Chinese
    "pt-BR": "pt-rBR",      # Brazilian Portuguese
    "hi": "hi",             # Hindi
    "es": "es",             # Spanish
    "ar": "ar",             # Arabic
    "id": "id",             # Indonesian
    "bn": "bn",             # Bengali
    "ru": "ru",             # Russian
    "ja": "ja",             # Japanese
    "de": "de",             # German
    "fr": "fr",             # French
    "ko": "ko",             # Korean
    "tr": "tr",             # Turkish
    "vi": "vi",             # Vietnamese
    "it": "it",             # Italian
    "th": "th",             # Thai
    "pl": "pl",             # Polish
    "uk": "uk",             # Ukrainian
}

# Human-readable locale names
LOCALE_NAMES = {
    "en": "English",
    "zh-CN": "Chinese (Simplified)",
    "zh-TW": "Chinese (Traditional)",
    "hi": "Hindi",
    "es": "Spanish",
    "ar": "Arabic",
    "pt-BR": "Portuguese (Brazil)",
    "id": "Indonesian",
    "bn": "Bengali",
    "ru": "Russian",
    "ja": "Japanese",
    "de": "German",
    "fr": "French",
    "ko": "Korean",
    "tr": "Turkish",
    "vi": "Vietnamese",
    "it": "Italian",
    "th": "Thai",
    "pl": "Polish",
    "uk": "Ukrainian",
}


def escape_android_string(text: str) -> str:
    r"""
    Escape special characters for Android XML string resources.

    Android requires:
    - Apostrophes (') must be escaped as \'
    - Quotes (") must be escaped as \"
    - Newlines (\n) and tabs (\t) should be kept as-is (already escaped)
    - Backslashes (\) must be escaped as \\
    - @ at the start must be escaped as \@
    - ? at the start must be escaped as \?

    However, since we're using ElementTree which handles XML escaping,
    we only need to handle Android-specific escapes (apostrophes, quotes).
    """
    if not text:
        return text

    # Escape backslashes first (must be done before other escapes)
    text = text.replace('\\', '\\\\')

    # Escape apostrophes
    text = text.replace("'", "\\'")

    # Escape quotes
    text = text.replace('"', '\\"')

    # Escape @ and ? at the start
    if text.startswith('@'):
        text = '\\' + text
    elif text.startswith('?'):
        text = '\\' + text

    return text


class MultiLanguageTranslator:
    """Translates strings to multiple languages using AI with structured output"""

    def __init__(self, metadata_file: Path, strings_file: Path, target_languages: List[str], metadata_subdir: str = "metadata_divesms"):
        self.metadata_file = metadata_file
        self.strings_file = strings_file
        self.target_languages = target_languages
        self.metadata_subdir = metadata_subdir
        self.metadata = {}
        self.strings = {}
        self.existing_translations = {}  # Track what's already translated

        # Initialize metadata_io for split format
        self.metadata_io: Optional[MetadataIO] = None
        i18n_dir = metadata_file.parent
        metadata_dir_path = i18n_dir / metadata_subdir
        if metadata_dir_path.exists():
            self.metadata_io = create_metadata_io(i18n_dir, metadata_subdir)

        self.load_data()

    def load_data(self):
        """Load metadata and source strings from split YAML files or legacy JSON"""
        # Try split format first
        if self.metadata_io and self.metadata_io.is_split_format():
            try:
                self.metadata = self.metadata_io.get_all_metadata()
                logger.info(f"✓ Loaded metadata for {len(self.metadata)} strings (split format)")
            except Exception as e:
                logger.warning(f"⚠ Error loading split format: {e}")
                logger.warning(f"  Falling back to legacy format...")
                logger.debug(traceback.format_exc())
                # Fall through to legacy loading below

        # Fall back to legacy JSON if split format not loaded
        if not self.metadata:
            if self.metadata_file.exists():
                try:
                    with open(self.metadata_file, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                        self.metadata = data.get('strings', {})
                    logger.info(f"✓ Loaded metadata for {len(self.metadata)} strings (legacy format)")
                except Exception as e:
                    logger.error(f"✗ Error loading metadata file: {e}")
                    logger.debug(traceback.format_exc())
                    return
            else:
                logger.error(f"✗ Metadata file not found: {self.metadata_file}")
                return

        # Load source strings from XML
        if self.strings_file.exists():
            try:
                tree = ET.parse(self.strings_file)
                root = tree.getroot()
                for elem in root.findall('.//string'):
                    name = elem.get('name')
                    if name:
                        self.strings[name] = elem.text or ''
                logger.info(f"✓ Loaded {len(self.strings)} source strings")
            except Exception as e:
                logger.error(f"✗ Error parsing strings file: {e}")
                logger.debug(traceback.format_exc())
        else:
            logger.error(f"✗ Strings file not found: {self.strings_file}")

    def load_existing_translations(self):
        """Load existing translations from all locale folders to support resume"""
        logger.info("📂 Checking for existing translations...")
        res_dir = self.strings_file.parent.parent

        # Map to track which strings have translations in which locales
        translations_by_string = {}  # {string_key: {locale: translation}}

        for api_locale in self.target_languages:
            android_locale = LOCALE_MAPPING.get(api_locale, api_locale)
            values_dir = res_dir / f"values-{android_locale}"
            strings_file = values_dir / 'strings.xml'

            if strings_file.exists():
                try:
                    tree = ET.parse(strings_file)
                    root = tree.getroot()
                    for elem in root.findall('.//string'):
                        name = elem.get('name')
                        if name and elem.text:
                            if name not in translations_by_string:
                                translations_by_string[name] = {}
                            translations_by_string[name][api_locale] = elem.text
                except Exception as e:
                    logger.warning(f"⚠ Could not parse {strings_file}: {e}")
                    continue

        # Now determine which strings have COMPLETE translations (all target languages)
        complete_strings = set()
        incomplete_strings = set()

        for string_key in self.metadata.keys():
            if string_key in translations_by_string:
                locales_present = set(translations_by_string[string_key].keys())
                target_locales = set(self.target_languages)

                if locales_present >= target_locales:
                    # All target languages present
                    complete_strings.add(string_key)
                else:
                    # Some languages missing
                    incomplete_strings.add(string_key)
                    missing = target_locales - locales_present
                    logger.debug(f"  {string_key}: missing {missing}")

        self.existing_translations = translations_by_string

        logger.info(f"✓ Found translations:")
        logger.info(f"  - Complete: {len(complete_strings)} strings")
        logger.info(f"  - Incomplete: {len(incomplete_strings)} strings")
        logger.info(f"  - Missing: {len(self.metadata) - len(complete_strings) - len(incomplete_strings)} strings")

        return complete_strings, incomplete_strings

    def build_multilang_prompt(self, string_key: str, source_text: str, metadata: Dict) -> str:
        """Build prompt for multi-language translation"""
        # Extract metadata
        category = metadata.get('category', 'general')
        ui = metadata.get('ui', {})
        context = metadata.get('context', {})
        purpose = metadata.get('purpose', '')
        constraints = metadata.get('constraints', {})
        guidance = metadata.get('translation_guidance', {})
        technical = metadata.get('technical', {})

        # Build UI location
        ui_location = ui.get('screen', 'Unknown')
        if ui.get('section'):
            ui_location += f" > {ui['section']}"
        ui_location += f" > {ui.get('element', 'text')}"

        # Build language list
        lang_names = [LOCALE_NAMES.get(lang, lang) for lang in self.target_languages]
        lang_list = ", ".join(lang_names)

        prompt = f"""Translate the following Android app string to ALL specified languages.

STRING KEY: {string_key}
SOURCE TEXT: {source_text}

=== CONTEXT INFORMATION ===

UI Location: {ui_location}
Element Type: {ui.get('element', 'text')}
Category: {category}
Purpose: {purpose}
"""

        if context.get('shown_when'):
            prompt += f"Shown When: {context['shown_when']}\n"

        if context.get('surrounding_elements'):
            surrounding = ', '.join(context['surrounding_elements'])
            prompt += f"Surrounding Elements: {surrounding}\n"

        prompt += "\n=== TRANSLATION CONSTRAINTS ===\n\n"

        max_length = constraints.get('max_length')
        if max_length:
            prompt += f"Maximum Length: {max_length} characters (CRITICAL - must fit in UI)\n"
            prompt += f"Reason: {constraints.get('reason', 'UI space limitation')}\n"
        else:
            prompt += "Maximum Length: No strict limit, but keep concise\n"

        tone = guidance.get('tone', 'neutral')
        style = guidance.get('style', 'descriptive')
        prompt += f"Tone: {tone}\n"
        prompt += f"Style: {style}\n"

        terminology = guidance.get('terminology', {})
        if terminology.get('domain'):
            prompt += f"Domain: {terminology['domain']} (use appropriate terminology)\n"

        prompt += "\n=== TECHNICAL REQUIREMENTS ===\n\n"

        if technical.get('format_specifiers'):
            prompt += "⚠️ CRITICAL: Contains format specifiers - MUST preserve exactly!\n"
            specifier_info = technical.get('specifier_info', [])
            if specifier_info:
                prompt += "Format specifiers:\n"
                for spec in specifier_info:
                    prompt += f"  - {spec['placeholder']} (position {spec['position']}): "
                    if 'represents' in spec:
                        prompt += f"{spec['represents']}\n"
                    else:
                        prompt += "variable\n"
            prompt += "Preserve ALL placeholders (%s, %d, %1$s, etc.) in exact same order!\n"

        if technical.get('contains_emoji'):
            emoji = technical.get('emoji_character', '')
            prompt += f"Contains emoji: {emoji}\n"
            if terminology.get('preserve_emoji'):
                prompt += "⚠️ Preserve emoji exactly in all translations.\n"

        if technical.get('html_formatting'):
            prompt += "⚠️ CRITICAL: Contains HTML tags - preserve all tags, translate only text!\n"

        prompt += "\n=== TERMINOLOGY GUIDANCE ===\n\n"

        if terminology.get('preferred'):
            preferred_items = []
            for item in terminology['preferred']:
                if isinstance(item, str):
                    preferred_items.append(item)
                elif isinstance(item, dict):
                    # Handle dict format, convert to string representation
                    preferred_items.append(str(item))
            preferred = ', '.join(preferred_items)
            prompt += f"Preferred terms: {preferred}\n"

        if terminology.get('avoid'):
            avoid_items = []
            for item in terminology['avoid']:
                if isinstance(item, str):
                    avoid_items.append(item)
                elif isinstance(item, dict):
                    avoid_items.append(str(item))
            avoid = ', '.join(avoid_items)
            prompt += f"Avoid: {avoid}\n"

        if terminology.get('critical'):
            prompt += "⚠️ CRITICAL: Translation must be unambiguous and use standard terminology.\n"

        if guidance.get('cultural_notes'):
            prompt += f"\nCultural Notes: {guidance['cultural_notes']}\n"

        prompt += f"""
=== TRANSLATION TASK ===

Translate to these {len(self.target_languages)} languages: {lang_list}

Requirements:
1. Translate naturally for native speakers
2. Maintain exact same meaning and intent
3. Respect all technical constraints (format specifiers, HTML, emoji)
4. Stay within length limits (CRITICAL for UI fit)
5. Match specified tone and style
6. Use appropriate domain terminology
7. Adapt culturally while preserving meaning

Return a JSON object with translations for ALL languages:
{{
"""
        for lang in self.target_languages:
            prompt += f'  "{lang}": "translated text in {LOCALE_NAMES.get(lang, lang)}",\n'

        prompt += """}

IMPORTANT:
- Provide ALL languages in the response
- Preserve format specifiers in same positions
- Stay within character limits
- Use natural, fluent translations
- Return ONLY the JSON object, no explanations
"""

        return prompt

    def translate_with_openai_structured(self, prompt: str, string_key: str) -> Dict[str, str]:
        """Translate using OpenAI with structured JSON output"""
        try:
            import openai
        except ImportError:
            logger.error("✗ Error: openai package not installed")
            logger.error("Install with: pip install openai")
            sys.exit(1)

        api_key = os.getenv('OPENAI_API_KEY')
        if not api_key:
            logger.error("✗ Error: OPENAI_API_KEY not set")
            sys.exit(1)

        try:
            client = openai.OpenAI(api_key=api_key)

            # Build JSON schema for structured output
            properties = {}
            required = []
            for lang in self.target_languages:
                properties[lang] = {"type": "string"}
                required.append(lang)

            response_format = {
                "type": "json_schema",
                "json_schema": {
                    "name": "translations",
                    "strict": True,
                    "schema": {
                        "type": "object",
                        "properties": properties,
                        "required": required,
                        "additionalProperties": False
                    }
                }
            }

            response = client.chat.completions.create(
                model=OPENAI_MODEL,
                messages=[
                    {
                        "role": "system",
                        "content": "You are a professional translator specializing in mobile app localization. Provide accurate, natural translations that fit UI context perfectly."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                response_format=response_format,
                temperature=0.3,
                max_completion_tokens=2000
            )

            translations = json.loads(response.choices[0].message.content)

            # Validate all languages present
            missing = set(self.target_languages) - set(translations.keys())
            if missing:
                logger.warning(f"⚠ [{string_key}] Missing translations for: {missing}")

            return translations

        except json.JSONDecodeError as e:
            logger.error(f"✗ [{string_key}] JSON decode error: {e}")
            logger.debug(f"Response content: {response.choices[0].message.content if 'response' in locals() else 'N/A'}")
            logger.debug(traceback.format_exc())
            return {}
        except Exception as e:
            logger.error(f"✗ [{string_key}] Error calling OpenAI: {e}")
            logger.debug(traceback.format_exc())
            return {}

    def translate_string(self, string_key: str, dry_run: bool = False) -> Dict[str, str]:
        """Translate a single string to all target languages"""
        try:
            if string_key not in self.metadata:
                logger.warning(f"⚠ [{string_key}] No metadata found, skipping")
                return {}

            source_text = self.strings.get(string_key, '')
            if not source_text:
                logger.warning(f"⚠ [{string_key}] No source text found, skipping")
                return {}

            metadata = self.metadata[string_key]

            try:
                prompt = self.build_multilang_prompt(string_key, source_text, metadata)
            except Exception as e:
                logger.error(f"✗ [{string_key}] Error building prompt: {e}")
                logger.debug(traceback.format_exc())
                return {}

            if dry_run:
                logger.info(f"\n{'='*80}")
                logger.info(f"DRY RUN - PROMPT FOR: {string_key}")
                logger.info(f"{'='*80}")
                logger.info(prompt)
                logger.info(f"{'='*80}\n")
                # Return source text as "translations" for dry run
                return {lang: source_text for lang in self.target_languages}

            logger.info(f"[{string_key}] Translating to {len(self.target_languages)} languages...")

            translations = self.translate_with_openai_structured(prompt, string_key)

            if translations:
                logger.info(f"[{string_key}] ✓ Completed ({len(translations)} languages)")
                return translations
            else:
                logger.error(f"[{string_key}] ✗ Translation failed")
                return {}

        except Exception as e:
            logger.error(f"✗ [{string_key}] Unexpected error: {e}")
            logger.debug(traceback.format_exc())
            return {}

    def translate_all(self, specific_key: Optional[str] = None, dry_run: bool = False, force: bool = False) -> Dict[str, Dict[str, str]]:
        """Translate all documented strings or a specific one"""
        all_translations = {}  # {string_key: {locale: translation}}
        failed_keys = []  # Track failed translations
        skipped_keys = []  # Track skipped strings

        try:
            if specific_key:
                # Specific key always translates (even if already exists)
                try:
                    translations = self.translate_string(specific_key, dry_run)
                    if translations:
                        all_translations[specific_key] = translations
                    else:
                        failed_keys.append(specific_key)
                except Exception as e:
                    logger.error(f"✗ [{specific_key}] Failed to translate: {e}")
                    logger.debug(traceback.format_exc())
                    failed_keys.append(specific_key)
            else:
                documented_keys = list(self.metadata.keys())

                # Load existing translations to support resume (unless force mode)
                complete_strings = set()
                incomplete_strings = set()
                if not force and not dry_run:
                    complete_strings, incomplete_strings = self.load_existing_translations()

                # Determine which strings need translation
                if force:
                    keys_to_translate = documented_keys
                    logger.info(f"\n🌐 FORCE MODE: Translating all {len(documented_keys)} strings to {len(self.target_languages)} languages")
                else:
                    keys_to_translate = [k for k in documented_keys if k not in complete_strings]
                    if complete_strings:
                        logger.info(f"\n🌐 RESUME MODE: Skipping {len(complete_strings)} already-translated strings")
                    logger.info(f"🌐 Translating {len(keys_to_translate)} strings to {len(self.target_languages)} languages")

                logger.info(f"{'='*80}")

                for i, key in enumerate(keys_to_translate, 1):
                    try:
                        logger.info(f"[{i}/{len(keys_to_translate)}] Processing {key}...")
                        translations = self.translate_string(key, dry_run)
                        if translations:
                            all_translations[key] = translations
                        else:
                            failed_keys.append(key)
                    except Exception as e:
                        logger.error(f"✗ [{key}] Failed to translate: {e}")
                        logger.debug(traceback.format_exc())
                        failed_keys.append(key)
                        # Continue with next string instead of failing

                skipped_keys = list(complete_strings)

            total_translations = sum(len(t) for t in all_translations.values())
            logger.info(f"\n✓ Completed: {len(all_translations)} strings × {len(self.target_languages)} languages = {total_translations} translations")

            if skipped_keys:
                logger.info(f"⏭️  Skipped: {len(skipped_keys)} already-translated strings")

            if failed_keys:
                logger.warning(f"\n⚠ Failed to translate {len(failed_keys)} strings:")
                for key in failed_keys[:10]:  # Show first 10
                    logger.warning(f"  - {key}")
                if len(failed_keys) > 10:
                    logger.warning(f"  ... and {len(failed_keys) - 10} more")

            return all_translations

        except Exception as e:
            logger.error(f"✗ Unexpected error in translate_all: {e}")
            logger.debug(traceback.format_exc())
            return all_translations  # Return whatever we have so far

    def save_translations(self, all_translations: Dict[str, Dict[str, str]]):
        """Save translations to locale-specific XML files"""
        if not all_translations:
            logger.warning("No translations to save")
            return

        res_dir = self.strings_file.parent.parent
        saved_locales = set()
        failed_locales = []

        try:
            # Group translations by locale
            by_locale = {}  # {locale: {string_key: translation}}
            for string_key, translations in all_translations.items():
                for api_locale, translation in translations.items():
                    android_locale = LOCALE_MAPPING.get(api_locale, api_locale)
                    if android_locale not in by_locale:
                        by_locale[android_locale] = {}
                    by_locale[android_locale][string_key] = translation

            # Save each locale
            for android_locale, translations in by_locale.items():
                try:
                    values_dir = res_dir / f"values-{android_locale}"
                    values_dir.mkdir(parents=True, exist_ok=True)
                    output_file = values_dir / 'strings.xml'

                    # Load existing translations if file exists
                    existing = {}
                    if output_file.exists():
                        try:
                            tree = ET.parse(output_file)
                            root = tree.getroot()
                            for elem in root.findall('.//string'):
                                name = elem.get('name')
                                if name:
                                    existing[name] = elem.text or ''
                        except Exception as e:
                            logger.warning(f"⚠ [{android_locale}] Could not load existing translations: {e}")
                            # Continue with empty existing dict

                    # Escape new translations from API before merging
                    try:
                        escaped_translations = {
                            key: escape_android_string(value)
                            for key, value in translations.items()
                        }
                    except Exception as e:
                        logger.error(f"✗ [{android_locale}] Error escaping translations: {e}")
                        logger.debug(traceback.format_exc())
                        failed_locales.append(android_locale)
                        continue

                    # Merge with new translations
                    existing.update(escaped_translations)

                    # Build XML
                    try:
                        root = ET.Element('resources')
                        root.set('xmlns:tools', 'http://schemas.android.com/tools')
                        root.set('tools:ignore', 'MissingTranslation')

                        for key in sorted(existing.keys()):
                            string_elem = ET.SubElement(root, 'string')
                            string_elem.set('name', key)
                            # Text is already escaped (either from existing file or from escaped_translations)
                            string_elem.text = existing[key]

                            # Preserve formatted attribute if needed
                            if key in self.metadata:
                                technical = self.metadata[key].get('technical', {})
                                if technical.get('format_specifiers'):
                                    string_elem.set('formatted', 'false')

                            # Auto-detect placeholders and add formatted="false"
                            # This prevents Android lint warnings for strings with multiple % symbols

                            # Pattern 1: Custom placeholders like %variableName%
                            has_custom_placeholders = existing[key] and re.search(r'%\w+%', existing[key])

                            # Pattern 2: Multiple Android format specifiers (%s, %d, %f, etc.)
                            # Count occurrences of % followed by common format specifiers
                            android_specs = re.findall(r'%[sdifgeoxX]', existing[key]) if existing[key] else []
                            has_multiple_android_specs = len(android_specs) >= 2

                            if (has_custom_placeholders or has_multiple_android_specs):
                                # Only add if not already set by format_specifiers above
                                if 'formatted' not in string_elem.attrib:
                                    string_elem.set('formatted', 'false')
                    except Exception as e:
                        logger.error(f"✗ [{android_locale}] Error building XML: {e}")
                        logger.debug(traceback.format_exc())
                        failed_locales.append(android_locale)
                        continue

                    # Pretty print and write file
                    try:
                        from xml.dom import minidom
                        xml_string = ET.tostring(root, encoding='UTF-8')
                        dom = minidom.parseString(xml_string)
                        pretty_xml = dom.toprettyxml(indent="  ", encoding='UTF-8')

                        lines = pretty_xml.decode('utf-8').split('\n')
                        lines = [line for line in lines if line.strip()]

                        # Write file
                        with open(output_file, 'w', encoding='utf-8') as f:
                            f.write('<?xml version="1.0" encoding="utf-8"?>\n')
                            f.write('\n'.join(lines[1:]))
                            f.write('\n')

                        saved_locales.add(android_locale)
                        logger.info(f"✓ Saved {len(translations)} strings to values-{android_locale}/strings.xml")
                    except Exception as e:
                        logger.error(f"✗ [{android_locale}] Error writing file: {e}")
                        logger.debug(traceback.format_exc())
                        failed_locales.append(android_locale)
                        continue

                except Exception as e:
                    logger.error(f"✗ [{android_locale}] Unexpected error saving locale: {e}")
                    logger.debug(traceback.format_exc())
                    failed_locales.append(android_locale)
                    # Continue with next locale

            logger.info(f"\n📁 Translations saved to {len(saved_locales)} locale folders")

            if failed_locales:
                logger.warning(f"\n⚠ Failed to save {len(failed_locales)} locales: {', '.join(failed_locales)}")

        except Exception as e:
            logger.error(f"✗ Unexpected error in save_translations: {e}")
            logger.debug(traceback.format_exc())


def print_usage():
    """Print usage information"""
    profiles_text = "\n".join([f"    {name}: {info['description']}" for name, info in PROFILES.items()])

    print(f"""
AI Translation with Context (Multi-Language, Universal) for DiveSMS

Usage:
    python translate_with_context.py [options]

Options:
    --profile PROFILE      Source profile (default: {DEFAULT_PROFILE})
    --all-languages        Translate to all 21 supported languages
    --languages CODES      Translate to specific languages (comma-separated)
                          Example: --languages ru,fr,es
    --string KEY          Translate only specified string key
    --output              Save translations to values-*/strings.xml files
    --dry-run             Show prompts without calling AI API
    --force               Force re-translate all strings (ignore existing)
    --provider PROVIDER   Use 'openai' or 'anthropic' (default: openai)
    --help                Show this help message

Available Profiles:
{profiles_text}

Environment Variables:
    OPENAI_API_KEY          Your OpenAI API key (required)
    ANTHROPIC_API_KEY       Your Anthropic API key (alternative)
    AI_TRANSLATION_PROVIDER Provider: 'openai' or 'anthropic'

Supported Languages (21):
    en, zh-CN, zh-TW, hi, es, ar, pt-BR, id, bn, ru, ja, de, fr, ko, tr, vi, it, th, pl, uk

Examples:
    # Translate all to all languages (auto-resumes from existing translations)
    python translate_with_context.py --all-languages --output

    # Force re-translate all strings (ignore existing)
    python translate_with_context.py --all-languages --output --force

    # Dry-run to see prompts without API calls
    python translate_with_context.py --all-languages --dry-run

    # Translate specific string to all languages
    python translate_with_context.py --string app_name --all-languages --output

    # Translate to specific languages only
    python translate_with_context.py --languages ru,fr,es --output

Resume Support:
    The script automatically detects existing translations and only translates
    missing strings. This allows you to:
    - Resume after interruption (Ctrl+C)
    - Add new strings without re-translating everything
    - Re-run safely without wasting API calls

    Use --force to ignore existing translations and re-translate everything.
""")


def main():
    try:
        logger.info(f"📝 Logging to: {log_file}")

        if '--help' in sys.argv:
            print_usage()
            return

        # Parse arguments
        target_languages = []
        specific_key = None
        save_output = False
        dry_run = False
        force = False
        profile = DEFAULT_PROFILE

        i = 1
        while i < len(sys.argv):
            arg = sys.argv[i]
            if arg == '--profile' and i + 1 < len(sys.argv):
                profile = sys.argv[i + 1]
                i += 2
            elif arg == '--all-languages':
                target_languages = ALL_LANGUAGES
                i += 1
            elif arg == '--languages' and i + 1 < len(sys.argv):
                target_languages = sys.argv[i + 1].split(',')
                i += 2
            elif arg == '--string' and i + 1 < len(sys.argv):
                specific_key = sys.argv[i + 1]
                i += 2
            elif arg == '--output':
                save_output = True
                i += 1
            elif arg == '--dry-run':
                dry_run = True
                i += 1
            elif arg == '--force':
                force = True
                i += 1
            elif arg == '--provider' and i + 1 < len(sys.argv):
                global AI_PROVIDER
                AI_PROVIDER = sys.argv[i + 1]
                i += 2
            else:
                i += 1

        # Validate profile
        if profile not in PROFILES:
            logger.error(f"✗ Error: Unknown profile '{profile}'")
            logger.error(f"Available profiles: {', '.join(PROFILES.keys())}")
            logger.error("Run with --help for usage information")
            return

        if not target_languages:
            logger.error("✗ Error: No target languages specified")
            logger.error("Use --all-languages or --languages CODES")
            logger.error("Run with --help for usage information")
            return

        # Get profile configuration
        profile_config = PROFILES[profile]
        metadata_subdir = profile_config['metadata_dir']
        strings_rel_path = profile_config['strings_path']

        # File paths
        script_dir = Path(__file__).parent
        project_root = script_dir.parent.parent
        metadata_file = script_dir / 'strings_metadata.json'  # Legacy path (used for fallback)
        strings_file = project_root / strings_rel_path

        logger.info(f"\n🔧 Using profile: {profile}")
        logger.info(f"   Metadata: {metadata_subdir}")
        logger.info(f"   Strings: {strings_rel_path}")
        if dry_run:
            logger.info(f"   Mode: DRY RUN (no API calls)")
        if force:
            logger.info(f"   Mode: FORCE (re-translate all)")

        # Initialize translator
        translator = MultiLanguageTranslator(metadata_file, strings_file, target_languages, metadata_subdir)

        # Translate
        all_translations = translator.translate_all(specific_key, dry_run, force)

        # Save if requested
        if not dry_run and save_output and all_translations:
            translator.save_translations(all_translations)
            logger.info(f"\n✅ Translation complete!")
            # Extract directory path from strings_rel_path (remove /values/strings.xml)
            res_dir = '/'.join(strings_rel_path.split('/')[:-2])
            logger.info(f"   Check: {res_dir}/values-*/strings.xml")

        logger.info(f"\n📝 Full log saved to: {log_file}")

    except KeyboardInterrupt:
        logger.warning("\n\n⚠ Translation interrupted by user")
        logger.info(f"📝 Partial log saved to: {log_file}")
        sys.exit(1)
    except Exception as e:
        logger.error(f"\n✗ Fatal error: {e}")
        logger.debug(traceback.format_exc())
        logger.info(f"📝 Error log saved to: {log_file}")
        sys.exit(1)


if __name__ == "__main__":
    main()
