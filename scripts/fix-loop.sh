#!/bin/bash
#
# fix-loop.sh
#
# Walks REVIEW_FINDINGS.md and asks Claude Code to implement ONE finding per
# iteration. After each fix the finding's title is prefixed with `[DONE]`,
# the change is committed, and the loop moves to the next finding. Stops
# when there are no more undone findings, or when MAX_ITER is reached.
#
# Usage:
#   nohup ./scripts/fix-loop.sh > logs/fix-loop.out 2>&1 &
#   echo $! > logs/fix-loop.pid
#
# Tail progress:
#   tail -f logs/fix-loop-*.log
#
# Stop early:
#   kill "$(cat logs/fix-loop.pid)"
#
# Tunables:
#   MAX_ITER (default 200)            hard cap on iterations (safety stop)
#   ITER_TIMEOUT (default 1800)       per-iteration wall clock cap, seconds
#   PRIORITY_ORDER (default           comma list of section headers to walk
#     "URGENT,HIGH,MEDIUM,LOW")       in order. Skip e.g. LOW with
#                                     PRIORITY_ORDER="URGENT,HIGH".
#   FIX_LOOP_ALLOW_DIRTY (default 0)  set to 1 to bypass the clean-tree
#                                     pre-flight. Pre-existing dirty files
#                                     will be folded into iter 1's commit.
#   FIX_LOOP_NO_COMMIT (default 0)    set to 1 to skip the per-iter commit.
#

set -uo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FINDINGS_FILE="$PROJECT_DIR/REVIEW_FINDINGS.md"
LOG_DIR="$PROJECT_DIR/logs"
LOG_FILE="$LOG_DIR/fix-loop-$(date +%Y%m%d-%H%M%S).log"
LOCK_FILE="$LOG_DIR/fix-loop.lock"
PROMPT_FILE=""
FINDING_FILE=""

MAX_ITER="${MAX_ITER:-200}"
ITER_TIMEOUT="${ITER_TIMEOUT:-1800}"
PRIORITY_ORDER="${PRIORITY_ORDER:-URGENT,HIGH,MEDIUM,LOW}"
FIX_LOOP_ALLOW_DIRTY="${FIX_LOOP_ALLOW_DIRTY:-0}"
FIX_LOOP_NO_COMMIT="${FIX_LOOP_NO_COMMIT:-0}"

mkdir -p "$LOG_DIR"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

cleanup() {
  local exit_code=$?
  [ -n "${PROMPT_FILE:-}" ] && rm -f "$PROMPT_FILE"
  [ -n "${FINDING_FILE:-}" ] && rm -f "$FINDING_FILE"
  if [ -f "$LOCK_FILE" ] && [ "$(cat "$LOCK_FILE" 2>/dev/null)" = "$$" ]; then
    rm -f "$LOCK_FILE"
  fi
  exit "$exit_code"
}

trap cleanup EXIT
trap 'log "Loop interrupted by signal, exiting"; exit 130' INT TERM

# --- Pre-flight checks ----------------------------------------------------

if ! command -v claude >/dev/null 2>&1; then
  log "FATAL: \`claude\` is not on PATH. Install Claude Code or fix PATH and retry."
  exit 1
fi

claude_version=$(claude --version 2>&1 || true)
if [ -z "$claude_version" ]; then
  log "FATAL: \`claude --version\` returned nothing. Your install looks broken."
  exit 1
fi
log "claude binary OK: $claude_version"

if ! command -v timeout >/dev/null 2>&1; then
  log "FATAL: \`timeout\` is not on PATH (install coreutils)."
  exit 1
fi

if [ ! -f "$FINDINGS_FILE" ]; then
  log "FATAL: $FINDINGS_FILE does not exist. Run review-loop.sh first."
  exit 1
fi

if [ ! -w "$FINDINGS_FILE" ]; then
  log "FATAL: $FINDINGS_FILE is not writable"
  exit 1
fi

if [ -f "$LOCK_FILE" ]; then
  existing_pid=$(cat "$LOCK_FILE" 2>/dev/null || true)
  if [ -n "$existing_pid" ] && kill -0 "$existing_pid" 2>/dev/null; then
    log "FATAL: another fix-loop is already running (pid $existing_pid)."
    log "       Either wait for it to finish or kill it: kill $existing_pid"
    LOCK_FILE=""
    exit 1
  else
    log "WARNING: stale lockfile from pid $existing_pid; reclaiming"
    rm -f "$LOCK_FILE"
  fi
fi
echo "$$" > "$LOCK_FILE"

