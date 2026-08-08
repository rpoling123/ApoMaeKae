#!/usr/bin/env bash
set -e
BASE="$HOME/ApoMaeKae_V8.3_Complete"
REPO="$HOME/ApoMaeKae"
if [ ! -d "$REPO/.git" ]; then
  git clone https://github.com/rpoling123/ApoMaeKae.git "$REPO"
fi
cd "$REPO"
git checkout main
git pull --rebase origin main
cp -a "$BASE/server/." "$REPO/server/"
cp -a "$BASE/android/." "$REPO/android/"
git add server android
git commit -m "V8.4 update app banner and server branding" || true
git push origin main
echo "DONE: pushed V8.4 to GitHub main. Render should auto-deploy if Auto-Deploy is On Commit."
