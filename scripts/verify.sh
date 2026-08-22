#!/usr/bin/env bash
#
# Single verification command for DiveSMS.
#   exit 0 = success, non-zero = failure.
#   Silent on success. On failure, every diagnostic line is prefixed with "ERROR:".
#
# Usage:
#   scripts/verify.sh [--skip-functions]
#
# Gates:
#   :domain:testDebugUnitTest
#   :presentation:testNoAnalyticsDebugUnitTest
#   :presentation:assembleNoAnalyticsDebug
#   functions/ TypeScript build (skip with --skip-functions)

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SKIP_FUNCTIONS=0

for arg in "$@"; do
    case "$arg" in
        --skip-functions) SKIP_FUNCTIONS=1 ;;
        -h|--help) sed -n '2,15p' "${BASH_SOURCE[0]}"; exit 0 ;;
        *) echo "ERROR: unknown argument: $arg" >&2; exit 2 ;;
    esac
done

LOG_DIR="$(mktemp -d)"
cleanup() { rm -rf "$LOG_DIR"; }
trap cleanup EXIT

err() { echo "ERROR: $*" >&2; }

# Print only the interesting region of a gradle log: the failure banner and what follows.
dump_failure() {
    local label="$1" log="$2"
    err "$label failed. Relevant output:"
    if grep -n -m1 -E '^(FAILURE:|\* What went wrong:)' "$log" >/dev/null 2>&1; then
        local start
        start="$(grep -n -m1 -E '^(FAILURE:|\* What went wrong:)' "$log" | cut -d: -f1)"
        sed -n "${start},\$p" "$log" | head -n 60 | sed 's/^/ERROR: /' >&2
    else
        grep -E -i 'error|exception|FAILED|Could not' "$log" | head -n 40 | sed 's/^/ERROR: /' >&2 \
            || tail -n 40 "$log" | sed 's/^/ERROR: /' >&2
    fi
}

run_gradle() {
    local label="$1"; shift
    local log="$LOG_DIR/$(echo "$label" | tr -c 'A-Za-z0-9_.-' '_').log"
    if ! (cd "$REPO_ROOT" && ./gradlew --console=plain "$@") >"$log" 2>&1; then
        dump_failure "$label" "$log"
        return 1
    fi
    return 0
}

FAILED=0

run_gradle ":domain:testDebugUnitTest" :domain:testDebugUnitTest || FAILED=1
run_gradle ":presentation:testNoAnalyticsDebugUnitTest" :presentation:testNoAnalyticsDebugUnitTest || FAILED=1
run_gradle ":presentation:assembleNoAnalyticsDebug" :presentation:assembleNoAnalyticsDebug || FAILED=1

if [ "$SKIP_FUNCTIONS" -eq 0 ]; then
    if command -v npm >/dev/null 2>&1; then
        FN_LOG="$LOG_DIR/functions.log"
        (
            set -e
            if [ -f "$REPO_ROOT/functions/package-lock.json" ]; then
                npm --prefix "$REPO_ROOT/functions" ci
            else
                npm --prefix "$REPO_ROOT/functions" install
            fi
            npm --prefix "$REPO_ROOT/functions" run build
        ) >"$FN_LOG" 2>&1 || {
            err "functions build (tsc strict) failed. Relevant output:"
            grep -E -i 'error|TS[0-9]{4}|npm ERR' "$FN_LOG" | head -n 40 | sed 's/^/ERROR: /' >&2 \
                || tail -n 40 "$FN_LOG" | sed 's/^/ERROR: /' >&2
            FAILED=1
        }
    else
        echo "ERROR: npm not found; skipping the functions/ TypeScript lane (not verified)." >&2
    fi
fi

exit "$FAILED"
