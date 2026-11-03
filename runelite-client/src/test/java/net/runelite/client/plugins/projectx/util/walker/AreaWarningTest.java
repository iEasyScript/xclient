package net.runelite.client.plugins.projectx.util.walker;

import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AreaWarningTest {

    @Test
    public void coversEveryWarningInterfaceWithAProceedButton() {
        // Interfaces 560-580 are the Doomsayer's warnings (Mort Myre gate among them); all share
        // one layout with the proceed button at child 17. The walker used to answer only 565 and 579.
        Set<Integer> groups = new HashSet<>();
        for (int component : AreaWarning.PROCEED_BUTTONS) {
            assertEquals("proceed button child of " + (component >>> 16), 17, component & 0xFFFF);
            groups.add(component >>> 16);
        }
        for (int group = InterfaceID.CWS_WARNING_9; group <= InterfaceID.CWS_WARNING_20; group++) {
            assertTrue("warning interface " + group + " is not answered", groups.contains(group));
        }
        // 21 Doomsayer warnings plus the revenant crevice warning (720)
        assertTrue(groups.contains(InterfaceID.WILDERNESS_CAVE_MID_WARNING));
        assertEquals(22, groups.size());
    }

    @Test
    public void revenantCrevicePrefersTheDontWarnAgainButton() {
        Map<Integer, Widget> widgets = new HashMap<>();
        widgets.put(InterfaceID.WildernessCaveMidWarning.WARN1, widget(false, "Let me jump"));
        widgets.put(InterfaceID.WildernessCaveMidWarning.WARN2, widget(false, "Let me jump, and don't warn me again!"));
        int proceed = AreaWarning.openProceedButton(widgets::get);
        assertEquals(InterfaceID.WildernessCaveMidWarning.WARN1, proceed);
        assertEquals(InterfaceID.WildernessCaveMidWarning.WARN2, AreaWarning.preferredButton(proceed, widgets::get));
    }

    @Test
    public void revenantCreviceFallsBackToProceedAndOtherWarningsAreUntouched() {
        Map<Integer, Widget> widgets = new HashMap<>();
        widgets.put(InterfaceID.WildernessCaveMidWarning.WARN1, widget(false, "Jump"));
        assertEquals(InterfaceID.WildernessCaveMidWarning.WARN1,
                AreaWarning.preferredButton(InterfaceID.WildernessCaveMidWarning.WARN1, widgets::get));
        assertEquals(InterfaceID.CwsWarning13.WARN1,
                AreaWarning.preferredButton(InterfaceID.CwsWarning13.WARN1, id -> widget(false, "Proceed regardless")));
        assertEquals(-1, AreaWarning.preferredButton(-1, id -> null));
    }

    @Test
    public void findsOnlyTheVisibleWarning() {
        Map<Integer, Widget> widgets = new HashMap<>();
        widgets.put(InterfaceID.CwsWarning10.WARN1, widget(true));   // loaded but hidden
        widgets.put(InterfaceID.CwsWarning13.WARN1, widget(false));  // the open one
        assertEquals(InterfaceID.CwsWarning13.WARN1, AreaWarning.openProceedButton(widgets::get));
    }

    @Test
    public void nothingOpenIsMinusOne() {
        assertEquals(-1, AreaWarning.openProceedButton(id -> null));
    }

    private static Widget widget(boolean hidden) {
        Widget w = mock(Widget.class);
        when(w.isHidden()).thenReturn(hidden);
        return w;
    }

    private static Widget widget(boolean hidden, String action) {
        Widget w = widget(hidden);
        when(w.getActions()).thenReturn(new String[]{action});
        return w;
    }
}
