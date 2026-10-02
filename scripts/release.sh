#!/usr/bin/env bash
#
# Release the client: bump, build, tag, publish on GitHub, announce on Discord.
#
# Usage, from anywhere in the client checkout:
#   scripts/release.sh 1.0.25 --notes-file notes.md            # a release
#   scripts/release.sh 1.0.25 --notes-file notes.md --api-bump # the plugin API changed too
#   scripts/release.sh 1.0.25 --notes-file notes.md --dry-run  # checks only, changes nothing
#
# Options:
#   --notes-file <file>  What changed, posted to the Discord updates channel. Required
#                        unless --no-announce.
#   --api-bump           Also bump projectx.pluginapi.version by one patch level. Needed
#                        whenever a public method plugins can call was added (a new Rs2*
#                        method counts), so plugins can gate on minClientVersion.
#   --no-announce        Skip the Discord post.
#   --dry-run            Run every check, print the plan, change nothing.
#
# Every step was once done by hand, and each check below exists because skipping it went
# wrong at least once: tags pushed from commits that only existed locally, a branch ten
# commits behind its releases, an asset name the launcher could not find, and
# announcements that silently posted nothing.
#
# Order matters. The GitHub release is what launchers act on -- they read releases/latest --
# so everything that can fail cheaply (checks, build, jar verification) happens before it,
# and the announcement only after it is confirmed live. A tag on its own changes nothing,
# so a run that dies between the tag and the release leaves players on the old version.
#
set -euo pipefail

REPO=iEasyScript/xclient
BRANCH=publish
SERVER=${XCLIENT_SERVER:-root@49.13.81.65}
SSH_KEY=${XCLIENT_SSH_KEY:-$HOME/.ssh/xclient_hetzner}
APP_DIR=/srv/xclient-web

log() { printf '\n\033[1;33m==> %s\033[0m\n' "$*"; }
die() { printf '\033[1;31merror:\033[0m %s\n' "$*" >&2; exit 1; }

version=""
notes_file=""
api_bump=false
announce=true
dry_run=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --notes-file) notes_file=${2:-}; shift 2 ;;
        --api-bump) api_bump=true; shift ;;
        --no-announce) announce=false; shift ;;
        --dry-run) dry_run=true; shift ;;
        -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
        -*) die "unknown option $1" ;;
        *) [[ -z "$version" ]] || die "one version only"; version=$1; shift ;;
    esac
done