PROMPT_FILE=$(mktemp "${TMPDIR:-/tmp}/fix-loop-prompt.XXXXXX") || {
  log "FATAL: mktemp failed"; exit 1;
}
FINDING_FILE=$(mktemp "${TMPDIR:-/tmp}/fix-loop-finding.XXXXXX") || {
  log "FATAL: mktemp failed"; exit 1;
}

cd "$PROJECT_DIR" || { log "FATAL: cannot cd to $PROJECT_DIR"; exit 1; }

# Pre-flight: refuse to start with a dirty working tree, unless explicitly
# overridden. We exclude paths the loop is allowed to touch on its own.
if [ "$FIX_LOOP_NO_COMMIT" != "1" ]; then
  if ! command -v git >/dev/null 2>&1 || ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    log "FATAL: not inside a git work tree; commits are required"
    log "       set FIX_LOOP_NO_COMMIT=1 to disable per-iter commits"
    exit 1
  fi

  dirty=$(git status --porcelain 2>/dev/null \
    | grep -vE '(^.. REVIEW_FINDINGS\.md(\.bak)?$|^.. logs/|^.. scripts/(fix|review)-loop.*\.sh$|^.. scripts/REVIEW_FINDINGS)' \
    || true)
  if [ -n "$dirty" ] && [ "$FIX_LOOP_ALLOW_DIRTY" != "1" ]; then
    log "FATAL: working tree has uncommitted changes outside the allow list:"
    while IFS= read -r line; do [ -n "$line" ] && log "    $line"; done <<< "$dirty"
    log "       Commit, stash, or discard them, then re-run."
    log "       To fold them into iter 1's commit instead, set FIX_LOOP_ALLOW_DIRTY=1."
    exit 1
  fi
  if [ -n "$dirty" ]; then
    log "WARNING: pre-existing dirty state will be folded into iter 1's commit:"
    while IFS= read -r line; do [ -n "$line" ] && log "    $line"; done <<< "$dirty"
  fi
fi

# --- Helpers --------------------------------------------------------------

# Locate the next undone finding under the given section header.
# Echoes "<title_line>\t<start_line>\t<end_line>" or nothing if none.
# title_line is the literal `### ...` heading (used as a unique anchor).
find_next_in_section() {
  local section="$1"
  awk -v section="$section" '
    BEGIN { in_section = 0 }
    /^## / {
      if ($0 == "## " section) { in_section = 1; next }
      if (in_section)          { exit }
      next
    }
    in_section && /^### / {
      if ($0 ~ /^### \[DONE\]/) next
      if (start == 0) {
        start = NR
        title = $0
      } else {
        # second finding seen -> we know where the first one ends
        print title "\t" start "\t" (NR - 1)
        printed = 1
        exit
      }
    }
    END {
      if (start != 0 && !printed) {
        # first finding ran to end-of-section / end-of-file. Caller fills
        # in the end line via finding_end_line() when end == 0.
        print title "\t" start "\t0"
      }
    }
  ' "$FINDINGS_FILE"
}

# Given a start line, locate the line just before the next `### ` or `## `
# heading (whichever comes first). Returns the end line, inclusive.
finding_end_line() {
  local start="$1"
  awk -v start="$start" '
    NR <= start { next }
    /^### / || /^## / { print NR - 1; found = 1; exit }
    END { if (!found) print NR }
  ' "$FINDINGS_FILE"
}

count_undone() {
  local total done_count
  # `grep -c` exits 1 when the pattern matches zero lines AND prints "0".
  # Using `|| echo 0` would emit "0\n0" and break the arithmetic, so we
  # set the var on success and fall back to 0 on failure separately.
  total=$(grep -c '^### ' "$FINDINGS_FILE" 2>/dev/null) || total=0
  done_count=$(grep -c '^### \[DONE\]' "$FINDINGS_FILE" 2>/dev/null) || done_count=0
  echo $((total - done_count))
}

# Convert a "### foo" title into "### [DONE] foo".
# Bash parameter expansion `${title/### /### [DONE] }` SILENTLY fails on
# some bash versions because the literal `[DONE]` in the replacement is
# parsed as a glob bracket expression. Sed is the safe path.
to_done_title() {
  printf '%s' "$1" | sed 's|^### |### [DONE] |'
}

# Mark a finding's title as [DONE] in the findings file. Idempotent.
# Uses a python helper because portable in-place sed across mac/linux is
# painful and the title may contain backticks, slashes, parentheses, etc.
mark_done() {
  local title="$1"
  python3 - "$FINDINGS_FILE" "$title" <<'PY'
import sys, pathlib
path = pathlib.Path(sys.argv[1])
title = sys.argv[2]
text = path.read_text()
new_title = title.replace("### ", "### [DONE] ", 1)
if title not in text:
    print("mark_done: title not found, no change", file=sys.stderr)
    sys.exit(2)
if new_title in text:
    print("mark_done: already marked done", file=sys.stderr)
    sys.exit(0)
path.write_text(text.replace(title, new_title, 1))
PY
}

