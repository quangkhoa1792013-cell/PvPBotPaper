#!/bin/bash

# ═══════════════════════════════════════════════════════════
#   PvPBot - Build Pipeline Script
#   Pipeline: clean → test/scan → build → cleanup
# ═══════════════════════════════════════════════════════════

# ── Metadata Configuration ─────────────────────────────────
TARGET_SERVER="Paper 1.21.11"
SUPPORTED_PLATFORMS="Paper, Purpur (1.21.x)"
REQUIRED_JAVA="Java 21"
PROJECT_TYPE="Pure NMS PvP Bot Engine"
SRC_DIR="src/main/java"
REPORT_FILE="build/pipeline-report.txt"

# ── Colors ────────────────────────────────────────────────
GREEN='\033[1;32m'
RED='\033[1;31m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
CYAN='\033[1;36m'
NC='\033[0m'

# ── Environment ──────────────────────────────────────────
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# ── Helpers ──────────────────────────────────────────────
abort() {
    echo -e "\n${RED}❌ $1${NC}"
    exit 1
}

step_header() {
    echo -e "\n${BLUE}══════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════${NC}"
}

# ═══════════════════════════════════════════════════════════
#   DASHBOARD
# ═══════════════════════════════════════════════════════════
clear
echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════════╗"
echo "║              PvPBot - Build Pipeline                ║"
echo "╠══════════════════════════════════════════════════════╣"
printf "║ %-25s : %-25s ║\n" "Target Server" "$TARGET_SERVER"
printf "║ %-25s : %-25s ║\n" "Platforms" "$SUPPORTED_PLATFORMS"
printf "║ %-25s : %-25s ║\n" "Required Java" "$REQUIRED_JAVA"
printf "║ %-25s : %-25s ║\n" "Project Type" "$PROJECT_TYPE"
printf "║ %-25s : %-25s ║\n" "Working Dir" "$(pwd)"
echo "╚══════════════════════════════════════════════════════╝"
echo -e "${NC}"

# ═══════════════════════════════════════════════════════════
#   STEP 1: CLEAN
# ═══════════════════════════════════════════════════════════
step_header "[1/4] Cleaning build cache..."

./gradlew clean
[ $? -ne 0 ] && abort "Clean failed!"
echo -e "${GREEN}✓ Clean complete.${NC}"

mkdir -p build
echo "PVPBOT BUILD REPORT - $(date)" > "$REPORT_FILE"
echo "=========================================" >> "$REPORT_FILE"
echo "Step 1: Clean -> OK" >> "$REPORT_FILE"

# ═══════════════════════════════════════════════════════════
#   STEP 2: TESTS & STATIC ANALYSIS
# ═══════════════════════════════════════════════════════════
step_header "[2/4] Running unit tests & static analysis..."

# 2a: Unit tests
./gradlew test
TEST_EXIT=$?
echo "Step 2a: Test -> $([ $TEST_EXIT -eq 0 ] && echo 'OK' || echo 'FAILED')" >> "$REPORT_FILE"
if [ $TEST_EXIT -ne 0 ]; then
    abort "Unit tests failed!"
fi
echo -e "${GREEN}✓ All tests passed.${NC}"

# 2b: Static security & performance scans
WARN_COUNT=0

echo -e "${YELLOW}→ Checking synchronous network calls...${NC}"
SYNC_HTTP=$(grep -rn -e "HttpURLConnection" -e "HttpClient" "$SRC_DIR" 2>/dev/null || true)
if [ -n "$SYNC_HTTP" ]; then
    echo -e "${RED}[WARN] Synchronous HTTP calls detected:${NC}"
    echo "$SYNC_HTTP"
    echo -e "[WARN] Synchronous network calls:\n$SYNC_HTTP" >> "$REPORT_FILE"
    WARN_COUNT=$((WARN_COUNT + 1))
else
    echo -e "${GREEN}  ✓ No blocking network calls.${NC}"
fi

echo -e "${YELLOW}→ Checking command permission gates...${NC}"
MISSING_PERMS=""
while IFS= read -r line; do
    MISSING_PERMS="${MISSING_PERMS}  - ${line}"$'\n'
