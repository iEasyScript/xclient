/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.projectx.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.html.HtmlEscapers;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ExternalPluginsChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManager;
import net.runelite.client.plugins.projectx.ui.search.ProjectXPluginSearch;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class ProjectXPluginListPanel extends ProjectXPluginPanel {
    static final int LIST_VIEW_WIDTH = PluginPanel.PANEL_WIDTH - PluginPanel.SCROLLBAR_WIDTH;
    static final int LIST_ITEM_WIDTH = LIST_VIEW_WIDTH - 20;

    private static final String RUNELITE_GROUP_NAME = ProjectXConfig.class.getAnnotation(ConfigGroup.class).value();
    private static final String PINNED_PLUGINS_CONFIG_KEY = "pinnedPlugins";
    private static final ImmutableList<String> CATEGORY_TAGS = ImmutableList.of(
            "Combat",
            "Chat",
            "Item",
            "Minigame",
            "Notification",
            "Plugin Hub",
            "Skilling",
            "XP"
    );

    private final ConfigManager configManager;
    private final PluginManager pluginManager;
    private final ProjectXPluginManager projectxPluginManager;
    private final Provider<ProjectXConfigPanel> configPanelProvider;
    private final List<ProjectXPluginConfigurationDescriptor> fakePlugins = new ArrayList<>();

    @Getter
    private final ExternalPluginManager externalPluginManager;

    @Getter
    private final ProjectXMultiplexingPluginPanel muxer;

    private final IconTextField searchBar;
    private final JScrollPane scrollPane;
    private final ProjectXFixedWidthPanel mainPanel;
    private List<ProjectXPluginListItem> pluginList;

    @Inject
    public ProjectXPluginListPanel(
            ConfigManager configManager,
            PluginManager pluginManager,
            ProjectXPluginManager projectxPluginManager,
            ExternalPluginManager externalPluginManager,
            EventBus eventBus,
            Provider<ProjectXConfigPanel> configPanelProvider) {
        super(false);

        this.configManager = configManager;
        this.pluginManager = pluginManager;
        this.externalPluginManager = externalPluginManager;
        this.configPanelProvider = configPanelProvider;
        this.projectxPluginManager = projectxPluginManager;

        muxer = new ProjectXMultiplexingPluginPanel(this) {
            @Override
            protected void onAdd(ProjectXPluginPanel p) {
                eventBus.register(p);
            }

            @Override
            protected void onRemove(ProjectXPluginPanel p) {
                eventBus.unregister(p);
            }
        };

        searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onSearchBarChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onSearchBarChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onSearchBarChanged();
            }
        });
        CATEGORY_TAGS.forEach(searchBar.getSuggestionListModel()::addElement);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel topPanel = new JPanel();
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        topPanel.setLayout(new BorderLayout(0, BORDER_OFFSET));
        topPanel.add(searchBar, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        mainPanel = new ProjectXFixedWidthPanel(LIST_VIEW_WIDTH);
        mainPanel.setBorder(new EmptyBorder(8, 10, 10, 10));
        mainPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
        mainPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel northPanel = new ProjectXFixedWidthPanel(LIST_VIEW_WIDTH);
        northPanel.setLayout(new BorderLayout());
        northPanel.add(mainPanel, BorderLayout.NORTH);

        scrollPane = new JScrollPane(northPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void rebuildPluginList() {
        List<String> pinnedPlugins = getPinnedPluginNames();

        Predicate<Plugin> isProjectXPlugin = plugin ->
                plugin.getClass().getPackage().getName().toLowerCase().contains("projectx");

        // populate pluginList with all non-hidden plugins
        pluginList = Stream.concat(
                        fakePlugins.stream(),
                        pluginManager.getPlugins().stream()
                                .filter(plugin -> {
                                    PluginDescriptor d = plugin.getClass().getAnnotation(PluginDescriptor.class);
                                    return !d.hidden() && (isProjectXPlugin.test(plugin) || d.isExternal());
                                })
                                .map(plugin ->
                                {
                                    PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
                                    Config config = pluginManager.getPluginConfigProxy(plugin);
                                    ConfigDescriptor configDescriptor = config == null ? null : configManager.getConfigDescriptor(config);
                                    List<String> conflicts = pluginManager.conflictsForPlugin(plugin).stream()
                                            .map(Plugin::getName)
                                            .collect(Collectors.toList());

                                    return new ProjectXPluginConfigurationDescriptor(
                                            descriptor.name(),
                                            descriptor.description(),
                                            descriptor.tags(),
                                            plugin,
                                            config,
                                            configDescriptor,
                                            conflicts);
                                })
                )
                .map(desc ->
                {
                    ProjectXPluginListItem listItem = new ProjectXPluginListItem(this, desc);
                    listItem.setPinned(pinnedPlugins.contains(desc.getName()));
                    return listItem;
                })
                .sorted(Comparator.comparing(p -> p.getPluginConfig().getName()))
                .collect(Collectors.toList());

        mainPanel.removeAll();
        refresh();
    }

    public void addFakePlugin(ProjectXPluginConfigurationDescriptor... descriptor) {
        Collections.addAll(fakePlugins, descriptor);
    }

    void refresh() {
        // update enabled / disabled status of all items
        pluginList.forEach(listItem ->
        {
            final Plugin plugin = listItem.getPluginConfig().getPlugin();
            if (plugin != null) {
                listItem.setPluginEnabled(pluginManager.isPluginEnabled(plugin));
            }
        });

        int scrollBarPosition = scrollPane.getVerticalScrollBar().getValue();

        onSearchBarChanged();
        searchBar.requestFocusInWindow();
        validate();

        scrollPane.getVerticalScrollBar().setValue(scrollBarPosition);
    }

    void openWithFilter(String filter) {
        searchBar.setText(filter);
        onSearchBarChanged();
        muxer.pushState(this);
    }

    private void onSearchBarChanged() {
        final String text = searchBar.getText();
        pluginList.forEach(mainPanel::remove);
        ProjectXPluginSearch.search(pluginList, text).forEach(mainPanel::add);
        revalidate();
    }

    void openConfigurationPanel(String configGroup) {
        for (ProjectXPluginListItem pluginListItem : pluginList) {
            if (pluginListItem.getPluginConfig().getName().equals(configGroup)) {
                openConfigurationPanel(pluginListItem.getPluginConfig());
                break;
            }
        }
    }

    void openConfigurationPanel(Plugin plugin) {
        for (ProjectXPluginListItem pluginListItem : pluginList) {
            if (pluginListItem.getPluginConfig().getPlugin() == plugin) {
                openConfigurationPanel(pluginListItem.getPluginConfig());
                break;
            }
        }
    }

    void openConfigurationPanel(ProjectXPluginConfigurationDescriptor plugin) {
        ProjectXConfigPanel panel = configPanelProvider.get();
        panel.init(plugin);
        muxer.pushState(this);
        muxer.pushState(panel);
    }

    void startPlugin(Plugin plugin) {
        projectxPluginManager.getOutdatedPluginUpdate(plugin)
                .ifPresent(this::showOutdatedPluginNotification);

        pluginManager.setPluginEnabled(plugin, true);

        try {
            pluginManager.startPlugin(plugin);
        } catch (PluginInstantiationException ex) {
            log.warn("Error when starting plugin {}", plugin.getClass().getSimpleName(), ex);
        }
    }

    void stopPlugin(Plugin plugin) {
        pluginManager.setPluginEnabled(plugin, false);

        try {
            pluginManager.stopPlugin(plugin);
        } catch (PluginInstantiationException ex) {
            log.warn("Error when stopping plugin {}", plugin.getClass().getSimpleName(), ex);
        }
    }

    private List<String> getPinnedPluginNames() {
        final String config = configManager.getConfiguration(RUNELITE_GROUP_NAME, PINNED_PLUGINS_CONFIG_KEY);

        if (config == null) {
            return Collections.emptyList();
        }

        return Text.fromCSV(config);
    }

    private void showOutdatedPluginNotification(ProjectXPluginManager.OutdatedPluginUpdate outdatedPluginUpdate) {
        String message = "<html>\""
                + HtmlEscapers.htmlEscaper().escape(outdatedPluginUpdate.getDisplayName())
                + "\" is out of date.<br><br>Installed version: "
                + HtmlEscapers.htmlEscaper().escape(outdatedPluginUpdate.getInstalledVersion())
                + "<br>Latest version: "
                + HtmlEscapers.htmlEscaper().escape(outdatedPluginUpdate.getLatestVersion())
                + "<br><br>Update it from the <strong>ProjectX Plugin Hub</strong> to get the latest fixes.</html>";

        JOptionPane.showMessageDialog(
                this,
                message,
                "ProjectX Plugin Update Available",
                JOptionPane.INFORMATION_MESSAGE
        );

        projectxPluginManager.rememberOutdatedPluginUpdateNotification(outdatedPluginUpdate);
    }

    void savePinnedPlugins() {
        final String value = pluginList.stream()
                .filter(ProjectXPluginListItem::isPinned)
                .map(p -> p.getPluginConfig().getName())
                .collect(Collectors.joining(","));

        configManager.setConfiguration(RUNELITE_GROUP_NAME, PINNED_PLUGINS_CONFIG_KEY, value);
    }

    @Subscribe
    public void onPluginChanged(PluginChanged event) {
        SwingUtilities.invokeLater(this::refresh);
    }

    @Override public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, 24);
        return d;
    }

    @Override
    public void onActivate() {
        super.onActivate();

        if (searchBar.getParent() != null) {
            searchBar.requestFocusInWindow();
        }
    }

    @Subscribe
    private void onExternalPluginsChanged(ExternalPluginsChanged ev) {
        SwingUtilities.invokeLater(this::rebuildPluginList);
    }

    @Subscribe
    private void onProfileChanged(ProfileChanged ev) {
        SwingUtilities.invokeLater(this::rebuildPluginList);
    }
}
