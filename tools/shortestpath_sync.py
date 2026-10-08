#!/usr/bin/env python3
"""
Imports transport data from Skretzo/shortest-path (BSD-2) into the ProjectX walker's TSVs.

Our walker descends from Skretzo/shortest-path but its TSV format diverged, so upstream rows
cannot be copied verbatim. This tool reads an upstream TSV, converts every row to OUR format
(the format parsed by shortestpath/Transport.java), and either prints the result or merges
the rows we are missing into our file.

Format differences handled here (upstream -> ours):

  menuOption menuTarget objectID   "Climb-up Ladder 1234"  ->  "Climb-up;Ladder;1234"
                                   The option/target split is taken from our own data for the
                                   same object id when we have it, else from the longest option
                                   we already use, else the first word (logged as "heuristic").
  Items                            upstream tokens ("AXE=1|UNLOCK_CANOE_AXE=1", "COINS=2500",
                                   "13121=1||13122=1", "AIR_RUNE=3&&LAW_RUNE=1") ->
                                   "Item IDs" (numeric ids, SPACE separated = "any one of") and
                                   "Currency" ("2500 Coins").
  VarPlayers                       -> Varplayers
  Wilderness level                 upstream 20/30 -> our 19/29 (house convention: the pathfinder
                                   buckets wilderness as 0/20/30/31 and gates on level <= max).

Item requirement semantics in OUR code (verified in PathfinderConfig.hasRequiredItems and
Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities):
  * the pathfinder gate flattens every id of every group and accepts ANY one of them;
  * the banking planner withdraws the best-stocked id of ONE group only.
So the only requirement shape we enforce correctly is a single "any one of" group, written as
space-separated ids. ";" between ids does NOT mean "all of" anywhere in our code -- it splits
the ids into separate groups, which makes the banking planner withdraw an arbitrary one of them
(possibly one the player does not own). This tool therefore never emits ";" in Item IDs, and
it SKIPS (never weakens) any upstream row whose requirement we cannot represent:
  * a true AND of two items (CROSSBOW=1&MITH_GRAPPLE=1)        -> skipped
  * a "must not carry" requirement (HEADSLOT=0)                -> skipped
  * an unlock gate (13393=1&UNLOCK_XERICS_HONOUR=1)            -> skipped
  * an item quantity above 1 outside spells/currency            -> skipped
  * an unknown item token, quest name or skill name             -> skipped
  * a value in a column our parser would silently ignore        -> skipped
An unlock that only RELIEVES a requirement (AXE=1|UNLOCK_CANOE_AXE=1) is dropped, which makes
the row stricter, never looser. "ITEM=1|COINS=5" is emitted as two rows (one item row, one
currency row) -- our convention for the Shantay pass.
Spell rune costs are dropped because our walker checks runes through Rs2Spells by display
name; a spell whose display name does not resolve through Rs2Magic.getRs2Spell is skipped.
Quest names and skill names are checked against our runelite-api Quest/Skill enums (the parser
drops unknown ones silently), and item tokens are resolved through upstream's ItemVariations.java
against our vendored gameval ItemID.

How "already have it" is decided (see FileSync):
  * rows with an origin: the exact origin->destination pair. An upstream row for the same object
    whose ends moved by at most --max-shift tiles (default 3) is applied as a TILE CORRECTION to
    our row, but only when the pairing is one-to-one; otherwise it is added as a new row.
  * teleports (no origin): same key (a shared item id / the spell name / the minigame name).
    Exact landing = present; a unique <= 3-tile shift = correction; a landing <= 12 tiles from
    one of ours = an upstream variant we do not import; anything further = new.
  * ships and charter ships: our rows land on the ship deck (plane 1) while upstream lands on the
    dock, so routes are matched port-to-port within 12 tiles and new rows to a port we already
    serve reuse OUR arrival tile. No tile corrections are made here.
  * cross-file: upstream moves rows between files (transports.tsv -> agility_shortcuts.tsv with an
    Agility level). If the exact route exists in another of our files, the row is not imported,
    unless it is the same object and our copy is no stricter -- then our copy is relocated
    (deleted there, imported here with upstream's requirements).
  * rows starting or ending at upstream's POH tile (1858 7051 0) are skipped: our POH is modelled
    in code (PohPanel, PohPortal, Mounted* items).
  * imported rows default isMembers to "Y" in the teleport item/spell/portal files (all upstream
    additions there are members content; over-gating is safe).

Usage (from the repository root):

    # 1. download upstream's current data + ItemVariations.java (needs the GitHub CLI)
    python tools/shortestpath_sync.py fetch --dest /tmp/sp-up

    # 2. see what would change for one file (new routes, tile corrections, skipped rows)
    python tools/shortestpath_sync.py diff --upstream /tmp/sp-up agility_shortcuts.tsv

    # 3. merge it: appends new rows under an "Imported from Skretzo/shortest-path" section,
    #    rewrites corrected tiles in place, prints a report
    python tools/shortestpath_sync.py apply --upstream /tmp/sp-up agility_shortcuts.tsv

    # just print the converted rows of an upstream file
    python tools/shortestpath_sync.py convert --upstream /tmp/sp-up canoes.tsv

Run each file's "diff" first and read the NEW/FIX/SKIP lines: new upstream data can need a
fresh exclusion, an EXTRA_OPTIONS entry for a multi-word menu option (NOTE "heuristic option
split"), or a hand edit. After applying, run TransportDataIntegrityTest and the walker suite:
    ./gradlew :client:runUnitTests --tests 'net.runelite.client.plugins.projectx.shortestpath.*'

Rows we deliberately do not want are listed in tools/shortestpath_sync_exclusions.tsv (with a
reason); they are never imported. seasonal_transports.tsv (Leagues/Deadman),
teleportation_boxes.tsv and teleportation_portals_poh.tsv (handled in code: Mounted* items and
the PohPortal enum) are not synced.
"""
import argparse
import pathlib
import re
import subprocess
import sys
from collections import OrderedDict, defaultdict

