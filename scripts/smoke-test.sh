#!/usr/bin/env bash
# End-to-end smoke test against a RUNNING backend (default http://localhost:8080).
# Needs: curl and python3 (only used to read JSON). Exit code 0 = everything passed.
set -u
BASE="${BASE:-http://localhost:8080/api}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@placement.local}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123}"
PASS=0; FAIL=0

json() { python3 -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1], {'d': d}))" "$1"; }
check() { # check "name" expected actual
  if [ "$2" = "$3" ]; then echo "  PASS  $1"; PASS=$((PASS+1)); else echo "  FAIL  $1 (expected $2, got $3)"; FAIL=$((FAIL+1)); fi
}
status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

EMAIL="smoke-$(date +%s)@test.local"

echo "== security =="
check "no token -> 401"           401 "$(status "$BASE/questions")"
check "garbage token -> 401"      401 "$(status -H 'Authorization: Bearer abc.def.ghi' "$BASE/questions")"

echo "== auth =="
REG=$(curl -s -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Smoke\",\"email\":\"$EMAIL\",\"password\":\"Password1!\"}")
TOKEN=$(echo "$REG" | json "d['token']")
check "register returns STUDENT"  STUDENT "$(echo "$REG" | json "d['user']['roles'][0]")"
check "duplicate email -> 409"    409 "$(status -X POST "$BASE/auth/register" -H 'Content-Type: application/json' -d "{\"name\":\"Smoke\",\"email\":\"$EMAIL\",\"password\":\"Password1!\"}")"
check "wrong password -> 401"     401 "$(status -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"password\":\"nope-nope\"}")"
AUTH="Authorization: Bearer $TOKEN"
check "student blocked from admin -> 403" 403 "$(status -H "$AUTH" "$BASE/admin/dashboard")"

ADMIN_TOKEN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" | json "d['token']")
AAUTH="Authorization: Bearer $ADMIN_TOKEN"
check "admin dashboard -> 200"    200 "$(status -H "$AAUTH" "$BASE/admin/dashboard")"

echo "== question bank =="
PAGE=$(curl -s -H "$AUTH" "$BASE/questions?difficulty=EASY&type=DSA&size=5")
check "filtered page has content" True "$(echo "$PAGE" | json "len(d['content'])>0")"
QID=$(echo "$PAGE" | json "d['content'][0]['id']")
check "bookmark -> 204"           204 "$(status -X POST -H "$AUTH" "$BASE/bookmarks/$QID")"
check "bookmark twice (idempotent) -> 204" 204 "$(status -X POST -H "$AUTH" "$BASE/bookmarks/$QID")"
check "record SOLVED submission -> 201" 201 "$(status -X POST -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"questionId\":$QID,\"language\":\"Java\",\"status\":\"SOLVED\",\"timeTakenMinutes\":10}" "$BASE/submissions")"
check "question now solved"       True "$(curl -s -H "$AUTH" "$BASE/questions/$QID" | json "d['solved'] and d['bookmarked']")"

echo "== timed test =="
TEST_ID=$(curl -s -H "$AUTH" "$BASE/tests" | json "d[0]['id']")
START=$(curl -s -X POST -H "$AUTH" "$BASE/tests/$TEST_ID/start")
ATTEMPT=$(echo "$START" | json "d['attemptId']")
check "no 'correct' flag leaks"   True "$(echo "$START" | json "'correct' not in d['questions'][0]['options'][0]")"
# answer every question with its FIRST option (some right, some wrong) and submit
ANSWERS=$(echo "$START" | json "__import__('json').dumps({'answers':[{'questionId':q['id'],'optionId':q['options'][0]['id']} for q in d['questions']]})")
RESULT=$(curl -s -X POST -H "$AUTH" -H 'Content-Type: application/json' -d "$ANSWERS" "$BASE/attempts/$ATTEMPT/submit")
check "submitted + scored"        SUBMITTED "$(echo "$RESULT" | json "d['summary']['status']")"
check "second submit -> 409"      409 "$(status -X POST -H "$AUTH" -H 'Content-Type: application/json' -d "$ANSWERS" "$BASE/attempts/$ATTEMPT/submit")"
check "other user cannot read attempt -> 404" 404 "$(status -H "$AAUTH" "$BASE/attempts/$ATTEMPT")"

echo "== stats and recommender =="
check "stats endpoint -> 200"     200 "$(status -H "$AUTH" "$BASE/stats/me")"
check "weak-topics endpoint -> 200" 200 "$(status -H "$AUTH" "$BASE/recommendations/weak-topics")"
check "recommendations -> 200"    200 "$(status -H "$AUTH" "$BASE/recommendations")"
check "companies list non-empty"  True "$(curl -s -H "$AUTH" "$BASE/companies" | json "len(d)>0")"

echo
echo "passed: $PASS   failed: $FAIL"
[ "$FAIL" -eq 0 ]
