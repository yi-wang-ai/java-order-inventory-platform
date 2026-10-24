#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUTPUT="$(mktemp -d)"
trap 'rm -rf "$OUTPUT"' EXIT
javac -d "$OUTPUT" src/main/java/io/portfolio/shop/domain/*.java src/test/java/io/portfolio/shop/domain/DomainSelfTest.java
java -cp "$OUTPUT" io.portfolio.shop.domain.DomainSelfTest
