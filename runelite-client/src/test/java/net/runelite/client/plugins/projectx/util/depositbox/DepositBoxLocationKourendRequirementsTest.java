package net.runelite.client.plugins.projectx.util.depositbox;

import java.util.Optional;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DepositBoxLocationKourendRequirementsTest {

    private static final DepositBoxLocation[] KOUREND_DEPOSIT_BOXES = {
            DepositBoxLocation.ARCEUUS,
            DepositBoxLocation.LOVAKENGJ,
            DepositBoxLocation.SHAYZIEN,
            DepositBoxLocation.KOUREND_CASTLE,
            DepositBoxLocation.PORT_PISCARILIUS,
            DepositBoxLocation.WINTERTODT_CAMP,
            DepositBoxLocation.MOUNT_QUIDAMORTEM,
            DepositBoxLocation.HOSIDIUS,
            DepositBoxLocation.MOUNT_KARUULM
    };

    private MockedStatic<ProjectX> projectx;
    private MockedStatic<Rs2Player> player;

    @Before
    public void setUp() {
        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenReturn(Optional.of(false));
        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
        player = Mockito.mockStatic(Rs2Player.class);
        player.when(() -> Rs2Player.getQuestState(any(Quest.class))).thenReturn(QuestState.NOT_STARTED);
    }

    @After
    public void tearDown() {
        player.close();
        projectx.close();
    }

    @Test
    public void kourendDepositBoxesAreUsableWithoutClientOfKourend() {
        player.when(Rs2Player::isMember).thenReturn(true);
        player.when(Rs2Player::isInMemberWorld).thenReturn(true);
        for (DepositBoxLocation box : KOUREND_DEPOSIT_BOXES) {
            assertTrue(box.name(), box.hasRequirements());
        }
    }

    @Test
    public void kourendDepositBoxesStillRequireMembership() {
        player.when(Rs2Player::isMember).thenReturn(false);
        player.when(Rs2Player::isInMemberWorld).thenReturn(false);
        for (DepositBoxLocation box : KOUREND_DEPOSIT_BOXES) {
            assertFalse(box.name(), box.hasRequirements());
        }
    }
}
