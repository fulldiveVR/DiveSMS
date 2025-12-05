# Translation Infrastructure (i18n) for Wize SMS (DiveSMS)

This folder contains all tools and metadata for translating Wize SMS strings using AI-powered contextual translation.

## Quick Start

### Prerequisites

Install Poetry (Python dependency manager):
```bash
curl -sSL https://install.python-poetry.org | python3 -
```

### Running Translations

```bash
cd _scripts/i18n

# Set your OpenAI API key
export OPENAI_API_KEY="sk-proj-your-key-here"

# Run translation (installs dependencies automatically via Poetry)
./translate_missing.sh
```

### Manual Setup (Optional)

```bash
# Install dependencies manually
poetry install

# Run individual commands
poetry run python manage_string_metadata.py validate
poetry run python manage_string_metadata.py stats
poetry run python translate_with_context.py --help
```

## Files

### Core System Files

**Metadata Storage:**
- **`metadata_divesms/`** - Category-based YAML files for token-efficient metadata storage
  - `index.json` - Fast lookup index mapping strings to categories
  - `defaults.json` - Default values to reduce redundancy
  - Category YAML files: `branding.yaml`, `conversations.yaml`, etc.

**Tools:**
- **`translate_with_context.py`** - AI translation engine (multi-language support)
- **`manage_string_metadata.py`** - Metadata management tool (list, show, validate, export)
- **`metadata_io.py`** - Shared library for reading/writing split metadata
- **`cleanup_orphaned_metadata.py`** - Remove metadata for deleted strings
- **`generate_metadata_from_xml.py`** - Auto-generate metadata from strings.xml (NEW!)
- **`update_index.py`** - Update index.json with all categories

**Scripts:**
- **`translate_all.sh`** - Translate ALL documented strings (force mode)
- **`translate_missing.sh`** - Translate only missing/incomplete strings (resume mode)

**Configuration:**
- **`pyproject.toml`** - Poetry configuration with dependencies (openai, anthropic, pyyaml)
- **`poetry.lock`** - Locked dependency versions (auto-generated, gitignored)

**Auto-generated:**
- **`.venv/`** - Python virtual environment (created by Poetry, gitignored)

## Supported Languages (21 total)

`en`, `zh-CN`, `zh-TW`, `hi`, `es`, `ar`, `pt-BR`, `id`, `bn`, `ru`, `ja`, `de`, `fr`, `ko`, `tr`, `vi`, `it`, `th`, `pl`, `uk`

## Common Commands

### Manage Metadata

```bash
# Show statistics
poetry run python manage_string_metadata.py stats

# Validate metadata
poetry run python manage_string_metadata.py validate

# Show specific string metadata
poetry run python manage_string_metadata.py show app_name

# List all documented strings
poetry run python manage_string_metadata.py list

# Clean up orphaned metadata (dry run)
poetry run python cleanup_orphaned_metadata.py

# Clean up orphaned metadata (execute)
poetry run python cleanup_orphaned_metadata.py --execute
```

### Translation

```bash
# ✨ NEW: Translate only missing strings (recommended - most cost-effective)
./translate_missing.sh

# Translate missing strings - dry run (see what would be translated)
./translate_missing.sh --dry-run

# Translate ALL strings from scratch (use with caution - costs more)
./translate_all.sh

# Force re-translate everything (ignore existing translations)
./translate_missing.sh --force

# Advanced: Direct Python usage
# Dry run (see prompts without API calls)
poetry run python translate_with_context.py --all-languages --dry-run

# Translate single string
poetry run python translate_with_context.py --string app_name --all-languages --output

# Translate to specific languages only
poetry run python translate_with_context.py --languages ru,fr,es --output
```

## Translation Scripts Comparison

### `translate_missing.sh` (Recommended)
**Resume Mode** - Only translates what's needed:
- ✅ Detects strings without translations
- ✅ Detects incomplete translations (missing some languages)
- ✅ Skips strings already translated to all 21 languages
- ✅ Most cost-effective (only pays for missing translations)
- ✅ Safe to run multiple times
- ✅ Supports `--dry-run` to preview without API calls

**Use when:**
- Adding new strings with metadata
- Some translation failed previously
- New languages added to the system
- Want to check translation status

