#!/bin/bash
set -euo pipefail

GREEN='\033[1;32m'; RED='\033[1;31m'; YELLOW='\033[1;33m'; BLUE='\033[1;34m'; NC='\033[0m'

MSG="${1:-}"
if [ -z "$MSG" ]; then
    read -r -p "$(echo -e "${YELLOW}Enter commit message (or press Enter for default 'Minor updates'):${NC} ")" MSG
    if [ -z "$MSG" ]; then
        MSG="Minor updates ($(date '+%Y-%m-%d %H:%M:%S'))"
    fi
fi

echo -e "${BLUE}→ Staging all changes...${NC}"
git add .
echo -e "${GREEN}✓ Staged.${NC}"

echo -e "${BLUE}→ Committing...${NC}"
if git commit -m "$MSG"; then
    echo -e "${GREEN}✓ Committed.${NC}"
else
    echo -e "${YELLOW}⚠ Nothing to commit — moving on.${NC}"
fi

echo -e "${BLUE}→ Pushing to origin master...${NC}"
if git push origin master; then
    echo -e "${GREEN}✓ Pushed successfully.${NC}"
else
    echo -e "${RED}❌ Push failed — check network or auth.${NC}"
    exit 1
fi