done < <(grep -rn "onCommand" "$SRC_DIR" 2>/dev/null \
    | grep -v "hasPermission" 2>/dev/null \
    | grep -v "public boolean onCommand" 2>/dev/null \
    | grep -v "implements" 2>/dev/null \
    | grep -v "/*" 2>/dev/null \
    | grep -v "*" 2>/dev/null \
    || true)
if [ -n "$MISSING_PERMS" ]; then
    echo -e "${RED}[WARN] Commands without hasPermission():${NC}"
    echo -e "$MISSING_PERMS"
    echo -e "[WARN] Missing permission checks:" >> "$REPORT_FILE"
    echo -e "$MISSING_PERMS" >> "$REPORT_FILE"
    WARN_COUNT=$((WARN_COUNT + 1))
else
    echo -e "${GREEN}  ✓ All commands have permission gates.${NC}"
fi

echo -e "${YELLOW}→ Checking unsafe Java reflection...${NC}"
UNSAFE_REFLECT=$(grep -rn -e "setAccessible(true)" -e "Class.forName" "$SRC_DIR" 2>/dev/null || true)
if [ -n "$UNSAFE_REFLECT" ]; then
    echo -e "${YELLOW}[WARN] Raw reflection usage:${NC}"
    echo "$UNSAFE_REFLECT"
    echo -e "[WARN] Unsafe reflection:\n$UNSAFE_REFLECT" >> "$REPORT_FILE"
    WARN_COUNT=$((WARN_COUNT + 1))
else
    echo -e "${GREEN}  ✓ No unsafe reflection.${NC}"
fi

echo -e "${YELLOW}→ Checking Thread.sleep usage...${NC}"
THREAD_SLEEP=$(grep -rn "Thread\.sleep" "$SRC_DIR" 2>/dev/null || true)
if [ -n "$THREAD_SLEEP" ]; then
    echo -e "${RED}[CRITICAL] Thread.sleep detected (blocks main thread):${NC}"
    echo "$THREAD_SLEEP"
    echo -e "[CRITICAL] Thread.sleep:\n$THREAD_SLEEP" >> "$REPORT_FILE"
    WARN_COUNT=$((WARN_COUNT + 1))
else
    echo -e "${GREEN}  ✓ No Thread.sleep found.${NC}"
fi

echo "Warnings: $WARN_COUNT" >> "$REPORT_FILE"
echo "Step 2: Scan -> OK ($WARN_COUNT warnings)" >> "$REPORT_FILE"

# ═══════════════════════════════════════════════════════════
#   STEP 3: BUILD
# ═══════════════════════════════════════════════════════════
step_header "[3/4] Compiling & reobfuscating production jar..."

./gradlew build
BUILD_EXIT=$?
echo "Step 3: Build -> $([ $BUILD_EXIT -eq 0 ] && echo 'OK' || echo 'FAILED')" >> "$REPORT_FILE"
if [ $BUILD_EXIT -ne 0 ]; then
    abort "Build failed!"
fi
echo -e "${GREEN}✓ Build successful.${NC}"
ls -lh build/libs/*.jar 2>/dev/null
ls -lh build/libs/*.jar >> "$REPORT_FILE" 2>/dev/null

# ═══════════════════════════════════════════════════════════
#   STEP 4: CLEAN UP TEMPORARY BUILD ARTIFACTS
# ═══════════════════════════════════════════════════════════
step_header "[4/4] Cleaning up temporary build artifacts..."

for dir in build/tmp build/classes build/generated build/reports build/generated-sources; do
    if [ -d "$dir" ]; then
        rm -rf "$dir"
        echo -e "  ${GREEN}✓ Removed${NC} $dir"
    fi
done

echo "Step 4: Cleanup -> OK" >> "$REPORT_FILE"
echo -e "${GREEN}✓ Temporary artifacts cleaned.${NC}"

# ═══════════════════════════════════════════════════════════
#   SUMMARY
# ═══════════════════════════════════════════════════════════
echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
if [ $WARN_COUNT -gt 0 ]; then
    echo -e "${YELLOW}║   Pipeline finished with $WARN_COUNT warning(s)         ║${NC}"
    echo -e "${YELLOW}║   See: $REPORT_FILE            ║${NC}"
else
    echo -e "${GREEN}║   Pipeline completed — 100% clean                      ║${NC}"
fi
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"

exit 0
