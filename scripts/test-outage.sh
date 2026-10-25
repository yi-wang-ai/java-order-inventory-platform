#!/usr/bin/env bash
# RabbitMQ outage recovery test — proves the transactional outbox delivers events
# even when the broker is temporarily unavailable.
# Requires: Docker stack running, APP_API_KEY set, docker CLI accessible.
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
: "${APP_API_KEY:?Set APP_API_KEY to the same key used by the app}"
RABBIT="${RABBIT_CONTAINER:-java-order-inventory-rabbitmq-1}"
CALL=(curl --silent --show-error --fail-with-body --max-time 20 -H "X-API-Key: $APP_API_KEY" -H 'Content-Type: application/json')

SKU="OUTAGE-$(date +%s)-$$"
echo "Creating product $SKU with stock=5"
"${CALL[@]}" -X POST "$BASE_URL/api/products" \
  -d "{\"sku\":\"$SKU\",\"stock\":5,\"priceCents\":2000}" | python3 -m json.tool

echo ""
echo "Step 1: Place order while RabbitMQ is up"
RESP1=$("${CALL[@]}" -X POST -H "Idempotency-Key: outage-before-$SKU" \
  "$BASE_URL/api/orders" -d "{\"sku\":\"$SKU\",\"quantity\":1}")
echo "$RESP1" | python3 -m json.tool
ID1=$(echo "$RESP1" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')
sleep 3

echo ""
echo "Step 2: Stopping RabbitMQ ($RABBIT)"
docker stop "$RABBIT"
sleep 2

echo ""
echo "Step 3: Place order while RabbitMQ is DOWN"
RESP2=$("${CALL[@]}" -X POST -H "Idempotency-Key: outage-during-$SKU" \
  "$BASE_URL/api/orders" -d "{\"sku\":\"$SKU\",\"quantity\":1}")
echo "$RESP2" | python3 -m json.tool
ID2=$(echo "$RESP2" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')
STATUS2=$(echo "$RESP2" | python3 -c 'import sys,json; print(json.load(sys.stdin)["status"])')

STOCK=$(curl -s "$BASE_URL/api/products/$SKU" -H "X-API-Key: $APP_API_KEY" | python3 -c 'import sys,json; print(json.load(sys.stdin)["stock"])')
echo "Stock after 2 orders: $STOCK (expected 3)"

if [ "$STATUS2" != "PLACED" ]; then
  echo "FAIL — order rejected while broker was down" >&2
  docker start "$RABBIT"; exit 1
fi
echo "PASS — order placed despite broker outage"

echo ""
echo "Step 4: Restarting RabbitMQ"
docker start "$RABBIT"
echo "Waiting up to 30s for outbox to deliver queued event..."
DELIVERED=0
for attempt in $(seq 1 15); do
  EVENTS=$(curl -s "$BASE_URL/api/orders/$ID2/events" -H "X-API-Key: $APP_API_KEY")
  COUNT=$(echo "$EVENTS" | python3 -c 'import sys,json; print(len(json.load(sys.stdin)))' 2>/dev/null || echo 0)
  if [ "$COUNT" -ge 1 ]; then DELIVERED=1; break; fi
  sleep 2
done

echo ""
if [ "$DELIVERED" -eq 1 ]; then
  echo "PASS — outbox event delivered after broker recovery"
  echo "$EVENTS" | python3 -m json.tool
else
  echo "FAIL — event not delivered within 30s" >&2; exit 1
fi
