#!/usr/bin/env bash
set -euo pipefail

run_annotated() {
    local title="$1"
    shift
    echo "::group::$title"
    "$@"
    echo "::endgroup::"
}
