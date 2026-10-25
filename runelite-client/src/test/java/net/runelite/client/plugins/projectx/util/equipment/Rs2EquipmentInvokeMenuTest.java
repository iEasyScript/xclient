package net.runelite.client.plugins.projectx.util.equipment;

import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.globval.enums.InterfaceTab;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.projectx.util.tabs.Rs2Tab;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class Rs2EquipmentInvokeMenuTest {

    private static Rs2ItemModel helmet() {
        Rs2ItemModel item = mock(Rs2ItemModel.class);
        when(item.getName()).thenReturn("Iron full helm");
        when(item.getSlot()).thenReturn(EquipmentInventorySlot.HEAD.getSlotIdx());
        when(item.getEquipmentActions()).thenReturn(Collections.emptyList());
        return item;
    }

    @Test
    public void invokeMenuReportsFailureWhenEquipmentTabDoesNotOpen() {
        Rs2ItemModel item = helmet();

        try (MockedStatic<ProjectX> projectx = Mockito.mockStatic(ProjectX.class);
             MockedStatic<Rs2Tab> tabs = Mockito.mockStatic(Rs2Tab.class))
        {
            tabs.when(() -> Rs2Tab.switchTo(InterfaceTab.EQUIPMENT)).thenReturn(false);

            assertFalse(Rs2Equipment.invokeMenu(item, "remove"));

            projectx.verify(() -> ProjectX.doInvoke(any(), any()), never());
        }
    }

    @Test
    public void unEquipReportsFailureWhenEquipmentTabDoesNotOpen() {
        Rs2ItemModel item = helmet();

        try (MockedStatic<Rs2Equipment> equipment = Mockito.mockStatic(Rs2Equipment.class, Answers.CALLS_REAL_METHODS);
             MockedStatic<ProjectX> projectx = Mockito.mockStatic(ProjectX.class);
             MockedStatic<Rs2Tab> tabs = Mockito.mockStatic(Rs2Tab.class))
        {
            equipment.when(() -> Rs2Equipment.get(EquipmentInventorySlot.HEAD)).thenReturn(item);
            tabs.when(() -> Rs2Tab.switchTo(InterfaceTab.EQUIPMENT)).thenReturn(false);

            assertFalse(Rs2Equipment.unEquip(EquipmentInventorySlot.HEAD));

            projectx.verify(() -> ProjectX.doInvoke(any(), any()), never());
        }
    }

    @Test
    public void invokeMenuReportsFailureWhenActionIsMissing() {
        Rs2ItemModel item = helmet();
        when(item.getIndexOfSubAction("Teleport")).thenReturn(null);

        try (MockedStatic<ProjectX> projectx = Mockito.mockStatic(ProjectX.class);
             MockedStatic<Rs2Tab> tabs = Mockito.mockStatic(Rs2Tab.class))
        {
            tabs.when(() -> Rs2Tab.switchTo(InterfaceTab.EQUIPMENT)).thenReturn(true);

            assertFalse(Rs2Equipment.invokeMenu(item, "Teleport"));

            projectx.verify(() -> ProjectX.doInvoke(any(), any()), never());
        }
    }

    @Test
    public void unEquipInvokesRemoveWhenEquipmentTabOpens() {
        Rs2ItemModel item = helmet();
        Client client = mock(Client.class);
        when(client.getCanvasWidth()).thenReturn(765);
        when(client.getCanvasHeight()).thenReturn(503);

        try (MockedStatic<Rs2Equipment> equipment = Mockito.mockStatic(Rs2Equipment.class, Answers.CALLS_REAL_METHODS);
             MockedStatic<ProjectX> projectx = Mockito.mockStatic(ProjectX.class);
             MockedStatic<Rs2Tab> tabs = Mockito.mockStatic(Rs2Tab.class);
             MockedStatic<Rs2Widget> widgets = Mockito.mockStatic(Rs2Widget.class))
        {
            equipment.when(() -> Rs2Equipment.get(EquipmentInventorySlot.HEAD)).thenReturn(item);
            projectx.when(ProjectX::getClient).thenReturn(client);
            tabs.when(() -> Rs2Tab.switchTo(InterfaceTab.EQUIPMENT)).thenReturn(true);
            widgets.when(() -> Rs2Widget.getWidget(anyInt(), anyInt())).thenReturn(null);

            assertTrue(Rs2Equipment.unEquip(EquipmentInventorySlot.HEAD));

            projectx.verify(() -> ProjectX.doInvoke(argThat(entry -> entry.getIdentifier() == 1), any()));
        }
    }
}
