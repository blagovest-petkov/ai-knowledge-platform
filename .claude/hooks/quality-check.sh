#!/usr/bin/env bash

set -uo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$PROJECT_DIR"

FAILED=0

echo "=== Java formatting ==="
if ! ./mvnw spotless:check; then
    FAILED=1
fi

echo "=== PostgreSQL SQL lint ==="
# Prefer the project-local virtualenv; fall back to sqlfluff on PATH.
if [ -x ".venv/bin/sqlfluff" ]; then
    SQLFLUFF=".venv/bin/sqlfluff"
else
    SQLFLUFF="sqlfluff"
fi

if [ -d "src/main/resources/db/migration" ]; then
    if ! "$SQLFLUFF" lint src/main/resources/db/migration/; then
        FAILED=1
    fi
fi

if [ "$FAILED" -ne 0 ]; then
    echo "Quality checks failed. Fix the issues before completing."
    exit 2
fi

echo "All quality checks passed."
