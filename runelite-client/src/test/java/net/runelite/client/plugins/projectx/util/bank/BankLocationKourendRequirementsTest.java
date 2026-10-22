package net.runelite.client.plugins.projectx.util.bank;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.plugins.projectx.util.bank.enums.BankLocation;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class BankLocationKourendRequirementsTest {

    private static final BankLocation[] KOUREND_BANKS = {
            BankLocation.LOVAKENGJ,
            BankLocation.HOSIDIUS,
            BankLocation.PISCARILIUS,
            BankLocation.ARCEUUS,
            BankLocation.WINTERTODT,
            BankLocation.SULPHUR_MINE,
            BankLocation.ZEAH_SAND_BANK,
            BankLocation.BLAST_MINE,
            BankLocation.SHAYZIEN_BANK,
            BankLocation.SHAYZIEN_CHEST,
            BankLocation.GREAT_KOUREND_CASTLE,
            BankLocation.HOSIDIUS_KITCHEN,
            BankLocation.VINERY_BANK,
            BankLocation.MOUNT_KARUULM
    };

    private MockedStatic<Rs2Player> player;

    @Before
    public void setUp() {
        player = Mockito.mockStatic(Rs2Player.class);
        player.when(() -> Rs2Player.getQuestState(any(Quest.class))).thenReturn(QuestState.NOT_STARTED);
    }

    @After
    public void tearDown() {
        player.close();
    }

    @Test
    public void kourendBanksAreUsableWithoutClientOfKourend() {
        player.when(Rs2Player::isMember).thenReturn(true);
        player.when(Rs2Player::isInMemberWorld).thenReturn(true);
        for (BankLocation bank : KOUREND_BANKS) {
            assertTrue(bank.name(), bank.hasRequirements());
        }
        assertFalse(BankLocation.CANIFIS.hasRequirements());
    }

    @Test
    public void kourendBanksStillRequireMembership() {
        player.when(Rs2Player::isMember).thenReturn(false);
        player.when(Rs2Player::isInMemberWorld).thenReturn(false);
        for (BankLocation bank : KOUREND_BANKS) {
            assertFalse(bank.name(), bank.hasRequirements());
        }
    }
}
