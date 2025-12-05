#!/bin/bash
# Translate only missing/incomplete strings to all supported languages
# Uses Poetry for dependency management
#
# This script automatically detects strings that:
# - Have metadata but are missing translations in some/all languages
# - Have incomplete translations (missing some target languages)
#
# Usage:
#   ./translate_missing.sh                    # Translate missing strings (default: aiWizeBrowser)
#   ./translate_missing.sh --profile main     # Translate missing strings for main module
#   ./translate_missing.sh --dry-run          # Show what would be translated without API calls
#   ./translate_missing.sh --force            # Force re-translate ALL strings (ignore existing)

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "🔍 Wize SMS (DiveSMS) - Translate Missing Strings"
echo "=============================================="
echo ""

# Parse arguments
PROFILE="divesms"
DRY_RUN=""
FORCE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN="--dry-run"
            shift
            ;;
        --force)
            FORCE="--force"
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --profile PROFILE    Use specific profile (divesms|main) [default: divesms]"
            echo "  --dry-run           Show what would be translated without making API calls"
            echo "  --force             Force re-translate ALL strings (ignore existing translations)"
            echo "  --help              Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                          # Translate missing aiWizeBrowser strings"
            echo "  $0 --profile main           # Translate missing main module strings"
            echo "  $0 --dry-run                # Show what would be translated"
            echo "  $0 --force                  # Re-translate everything"
            echo ""
            exit 0
            ;;
        *)
            echo "❌ Unknown option: $1"
            echo "Run with --help for usage information"
            exit 1
            ;;
    esac
done

# Check if Poetry is installed
if ! command -v poetry &> /dev/null; then
    echo "❌ Error: Poetry is not installed"
    echo ""
    echo "Install it with:"
    echo "  curl -sSL https://install.python-poetry.org | python3 -"
    echo "  or visit: https://python-poetry.org/docs/#installation"
    echo ""
    exit 1
fi

# Check API key (only if not dry-run)
if [ -z "$DRY_RUN" ]; then
    if [ -z "$OPENAI_API_KEY" ] && [ -z "$ANTHROPIC_API_KEY" ]; then
        echo "❌ Error: No API key found"
        echo ""
        echo "Set one of:"
        echo "  export OPENAI_API_KEY='sk-proj-your-key-here'"
        echo "  export ANTHROPIC_API_KEY='sk-ant-your-key-here'"
        echo ""
        echo "Or use --dry-run to preview without API calls"
        exit 1
    fi
fi

# Install dependencies if needed
if [ ! -d "$SCRIPT_DIR/.venv" ] || [ ! -f "$SCRIPT_DIR/poetry.lock" ]; then
    echo "📦 Installing dependencies with Poetry..."
    cd "$SCRIPT_DIR"
    poetry install --no-interaction --no-ansi
    echo "✅ Dependencies installed"
    echo ""
fi

# Change to script directory for Poetry
cd "$SCRIPT_DIR"

# Validate metadata
echo "📋 Validating metadata..."
poetry run python manage_string_metadata.py validate
echo ""

if [ $? -ne 0 ]; then
    echo "❌ Validation failed. Fix errors before translating."
    exit 1
fi

# Show statistics
poetry run python manage_string_metadata.py stats
echo ""

# Run translation in RESUME mode (only missing strings)
# The translate_with_context.py script automatically detects missing translations
# and only translates what's needed (unless --force is specified)

if [ -n "$FORCE" ]; then
    echo "⚠️  FORCE MODE: This will re-translate ALL documented strings"
    echo ""

    # Calculate documented strings count
    DOCUMENTED=$(poetry run python -c "
from metadata_io import create_metadata_io
metadata_io = create_metadata_io(metadata_subdir='metadata_divesms')
metadata = metadata_io.get_all_metadata()
print(len(metadata))
")

    COST_LOW=$(poetry run python -c "print(f'{$DOCUMENTED * 0.03:.2f}')")
    COST_HIGH=$(poetry run python -c "print(f'{$DOCUMENTED * 0.05:.2f}')")

    echo "   Strings to translate: $DOCUMENTED"
    echo "   Target languages: 21"
    echo "   Total translations: $((DOCUMENTED * 21))"
    echo "   Estimated cost: \$$COST_LOW - \$$COST_HIGH"
    echo ""
    read -p "Continue with force mode? (y/N): " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Cancelled."
        exit 0
    fi
elif [ -z "$DRY_RUN" ]; then
    echo "🔄 RESUME MODE: Only translating missing/incomplete strings"
    echo "   (Use --force to re-translate everything)"
    echo ""
fi

# Build command
CMD="poetry run python translate_with_context.py --profile $PROFILE --all-languages --output"

if [ -n "$DRY_RUN" ]; then
    CMD="$CMD $DRY_RUN"
fi

if [ -n "$FORCE" ]; then
    CMD="$CMD $FORCE"
fi

# Run translation
echo "🚀 Starting translation..."
echo ""

$CMD

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    if [ -z "$DRY_RUN" ]; then
        echo "✅ Translation complete!"
        echo ""

        if [ "$PROFILE" = "divesms" ]; then
            echo "📁 Translations saved to:"
            echo "   presentation/src/main/res/values-*/strings.xml"
        elif [ "$PROFILE" = "main" ]; then
            echo "📁 Translations saved to:"
            echo "   flat/src/main/res/values-*/strings.xml"
        fi

        echo ""
        echo "💡 Tip: Run ./translate_missing.sh again to check if any strings still need translation"
    else
        echo "✅ Dry run complete (no changes made)"
    fi
else
    echo "❌ Translation failed with exit code $EXIT_CODE"
fi

exit $EXIT_CODE