# Stage and commit everything dirty in the working tree, except files the
# loop manages internally (.bak backups, log files, the scripts themselves).
# Returns 0 on a successful commit, 1 if there was nothing to commit, 2 on
# git failure. Caller decides what to log.
commit_iteration() {
  local title="$1"
  local section="$2"

  if [ "$FIX_LOOP_NO_COMMIT" = "1" ]; then
    return 1
  fi

  # Stage everything except the explicit allow-list of loop-internal paths.
  # `git add -A` first, then unstage anything that should not be committed.
  git add -A -- . >> "$LOG_FILE" 2>&1 || {
    log "iteration $iter: WARNING git add failed"
    return 2
  }

  # Always keep REVIEW_FINDINGS.md staged. Drop everything else we manage.
  git reset HEAD -- \
    REVIEW_FINDINGS.md.bak \
    logs/ \
    scripts/fix-loop.sh \
    scripts/review-loop.sh \
    >> "$LOG_FILE" 2>&1 || true

  # If there is nothing left to commit, bail.
  if git diff --cached --quiet; then
    return 1
  fi

  # Build a commit subject from the finding title: strip leading "### ",
  # collapse backticks, drop the [DONE] marker (we add the prefix below),
  # and trim to a reasonable length.
  local subject_body
  subject_body=$(printf '%s' "$title" \
    | sed -e 's|^### \[DONE\] ||' -e 's|^### ||' -e 's|`||g')
  if [ ${#subject_body} -gt 65 ]; then
    subject_body="${subject_body:0:62}..."
  fi

  local subject="fix(review): ${subject_body}"

  if git commit -m "$subject" -m "Implements ${section} finding from REVIEW_FINDINGS.md (fix-loop iter ${iter})." \
       >> "$LOG_FILE" 2>&1; then
    return 0
  else
    log "iteration $iter: WARNING git commit failed"
    return 2
  fi
}

build_prompt() {
  local iter="$1"
  local title="$2"
  local body_path="$3"
  local done_title
  done_title=$(to_done_title "$title")

  cat > "$PROMPT_FILE" <<PROMPT_EOF
You are running iteration $iter of an autonomous fix loop over the ProjectX
codebase. Your job: implement ONE finding from REVIEW_FINDINGS.md, verify the
fix compiles, then mark the finding as done.

Project dir: $PROJECT_DIR
Findings file: $FINDINGS_FILE

THIS ITERATION'S FINDING (verbatim from REVIEW_FINDINGS.md):

$(cat "$body_path")

CRITICAL CONSTRAINTS

1. Implement ONLY this one finding. Do NOT touch any other finding, do NOT
   "improve" surrounding code, do NOT refactor unrelated callers. Stay
   surgical.

2. Read the referenced file(s) BEFORE editing. Confirm the line numbers and
   surrounding context still match the finding description. If the code has
   already been changed (e.g. someone else fixed it, or the line numbers
   have drifted significantly), STOP and mark the finding done with a note
   instead of forcing a change.

3. Follow the **Fix:** section as a guide, but use your judgment if the
   suggested fix would break callers or has a cleaner alternative that
   achieves the same outcome. The **Issue:** is what must go away.

4. After editing, you MUST verify the change compiles:

       ./gradlew :client:compileJava

   The Gradle subproject is \`:client\`, NOT \`:runelite-client\`. If the
   compile fails, fix the compile error before declaring the finding done.
   Do not commit broken code.

5. Once the fix compiles, mark the finding done by editing
   REVIEW_FINDINGS.md: change the line

       $title

   to

       $done_title

   Use the Edit tool with old_string set to the exact original line above.
   Do NOT reorder, delete, or otherwise modify the rest of the finding body.

6. Do NOT run tests, do NOT launch the client, do NOT touch git (do not
   stage, commit, push, pull, or branch). The only verification required
   is \`./gradlew :client:compileJava\`. The fix loop wrapper handles
   committing the change after you finish.

7. Do NOT add code comments explaining what you changed. Do NOT add
   "// fixed by fix-loop" markers. The diff is the documentation.

PROCESS

Step 1 - Read the file(s) listed under **File(s)** in the finding above.
         Read enough context (20-50 lines around each line reference) to
         understand the surrounding code.

Step 2 - Implement the fix using Edit (preferred) or Write. Keep the change
         minimal and focused.

Step 3 - Run \`./gradlew :client:compileJava\` via Bash. If it fails, read
         the error, fix it, and retry. Do not proceed past this step until
         compileJava succeeds.

Step 4 - Edit REVIEW_FINDINGS.md to prefix the finding's title with
         \`[DONE]\` as described in constraint 5.

Step 5 - Stop. Output a one-line summary of what you changed.

Begin now.
PROMPT_EOF
}

# --- Main loop ------------------------------------------------------------

log "===== ProjectX fix loop starting ====="
log "Project dir: $PROJECT_DIR"
log "Findings file: $FINDINGS_FILE"
log "Priority order: $PRIORITY_ORDER"
log "Max iterations: $MAX_ITER, per-iter timeout: ${ITER_TIMEOUT}s"
log "Log file: $LOG_FILE"
log "Undone findings at start: $(count_undone)"

iter=0
fixed_count=0
skipped_count=0

while [ "$iter" -lt "$MAX_ITER" ]; do
  iter=$((iter + 1))

  # Pick the next undone finding by walking the priority order.
  next=""
  picked_section=""
  IFS=',' read -ra sections <<< "$PRIORITY_ORDER"
  for section in "${sections[@]}"; do
    candidate=$(find_next_in_section "$section")
    if [ -n "$candidate" ]; then
      next="$candidate"
      picked_section="$section"
      break
    fi
  done

  if [ -z "$next" ]; then
    log "No undone findings remain in [$PRIORITY_ORDER]. Stopping."
    break
  fi

  title=$(echo "$next" | awk -F'\t' '{print $1}')
  start_line=$(echo "$next" | awk -F'\t' '{print $2}')
  end_line=$(echo "$next" | awk -F'\t' '{print $3}')

  if [ -z "$end_line" ] || [ "$end_line" = "0" ]; then
    end_line=$(finding_end_line "$start_line")
  fi

  if [ -z "$start_line" ] || [ -z "$end_line" ] || [ "$end_line" -lt "$start_line" ]; then
    log "iteration $iter: could not parse finding range; aborting"
    break
  fi

  log "--- iteration $iter/$MAX_ITER: $picked_section finding at lines $start_line-$end_line ---"
  log "    title: $title"

  # Extract the finding body to a scratch file the prompt embeds verbatim.
  sed -n "${start_line},${end_line}p" "$FINDINGS_FILE" > "$FINDING_FILE"

  # Backup before each fix so a busted edit is recoverable.
  cp "$FINDINGS_FILE" "$FINDINGS_FILE.bak" 2>/dev/null || \
    log "WARNING: could not back up findings file"

  build_prompt "$iter" "$title" "$FINDING_FILE"

  start_epoch=$(date +%s)
  timeout "$ITER_TIMEOUT" bash -c '
    cat "$1" | claude -p \
      --allowedTools "Read,Edit,Write,Glob,Grep,Bash" \
      --model sonnet
  ' _ "$PROMPT_FILE" >> "$LOG_FILE" 2>&1
  iter_status=$?
  elapsed=$(( $(date +%s) - start_epoch ))

  if [ $iter_status -eq 0 ]; then
    log "iteration $iter: claude exited cleanly after ${elapsed}s"
  elif [ $iter_status -eq 124 ]; then
    log "iteration $iter: hit ${ITER_TIMEOUT}s timeout (kept partial output)"
  elif [ $iter_status -eq 127 ]; then
    log "iteration $iter: command not found - claude vanished?"
    break
  else
    log "iteration $iter: claude exited with status $iter_status after ${elapsed}s"
  fi

  # Verify the finding actually got marked done.
  done_title=$(to_done_title "$title")
  marked_by_claude=0
  if grep -Fxq "$done_title" "$FINDINGS_FILE"; then
    marked_by_claude=1
    fixed_count=$((fixed_count + 1))
    log "iteration $iter: marked DONE by claude. Total fixed: $fixed_count"
  else
    skipped_count=$((skipped_count + 1))
    log "WARNING: finding was not marked DONE. Force-marking to avoid an infinite loop."
    log "         Review the log above to confirm the fix was actually attempted."
    if mark_done "$title" 2>>"$LOG_FILE"; then
      log "         force-mark succeeded"
    else
      log "         force-mark FAILED. Stopping to avoid spinning."
      break
    fi
  fi

  # Commit the iteration's work (source changes + marked finding) so the
  # next iteration starts from a clean tree.
  commit_iteration "$title" "$picked_section"
  case $? in
    0) log "iteration $iter: committed" ;;
    1) log "iteration $iter: nothing to commit (no source changes detected)" ;;
    2) log "iteration $iter: commit FAILED. Stopping so you can investigate."
       break ;;
  esac
done

# --- Final summary --------------------------------------------------------
log "===== ProjectX fix loop complete ====="
log "Iterations run: $iter"
log "Fixed by claude: $fixed_count"
log "Force-marked (no fix detected): $skipped_count"
log "Undone findings remaining: $(count_undone)"
