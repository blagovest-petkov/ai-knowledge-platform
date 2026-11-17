#!/usr/bin/env bash

set -uo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$PROJECT_DIR" || {
    echo "ERROR: Cannot access project directory: $PROJECT_DIR"
    exit 1
}

FAILED=0

echo "=== Java formatting (Spotless apply) ==="
if ! ./mvnw spotless:apply; then
    echo "Spotless formatting failed."
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
        echo "SQLFluff found issues."
        FAILED=1
    fi
else
    echo "No Flyway migration directory found. Skipping SQLFluff."
fi

echo "=== Maven verification (compile + tests + Spotless check) ==="
if ! ./mvnw verify; then
    echo "Maven verification failed."
    FAILED=1
fi

if [ "$FAILED" -ne 0 ]; then
    echo "Quality checks failed. Fix the issues before completing."
    exit 2
fi

echo "All quality checks passed."
exit 0
