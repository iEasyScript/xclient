package net.runelite.client.plugins.projectx.util.combat;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2CombatSetAttackStyleTest {

    private static final int STYLE_WIDGET_ID = 38862852;

    private MockedStatic<ProjectX> projectx;
    private MockedStatic<Rs2Widget> widgets;
    private Client client;
    private Widget styleWidget;

    @Before
    public void setUp() {
        client = mock(Client.class);
        styleWidget = mock(Widget.class);
        when(styleWidget.getId()).thenReturn(STYLE_WIDGET_ID);

        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClient).thenReturn(client);
        widgets = Mockito.mockStatic(Rs2Widget.class);
    }

    @After
    public void tearDown() {
        widgets.close();
        projectx.close();
    }

    @Test
    public void clicksStyleWhenLabelWidgetIsMissing() {
        when(client.getWidget(WidgetInfo.COMBAT_STYLE_ONE)).thenReturn(styleWidget);
        widgets.when(() -> Rs2Widget.getWidget(STYLE_WIDGET_ID + 3)).thenReturn(null);

        assertTrue(Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_ONE));

        projectx.verify(() -> ProjectX.log("Setting attack style to COMBAT_STYLE_ONE"));
        widgets.verify(() -> Rs2Widget.clickWidget(styleWidget));
    }

    @Test
    public void logsLabelTextWhenLabelWidgetIsPresent() {
        Widget label = mock(Widget.class);
        when(label.getText()).thenReturn("Accurate");
        when(client.getWidget(WidgetInfo.COMBAT_STYLE_ONE)).thenReturn(styleWidget);
        widgets.when(() -> Rs2Widget.getWidget(STYLE_WIDGET_ID + 3)).thenReturn(label);

        assertTrue(Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_ONE));

        projectx.verify(() -> ProjectX.log("Setting attack style to Accurate"));
        widgets.verify(() -> Rs2Widget.clickWidget(styleWidget));
    }

    @Test
    public void returnsFalseWithoutClickingWhenStyleWidgetIsMissing() {
        when(client.getWidget(WidgetInfo.COMBAT_STYLE_ONE)).thenReturn(null);

        assertFalse(Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_ONE));

        projectx.verify(() -> ProjectX.log(anyString()), Mockito.never());
        widgets.verify(() -> Rs2Widget.clickWidget(styleWidget), Mockito.never());
    }
}
