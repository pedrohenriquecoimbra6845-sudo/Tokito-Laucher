#!/usr/bin/env bash
# Tokito Launcher - issues a TokiAuth Original license key (ECDSA P-256 signature, see TokiAuthLicense.kt).
#
# Usage:  tokiauth-license.sh <player name> [std|admin] [private-key.pem]
#   std    normal paid license (R$ 3)
#   admin  free license for the admin / friends of the project
# The private key can also come from the file named in $TOKIAUTH_SIGNING_KEY_FILE.
# The GitHub workflow "TokiAuth: emitir licença" runs this script with the secret TOKIAUTH_SIGNING_KEY.
set -euo pipefail

name="${1:?usage: $0 <player name> [std|admin] [private-key.pem]}"
kind="${2:-std}"
key="${3:-${TOKIAUTH_SIGNING_KEY_FILE:-tokiauth_private.pem}}"

if ! [[ "$name" =~ ^[A-Za-z0-9_]{3,16}$ ]]; then
  echo "Invalid player name (3-16 letters, numbers or _): $name" >&2
  exit 1
fi
if [[ "$kind" != "std" && "$kind" != "admin" ]]; then
  echo "Kind must be std or admin: $kind" >&2
  exit 1
fi
if [[ ! -f "$key" ]]; then
  echo "Private key not found: $key" >&2
  exit 1
fi

lower="$(printf '%s' "$name" | tr 'A-Z' 'a-z')"
payload="$(printf '{"n":"%s","k":"%s","i":%s}' "$lower" "$kind" "$(date +%s)")"
b64url() { base64 -w0 | tr '+/' '-_' | tr -d '='; }
p="$(printf '%s' "$payload" | b64url)"
s="$(printf '%s' "$payload" | openssl dgst -sha256 -sign "$key" | b64url)"
printf 'TKA1.%s.%s\n' "$p" "$s"