REPO = pathlib.Path(__file__).resolve().parent.parent
DATA_DIR = REPO / "runelite-client/src/main/resources/net/runelite/client/plugins/projectx/shortestpath"
GAMEVAL_ITEM_ID = REPO / "runelite-api/src/main/java/net/runelite/api/gameval/ItemID.java"
QUEST_JAVA = REPO / "runelite-api/src/main/java/net/runelite/api/Quest.java"
SKILL_JAVA = REPO / "runelite-api/src/main/java/net/runelite/api/Skill.java"
RS2_SPELLS_JAVA = REPO / "runelite-client/src/main/java/net/runelite/client/plugins/projectx/util/magic/Rs2Spells.java"
MAGIC_ACTION_JAVA = REPO / "runelite-client/src/main/java/net/runelite/client/plugins/skillcalculator/skills/MagicAction.java"
EXCLUSIONS = pathlib.Path(__file__).resolve().parent / "shortestpath_sync_exclusions.tsv"

UPSTREAM_REPO = "Skretzo/shortest-path"
UPSTREAM_TRANSPORTS = "src/main/resources/transports"
UPSTREAM_ITEM_VARIATIONS = "src/main/java/shortestpath/ItemVariations.java"
ITEM_VARIATIONS_FILE = "ItemVariations.java"

# upstream file -> our file. Files mapped to None are deliberately not synced.
FILE_MAP = {
    "agility_shortcuts.tsv": "agility_shortcuts.tsv",
    "boats.tsv": "boats.tsv",
    "canoes.tsv": "canoes.tsv",
    "charter_ships.tsv": "charter_ships.tsv",
    "fairy_rings.tsv": "fairy_rings.tsv",
    "gnome_gliders.tsv": "gnome_gliders.tsv",
    "hot_air_balloons.tsv": "hot_air_balloons.tsv",
    "magic_carpets.tsv": "magic_carpets.tsv",
    "magic_mushtrees.tsv": "magic_mushtrees.tsv",
    "minecarts.tsv": "minecarts.tsv",
    "quetzals.tsv": "quetzals.tsv",
    "quetzal_whistle.tsv": "teleportation_items.tsv",
    "ships.tsv": "ships.tsv",
    "spirit_trees.tsv": "spirit_trees.tsv",
    "teleportation_items.tsv": "teleportation_items.tsv",
    "teleportation_levers.tsv": "teleportation_levers.tsv",
    "teleportation_minigames.tsv": "teleportation_minigames.tsv",
    "teleportation_portals.tsv": "teleportation_portals.tsv",
    "teleportation_spells.tsv": "teleportation_spells.tsv",
    "teleportation_spells_home.tsv": "teleportation_spells.tsv",
    "transports.tsv": "transports.tsv",
    "wilderness_obelisks.tsv": "wilderness_obelisks.tsv",
    "seasonal_transports.tsv": None,
    "teleportation_boxes.tsv": None,
    "teleportation_portals_poh.tsv": None,
}

# Column names exactly as Transport.java reads them.
COL_ORIGIN = "Origin"
COL_DEST = "Destination"
COL_OBJECT = "menuOption menuTarget objectID"
COL_SKILLS = "Skills"
COL_ITEMS = "Item IDs"
COL_CURRENCY = "Currency"
COL_QUESTS = "Quests"
COL_VARBITS = "Varbits"
COL_VARPLAYERS = "Varplayers"
COL_DURATION = "Duration"
COL_DISPLAY = "Display info"
COL_CONSUMABLE = "Consumable"
COL_WILDERNESS = "Wilderness level"
COL_MEMBERS = "isMembers"
PARSED_COLUMNS = {COL_ORIGIN, COL_DEST, COL_OBJECT, COL_SKILLS, COL_ITEMS, COL_CURRENCY, COL_QUESTS,
                  COL_VARBITS, COL_VARPLAYERS, COL_DURATION, COL_DISPLAY, COL_CONSUMABLE,
                  COL_WILDERNESS, COL_MEMBERS}
# Columns in our files that the parser ignores but that carry only cosmetic data.
COSMETIC_ALIASES = {"Display Info": COL_DISPLAY, "Info": COL_DISPLAY}

# upstream column -> our column
UPSTREAM_COLUMNS = {
    "Origin": COL_ORIGIN,
    "Destination": COL_DEST,
    "menuOption menuTarget objectID": COL_OBJECT,
    "Skills": COL_SKILLS,
    "Items": "Items",  # converted specially
    "Quests": COL_QUESTS,
    "Varbits": COL_VARBITS,
    "VarPlayers": COL_VARPLAYERS,
    "Duration": COL_DURATION,
    "Display info": COL_DISPLAY,
    "Consumable": COL_CONSUMABLE,
    "Wilderness level": COL_WILDERNESS,
}

CURRENCIES = {"COINS": "Coins", "ECTO_TOKEN": "Ecto-token"}
# Upstream's POH anchor. Our POH is modelled in code (PohPanel / PohPortal / Mounted* items), so
# rows that start or end inside upstream's POH instance are never imported.
POH_TILE = "1858 7051 0"
# Multi-word menu options that do not occur in our data yet (the split is otherwise ambiguous).
EXTRA_OPTIONS = ["Leave Tomb", "Walk through", "Jump off", "The Pandemonium", "Gloomthorn Trail"]
# Options that name an action rather than a destination (ships: the option is usually the port).
GENERIC_OPTIONS = {"Travel", "Talk-to", "Take-boat", "Quick-Travel", "Board", "Charter"}
# Teleports: an upstream row is "already present" when one of ours has the same key (shared item
# id / spell / minigame) and lands within this many tiles. Only a unique pairing within
# --max-shift tiles is applied as a tile correction.
TELEPORT_PRESENCE_RADIUS = 12
# Ships and charter ships: our rows land on the ship deck (plane 1) at each port while upstream
# lands on the dock, so routes are matched port-to-port within this radius and new rows to ports
# we already serve reuse OUR arrival tile for that port.
PORT_RADIUS = 12
PORT_MODEL_FILES = {"ships.tsv", "charter_ships.tsv"}
# Our files a cross-file duplicate may be relocated out of (see FileSync.resolve_cross_file).
RELOCATABLE_FILES = {"transports.tsv", "agility_shortcuts.tsv", "boats.tsv", "canoes.tsv", "charter_ships.tsv",
                     "ships.tsv", "minecarts.tsv", "teleportation_portals.tsv", "magic_carpets.tsv", "npcs.tsv"}
