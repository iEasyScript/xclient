#!/bin/bash
set -euo pipefail

PROMPT="${1:?Usage: test-loop.sh <prompt> [max-iterations]}"
MAX_ITER="${2:-5}"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$PROJECT_DIR/logs"
LOG_FILE="$LOG_DIR/test-loop-$(date +%Y%m%d-%H%M%S).log"
PROMPT_FILE=$(mktemp "${TMPDIR:-/tmp}/test-loop-prompt.XXXXXX")

trap 'rm -f "$PROMPT_FILE"' EXIT

mkdir -p "$LOG_DIR"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

cd "$PROJECT_DIR"

cat > "$PROMPT_FILE" <<PROMPT_EOF
You are an autonomous ProjectX development agent. Follow docs/AGENTIC_TESTING_LOOP.md for reference.

Project dir: $PROJECT_DIR
Max iterations: $MAX_ITER

GOAL: $PROMPT

IMPORTANT RULES:

1. If the goal mentions verify, test, check, confirm, debug, or anything implying you need
   to see the result in-game — you MUST launch the client and verify. Do NOT just make code changes
   and call it done. The whole point of this loop is to verify changes at runtime.

2. VERIFY MEANS TESTING ACTUAL BEHAVIOR, NOT CONFIG VALUES. When the goal says 'verify that it
   works', you must test the actual in-game effect, not just read config values or check if a
   boolean is set. For example:
   - 'disable level up interface' means: trigger a level up in-game and confirm the popup does
     NOT appear, or search for the level-up widget and confirm it is not visible.
   - 'verify banking works' means: actually open a bank, deposit items, and check they moved.
   - 'verify walking works' means: walk to a location and confirm the player arrived.
   Use the CLI to observe actual game state (widgets, inventory, position) and take screenshots
   to visually confirm. Reading a config file or checking a boolean is NOT verification.

3. IF YOU NOTICE A BUG IN THE PROJECTX CLI API, FIX IT BEFORE CONTINUING. While using the
   projectx-cli, if a command returns wrong data, crashes, hangs, mis-parses arguments, has
   broken output formatting, or otherwise behaves incorrectly, STOP using the CLI to verify
   your original goal. First:
   - Locate the offending CLI command source (typically under runelite-client/.../agentserver/
     or the projectx-cli script + its server-side handler).
   - Fix the bug at the root cause — do not work around it, do not switch to a different CLI
     command just to bypass it, and do not silently tolerate the broken behavior.
   - Recompile (./gradlew :client:compileJava), relaunch the client, and confirm the CLI
     command now works.
   - Only then resume verifying your original goal.
   Reason: a broken CLI poisons every subsequent verification step in the loop. Leaving it
   broken means future iterations (and other agents) will hit the same bug. Fixing it once
   pays for itself immediately.

You have up to $MAX_ITER iterations to achieve the goal.

## Step-by-step workflow

1. Explore the codebase to understand what needs to change.
2. Make the necessary code changes.
3. COMPILE: ./gradlew :client:compileJava — if it fails, read errors, fix, and retry.
4. LAUNCH the client and VERIFY the change works in-game (see below).
5. If verification fails, analyze why, fix the code, and loop back to step 2.

## How to launch the client and verify

You have two approaches. Pick whichever fits the goal best, or combine them.

### Approach A: Launch client in background + use ProjectX CLI to verify

