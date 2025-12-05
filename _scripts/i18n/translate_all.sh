#!/bin/bash
# Translate all documented strings to all supported languages
# Uses Poetry for dependency management

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "🌐 Wize SMS (DiveSMS) Translation System"
echo "======================================"
echo ""

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

# Check API key
if [ -z "$OPENAI_API_KEY" ]; then
    echo "❌ Error: OPENAI_API_KEY environment variable not set"
    echo ""
    echo "Set it with:"
    echo "  export OPENAI_API_KEY='sk-proj-your-key-here'"
    echo ""
    exit 1
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

# Calculate estimated cost
DOCUMENTED=$(poetry run python -c "
from metadata_io import create_metadata_io
metadata_io = create_metadata_io(metadata_subdir='metadata_divesms')
metadata = metadata_io.get_all_metadata()
print(len(metadata))
")

COST_LOW=$(poetry run python -c "print(f'{$DOCUMENTED * 0.03:.2f}')")
COST_HIGH=$(poetry run python -c "print(f'{$DOCUMENTED * 0.05:.2f}')")

echo "⚠️  This will translate all documented strings to 21 languages"
echo ""
echo "   Strings to translate: $DOCUMENTED"
echo "   Target languages: 21"
echo "   Total translations: $((DOCUMENTED * 21))"
echo "   Estimated cost: \$$COST_LOW - \$$COST_HIGH"
echo ""
read -p "Continue? (y/N): " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Cancelled."
    exit 0
fi

# Run translation
echo ""
echo "🚀 Starting translation..."
echo ""

poetry run python translate_with_context.py --all-languages --output

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Translation complete!"
    echo ""
    echo "📁 Translations saved to:"
    echo "   presentation/src/main/res/values-*/strings.xml"
else
    echo "❌ Translation failed with exit code $EXIT_CODE"
fi

exit $EXIT_CODE