# Upstream has no isMembers column. Every row it adds to these files is members-only content, and
# over-gating is safe (a free world simply cannot use the row), so imported rows default to "Y"
# here. transports.tsv keeps the previous import's convention of leaving the cell empty.
MEMBERS_DEFAULT_FILES = {"teleportation_items.tsv", "teleportation_spells.tsv", "teleportation_portals.tsv"}
VAR_TOKEN = re.compile(r"^\d+(=|>|<|&|@)\d+$")
COORD = re.compile(r"^-?\d+ -?\d+ -?\d+$")


# --------------------------------------------------------------------------------------------
# Reference data from the repository
# --------------------------------------------------------------------------------------------

def strip_java_comments(text):
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def load_gameval_item_ids():
    """Top-level constants of gameval/ItemID.java (nested Cert/Placeholder classes are skipped)."""
    ids = {}
    depth = 0
    const = re.compile(r"public static final int (\w+)\s*=\s*(-?\d+)\s*;")
    for line in strip_java_comments(GAMEVAL_ITEM_ID.read_text(encoding="utf-8")).splitlines():
        if depth == 1:
            m = const.search(line)
            if m:
                ids[m.group(1)] = int(m.group(2))
        depth += line.count("{") - line.count("}")
    return ids


def load_quest_names():
    text = QUEST_JAVA.read_text(encoding="utf-8")
    return {m.group(1).lower(): m.group(1) for m in re.finditer(r'^\s*\w+\(\d+,\s*"([^"]+)"', text, re.M)}


def load_skill_names():
    text = SKILL_JAVA.read_text(encoding="utf-8")
    return {m.group(1) for m in re.finditer(r'^\s*\w+\("([^"]+)"', text, re.M)}


def load_spell_names():
    """Display names our walker can cast: Rs2Spells constants -> MagicAction display names."""
    actions = {}
    for m in re.finditer(r'^\s*(\w+)\("([^"]+)"', MAGIC_ACTION_JAVA.read_text(encoding="utf-8"), re.M):
        actions[m.group(1)] = m.group(2)
    names = []
    for m in re.finditer(r"^\s*\w+\(MagicAction\.(\w+)", RS2_SPELLS_JAVA.read_text(encoding="utf-8"), re.M):
        if m.group(1) in actions:
            names.append(actions[m.group(1)])
    return names


def spell_resolves(display_info, spell_names):
    """Mirror of PathfinderConfig.isTeleportationSpellUsable + Rs2Magic.getRs2Spell."""
    if ":" in display_info:
        key = display_info.split(":")[0].strip().lower()
    else:
        key = display_info.lower()
    return any(key in name.lower() for name in spell_names)


def load_item_variations(path, item_ids):
    """Parses upstream ItemVariations.java into NAME -> [numeric ids]."""
    text = strip_java_comments(pathlib.Path(path).read_text(encoding="utf-8"))
    body = text[text.index("enum ItemVariations"):]
    body = body[body.index("{") + 1:]
    body = body[:re.search(r"^\s*;", body, re.M).start()]
    variations = {}
    for m in re.finditer(r"([A-Z][A-Z0-9_]*)\s*\(([^()]*)\)", body, re.S):
        names = re.findall(r"ItemID\.(\w+)", m.group(2))
        resolved = []
        for n in names:
            if n not in item_ids:
                raise SystemExit("ItemVariations.%s references ItemID.%s which is not in our gameval ItemID"
                                 % (m.group(1), n))
            resolved.append(item_ids[n])
        variations[m.group(1)] = resolved
    return variations


# --------------------------------------------------------------------------------------------
# TSV model
# --------------------------------------------------------------------------------------------

def parse_header(line):
    if line.startswith("# "):
        line = line.replace("# ", "#", 1)
    if line.startswith("#"):
        line = line.replace("#", "", 1)
    return line.split("\t")


class Tsv:
    def __init__(self, path):
        self.path = pathlib.Path(path)
        raw = self.path.read_bytes().decode("utf-8")
        self.crlf = "\r\n" in raw
        self.trailing_newline = raw.endswith("\n")
        self.lines = raw.split("\n")
        if self.trailing_newline:
            self.lines.pop()
        self.lines = [l[:-1] if l.endswith("\r") else l for l in self.lines]
        self.header_line = self.lines[0]
        self.columns = parse_header(self.header_line)
        self.header_has_hash = self.header_line.startswith("#")

    def rows(self):
        """Yields (line_index, section_comment, field dict) for every data row."""
        section = ""
        for i, line in enumerate(self.lines[1:], start=1):
            if line.startswith("#"):
                section = line
                continue
            if not line.strip():
                continue
            fields = line.split("\t")
            yield i, section, {c: (fields[k] if k < len(fields) else "") for k, c in enumerate(self.columns)}

    def write(self):
        nl = "\r\n" if self.crlf else "\n"
        text = nl.join(self.lines) + (nl if self.trailing_newline else "")
        self.path.write_bytes(text.encode("utf-8"))


def format_row(columns, values):
    cells = [values.get(c, "") for c in columns]
    return "\t".join(cells)


# --------------------------------------------------------------------------------------------
# Conversion
# --------------------------------------------------------------------------------------------

class Skip(Exception):
    pass


