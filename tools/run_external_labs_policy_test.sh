#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v kotlinc >/dev/null || { echo 'Kotlin CLI indisponível; use :core:test no Gradle'; exit 1; }
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
kotlinc core/src/main/kotlin/br/com/pulsobateria/core/ExternalLabsPolicy.kt tools/test_external_labs_policy.kt -include-runtime -d "$TMP/external-labs-tests.jar"
java -jar "$TMP/external-labs-tests.jar"
