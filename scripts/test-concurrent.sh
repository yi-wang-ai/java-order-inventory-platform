#!/usr/bin/env bash
# Concurrent order safety test — proves the conditional stock UPDATE prevents overselling.
# Requires: Docker stack running, APP_API_KEY set.
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
: "${APP_API_KEY:?Set APP_API_KEY to the same key used by the app}"
CALL=(curl --silent --show-error --max-time 20 -H "X-API-Key: $APP_API_KEY" -H 'Content-Type: application/json')
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT

STOCK=2
WORKERS=5
SKU="RACE-$(date +%s)-$$"

echo "Creating product $SKU with stock=$STOCK"
"${CALL[@]}" -X POST "$BASE_URL/api/products" \
  -d "{\"sku\":\"$SKU\",\"stock\":$STOCK,\"priceCents\":1000}" | python3 -m json.tool

echo "Firing $WORKERS concurrent orders (qty=1 each)..."
for i in $(seq 1 "$WORKERS"); do
  curl -s -o "$TMP/resp_$i.json" -w "%{http_code}" \
    -X POST "$BASE_URL/api/orders" \
    -H "X-API-Key: $APP_API_KEY" -H 'Content-Type: application/json' \
    -H "Idempotency-Key: race-${SKU}-${i}" \
    -d "{\"sku\":\"$SKU\",\"quantity\":1}" > "$TMP/http_$i.txt" &
done
wait

PLACED=0; REJECTED=0
for i in $(seq 1 "$WORKERS"); do
  HTTP=$(cat "$TMP/http_$i.txt")
  if [ "$HTTP" = "201" ]; then
    PLACED=$((PLACED+1)); echo "  Order $i: HTTP 201 PLACED"
  else
    REJECTED=$((REJECTED+1))
    ERR=$(python3 -c "import json; print(json.load(open('$TMP/resp_$i.json')).get('error','?'))" 2>/dev/null || echo '?')
    echo "  Order $i: HTTP $HTTP REJECTED ($ERR)"
  fi
done

FINAL=$(curl -s "$BASE_URL/api/products/$SKU" -H "X-API-Key: $APP_API_KEY" | python3 -c 'import sys,json; print(json.load(sys.stdin)["stock"])')
echo ""
echo "Placed: $PLACED  Rejected: $REJECTED  Final stock: $FINAL"

if [ "$PLACED" -le "$STOCK" ] && [ "$FINAL" -ge 0 ]; then
  echo "PASS — no oversell (placed <= initial stock, stock >= 0)"
else
  echo "FAIL — oversell detected" >&2; exit 1
fi