class Converter:
    def __init__(self, upstream_dir):
        self.item_ids = load_gameval_item_ids()
        self.quests = load_quest_names()
        self.skills = load_skill_names()
        self.spells = load_spell_names()
        iv = pathlib.Path(upstream_dir) / ITEM_VARIATIONS_FILE
        if not iv.exists():
            raise SystemExit("%s not found -- run the fetch command first" % iv)
        self.variations = load_item_variations(iv, self.item_ids)
        self.known_objects = {}   # object id -> (option, target) from our data
        self.known_options = set()
        for f in sorted(DATA_DIR.glob("*.tsv")):
            tsv = Tsv(f)
            if COL_OBJECT not in tsv.columns:
                continue
            for _, _, row in tsv.rows():
                m = re.match(r"^([^;]+);([^;]+);(\d+)$", row.get(COL_OBJECT, "").strip())
                if m:
                    self.known_objects.setdefault(int(m.group(3)), (m.group(1).strip(), m.group(2).strip()))
                    self.known_options.add(m.group(1).strip())
        self.known_options.update(EXTRA_OPTIONS)

    # ---- menuOption menuTarget objectID ----
    def convert_object(self, text, notes):
        text = " ".join(text.split())
        if not text:
            return ""
        m = re.match(r"^(.*\S)\s+(\d+)$", text)
        if not m:
            raise Skip("object cell has no object id: '%s'" % text)
        words, obj = m.group(1), int(m.group(2))
        known = self.known_objects.get(obj)
        if known and ("%s %s" % known) == words:
            return "%s;%s;%d" % (known[0], known[1], obj)
        best = None
        for opt in self.known_options:
            if words.startswith(opt + " ") and (best is None or len(opt) > len(best)):
                best = opt
        if best is None:
            best = words.split(" ")[0]
            notes.append("heuristic option split '%s' -> '%s'" % (words, best))
        target = words[len(best):].strip()
        if not target:
            raise Skip("object cell has no target: '%s'" % text)
        if ";" in words:
            raise Skip("object cell contains ';': '%s'" % text)
        return "%s;%s;%d" % (best, target, obj)

    # ---- Items ----
    def convert_items(self, text, is_spell, notes):
        """Returns a list of (item_ids_cell, currency_cell) alternatives (one output row each)."""
        norm = text.replace(" ", "").replace("&&", "&").replace("||", "|").upper()
        if not norm:
            return [("", "")]
        groups = []
        for part in norm.split("&"):
            branches = []
            for alt in part.split("|"):
                if alt.count("=") != 1:
                    raise Skip("malformed item token '%s'" % alt)
                name, qty = alt.split("=")
                try:
                    qty = int(qty)
                except ValueError:
                    raise Skip("malformed item quantity '%s'" % alt)
                branches.append((name, qty))
            groups.append(branches)

        if is_spell:
            for branches in groups:
                for name, _ in branches:
                    if not name.endswith("_RUNE"):
                        raise Skip("spell needs a non-rune item %s (not checked by Rs2Spells)" % name)
            return [("", "")]

        item_groups = []
        currency = None
        for branches in groups:
            if any(q == 0 for _, q in branches):
                raise Skip("'must not carry' item requirement (%s) is not expressible" % text)
            unlocks = [n for n, _ in branches if n.startswith("UNLOCK_")]
            rest = [(n, q) for n, q in branches if not n.startswith("UNLOCK_")]
            if unlocks and not rest:
                raise Skip("unlock gate %s is not expressible" % "|".join(unlocks))
            if unlocks:
                notes.append("dropped relief alternative %s (row is stricter)" % "|".join(unlocks))
            currencies = [(n, q) for n, q in rest if n in CURRENCIES]
            items = [(n, q) for n, q in rest if n not in CURRENCIES]
            if currencies and len(currencies) > 1:
                raise Skip("several alternative currencies %s" % text)
            if currencies and not items:
                if currency is not None:
                    raise Skip("two currency requirements %s" % text)
                currency = currencies[0]
                continue
            ids = []
            for name, qty in items:
                if qty > 1:
                    raise Skip("item quantity %s=%d is not expressible" % (name, qty))
                if name in self.variations:
                    ids.extend(self.variations[name])
                elif name.isdigit():
                    ids.append(int(name))
                else:
                    raise Skip("unknown item token '%s'" % name)
            item_groups.append((sorted(set(ids), key=ids.index), currencies[0] if currencies else None))

        if len(item_groups) > 1:
            raise Skip("AND of several items (%s) is not enforced by our item gate" % text)
        cur_cell = "%d %s" % (currency[1], CURRENCIES[currency[0]]) if currency else ""
        if not item_groups:
            return [("", cur_cell)]
        ids, alt_currency = item_groups[0]
        out = [(" ".join(str(i) for i in ids), cur_cell)]
        if alt_currency is not None:
            if currency is not None:
                raise Skip("item-or-currency combined with another currency %s" % text)
            out.append(("", "%d %s" % (alt_currency[1], CURRENCIES[alt_currency[0]])))
        return out

    # ---- other fields ----
    def convert_skills(self, text):
        text = text.strip()
        if not text:
            return ""
        out = []
        for req in text.split(";"):
            if not req:
                continue
            parts = req.split(" ")
            if len(parts) != 2 or not parts[0].isdigit():
                raise Skip("unsupported skill requirement '%s'" % req)
            if parts[1] not in self.skills:
                raise Skip("unsupported skill '%s'" % parts[1])
            out.append(req)
        return ";".join(out)

    def convert_quests(self, text):
        text = text.strip()
        if not text:
            return ""
        out = []
        for q in text.split(";"):
            q = q.strip()
            if not q:
                continue
            name = q.split("=")[0].strip()
            if name.lower() not in self.quests:
                raise Skip("unknown quest '%s'" % q)
            out.append(q)
        return ";".join(out)

    @staticmethod
    def convert_vars(text, label):
        text = text.strip()
        if not text:
            return ""
        out = []
        for tok in text.split(";"):
            if not tok:
                continue
            if not VAR_TOKEN.match(tok):
                raise Skip("malformed %s token '%s'" % (label, tok))
            out.append(tok)
        return ";".join(out)

    def convert_row(self, up, our_file):
        """Converts one upstream row dict -> list of our-format row dicts (+ notes). Raises Skip."""
        notes = []
        out = {}
        is_spell = our_file == "teleportation_spells.tsv"
        for col, value in up.items():
            value = value.strip() if col != "Display info" else value.strip()
            if col in ("Region override",):
                if value:
                    raise Skip("region override '%s'" % value)
                continue
            if col not in UPSTREAM_COLUMNS:
                if value:
                    raise Skip("unknown upstream column '%s'='%s'" % (col, value))
                continue
            if col in ("Origin", "Destination"):
                if value and not COORD.match(" ".join(value.split())):
                    raise Skip("malformed coordinate '%s'" % value)
                out[UPSTREAM_COLUMNS[col]] = " ".join(value.split())
            elif col == "menuOption menuTarget objectID":
                out[COL_OBJECT] = self.convert_object(value, notes)
            elif col == "Skills":
                out[COL_SKILLS] = self.convert_skills(value)
            elif col == "Quests":
                out[COL_QUESTS] = self.convert_quests(value)
            elif col == "Varbits":
                out[COL_VARBITS] = self.convert_vars(value, "varbit")
            elif col == "VarPlayers":
                out[COL_VARPLAYERS] = self.convert_vars(value, "varplayer")
            elif col == "Duration":
                if value and not value.isdigit():
                    raise Skip("malformed duration '%s'" % value)
                out[COL_DURATION] = value
            elif col == "Wilderness level":
                if value and not value.isdigit():
                    raise Skip("malformed wilderness level '%s'" % value)
                out[COL_WILDERNESS] = str(int(value) - 1) if value and int(value) > 0 else value
            elif col == "Consumable":
                if value not in ("", "T", "F"):
                    raise Skip("malformed consumable flag '%s'" % value)
                out[COL_CONSUMABLE] = value
            elif col == "Display info":
                out[COL_DISPLAY] = value
            elif col == "Items":
                out["Items"] = value
        if POH_TILE in (out.get(COL_ORIGIN, ""), out.get(COL_DEST, "")):
            raise Skip("POH-anchored row (our POH is modelled in code)")
        display = out.get(COL_DISPLAY, "")
        if our_file == "magic_carpets.tsv":
            # our walker clicks the dialogue option by this text; upstream prefixes the option number
            out[COL_DISPLAY] = re.sub(r"^\d+:\s*", "", display)
        if our_file == "fairy_rings.tsv":
            out[COL_DISPLAY] = display.replace(" ", "")  # our codes are written "BJP", upstream "B J P"
        if our_file.startswith("teleportation_") and display.endswith(" (Outside)"):
            out[COL_DISPLAY] = display[:-len(" (Outside)")] + ": Outside"
        if is_spell:
            if not spell_resolves(out.get(COL_DISPLAY, ""), self.spells):
                raise Skip("spell '%s' is not castable by our walker (no Rs2Spells entry)" % out.get(COL_DISPLAY, ""))
        alternatives = self.convert_items(out.pop("Items", ""), is_spell, notes)
        rows = []
        for item_cell, currency_cell in alternatives:
            r = dict(out)
            r[COL_ITEMS] = item_cell
            r[COL_CURRENCY] = currency_cell
            rows.append(r)
        return rows, notes


