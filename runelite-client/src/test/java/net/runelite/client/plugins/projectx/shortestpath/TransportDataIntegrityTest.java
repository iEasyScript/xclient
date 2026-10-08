package net.runelite.client.plugins.projectx.shortestpath;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Row-level validation of every transport TSV the walker loads. {@link Transport} parses rows
 * leniently: an unknown quest name, a misspelt skill, a varbit typed into the wrong column or a
 * header the parser does not recognise is silently dropped, and the row is then treated as having
 * no such requirement. A route missing a requirement sends players somewhere they cannot go, so
 * this test re-reads each row and asserts that every non-empty requirement cell actually reached
 * the parsed {@link Transport}.
 *
 * <p>It also pins the item-requirement shape: a row lists "any one of" items as a single
 * space-separated group. The banking planner withdraws from one group only, so ';'-separated
 * groups would make a banked route fetch an arbitrary item.
 */
public class TransportDataIntegrityTest {

    /** Mirrors {@code Transport.appendStandardTransportFiles}. */
    private static final Map<String, TransportType> FILES = new LinkedHashMap<>();

    static {
        FILES.put("transports.tsv", TransportType.TRANSPORT);
        FILES.put("agility_shortcuts.tsv", TransportType.AGILITY_SHORTCUT);
        FILES.put("boats.tsv", TransportType.BOAT);
        FILES.put("canoes.tsv", TransportType.CANOE);
        FILES.put("charter_ships.tsv", TransportType.CHARTER_SHIP);
        FILES.put("ships.tsv", TransportType.SHIP);
        FILES.put("fairy_rings.tsv", TransportType.FAIRY_RING);
        FILES.put("gnome_gliders.tsv", TransportType.GNOME_GLIDER);
        FILES.put("minecarts.tsv", TransportType.MINECART);
        FILES.put("spirit_trees.tsv", TransportType.SPIRIT_TREE);
        FILES.put("quetzals.tsv", TransportType.QUETZAL);
        FILES.put("teleportation_items.tsv", TransportType.TELEPORTATION_ITEM);
        FILES.put("teleportation_minigames.tsv", TransportType.TELEPORTATION_MINIGAME);
        FILES.put("teleportation_levers.tsv", TransportType.TELEPORTATION_LEVER);
        FILES.put("teleportation_portals.tsv", TransportType.TELEPORTATION_PORTAL);
        FILES.put("teleportation_spells.tsv", TransportType.TELEPORTATION_SPELL);
        FILES.put("wilderness_obelisks.tsv", TransportType.WILDERNESS_OBELISK);
        FILES.put("magic_carpets.tsv", TransportType.MAGIC_CARPET);
        FILES.put("hot_air_balloons.tsv", TransportType.HOT_AIR_BALLOON);
        FILES.put("magic_mushtrees.tsv", TransportType.MAGIC_MUSHTREE);
        FILES.put("seasonal_transports.tsv", TransportType.SEASONAL_TRANSPORT);
        FILES.put("npcs.tsv", TransportType.NPC);
    }

    /** Columns {@link Transport} reads. */
    private static final Set<String> PARSED_COLUMNS = new HashSet<>(Arrays.asList(
            "Origin", "Destination", "menuOption menuTarget objectID", "Skills", "Item IDs", "Currency",
            "Quests", "Varbits", "Varplayers", "Duration", "Display info", "Consumable", "Wilderness level",
            "isMembers"));

    /** Columns the parser ignores that only ever carry cosmetic text. */
    private static final Set<String> COSMETIC_COLUMNS = new HashSet<>(Arrays.asList("Display Info", "Info"));

    private static final Set<String> CURRENCIES = new HashSet<>(Arrays.asList("Coins", "Ecto-token"));
    private static final Pattern COORDINATE = Pattern.compile("^\\d+ \\d+ \\d+$");
    private static final Pattern VAR_TOKEN = Pattern.compile("^\\d+[=><&@]\\d+$");
    private static final Pattern ITEM_GROUP = Pattern.compile("^\\d+( \\d+)*$");

    @Test
    public void everyRequirementCellReachesTheParsedTransport() throws IOException {
        List<String> problems = new ArrayList<>();
        int rows = 0;
        for (Map.Entry<String, TransportType> file : FILES.entrySet()) {
            for (Row row : readRows(file.getKey())) {
                rows++;
                checkRow(row, file.getValue(), problems);
            }
        }
        assertTrue("expected the walker TSVs to hold thousands of rows, read " + rows, rows > 5000);
        assertTrue(problems.size() + " malformed transport rows:\n" + String.join("\n", problems),
                problems.isEmpty());
    }

    @Test
    public void canoeRoutesAcceptAnyOneAxe() throws IOException {
        int canoeRows = 0;
        for (Row row : readRows("canoes.tsv")) {
            Transport transport = new Transport(row.fields, TransportType.CANOE);
            canoeRows++;
            assertEquals("canoe row " + row.where() + " must list its axes as one 'any of' group",
                    1, transport.getItemIdRequirements().size());
            Set<Integer> axes = transport.getItemIdRequirements().iterator().next();
            for (int axe : new int[]{ItemID.BRONZE_AXE, ItemID.RUNE_AXE, ItemID.DRAGON_AXE,
                    ItemID.CRYSTAL_AXE, ItemID.INFERNAL_AXE}) {
                assertTrue("canoe row " + row.where() + " should accept axe " + axe, axes.contains(axe));
            }
        }
        assertTrue("canoes.tsv should not be empty", canoeRows >= 20);
    }

