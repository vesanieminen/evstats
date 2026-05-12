#!/usr/bin/env bash
set -euo pipefail

# Fix ownership on cache-volume mountpoints. Docker creates named-volume
# targets as root when they don't pre-exist in the image; chown them back
# to vscode so installers running as vscode can write here.
sudo chown -R vscode: \
  /home/vscode/.m2 \
  /home/vscode/.gradle \
  /home/vscode/.npm \
  /home/vscode/.local \
  /home/vscode/.cache

# Install extras not in the base image. Avoiding the apt-packages
# devcontainer feature because its nanolayer binary crashes on Apple
# Silicon's 16KB-page Linux VM ("Unsupported system page size").
sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq --no-install-recommends tmux

# Pre-seed Claude Code config and credentials so the first-run wizard skips
# and OAuth auth survives. Workaround for anthropics/claude-code#8938 —
# CLAUDE_CODE_OAUTH_TOKEN alone doesn't satisfy the credential-file lookup
# Claude Code does at startup, and the wizard doesn't know onboarding is
# already complete. If the token env var is empty (host hasn't set it),
# leave the files alone and the interactive flow will run.
mkdir -p ~/.claude
cat > ~/.claude.json <<'JSON'
{
  "hasCompletedOnboarding": true,
  "theme": "dark"
}
JSON
if [[ -n "${CLAUDE_CODE_OAUTH_TOKEN:-}" ]]; then
  cat > ~/.claude/.credentials.json <<JSON
{
  "claudeAiOauth": {
    "accessToken": "$CLAUDE_CODE_OAUTH_TOKEN",
    "refreshToken": "$CLAUDE_CODE_OAUTH_TOKEN",
    "expiresAt": 9999999999999,
    "scopes": ["user:inference", "user:profile"]
  }
}
JSON
  chmod 600 ~/.claude/.credentials.json
fi

# Project tooling. Codex CLI (`@openai/codex`) intentionally omitted from
# the global install — add it back to the line below to re-enable.
npm install -g @playwright/cli@latest
playwright-cli install --skills
claude mcp add vaadin --transport http https://mcp.vaadin.com/docs
npm install
npx playwright install --with-deps chromium
