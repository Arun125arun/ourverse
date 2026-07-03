#!/usr/bin/env bash
# OurVerse admin stats — counts only, never reads message/note content.
# Usage: bash ~/lovenote/stats.sh
set -euo pipefail

python3 - <<'EOF'
import json, urllib.request, datetime, pathlib

# --- access token from the Firebase CLI login ---
cfg = json.load(open(pathlib.Path.home() / ".config/configstore/firebase-tools.json"))
tok = urllib.request.urlopen(urllib.request.Request(
    "https://oauth2.googleapis.com/token",
    data=("client_id=563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com"
          "&client_secret=j9iVZfS8kkCEFUPaAeJV0sAi&grant_type=refresh_token"
          f"&refresh_token={cfg['tokens']['refresh_token']}").encode(),
)).read()
token = json.loads(tok)["access_token"]
BASE = "https://firestore.googleapis.com/v1/projects/ourverse-98c44/databases/(default)/documents"
HDRS = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

def get(url):
    return json.loads(urllib.request.urlopen(urllib.request.Request(url, headers=HDRS)).read())

def count(parent, coll, since=None):
    q = {"structuredAggregationQuery": {
        "structuredQuery": {"from": [{"collectionId": coll}]},
        "aggregations": [{"count": {}, "alias": "n"}]}}
    if since:
        q["structuredAggregationQuery"]["structuredQuery"]["where"] = {
            "fieldFilter": {"field": {"fieldPath": "sentAt"},
                            "op": "GREATER_THAN_OR_EQUAL",
                            "value": {"timestampValue": since}}}
    req = urllib.request.Request(f"{BASE}/{parent}:runAggregationQuery",
                                 data=json.dumps(q).encode(), headers=HDRS)
    rows = json.loads(urllib.request.urlopen(req).read())
    return int(rows[0]["result"]["aggregateFields"]["n"]["integerValue"])

today = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT00:00:00Z")

# display names (profile info only)
names = {}
resp = get(f"{BASE}/users?pageSize=300&mask.fieldPaths=displayName")
for d in resp.get("documents", []):
    uid = d["name"].split("/")[-1]
    names[uid] = d.get("fields", {}).get("displayName", {}).get("stringValue", uid[:6])

resp = get(f"{BASE}/couples?pageSize=300&mask.fieldPaths=members")
couples = resp.get("documents", [])
print(f"\n💜 OurVerse stats — {datetime.date.today()}")
print(f"   Registered users: {len(names)}")
print(f"   Couples: {len(couples)}\n")
for c in couples:
    cid = c["name"].split("/")[-1]
    members = [v["stringValue"] for v in
               c.get("fields", {}).get("members", {}).get("arrayValue", {}).get("values", [])]
    label = " ❤ ".join(names.get(m, m[:6]) for m in members) or "(empty)"
    if len(members) < 2:
        print(f" • {label} — waiting for partner")
        continue
    parent = f"couples/{cid}"
    msgs = count(parent, "messages")
    msgs_today = count(parent, "messages", since=today)
    notes = count(parent, "notes")
    mems = count(parent, "memories")
    print(f" • {label}: {msgs} messages ({msgs_today} today), {notes} notes, {mems} memories")
print()
EOF
