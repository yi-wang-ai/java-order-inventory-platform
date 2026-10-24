#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
: "${APP_API_KEY:?Set APP_API_KEY to the same key used by the app}"
echo 'Waiting for the local API to start...'
READY=0
for attempt in $(seq 1 60); do
  if curl -fsS --max-time 2 "$BASE_URL/actuator/health" >/dev/null 2>&1; then READY=1; break; fi
  sleep 2
done
if [[ "$READY" -ne 1 ]]; then echo 'API health check did not become ready' >&2; exit 1; fi
SKU="DEMO-$(date +%s)-$$"
IDEMPOTENCY="idem-$(date +%s)-$$"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
CALL=(curl --silent --show-error --fail-with-body --max-time 20 -H "X-API-Key: $APP_API_KEY" -H 'Content-Type: application/json')

echo '1. Create product'
"${CALL[@]}" -X POST "$BASE_URL/api/products" -d "{\"sku\":\"$SKU\",\"stock\":5,\"priceCents\":2599}" > "$TMP/product.json"
cat "$TMP/product.json"; echo

echo '2. Place order'
"${CALL[@]}" -X POST -H "Idempotency-Key: $IDEMPOTENCY" "$BASE_URL/api/orders" \
  -d "{\"sku\":\"$SKU\",\"quantity\":2}" > "$TMP/order.json"
cat "$TMP/order.json"; echo

export PROJECT_TMP="$TMP"
ID="$(python3 -c 'import json,os;print(json.load(open(os.environ["PROJECT_TMP"]+"/order.json"))["id"])')"

echo '3. Replay same request: expect same order ID'
"${CALL[@]}" -X POST -H "Idempotency-Key: $IDEMPOTENCY" "$BASE_URL/api/orders" \
  -d "{\"sku\":\"$SKU\",\"quantity\":2}" > "$TMP/replay.json"
python3 - <<'PY'
import json,os
p=os.environ['PROJECT_TMP']
a=json.load(open(p+'/order.json'));b=json.load(open(p+'/replay.json'))
assert a['id']==b['id'], 'Idempotency failed'
print('PASS: same order ID',a['id'])
PY

echo '4. Expect remaining stock = 3'
"${CALL[@]}" "$BASE_URL/api/products/$SKU" > "$TMP/stock.json"
python3 - <<'PY'
import json,os
x=json.load(open(os.environ['PROJECT_TMP']+'/stock.json'))
assert x['stock']==3, f"Expected stock 3, got {x['stock']}"
print('PASS: stock 3')
PY

echo '5. Cancel order; stock returns to 5'
"${CALL[@]}" -X POST "$BASE_URL/api/orders/$ID/cancel" > "$TMP/cancel.json"
"${CALL[@]}" "$BASE_URL/api/products/$SKU" > "$TMP/restored.json"
python3 - <<'PY'
import json,os
p=os.environ['PROJECT_TMP']
a=json.load(open(p+'/cancel.json'));b=json.load(open(p+'/restored.json'))
assert a['status']=='CANCELLED' and b['stock']==5
print('PASS: cancellation restored stock')
PY

echo '6. Wait for both created and cancelled events'
for attempt in $(seq 1 20); do
  "${CALL[@]}" "$BASE_URL/api/orders/$ID/events" > "$TMP/events.json"
  if python3 - <<'PY'
import json,os,sys
x=json.load(open(os.environ['PROJECT_TMP']+'/events.json'))
sys.exit(0 if len(x)>=2 else 1)
PY
  then
    echo 'PASS: two broker-delivered events'; exit 0
  fi
  sleep 2
done
echo 'FAIL: RabbitMQ events not delivered after 40 seconds' >&2
exit 1
