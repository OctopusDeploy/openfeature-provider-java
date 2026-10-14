#!/usr/bin/env bash
# PROTOTYPE, throwaway. Answers one question for BMBB-1251:
#   Does central-publishing-maven-plugin 0.12.0 with skipUpload=true build a clean bundle (no upload) on Maven 3.10?
#
# Run from the repo root:  ./prototype-central-plugin-0.12.0/run.sh
# Each scenario patches pom.xml, builds with GPG skipped, and diffs the bundle's file list against the
# Oct 7 bundle that validated on Central (known-good-oct7-unsigned.txt, .asc files removed).
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
CACHE="${PROTO_CACHE:-$HERE/.cache}"
OUT="$HERE/out"
mkdir -p "$CACHE" "$OUT"
cd "$ROOT"
cp pom.xml "$OUT/pom.xml.orig"
trap 'cp "$OUT/pom.xml.orig" "$ROOT/pom.xml"' EXIT

maven() { # $1 = version; echoes the mvn path
  local d="$CACHE/apache-maven-$1"
  if [ ! -x "$d/bin/mvn" ]; then
    curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$1/apache-maven-$1-bin.tar.gz" | tar xz -C "$CACHE"
  fi
  echo "$d/bin/mvn"
}

# name | maven | plugin version | skip property
SCENARIOS=(
  "control-0.8.0-skipPublishing-mvn3.10.0|3.10.0|0.8.0|skipPublishing"
  "0.12.0-skipUpload-mvn3.9.16|3.9.16|0.12.0|skipUpload"
  "0.12.0-skipUpload-mvn3.10.0|3.10.0|0.12.0|skipUpload"
  "0.12.0-skipPublishing-mvn3.10.0|3.10.0|0.12.0|skipPublishing"
)

printf '\n%-40s %-8s %-6s %s\n' SCENARIO BUILD FILES "DIFF vs known-good"
for s in "${SCENARIOS[@]}"; do
  IFS='|' read -r name mv pv prop <<<"$s"
  cp "$OUT/pom.xml.orig" pom.xml
  sed -i "/central-publishing-maven-plugin/{n;s|<version>0.8.0</version>|<version>$pv</version>|}" pom.xml
  sed -i "s|<skipPublishing>true</skipPublishing>|<$prop>true</$prop>|" pom.xml

  log="$OUT/$name.log"
  MVN="$(maven "$mv")"
  # Placeholder `central` server, as setup-java writes in CI. Any real upload attempt would fail auth and show in the log.
  # SETTINGS=none leaves it out, to check whether a scenario needs it at all.
  gs=()
  [ "${SETTINGS:-}" != none ] && gs=(-gs "$HERE/settings-placeholder.xml")
  "$MVN" -B "${gs[@]}" clean deploy -Dgpg.skip=true -DskipTests >"$log" 2>&1
  rc=$?

  zip="target/central-publishing/central-bundle.zip"
  list="$OUT/$name.files.txt"
  if [ -f "$zip" ]; then
    ver=$(sed -n 's|.*<version>\(.*\)</version> <!-- x-release-please-version -->.*|\1|p' pom.xml)
    unzip -Z1 "$zip" | sed "s|$ver|VERSION|g" | sort >"$list"
    n=$(wc -l <"$list")
    d=$(diff "$HERE/known-good-oct7-unsigned.txt" "$list" | grep '^[<>]' | tr '\n' ' ')
    [ -z "$d" ] && d="identical"
  else
    : >"$list"; n=0; d="NO BUNDLE"
  fi
  printf '%-40s %-8s %-6s %s\n' "$name" "rc=$rc" "$n" "$d"
  grep -iE 'upload|skipping central|no files to stage|bundle|ERROR' "$log" | sed 's/^/    /' | head -6
done
echo
echo "Logs and file lists: $OUT"