# --------------------------------------------------------------------------------------------
# Fitting converted rows into one of our files
# --------------------------------------------------------------------------------------------

def our_column_for(columns, parsed_name):
    """The column of our file that carries `parsed_name`, honouring cosmetic aliases."""
    if parsed_name in columns:
        return parsed_name
    for alias, target in COSMETIC_ALIASES.items():
        if target == parsed_name and alias in columns:
            return alias
    return None


def fit_row(row, columns, our_file, added_columns, notes):
    """Maps a converted row onto our file's columns. Requirement values with no column are added
    as new trailing columns (recorded in added_columns); returns the row dict keyed by our columns."""
    fitted = {}
    for name, value in row.items():
        if not value:
            continue
        col = our_column_for(columns + added_columns, name)
        if col is None:
            if name == COL_DISPLAY:
                continue  # purely cosmetic
            if name == COL_OBJECT:
                # Files without an object column (fairy rings, quetzals, teleports) are handled
                # by type-specific walker code that never reads the object cell.
                if our_file.startswith("teleportation_"):
                    raise Skip("teleport with an object interaction '%s'" % value)
                notes.append("dropped object cell '%s' (file has no object column)" % value)
                continue
            added_columns.append(name)
            col = name
        fitted[col] = value
    if COL_OBJECT in columns and fitted.get(COL_ORIGIN) and fitted.get(COL_DEST) \
            and not fitted.get(COL_OBJECT):
        raise Skip("direct transport with no object/NPC to interact with")
    return fitted


def wp(text):
    p = text.split()
    return (int(p[0]), int(p[1]), int(p[2])) if len(p) == 3 else None


def cheb(a, b):
    """Same-plane Chebyshev distance (huge across planes)."""
    if a is None or b is None:
        return 0 if a == b else 10 ** 6
    if a[2] != b[2]:
        return 10 ** 6
    return max(abs(a[0] - b[0]), abs(a[1] - b[1]))


def cheb_xy(a, b):
    """Chebyshev distance ignoring the plane."""
    if a is None or b is None:
        return 0 if a == b else 10 ** 6
    return max(abs(a[0] - b[0]), abs(a[1] - b[1]))


def object_id(row):
    m = re.match(r"^([^;]+);([^;]+);(\d+)$", row.get(COL_OBJECT, "").strip())
    return (m.group(1), m.group(2), int(m.group(3))) if m else None


def item_set(row):
    return {int(t) for t in re.split(r"[ ;]", row.get(COL_ITEMS, "")) if t.strip().isdigit()}


def spell_key(row):
    return row.get(COL_DISPLAY, "").split(":")[0].strip().lower()


def minigame_key(row):
    d = row.get(COL_DISPLAY, "").replace(" Minigame Teleport", "")
    return re.sub(r":\s*\d+\.\s*", ": ", d).strip().lower()


def display_tail(row):
    return row.get(COL_DISPLAY, "").split(":")[-1].strip().lower()


def port_name(row, our_file):
    """Destination port of a ship/charter row: the charter menu text, else the ship's option."""
    if our_file == "charter_ships.tsv":
        return row.get(COL_DISPLAY, "").strip() or None
    oid = object_id(row)
    if oid and oid[0] not in GENERIC_OPTIONS:
        return oid[0]
    return row.get(COL_DISPLAY, "").strip() or None


def skill_levels(row):
    out = {}
    for req in row.get(COL_SKILLS, "").split(";"):
        parts = req.strip().split(" ")
        if len(parts) == 2 and parts[0].isdigit():
            out[parts[1]] = int(parts[0])
    return out


def split_set(row, col):
    return {t.strip() for t in row.get(col, "").split(";") if t.strip()}


def stricter(ours, upstream):
    """True when our row carries a requirement that the upstream row does not."""
    if item_set(ours) and item_set(ours) != item_set(upstream):
        return True
    if ours.get(COL_CURRENCY, "").strip() and ours.get(COL_CURRENCY, "").strip() != upstream.get(COL_CURRENCY, "").strip():
        return True
    for col in (COL_QUESTS, COL_VARBITS, COL_VARPLAYERS):
        if not split_set(ours, col) <= split_set(upstream, col):
            return True
    up = skill_levels(upstream)
    return any(level > up.get(skill, 0) for skill, level in skill_levels(ours).items())


