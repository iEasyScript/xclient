package net.runelite.client.plugins.projectx.util.walker;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FossilRowboatOptionTest {
    /** The camp boat's menu on an account with every trip unlocked. */
    private static final List<String> ALL = Arrays.asList("Row to the barge.", "Row to the north of the island.",
            "Row to the barge and travel to the Digsite.", "Row out to sea, north of the island.", "Cancel.");
    /** Nothing unlocked yet: option 2 is the Digsite, which pressing "2" used to pick for the north. */
    private static final List<String> LOCKED = Arrays.asList("Row to the barge.",
            "Row to the barge and travel to the Digsite.", "Cancel.");

    @Test
    public void picksEachDestinationByText() {
        assertEquals("Row to the north of the island.", Rs2Walker.fossilRowboatOption("2. North of Island", ALL));
        assertEquals("Row out to sea, north of the island.", Rs2Walker.fossilRowboatOption("4. To Sea", ALL));
        assertEquals("Row to the barge.", Rs2Walker.fossilRowboatOption("3. The Camp", ALL));
    }

    @Test
    public void missingTripIsNull() {
        assertNull(Rs2Walker.fossilRowboatOption("2. North of Island", LOCKED));
        assertNull(Rs2Walker.fossilRowboatOption("4. To Sea", LOCKED));
    }

    @Test
    public void campMatchesTheCampWording() {
        assertEquals("Row to the camp.", Rs2Walker.fossilRowboatOption("2. The Camp",
                Arrays.asList("Row to the camp.", "Row out to sea.", "Cancel.")));
    }
}
