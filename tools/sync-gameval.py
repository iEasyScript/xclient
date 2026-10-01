"""
Brings the vendored RuneLite API game-value classes up to a given release.

These are ids: items, objects, interfaces, varbits, varps. RuneLite regenerates them
whenever the game changes, and we vendor runelite-api rather than depending on it,
so bumping project.build.version renames our copy without changing a single number
inside it. A client can therefore report 1.13.1 while addressing the interfaces of
the release before -- which is not a theoretical problem: 1.13.1 moved 520
components, and constants are inlined into every plugin jar at compile time.

Local changes are preserved rather than trampled. Ours are listed in KEEP, which is
deliberately explicit: a silent three-way merge of a generated file is how a fork
loses a fix nobody remembers making.

    python tools/sync-gameval.py 1.13.1
"""
import pathlib
import re
import sys
import urllib.request

API = pathlib.Path("runelite-api/src/main/java/net/runelite/api")

# Generated id classes worth tracking. gameval/ is the current set; the two at the
# end are the legacy classes RuneLite still regenerates alongside them.
FILES = [
    "gameval/InterfaceID.java",
    "gameval/VarbitID.java",
    "gameval/VarPlayerID.java",
    "gameval/VarClientID.java",
    "gameval/ItemID.java",
    "gameval/ObjectID.java",
    # The overflow half of ObjectID. There are more object ids than fit in one class's
    # constant pool, so RuneLite splits them and ObjectID extends this. Leaving it out
    # meant half the object ids in the fork were never updated by a bump, and nothing
    # said so -- RIVER_FISHING_SPOT lives in here, and the Moons of Peril script was
    # fishing at an id with no menu options on it.
    "gameval/ObjectID1.java",
    "gameval/NpcID.java",
    "gameval/AnimationID.java",
    "gameval/DBTableID.java",
    "gameval/InventoryID.java",
    "gameval/SpotanimID.java",
    "gameval/SpriteID.java",
    "ItemID.java",
    "NullItemID.java",
]

# Values this fork deliberately disagrees with upstream about, re-applied after the
# update, qualified by nested class: six different constants in InterfaceID are
# called INV, so an unqualified name rewrites whichever comes first and silently
# corrupts an unrelated interface.
#
# Empty, and that is the right answer today. The one override this fork carried set
# SeedVaultDeposit.INV to 0x0276_0001, which was simply the value from before the
# game gained a LOCKED_SLOT component ahead of it -- a stale copy rather than a
# correction. These files are generated from the live cache, so upstream is right
# about ids by construction, and keeping our number made RuneLite's own item-prices
# plugin read the wrong component.
KEEP: dict[str, dict[tuple[str, str], str]] = {}


def fetch(version: str, name: str) -> str | None:
    url = (
        f"https://raw.githubusercontent.com/runelite/runelite/runelite-parent-{version}"
        f"/runelite-api/src/main/java/net/runelite/api/{name}"
    )
    try:
        with urllib.request.urlopen(url, timeout=60) as r:
            return r.read().decode("utf-8")
    except Exception as e:
        print(f"  {name}: could not fetch ({e})")
        return None


def main() -> None:
    if len(sys.argv) < 2:
        raise SystemExit("usage: sync-gameval.py <runelite-version>")
    version = sys.argv[1]

    updated = unchanged = skipped = 0

    for name in FILES:
        target = API / name
        if not target.exists():
            print(f"  {name}: not vendored here, skipping")
            skipped += 1
            continue

        fresh = fetch(version, name)
        if fresh is None:
            skipped += 1
            continue

        for (owner, const), ours in KEEP.get(name, {}).items():
            # Find the constant inside its own class, not the first one that shares
            # its name.
            block = re.search(
                rf"class {re.escape(owner)}.*?(public static final int {re.escape(const)} = )([^;]+);",
                fresh,
                re.DOTALL,
            )
            if not block:
                print(f"  {name}: {owner}.{const} is gone upstream -- re-check this override")
                continue
            if block.group(2) != ours:
                fresh = fresh[: block.start(2)] + ours + fresh[block.end(2) :]
                print(f"  {name}: kept our {owner}.{const} = {ours} (upstream has {block.group(2)})")

        current = target.read_bytes()
        # Written with whatever line endings the tree already uses, so the diff is
        # the ids that moved rather than every line of a 40,000 line file.
        out = fresh.encode("utf-8")
        if b"\r\n" in current:
            out = out.replace(b"\r\n", b"\n").replace(b"\n", b"\r\n")

        if out == current:
            unchanged += 1
            continue

        target.write_bytes(out)
        updated += 1
        print(f"  {name}: updated")

    print(f"\n{updated} updated, {unchanged} already current, {skipped} skipped")


if __name__ == "__main__":
    main()