def load_exclusions():
    """tools/shortestpath_sync_exclusions.tsv: upstream file, origin, destination, match, reason.
    origin/destination are a tile or '*'; match is '*', an object id, 'item:<id>',
    'display:<prefix>' or 'option:<menu option>'."""
    out = defaultdict(list)
    if not EXCLUSIONS.exists():
        return out
    for line in EXCLUSIONS.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        cells = line.split("\t")
        out[cells[0]].append((cells[1].strip(), cells[2].strip(), cells[3].strip(),
                              cells[4].strip() if len(cells) > 4 else ""))
    return out


def exclusion_reason(exclusions, up_file, row):
    for origin, dest, match, reason in exclusions.get(up_file, []):
        if origin not in ("*", row.get(COL_ORIGIN, "")):
            continue
        if dest not in ("*", row.get(COL_DEST, "")):
            continue
        oid = object_id(row)
        if match == "*":
            pass
        elif match.startswith("item:"):
            if int(match[5:]) not in item_set(row):
                continue
        elif match.startswith("display:"):
            if not row.get(COL_DISPLAY, "").startswith(match[8:]):
                continue
        elif match.startswith("option:"):
            if not oid or oid[0] != match[7:]:
                continue
        elif not oid or str(oid[2]) != match:
            continue
        return reason or "excluded"
    return None