[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "give the version as X.Y.Z, without a v -- the launcher builds URLs from it"
if $announce; then
    [[ -n "$notes_file" ]] || die "--notes-file is required (or --no-announce)"
    [[ -s "$notes_file" ]] || die "$notes_file is missing or empty"
    notes_file=$(cd "$(dirname "$notes_file")" && pwd)/$(basename "$notes_file")
fi

cd "$(git rev-parse --show-toplevel)"
[[ -f gradle.properties && -d runelite-client ]] || die "run this from the client checkout"

current=$(sed -n 's/^projectx\.version=//p' gradle.properties)
api_current=$(sed -n 's/^projectx\.pluginapi\.version=//p' gradle.properties)
api_next=$api_current
if $api_bump; then
    api_next=$(awk -F. -v OFS=. '{ $NF = $NF + 1; print }' <<<"$api_current")
fi

# ---------------------------------------------------------------- checks

log "Checking"

[[ "$(git branch --show-current)" == "$BRANCH" ]] || die "releases come from $BRANCH; you are on $(git branch --show-current)"

# IDE run configurations are not part of the build, and one is routinely left modified.
dirty=$(git status --porcelain --untracked-files=no -- . ':!.run')
[[ -z "$dirty" ]] || die "uncommitted changes would be built into the jar but not into the tag:
$dirty"

git fetch -q origin "$BRANCH" --tags
behind=$(git rev-list --count "HEAD..origin/$BRANCH")
[[ "$behind" -eq 0 ]] || die "$BRANCH is $behind commit(s) behind origin/$BRANCH -- pull first"

git rev-parse -q --verify "refs/tags/$version" >/dev/null && die "tag $version already exists"
gh release view "$version" --repo "$REPO" >/dev/null 2>&1 && die "a $version release already exists on GitHub"

latest=$(gh api "repos/$REPO/releases/latest" -q .tag_name)
newest=$(printf '%s\n%s\n' "$latest" "$version" | sort -V | tail -1)
[[ "$newest" == "$version" && "$latest" != "$version" ]] || die "$version is not newer than the live $latest"

if [[ "$current" != "$version" && "$current" != "$latest" ]]; then
    die "gradle.properties says $current, which is neither the live $latest nor $version"
fi

if $announce; then
    ssh -i "$SSH_KEY" -o BatchMode=yes -o ConnectTimeout=10 "$SERVER" true \
        || die "cannot reach $SERVER to announce -- fix that, or pass --no-announce"
fi

echo "  live release      $latest"
echo "  this release      $version"
echo "  client version    $current -> $version"
echo "  plugin API        $api_current -> $api_next"
echo "  announce          $announce${notes_file:+ ($notes_file)}"

if $dry_run; then
    log "Dry run: every check passed, nothing changed"
    exit 0
fi

# ---------------------------------------------------------------- bump

if [[ "$current" != "$version" || "$api_next" != "$api_current" ]]; then
    log "Bumping to $version"
    sed -i "s/^projectx\.version=.*/projectx.version=$version/" gradle.properties
    sed -i "s/^projectx\.pluginapi\.version=.*/projectx.pluginapi.version=$api_next/" gradle.properties
    git add gradle.properties
    git commit -q -m "Release $version"
else
    echo "gradle.properties already at $version, nothing to bump"
fi

# ---------------------------------------------------------------- build

log "Building"
./gradlew :client:assemble -q

jar=runelite-client/build/libs/projectx-$version.jar
[[ -f "$jar" ]] || die "$jar was not built -- the launcher fetches exactly that name"

# The jar must say the version it is published as, or the client reports the wrong one.
embedded=$(unzip -p "$jar" net/runelite/client/runelite.properties | tr -d '\r' | sed -n 's/^projectx\.version=//p')
[[ "$embedded" == "$version" ]] || die "$jar reports version '$embedded', not $version"
size=$(wc -c <"$jar" | tr -d ' ')
echo "  $jar, $size bytes"

# ---------------------------------------------------------------- publish

log "Publishing"
# The branch goes first, so the tag never points at a commit only this machine has.
git push -q origin "$BRANCH"
git tag "$version"
git push -q origin "$version"
gh release create "$version" --repo "$REPO" --title "Project X $version" --generate-notes "$jar"

live=$(gh api "repos/$REPO/releases/latest" -q .tag_name)
asset=$(gh api "repos/$REPO/releases/latest" -q ".assets[] | select(.name == \"projectx-$version.jar\") | .size")
[[ "$live" == "$version" ]] || die "GitHub's latest release is $live, not $version -- launchers will not pick this up"
[[ "$asset" == "$size" ]] || die "the uploaded asset is ${asset:-missing}, not $size bytes"
echo "  live: https://github.com/$REPO/releases/tag/$version"

# ---------------------------------------------------------------- announce

if $announce; then
    log "Announcing"
    # .env is sourced inside the sudo, as the user that owns it: the server's sudoers has
    # env_reset, so anything sourced as root is gone by the time node starts, and
    # announce.js treats a missing webhook as "post nothing" rather than an error.
    remote_notes=/tmp/release-$version-notes.md
    ssh -i "$SSH_KEY" -o BatchMode=yes "$SERVER" "cat > $remote_notes && chown xclient $remote_notes" <"$notes_file"
    result=$(ssh -i "$SSH_KEY" -o BatchMode=yes "$SERVER" \
        "cd $APP_DIR && sudo -u xclient -H bash -c 'set -a && . ./.env && set +a && node scripts/announce.js --client $version --notes-file $remote_notes'; rm -f $remote_notes" 2>&1)
    echo "  $result"
    [[ "$result" == *"posted"* ]] || die "the release is live but the announcement did not post -- rerun announce.js by hand"
fi

log "Released $version"
