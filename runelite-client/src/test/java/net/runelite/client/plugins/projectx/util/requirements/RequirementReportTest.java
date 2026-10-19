package net.runelite.client.plugins.projectx.util.requirements;

import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequirementReportTest
{
    private final FakeRequirementContext ctx = new FakeRequirementContext();

    private static Requirement fixed(String name, RequirementStatus status)
    {
        return Requirement.of(name, c ->
        {
            switch (status)
            {
                case MET:
                    return RequirementResult.met("ok");
                case UNMET:
                    return RequirementResult.unmet("no");
                default:
                    return RequirementResult.unknown("?");
            }
        });
    }

    private static List<String> names(RequirementReport report)
    {
        return report.getOutcomes().stream().map(RequirementOutcome::getDescription).collect(Collectors.toList());
    }

    @Test
    public void worstFirstAndDeclarationOrderKept()
    {
        List<Requirement> reqs = List.of(
            fixed("met A", RequirementStatus.MET),
            fixed("rec unknown", RequirementStatus.UNKNOWN).recommended(),
            fixed("req unknown", RequirementStatus.UNKNOWN),
            fixed("rec unmet", RequirementStatus.UNMET).recommended(),
            fixed("req unmet 1", RequirementStatus.UNMET),
            fixed("met B", RequirementStatus.MET),
            fixed("req unmet 2", RequirementStatus.UNMET));

        RequirementReport report = RequirementReport.evaluate("Test", reqs, ctx);

        assertEquals(List.of("req unmet 1", "req unmet 2", "req unknown", "rec unmet", "rec unknown", "met A", "met B"),
            names(report));
        assertEquals("2 missing, 1 recommended missing, 2 unknown, 2 met", report.summary());
        assertTrue(report.hasUnmetRequired());
        assertFalse(report.allMet());
    }

    @Test
    public void summaryWhenAllMet()
    {
        RequirementReport one = RequirementReport.evaluate("T", List.of(fixed("a", RequirementStatus.MET)), ctx);
        assertEquals("Requirement met", one.summary());
        RequirementReport three = RequirementReport.evaluate("T", List.of(
            fixed("a", RequirementStatus.MET), fixed("b", RequirementStatus.MET), fixed("c", RequirementStatus.MET)), ctx);
        assertEquals("All 3 requirements met", three.summary());
        assertTrue(three.allMet());
        assertFalse(three.needsAttention(true));
    }

    @Test
    public void emptyReport()
    {
        RequirementReport report = RequirementReport.empty("T");
        assertTrue(report.isEmpty());
        assertEquals("Nothing to check", report.summary());
        assertFalse(report.needsAttention(true));
    }

    @Test
    public void needsAttentionRules()
    {
        // A required one unknown: show it.
        assertTrue(RequirementReport.evaluate("T", List.of(fixed("a", RequirementStatus.UNKNOWN)), ctx).needsAttention(false));

        // A recommended one missing: show it only when warning about recommended.
        RequirementReport recUnmet = RequirementReport.evaluate("T",
            List.of(fixed("a", RequirementStatus.UNMET).recommended(), fixed("b", RequirementStatus.MET)), ctx);
        assertTrue(recUnmet.needsAttention(true));
        assertFalse(recUnmet.needsAttention(false));

        // A recommended one only unknown: never stops a start.
        RequirementReport recUnknown = RequirementReport.evaluate("T",
            List.of(fixed("a", RequirementStatus.UNKNOWN).recommended()), ctx);
        assertFalse(recUnknown.needsAttention(true));
    }

    @Test
    public void allUnknownReport()
    {
        RequirementReport report = RequirementReport.allUnknown("T",
            List.of(fixed("a", RequirementStatus.MET), fixed("b", RequirementStatus.MET)), "Game not loaded");
        assertEquals(2, report.count(RequirementStatus.UNKNOWN));
        assertEquals("Game not loaded", report.getOutcomes().get(0).getDetail());
        assertEquals("2 unknown", report.summary());
    }

    @Test
    public void nullRequirementsAreSkipped()
    {
        List<Requirement> reqs = new java.util.ArrayList<>();
        reqs.add(null);
        reqs.add(fixed("a", RequirementStatus.MET));
        assertEquals(1, RequirementReport.evaluate("T", reqs, ctx).getOutcomes().size());
    }

    @Test
    public void declaredRequirementReplacesInferredWithTheSameKey()
    {
        InventorySetup main = new InventorySetup(List.of(), List.of(), null, null, null, new HashMap<>(),
            "Main", "", null, false, null, false, false, 4, false, -1);
        InventorySetup other = new InventorySetup(List.of(), List.of(), null, null, null, new HashMap<>(),
            "Other", "", null, false, null, false, false, 4, false, -1);

        List<Requirement> declared = List.of(Requirements.inventorySetup("main"));
        List<Requirement> inferred = List.of(
            Requirements.inventorySetup(main).recommended(),
            Requirements.inventorySetup(other).recommended());

        List<Requirement> merged = RequirementCheck.merge(declared, inferred);
        assertEquals(2, merged.size());
        assertEquals(RequirementSeverity.REQUIRED, merged.get(0).getSeverity());
        assertEquals("Inventory setup \"Other\"", merged.get(1).getDescription());
    }
}