class FileSync:
    """Computes new rows and tile corrections for one upstream file against our file."""

    def __init__(self, conv, upstream_dir, up_file, max_shift=3):
        self.up_file = up_file
        self.our_file = FILE_MAP[up_file]
        if self.our_file is None:
            raise SystemExit("%s is deliberately not synced" % up_file)
        self.up = Tsv(pathlib.Path(upstream_dir) / up_file)
        self.ours = Tsv(DATA_DIR / self.our_file)
        self.conv = conv
        self.max_shift = max_shift
        self.has_origin = COL_ORIGIN in self.ours.columns
        self.port_model = self.our_file in PORT_MODEL_FILES
        self.added_columns = []
        self.skipped = []      # (upstream line, reason, raw line)
        self.excluded = []     # (upstream line, reason, raw line)
        self.notes = []        # (upstream line, note)
        self.converted = []    # (upstream line, section, row dict in our columns)
        self.ours_rows = [(i, r) for i, _, r in self.ours.rows()]
        port_tiles = defaultdict(set)
        if self.port_model:
            for _, r in self.ours_rows:
                name = port_name(r, self.our_file)
                if name and r.get(COL_DEST):
                    port_tiles[name].add(r[COL_DEST])
        exclusions = load_exclusions()
        for i, section, up_row in self.up.rows():
            raw = self.up.lines[i]
            try:
                rows, notes = conv.convert_row(up_row, self.our_file)
                fitted = [fit_row(r, self.ours.columns, self.our_file, self.added_columns, notes) for r in rows]
            except Skip as e:
                self.skipped.append((i + 1, str(e), raw))
                continue
            for r in fitted:
                reason = exclusion_reason(exclusions, up_file, r)
                if reason:
                    self.excluded.append((i + 1, reason, raw))
                    continue
                if self.port_model:
                    tiles = port_tiles.get(port_name(r, self.our_file) or "", set())
                    near = sorted((cheb_xy(wp(t), wp(r.get(COL_DEST, ""))), t) for t in tiles)
                    near = [t for dist, t in near if dist <= PORT_RADIUS]
                    if near and r.get(COL_DEST) not in tiles:
                        notes.append("destination %s -> our arrival tile %s" % (r.get(COL_DEST), near[0]))
                        r[COL_DEST] = near[0]
                self.converted.append((i + 1, section, r))
            for n in notes:
                self.notes.append((i + 1, n))

    # identity -------------------------------------------------------------------------
    def present_in(self, row):
        """Index of one of our rows representing the same route, or None."""
        o, d = wp(row.get(COL_ORIGIN, "")), wp(row.get(COL_DEST, ""))
        if self.port_model:
            for idx, r in self.ours_rows:
                if cheb_xy(o, wp(r.get(COL_ORIGIN, ""))) <= PORT_RADIUS \
                        and cheb_xy(d, wp(r.get(COL_DEST, ""))) <= PORT_RADIUS:
                    return idx
            return None
        if self.has_origin:
            for idx, r in self.ours_rows:
                if r.get(COL_ORIGIN, "") == row.get(COL_ORIGIN, "") and r.get(COL_DEST, "") == row.get(COL_DEST, ""):
                    if self._same_requirement_shape(row, r):
                        return idx
            return None
        for idx, r in self.ours_rows:
            if r.get(COL_DEST, "") == row.get(COL_DEST, "") and self._same_teleport(row, r):
                return idx
        return None

    def near_existing_teleport(self, row):
        # A teleport we already have, landing a few tiles away (an upstream variant or a tile we
        # could not pair unambiguously): not a new route.
        if self.has_origin:
            return False
        d = wp(row.get(COL_DEST, ""))
        return any(cheb_xy(d, wp(r.get(COL_DEST, ""))) <= TELEPORT_PRESENCE_RADIUS and self._same_teleport(row, r)
                   for _, r in self.ours_rows)

    def _same_teleport(self, a, b):
        if self.our_file == "teleportation_items.tsv":
            return bool(item_set(a) & item_set(b))
        if self.our_file == "teleportation_spells.tsv":
            return spell_key(a) == spell_key(b)
        if self.our_file == "teleportation_minigames.tsv":
            return minigame_key(a) == minigame_key(b)
        return True

    @staticmethod
    def _same_requirement_shape(a, b):
        # The Shantay-pass convention keeps an item row and a currency row for the same tiles.
        a_cur = bool(a.get(COL_CURRENCY)) and not a.get(COL_ITEMS)
        b_cur = bool(b.get(COL_CURRENCY)) and not b.get(COL_ITEMS)
        if a_cur != b_cur and (a.get(COL_ITEMS) or b.get(COL_ITEMS)):
            return False
        return True

    def correction_candidate(self, up_row, our_row):
        """Upstream row looks like the same transport as our row with tiles moved a little."""
        if self.port_model:
            return None
        if self.has_origin:
            ua, oa = object_id(up_row), object_id(our_row)
            if ua is None or oa is None:
                same = ua is None and oa is None \
                    and up_row.get(COL_DISPLAY, "") == our_row.get(COL_DISPLAY, "")
            else:
                same = ua[2] == oa[2] or (ua[0] == oa[0] and ua[1] == oa[1])
            if not same:
                return None
            do = cheb(wp(up_row.get(COL_ORIGIN, "")), wp(our_row.get(COL_ORIGIN, "")))
            dd = cheb(wp(up_row.get(COL_DEST, "")), wp(our_row.get(COL_DEST, "")))
            if do <= self.max_shift and dd <= self.max_shift and do + dd > 0:
                return do + dd
            return None
        a, b = wp(up_row.get(COL_DEST, "")), wp(our_row.get(COL_DEST, ""))
        if a is None or b is None or a == b or cheb_xy(a, b) > self.max_shift:
            return None
        if not self._same_teleport(up_row, our_row):
            return None
        ta, tb = display_tail(up_row), display_tail(our_row)
        if self.our_file == "teleportation_items.tsv" and not (ta in tb or tb in ta):
            return None
        return cheb_xy(a, b) + (1 if a[2] != b[2] else 0)

    def plan(self):
        missing = []
        matched_ours = set()
        for line, section, row in self.converted:
            idx = self.present_in(row)
            if idx is None:
                missing.append((line, section, row))
            else:
                matched_ours.add(idx)
        # Only our rows that no upstream row matches exactly are candidates for a tile correction.
        unmatched_ours = [(i, r) for i, r in self.ours_rows if i not in matched_ours]
        pairs = []
        for mi, (line, section, row) in enumerate(missing):
            for oi, orow in unmatched_ours:
                score = self.correction_candidate(row, orow)
                if score is not None:
                    pairs.append((score, mi, oi))
        # A correction must be unambiguous: one upstream row <-> one of our rows.
        m_count = defaultdict(int)
        o_count = defaultdict(int)
        for _, mi, oi in pairs:
            m_count[mi] += 1
            o_count[oi] += 1
        corrections, used_m = [], set()
        for score, mi, oi in sorted(pairs):
            if m_count[mi] == 1 and o_count[oi] == 1:
                used_m.add(mi)
                corrections.append((oi, missing[mi]))
        new_rows = [m for k, m in enumerate(missing) if k not in used_m]
        self.variants = [m for m in new_rows if self.near_existing_teleport(m[2])]
        new_rows = [m for m in new_rows if not self.near_existing_teleport(m[2])]
        # Upstream sometimes lists the same route twice (e.g. two option numbers for one carpet
        # destination); keep each distinct converted row once.
        seen, unique = set(), []
        for line, section, row in new_rows:
            key = tuple(sorted(row.items()))
            if key not in seen:
                seen.add(key)
                unique.append((line, section, row))
        return self.resolve_cross_file(unique), corrections

    # cross-file duplicates -------------------------------------------------------------
    def resolve_cross_file(self, new_rows):
        """Upstream moves rows between files (e.g. transports.tsv -> agility_shortcuts.tsv with an
        Agility level added). A new row whose exact origin->destination already exists in another
        of our transport files is not imported -- unless it is the same object and our copy is no
        stricter than upstream's, in which case our copy is relocated (deleted there, imported
        here with upstream's requirements)."""
        self.relocations = []      # (our other file, line index, our row, upstream line)
        self.elsewhere = []        # (upstream line, our other file, our line number)
        if not self.has_origin:
            return new_rows
        index = defaultdict(list)
        for f in sorted(DATA_DIR.glob("*.tsv")):
            if f.name == self.our_file or f.name not in RELOCATABLE_FILES:
                continue
            tsv = Tsv(f)
            if COL_ORIGIN not in tsv.columns:
                continue
            for i, _, r in tsv.rows():
                index[(r.get(COL_ORIGIN, ""), r.get(COL_DEST, ""))].append((f.name, i, r))
        kept = []
        for line, section, row in new_rows:
            hits = index.get((row.get(COL_ORIGIN, ""), row.get(COL_DEST, "")), [])
            if not hits:
                kept.append((line, section, row))
                continue
            movable = [h for h in hits if self._same_object(row, h[2]) and not stricter(h[2], row)]
            if len(movable) == len(hits):
                for f, i, r in movable:
                    self.relocations.append((f, i, r, line))
                kept.append((line, section, row))
            else:
                self.elsewhere.append((line, hits[0][0], hits[0][1] + 1))
        return kept

    @staticmethod
    def _same_object(a, b):
        oa, ob = object_id(a), object_id(b)
        return bool(oa and ob and oa[2] == ob[2])

    def apply_relocations(self):
        by_file = defaultdict(set)
        for f, i, _, _ in self.relocations:
            by_file[f].add(i)
        for f, lines in by_file.items():
            tsv = Tsv(DATA_DIR / f)
            tsv.lines = [l for k, l in enumerate(tsv.lines) if k not in lines]
            tsv.write()

    def apply(self, new_rows, corrections, section_title):
        if self.our_file in MEMBERS_DEFAULT_FILES and COL_MEMBERS in self.ours.columns:
            for _, _, row in new_rows:
                row.setdefault(COL_MEMBERS, "Y")
        used = [c for c in self.added_columns if any(row.get(c) for _, _, row in new_rows)]
        self.added_columns = used
        columns = self.ours.columns + used
        if self.added_columns:
            prefix = "# " if self.ours.header_line.startswith("# ") else ("#" if self.ours.header_has_hash else "")
            self.ours.lines[0] = prefix + "\t".join(columns)
        changed = []
        for oi, (line, section, row) in corrections:
            old = self.ours.lines[oi]
            cells = old.split("\t")
            for col in (COL_ORIGIN, COL_DEST):
                if col in self.ours.columns:
                    k = self.ours.columns.index(col)
                    while len(cells) <= k:
                        cells.append("")
                    cells[k] = row.get(col, "")
            self.ours.lines[oi] = "\t".join(cells)
            changed.append((old, self.ours.lines[oi], line))
        if new_rows:
            while self.ours.lines and not self.ours.lines[-1].strip():
                self.ours.lines.pop()
            self.ours.lines.append("")
            self.ours.lines.append("# " + section_title)
            last_section = None
            for line, section, row in new_rows:
                if section and section != last_section:
                    self.ours.lines.append(section.rstrip("\t"))
                    last_section = section
                self.ours.lines.append(format_row(columns, row).rstrip("\t"))
        self.ours.write()
        self.apply_relocations()
        return changed