    private static void checkRow(Row row, TransportType type, List<String> problems) {
        Transport transport;
        try {
            transport = new Transport(row.fields, type);
        } catch (RuntimeException e) {
            problems.add(row.where() + " does not parse: " + e);
            return;
        }
        if (row.extraCells) {
            problems.add(row.where() + " has a value beyond the last header column");
        }
        for (Map.Entry<String, String> cell : row.fields.entrySet()) {
            String column = cell.getKey();
            String value = cell.getValue().trim();
            if (value.isEmpty()) {
                continue;
            }
            if (!PARSED_COLUMNS.contains(column) && !COSMETIC_COLUMNS.contains(column)) {
                problems.add(row.where() + " has '" + value + "' in column '" + column + "' which the parser ignores");
                continue;
            }
            switch (column) {
                case "Origin":
                case "Destination":
                    if (!COORDINATE.matcher(value).matches()) {
                        problems.add(row.where() + " bad " + column + " '" + value + "'");
                    }
                    break;
                case "menuOption menuTarget objectID":
                    if (transport.getObjectId() <= 0 || transport.getAction() == null || transport.getName() == null) {
                        problems.add(row.where() + " object cell '" + value + "' did not parse");
                    }
                    break;
                case "Skills":
                    checkSkills(row, value, transport, problems);
                    break;
                case "Item IDs":
                    if (!ITEM_GROUP.matcher(value).matches() || transport.getItemIdRequirements().size() != 1) {
                        problems.add(row.where() + " Item IDs '" + value
                                + "' must be one space-separated 'any of' group");
                    }
                    break;
                case "Currency":
                    if (transport.getCurrencyAmount() <= 0 || !CURRENCIES.contains(transport.getCurrencyName())) {
                        problems.add(row.where() + " currency '" + value + "' did not parse");
                    }
                    break;
                case "Quests":
                    if (transport.getQuests().size() != value.split(";").length) {
                        problems.add(row.where() + " quests '" + value + "' resolved to " + transport.getQuests().keySet());
                    }
                    break;
                case "Varbits":
                    checkVars(row, value, transport.getVarbits().size(), problems);
                    break;
                case "Varplayers":
                    checkVars(row, value, transport.getVarplayers().size(), problems);
                    break;
                case "Duration":
                case "Wilderness level":
                    if (!value.matches("\\d+")) {
                        problems.add(row.where() + " bad " + column + " '" + value + "'");
                    }
                    break;
                case "Consumable":
                    if (!"T".equals(value) && !"F".equals(value)) {
                        problems.add(row.where() + " consumable flag '" + value + "' must be T or F");
                    }
                    break;
                case "isMembers":
                    if (!"Y".equals(value) && !"N".equals(value)) {
                        problems.add(row.where() + " isMembers '" + value + "' must be Y or N");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static void checkSkills(Row row, String value, Transport transport, List<String> problems) {
        for (String requirement : value.split(";")) {
            String[] parts = requirement.split(" ");
            Skill skill = null;
            if (parts.length == 2 && parts[0].matches("\\d+")) {
                for (Skill s : Skill.values()) {
                    if (s.getName().equals(parts[1])) {
                        skill = s;
                    }
                }
            }
            if (skill == null || transport.getSkillLevels()[skill.ordinal()] != Integer.parseInt(parts[0])) {
                problems.add(row.where() + " skill requirement '" + requirement + "' did not parse");
            }
        }
    }

    private static void checkVars(Row row, String value, int parsed, List<String> problems) {
        String[] tokens = value.split(";");
        for (String token : tokens) {
            if (!VAR_TOKEN.matcher(token).matches()) {
                problems.add(row.where() + " var token '" + token + "' is malformed");
                return;
            }
        }
        if (parsed != tokens.length) {
            problems.add(row.where() + " vars '" + value + "' parsed to " + parsed + " checks");
        }
    }

    private static final class Row {
        final String file;
        final int line;
        final Map<String, String> fields;
        final boolean extraCells;

        Row(String file, int line, Map<String, String> fields, boolean extraCells) {
            this.file = file;
            this.line = line;
            this.fields = fields;
            this.extraCells = extraCells;
        }

        String where() {
            return file + ":" + line;
        }
    }

    /** Same header/row handling as {@code Transport.addTransports}. */
    private static List<Row> readRows(String file) throws IOException {
        List<Row> rows = new ArrayList<>();
        try (InputStream stream = ShortestPathPlugin.class.getResourceAsStream(file)) {
            assertNotNull("missing resource " + file, stream);
            String[] lines = new String(Util.readAllBytes(stream), StandardCharsets.UTF_8).split("\n", -1);
            String header = stripCr(lines[0]);
            header = header.startsWith("# ") ? header.replace("# ", "#") : header;
            header = header.startsWith("#") ? header.replace("#", "") : header;
            String[] columns = header.split("\t");
            assertFalse(file + " has an empty header", columns.length == 0);
            for (int i = 1; i < lines.length; i++) {
                String line = stripCr(lines[i]);
                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }
                String[] cells = line.split("\t");
                Map<String, String> fields = new HashMap<>();
                for (int c = 0; c < columns.length && c < cells.length; c++) {
                    fields.put(columns[c], cells[c]);
                }
                boolean extra = false;
                for (int c = columns.length; c < cells.length; c++) {
                    extra |= !cells[c].isBlank();
                }
                rows.add(new Row(file, i + 1, fields, extra));
            }
        }
        return rows;
    }

    private static String stripCr(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    @Test
    public void everyLoadedTransportHasACoordinateEndpoint() {
        HashMap<WorldPoint, Set<Transport>> transports = Transport.loadAllFromResources();
        for (Map.Entry<WorldPoint, Set<Transport>> entry : transports.entrySet()) {
            for (Transport transport : entry.getValue()) {
                assertNotNull("transport without destination at " + entry.getKey(), transport.getDestination());
            }
        }
    }
}
