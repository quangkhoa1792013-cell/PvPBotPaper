#!/bin/bash

# ═══════════════════════════════════════════════════════════
#   PvPBot - Release Automation Script
#   Creates a tagged release and pushes to GitHub
# ═══════════════════════════════════════════════════════════

set -euo pipefail

# ── Colors ────────────────────────────────────────────────
GREEN='\033[1;32m'
RED='\033[1;31m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
CYAN='\033[1;36m'
NC='\033[0m'

abort() {
    echo -e "\n${RED}❌ $1${NC}"
    exit 1
}

# ═══════════════════════════════════════════════════════════
#   STEP 1: VERSION INPUT
# ═══════════════════════════════════════════════════════════
VERSION="${1:-}"
if [ -z "$VERSION" ]; then
    echo -e "${YELLOW}No version argument provided.${NC}"
    read -r -p "Please enter the version to release (e.g., 1.0.1): " VERSION
    if [ -z "$VERSION" ]; then
        abort "No version entered. Aborting."
    fi
fi

echo -e "${BLUE}Preparing release of ${CYAN}PvPBot v$VERSION${NC}"

# ═══════════════════════════════════════════════════════════
#   STEP 2: DUPLICATE TAG CHECK
# ═══════════════════════════════════════════════════════════
echo -e "${YELLOW}→ Checking for existing tag v$VERSION...${NC}"

if git tag -l "v$VERSION" | grep -q .; then
    abort "Version tag v$VERSION already exists locally! Please choose a different version."
fi

# Fetch remote tags to check there too
git fetch --tags origin 2>/dev/null || true
if git tag -l "v$VERSION" | grep -q .; then
    abort "Version tag v$VERSION already exists on remote! Please choose a different version."
fi

echo -e "${GREEN}✓ Tag v$VERSION is available.${NC}"

# ═══════════════════════════════════════════════════════════
#   STEP 3: RUN BUILD PIPELINE
# ═══════════════════════════════════════════════════════════
echo -e "\n${BLUE}══════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Running build pipeline (clean → scan → test → build)${NC}"
echo -e "${BLUE}══════════════════════════════════════════════════${NC}"

./build.sh
BUILD_STATUS=$?
if [ $BUILD_STATUS -ne 0 ]; then
    abort "Build pipeline failed! Fix errors before releasing."
fi

echo -e "${GREEN}✓ Build pipeline passed.${NC}"

# ═══════════════════════════════════════════════════════════
#   STEP 4: CONFIRMATION GATE
# ═══════════════════════════════════════════════════════════
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║           Release Summary                            ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════╣${NC}"
printf "${CYAN}║${NC} %-20s : %-25s ${CYAN}║${NC}\n" "Version" "v$VERSION"
printf "${CYAN}║${NC} %-20s : %-25s ${CYAN}║${NC}\n" "Build Status" "PASSED"
printf "${CYAN}║${NC} %-20s : %-25s ${CYAN}║${NC}\n" "Actions" "Commit + Tag + Push"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
echo ""

read -r -p "$(echo -e "${YELLOW}⚠️ Are you sure you want to commit, tag, and publish PvPBot v$VERSION to GitHub? [y/N]:${NC} ")" CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    abort "Release cancelled by user."
fi

# ═══════════════════════════════════════════════════════════
#   STEP 5: VERSION BUMP & PUBLISH
# ═══════════════════════════════════════════════════════════
echo -e "\n${BLUE}→ Updating plugin.yml version...${NC}"
sed -i "s/version:.*/version: '$VERSION'/g" src/main/resources/plugin.yml
echo -e "${GREEN}✓ plugin.yml updated.${NC}"

echo -e "${BLUE}→ Committing version bump...${NC}"
git add src/main/resources/plugin.yml
git commit -m "Release version v$VERSION"
echo -e "${GREEN}✓ Committed.${NC}"

echo -e "${BLUE}→ Creating annotated tag v$VERSION...${NC}"
git tag -a "v$VERSION" -m "Release version v$VERSION"
echo -e "${GREEN}✓ Tag created.${NC}"

echo -e "${BLUE}→ Pushing to origin master with tags...${NC}"
git push origin master --tags
echo -e "${GREEN}✓ Push complete.${NC}"

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║   PvPBot v$VERSION released successfully!              ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════╝${NC}"