# --------------------------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------------------------

def gh_raw(path):
    return subprocess.run(["gh", "api", "repos/%s/contents/%s" % (UPSTREAM_REPO, path),
                           "-H", "Accept: application/vnd.github.raw"],
                          check=True, capture_output=True).stdout


def cmd_fetch(args):
    dest = pathlib.Path(args.dest)
    dest.mkdir(parents=True, exist_ok=True)
    listing = subprocess.run(["gh", "api", "repos/%s/contents/%s" % (UPSTREAM_REPO, UPSTREAM_TRANSPORTS),
                              "--jq", ".[].name"], check=True, capture_output=True, text=True).stdout.split()
    for name in listing:
        if name.endswith(".tsv"):
            (dest / name).write_bytes(gh_raw("%s/%s" % (UPSTREAM_TRANSPORTS, name)))
            print("fetched", name)
    (dest / ITEM_VARIATIONS_FILE).write_bytes(gh_raw(UPSTREAM_ITEM_VARIATIONS))
    sha = subprocess.run(["gh", "api", "repos/%s/commits/master" % UPSTREAM_REPO, "--jq", ".sha"],
                         check=True, capture_output=True, text=True).stdout.strip()
    (dest / "UPSTREAM_SHA").write_text(sha + "\n")
    print("upstream master", sha)


def print_report(fs, new_rows, corrections, verbose):
    print("== %s -> %s" % (fs.up_file, fs.our_file))
    print("   upstream rows converted: %d, skipped: %d, excluded: %d" % (len(fs.converted), len(fs.skipped), len(fs.excluded)))
    ex = defaultdict(int)
    for _, reason, _ in fs.excluded:
        ex[reason] += 1
    for r, n in sorted(ex.items(), key=lambda x: -x[1]):
        print("   excluded x%d: %s" % (n, r))
    print("   new rows: %d, tile corrections: %d, added columns: %s" % (len(new_rows), len(corrections),
          [c for c in fs.added_columns if any(r.get(c) for _, _, r in new_rows)] or "-"))
    if getattr(fs, "relocations", None):
        print("   relocated: %d of our rows move here from another file (upstream is at least as strict)"
              % len(fs.relocations))
    if getattr(fs, "elsewhere", None):
        print("   not imported: %d rows already present in another of our files with stricter requirements"
              % len(fs.elsewhere))
    if getattr(fs, "variants", None):
        print("   not imported: %d upstream variants of teleports we already have (same key, lands <= %d tiles away)"
              % (len(fs.variants), TELEPORT_PRESENCE_RADIUS))
    reasons = defaultdict(int)
    for _, reason, _ in fs.skipped:
        reasons[re.sub(r"'[^']*'|\([^)]*\)", "...", reason)] += 1
    for r, n in sorted(reasons.items(), key=lambda x: -x[1]):
        print("   skip x%d: %s" % (n, r))
    if verbose:
        for line, reason, raw in fs.skipped:
            print("   SKIP L%d %s | %s" % (line, reason, raw))
        for line, note in fs.notes:
            print("   NOTE L%d %s" % (line, note))
        for oi, (line, _, row) in corrections:
            print("   FIX  ours L%d <- upstream L%d: %s" % (oi + 1, line, fs.ours.lines[oi]))
            print("                      new: %s -> %s" % (row.get(COL_ORIGIN, ""), row.get(COL_DEST, "")))
        for f, i, r, line in getattr(fs, "relocations", []):
            print("   MOVE %s L%d -> upstream L%d" % (f, i + 1, line))
        for line, f, ln in getattr(fs, "elsewhere", []):
            print("   ELSEWHERE upstream L%d already in %s L%d" % (line, f, ln))
        for line, _, row in new_rows:
            print("   NEW  L%d %s" % (line, format_row(fs.ours.columns + fs.added_columns, row)))


def cmd_convert(args):
    conv = Converter(args.upstream)
    fs = FileSync(conv, args.upstream, args.file)
    cols = fs.ours.columns + fs.added_columns
    print("# " + "\t".join(cols))
    for _, _, row in fs.converted:
        print(format_row(cols, row))
    for line, reason, raw in fs.skipped:
        print("SKIP L%d %s | %s" % (line, reason, raw), file=sys.stderr)


def cmd_diff(args):
    conv = Converter(args.upstream)
    for f in args.files:
        fs = FileSync(conv, args.upstream, f, args.max_shift)
        new_rows, corrections = fs.plan()
        if args.no_corrections:
            new_rows += [m for _, m in corrections]
            corrections = []
        print_report(fs, new_rows, corrections, True)


def cmd_apply(args):
    conv = Converter(args.upstream)
    sha_file = pathlib.Path(args.upstream) / "UPSTREAM_SHA"
    sha = sha_file.read_text().strip()[:10] if sha_file.exists() else "master"
    for f in args.files:
        fs = FileSync(conv, args.upstream, f, args.max_shift)
        new_rows, corrections = fs.plan()
        if args.no_corrections:
            new_rows += [m for _, m in corrections]
            corrections = []
        title = args.title or "Imported from Skretzo/shortest-path %s (%s)" % (sha, f)
        changed = fs.apply(new_rows, corrections, title)
        print_report(fs, new_rows, corrections, args.verbose)
        for old, new, line in changed:
            print("   corrected: %s\n          -> %s   (upstream line %d)" % (old, new, line))


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    f = sub.add_parser("fetch", help="download upstream TSVs and ItemVariations.java")
    f.add_argument("--dest", required=True)
    f.set_defaults(func=cmd_fetch)
    c = sub.add_parser("convert", help="print an upstream file converted to our format")
    c.add_argument("--upstream", required=True)
    c.add_argument("file")
    c.set_defaults(func=cmd_convert)
    for name, fn in (("diff", cmd_diff), ("apply", cmd_apply)):
        s = sub.add_parser(name)
        s.add_argument("--upstream", required=True)
        s.add_argument("--max-shift", type=int, default=3, help="max tiles a corrected endpoint may move")
        s.add_argument("--no-corrections", action="store_true", help="add corrected rows as new rows instead")
        s.add_argument("files", nargs="+")
        if name == "apply":
            s.add_argument("--title", help="section comment for the appended rows")
            s.add_argument("--verbose", action="store_true")
        s.set_defaults(func=fn)
    args = p.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