This is best when you want to inspect game state, poke at widgets, or verify something interactively.

  1. Launch the client in the background:
     ./gradlew :client:run &
     CLIENT_PID=\$!

  2. Wait for the client to start and the player to log in:
     sleep 30
     $PROJECT_DIR/projectx-cli login wait --timeout 120

  3. Use the CLI to verify your changes:
     $PROJECT_DIR/projectx-cli state
     $PROJECT_DIR/projectx-cli widgets search "level-up,notification"
     $PROJECT_DIR/projectx-cli widgets describe <groupId> <childId> --depth 3
     $PROJECT_DIR/projectx-cli widgets click <groupId> <childId>
     $PROJECT_DIR/projectx-cli inventory
     $PROJECT_DIR/projectx-cli skills
     $PROJECT_DIR/projectx-cli scripts

     See docs/PROJECTX_CLI.md for all available commands.

  4. Take screenshots to see what the client looks like:
     $PROJECT_DIR/projectx-cli screenshot save --label mystep
     This saves a PNG to ~/.runelite/test-results/screenshots/ and returns the path.
     You can then read the screenshot image to see the client state visually.
     TAKE A SCREENSHOT whenever:
     - You are unsure what the client is showing
     - A CLI command returns unexpected results
     - You want to verify a visual change (widget visibility, interface state)
     - The client seems stuck or unresponsive
     - Before and after making a change to compare

  5. When done, stop the client:
     kill \$CLIENT_PID 2>/dev/null; wait \$CLIENT_PID 2>/dev/null || true

### Approach B: Write an ExampleScript + launch in test mode

This is best when you need automated assertions that run inside the client.

The ExampleScript at:
  runelite-client/src/main/java/net/runelite/client/plugins/projectx/example/ExampleScript.java

is a script that runs inside the game client. You can REPLACE its contents with custom verification
code. The script extends Script and has access to the full ProjectX API (Rs2Widget, Rs2Inventory,
Rs2Player, etc). See the CLAUDE.md in the projectx directory for the full API reference.

To launch:
  1. rm -f \$HOME/.runelite/test-results/result.json
  2. ./gradlew :client:runTest -Dprojectx.test.mode=true -Dprojectx.test.script='Example' -Dprojectx.test.timeout=120000
  3. Read results from \$HOME/.runelite/test-results/result.json
  4. Exit codes: 0=pass, 1=fail, 2=timeout, 3=crash

### Combining both approaches

You can also launch in test mode AND use the CLI at the same time. Launch the runTest in the
background, then use the CLI to inspect state while the ExampleScript runs.

## Gotchas

### ProjectX.showMessage() blocks the client
ProjectX.showMessage() opens a JOptionPane dialog that BLOCKS the entire client until someone
clicks OK. If you see the client hanging and not responding to CLI commands, a showMessage dialog
may be open. Do NOT use ProjectX.showMessage() in any code you write for the ExampleScript.
If existing code calls showMessage() and it blocks you, you need to either:
- Remove or bypass the showMessage() call in the source code
- Or, on Linux X11 only, use xdotool to send a Return keypress to dismiss the dialog:
  xdotool search --name 'Message' key Return
  (xdotool is not available on macOS, Wayland, or Windows — prefer fixing the source.)

### Client takes time to start
After launching ./gradlew :client:run, the client needs ~30-60 seconds to fully start
and log in. Always wait and confirm with ./projectx-cli state or login wait before proceeding.

### Agent Server plugin must be enabled
The CLI only works when the Agent Server plugin is enabled in the client. If CLI calls fail with
connection refused, the plugin may not be enabled. Check the plugin list or enable it via config.

## Reporting

Report a clear summary after each iteration: what you changed, whether it compiled, whether you
launched the client, what CLI commands you ran, and what the results were.
If you took screenshots, mention the paths so they can be reviewed.
PROMPT_EOF

log "=== test-loop started ==="
log "Prompt: $PROMPT"
log "Max iterations: $MAX_ITER"
log "Project dir: $PROJECT_DIR"
log "Log file: $LOG_FILE"
log "Launching claude agent..."

cat "$PROMPT_FILE" | claude -p --allowedTools "Bash,Read,Edit,Write,Glob,Grep" \
  --model sonnet --verbose 2>&1 | tee -a "$LOG_FILE"

EXIT_CODE=${PIPESTATUS[1]}

log "=== test-loop finished (exit code: $EXIT_CODE) ==="
log "Full log saved to: $LOG_FILE"

exit $EXIT_CODE