### `translate_all.sh` (Force Mode)
**Force Mode** - Re-translates everything:
- ⚠️ Ignores existing translations
- ⚠️ Translates ALL documented strings
- ⚠️ More expensive (pays for all strings)
- ⚠️ Requires explicit confirmation

**Use when:**
- Starting fresh (first time setup)
- Major improvements to translation system
- Want to regenerate all translations with new prompts

## How It Works

1. **Metadata** provides rich context for each string (UI location, purpose, constraints)
2. **AI Translation** uses GPT-4/Claude with structured output to translate one string to all 21 languages in one request
3. **Validation** ensures format specifiers preserved, length limits respected
4. **Resume Logic** tracks existing translations and only translates missing ones
5. **Output** saves to standard Android locale folders (`values-*/strings.xml`)

## Cost Estimate

- Approximately $0.03-0.05 per string for 21 languages
- Example: 100 strings = ~$3-5 USD

## Generating Metadata Automatically

### For All Strings (First Time Setup)

```bash
# Generate metadata for ALL strings in strings.xml
poetry run python generate_metadata_from_xml.py

# Update index.json
poetry run python update_index.py

# Validate
poetry run python manage_string_metadata.py validate
```

This creates metadata for all 321 strings across 25 categories with:
- Technical analysis (format specifiers, HTML, emoji)
- Estimated max length constraints
- Category-specific tone/style guidance
- Basic UI context

### Enriched Mode (Includes Code Search)

For more detailed metadata with code/layout references:

```bash
# Slower but includes file references
poetry run python generate_metadata_from_xml.py --enrich
```

## Adding New Metadata Manually

### Step-by-Step Guide

1. Choose the appropriate category file in `metadata_divesms/` (e.g., `conversations.yaml`)
2. Add your string following the existing format:

```yaml
your_string_key:
  category: conversations
  constraints:
    max_length: 50
    reason: UI space limitation
  context:
    shown_when: Description of when this appears
    surrounding_elements:
    - Element 1
    - Element 2
  purpose: Clear description of what this string is for
  translation_guidance:
    tone: friendly
    style: action-oriented
  ui:
    element: button
    position: Bottom of screen
    screen: Main
    section: Actions
```

3. Update `index.json` to include your new string in the category
4. Validate: `poetry run python manage_string_metadata.py validate`
5. Test: `poetry run python translate_with_context.py --string your_string_key --all-languages --dry-run`
6. Translate: `./translate_missing.sh`

### Metadata Fields

- **category**: Logical grouping (e.g., branding, conversations, settings)
- **ui**: Where the string appears (screen, section, element, position)
- **context**: When it's shown and surrounding elements
- **purpose**: What the string is for
- **constraints**: Length limits, technical requirements
- **translation_guidance**: Tone, style, terminology preferences
- **technical**: Format specifiers, HTML, emoji, plurals

## Project Structure

```
_scripts/i18n/
├── metadata_divesms/         # Metadata organized by category
│   ├── index.json           # Index of all strings
│   ├── defaults.json        # Default values
│   ├── branding.yaml        # Brand-related strings
│   ├── conversations.yaml   # Conversation-related strings
│   └── ...                  # Other categories
├── translate_with_context.py # Main translation script
├── manage_string_metadata.py # Metadata management tool
├── metadata_io.py           # Metadata I/O library
├── cleanup_orphaned_metadata.py # Cleanup tool
├── translate_all.sh         # Shell script for full translation
├── translate_missing.sh     # Shell script for incremental translation
├── setup_venv.sh           # Virtual environment setup
├── pyproject.toml          # Poetry dependencies
├── .gitignore              # Git ignore rules
└── README.md               # This file
```

## Troubleshooting

### "OPENAI_API_KEY not set"
```bash
export OPENAI_API_KEY="sk-proj-your-key"
```

### "No module named 'openai'"
Virtual environment not activated or dependencies not installed:
```bash
poetry install
```

### Poetry issues
Delete and recreate:
```bash
rm -rf .venv poetry.lock
poetry install
```

### Translation quality issues
1. Check metadata accuracy
2. Update terminology guidance
3. Add more context details
4. Test with dry-run first

---

**Version:** 1.0
**Last Updated:** 2025-12-05
**Project:** Wize SMS (DiveSMS)
