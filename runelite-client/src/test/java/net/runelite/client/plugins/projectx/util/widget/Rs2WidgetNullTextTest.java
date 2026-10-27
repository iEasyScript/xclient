package net.runelite.client.plugins.projectx.util.widget;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2WidgetNullTextTest {

    private MockedStatic<ProjectX> projectX;

    @Before
    public void setUp() throws Exception {
        Client client = mock(Client.class);
        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(inv ->
                Optional.ofNullable(((Callable<?>) inv.getArgument(0)).call()));
        projectX = Mockito.mockStatic(ProjectX.class);
        projectX.when(ProjectX::getClient).thenReturn(client);
        projectX.when(ProjectX::getClientThread).thenReturn(clientThread);
    }

    @After
    public void tearDown() {
        projectX.close();
    }

    private static Widget brokenTextWidget() {
        Widget widget = mock(Widget.class);
        when(widget.getText()).thenThrow(new NullPointerException());
        when(widget.getName()).thenThrow(new NullPointerException());
        return widget;
    }

    @Test
    public void findWidgetSkipsSiblingWhoseTextGetterThrows() {
        Widget broken = brokenTextWidget();
        Widget match = mock(Widget.class);
        when(match.getText()).thenReturn("Make");
        when(match.getName()).thenReturn("");
        assertSame(match, Rs2Widget.findWidget("Make", Arrays.asList(broken, match), true));
        assertNull(Rs2Widget.findWidget("Make", Collections.singletonList(broken), false));
    }

    @Test
    public void searchChildrenDescendsPastParentWhoseTextGetterThrows() {
        Widget parent = brokenTextWidget();
        Widget match = mock(Widget.class);
        when(match.getText()).thenReturn("<col=ffffff>Make</col>");
        when(match.getName()).thenReturn(null);
        when(parent.getDynamicChildren()).thenReturn(new Widget[]{match});
        assertSame(match, Rs2Widget.searchChildren("make", parent, false));
    }

    @Test
    public void findWidgetMatchesActionWhenTextGetterThrows() {
        Widget widget = brokenTextWidget();
        when(widget.getActions()).thenReturn(new String[]{null, "Make"});
        assertSame(widget, Rs2Widget.findWidget("Make", Collections.singletonList(widget), true));
    }
}
