/*
 * Copyright (c) 2023 ProjectX
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
package net.runelite.client.plugins.projectx.externalplugins;

import com.fasterxml.jackson.databind.annotation.NoClass;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.google.common.io.Files;
import com.google.common.reflect.ClassPath;
import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.CreationException;
import com.google.inject.Injector;
import com.google.inject.Module;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ExternalPluginsChanged;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.projectx.ProjectXApi;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import net.runelite.client.plugins.projectx.util.misc.Rs2UiHelper;
import net.runelite.client.ui.SplashScreen;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ProjectXPluginManager {
    private static final File PLUGIN_DIR = new File(RuneLite.RUNELITE_DIR, "projectx-plugins");
    private static final String INSTALLED_VERSION_GROUP = "projectxPluginVersions";
    private static final String INSTALLED_VERSION_KEY_PREFIX = "plugin.";
    private static final String UPDATE_NOTIFICATION_GROUP = "projectxPluginUpdateNotifications";
    private static final String UPDATE_NOTIFICATION_KEY_PREFIX = "plugin.";

    /** Long enough for start-up to finish before the first reconciliation. */
    private static final long RECONCILE_DELAY_SECONDS = 30;
    /** How often a purchase made while the client is running is noticed. */
    private static final long RECONCILE_INTERVAL_SECONDS = 5 * 60;

    private final OkHttpClient okHttpClient;
    private final ProjectXPluginClient projectxPluginClient;
    private final EventBus eventBus;
    private final ScheduledExecutorService executor;
    private final PluginManager pluginManager;
    private final Gson gson;
    private final ConfigManager configManager;
    private final ProjectXApi projectxApi;
    private final ProjectXEntitlements entitlements;
    private final ProjectXAccount account;

    private final Map<String, URLClassLoader> loaders = new ConcurrentHashMap<>();

    /**
     * Jars that downloaded cleanly but yielded no usable plugin, against the
     * fingerprint of the jar that failed.
     *
     * <p>Without this the refresh loop is unbounded. A plugin is considered
     * loaded by looking for it among the registered plugins, so a jar
     * containing no {@code @PluginDescriptor(isExternal = true)} class -- a
     * broken build, a placeholder, a jar whose classes will not link against
     * this client -- never appears there, is therefore still "missing" on the
     * next pass, and is fetched and retried forever. That is a request every
     * few seconds, from every client that has the plugin installed, for as long
     * as the client runs.
     *
     * <p>Keyed by fingerprint rather than by name so that publishing a fixed
     * build clears the memo by itself: the new jar has a different hash, does
     * not match the recorded failure, and is tried again. Only the exact bytes
     * already known not to work are skipped.
     */
    private final Map<String, String> unloadableJars = new ConcurrentHashMap<>();

    @Inject
    @Named("safeMode")
    private boolean safeMode;

    private final Map<String, ProjectXPluginManifest> manifestMap = new ConcurrentHashMap<>();

    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private volatile boolean profileRefreshInProgress = false;
    private static final String PLUGIN_PACKAGE = "net.runelite.client.plugins.projectx";

    @Inject
    private ProjectXPluginManager(
            OkHttpClient okHttpClient,
            ProjectXPluginClient projectxPluginClient,
            EventBus eventBus,
            ScheduledExecutorService executor,
            PluginManager pluginManager,
            Gson gson,
            ConfigManager configManager,
            ProjectXApi projectxApi,
            ProjectXEntitlements entitlements,
            ProjectXAccount account
    ) {
        this.okHttpClient = okHttpClient;
        this.projectxPluginClient = projectxPluginClient;
        this.eventBus = eventBus;
        this.executor = executor;
        this.pluginManager = pluginManager;
        this.gson = gson;
        this.configManager = configManager;
        this.projectxApi = projectxApi;
        this.entitlements = entitlements;
        this.account = account;

        PLUGIN_DIR.mkdirs();
    }

    /**
     * Initializes the ProjectXPluginManager
     */
    public void init() {
        /*
         * Who is signed in has to be settled before the plugins load: the gate
         * filters classes out of that load, and a role that arrived afterwards
         * would be too late to hide anything. One request, and a failure leaves
         * the user as a plain USER.
         */
        account.refresh();
        ProjectXDeveloperGate.bind(account);

        loadManifest();

        /*
         * Nothing used to bring a purchase down on its own.
         *
         * A script bought on the website only ever reached the client when the
         * user opened the plugin hub and pressed Install: refresh() reconciles
         * against the plugins already loaded, so it could never fetch one that
         * was not there yet, and nothing called it in any case. The first delay
         * lets start-up finish -- entitlements are started just after this -- and
         * the interval is what makes a purchase arrive in a running client.
         */
        executor.scheduleWithFixedDelay(
            this::reconcile, RECONCILE_DELAY_SECONDS, RECONCILE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Brings this machine into line with what the account is entitled to.
     *
     * <p>Deliberately tolerant: every step is safe to repeat, and a failure in
     * one pass is retried by the next rather than reported to the user, because
     * the ordinary cause is a network that came back a minute later.
     */
    private void reconcile() {
        if (safeMode || isShuttingDown.get()) {
            return;
        }

        try {
            account.refreshIfStale();
            entitlements.refresh();
            installEntitled();
            updateOutdated();
            refresh();
        } catch (Exception e) {
            log.warn("Could not reconcile installed plugins: {}", e.getMessage());
            log.debug("Reconciliation error", e);
        }
    }

    /**
     * Installs anything the account has paid for and this machine does not have.
     *
     * <p>Only paid scripts, and only entitled ones. The free catalogue is browsed
     * and chosen from; a purchase is an instruction, so it is acted on.
     */
    private void installEntitled() {
        for (ProjectXPluginManifest manifest : manifestMap.values()) {
            String internalName = manifest.getInternalName();
            if (internalName == null || internalName.isEmpty() || manifest.isDisable()) {
                continue;
            }
            if (!entitlements.isPaid(internalName) || !entitlements.isEntitled(internalName)) {
                continue;
            }
            if (isKnownUnloadable(internalName) || loadedExternalNames().contains(internalName)) {
                continue;
            }
            if (getPluginJarFile(internalName).exists()) {
                // Present but not loaded: a load attempt, not another download.
                loadSideLoadPlugin(internalName);
                continue;
            }

            log.info("Installing {}, which this account has access to", internalName);
            install(manifest, null);
        }
    }

    /**
     * Moves installed plugins onto the build the site is currently publishing.
     *
     * <p>Without this an author cannot reach the people already running their
     * script. A new version is reviewed, approved and published, and every
     * client that already had the old one kept it: the periodic refresh asks for
     * the version it already has, and the only thing that ever offered an
     * upgrade was a dialog shown when the user happened to start the plugin by
     * hand, telling them to go and update it themselves. A fix nobody receives
     * is not a fix.
     *
     * <p>A paid script still has to be paid for -- an expired subscription is
     * left on what it has rather than handed a newer build.
     */
    private void updateOutdated() {
        for (ProjectXPluginManifest manifest : manifestMap.values()) {
            String internalName = manifest.getInternalName();
            if (internalName == null || internalName.isEmpty() || manifest.isDisable()) {
                continue;
            }

            String latest = manifest.getVersion();
            if (Strings.isNullOrEmpty(latest) || !getPluginJarFile(internalName).exists()) {
                continue;
            }
            if (isKnownUnloadable(internalName)) {
                continue;
            }
            if (entitlements.isPaid(internalName) && !entitlements.isEntitled(internalName)) {
                continue;
            }

            String installed = getInstalledPluginVersion(internalName).orElse(null);
            if (installed == null || latest.equals(installed)) {
                continue;
            }

            log.info("Updating {} from {} to {}", internalName, installed, latest);

            /*
             * Fetch before disturbing anything. downloadPlugin reads the whole
             * body into memory before it touches the file, so a fetch that fails
             * leaves the working version exactly where it was -- whereas the
             * obvious remove()-then-install() would have deleted it first and
             * left the user with nothing to run.
             */
            if (!downloadPlugin(internalName, null)) {
                log.warn("Could not fetch {} {}; staying on {}", internalName, latest, installed);
                continue;
            }

            unloadPlugin(internalName);
            loadSideLoadPlugin(internalName);
            eventBus.post(new ExternalPluginsChanged());
        }
    }

    /**
     * Loads the plugin manifest list from the remote server and updates the local manifest map.
     * If the manifest has changed, posts an ExternalPluginsChanged event.
     */
    private void loadManifest() {
        try {
            List<ProjectXPluginManifest> manifests = projectxPluginClient.downloadManifest();
            Map<String, ProjectXPluginManifest> next = new HashMap<>(manifests.size());

            com.google.gson.JsonArray allReleases = null;
            try {
                allReleases = projectxPluginClient.fetchAllReleases();
                log.debug("Fetched {} releases from GitHub", allReleases.size());
            } catch (IOException ex) {
                log.warn("Failed to fetch GitHub releases: {}", ex.getMessage());
                log.debug("Releases fetch error", ex);
            }

            entitlements.updatePaidPlugins(manifests);

            for (ProjectXPluginManifest m : manifests) {
                next.put(m.getInternalName(), m);
                if (allReleases != null) {
                    projectxPluginClient.findFirstAssetCreatedAt(m, allReleases)
                            .ifPresent(m::setAddedAt);

                    try {
                        List<String> versions = projectxPluginClient.parseVersionsFromReleases(m, allReleases);
                        m.setAvailableVersions(versions);
                    } catch (IOException ex) {
                        log.warn("Failed to parse available versions for {}: {}", m.getInternalName(), ex.getMessage());
                        log.debug("Version parse error", ex);
                    }
                }
            }
            boolean changed = !next.keySet().equals(manifestMap.keySet())
                    || next.entrySet().stream().anyMatch(e -> {
                ProjectXPluginManifest cur = manifestMap.get(e.getKey());
                return cur == null || !Objects.equals(cur.getSha256(), e.getValue().getSha256());
            });
            if (changed) {
                manifestMap.clear();
                manifestMap.putAll(next);
                log.info("Loaded {} plugin manifests.", manifestMap.size());
                eventBus.post(new ExternalPluginsChanged());
            } else {
                log.debug("Plugin manifests unchanged ({} entries), skipping event.", manifestMap.size());
            }
        } catch (Exception e) {
            log.error("Failed to fetch plugin manifests", e);
        }
    }

    public Map<String, ProjectXPluginManifest> getManifestMap() {
        return Collections.unmodifiableMap(manifestMap);
    }

    /**
     * Gets the File object for the plugin JAR file corresponding to the given internal name.
     *
     * @param internalName the internal name of the plugin
     * @return the File object representing the plugin JAR
     */
    private File getPluginJarFile(String internalName) {
        return new File(PLUGIN_DIR, internalName + ".jar");
    }

    /**
     * Gets the plugin manifest for a given plugin instance.
     *
     * @param internalName
     * @return
     */
    private byte[] getPluginJarByteArray(String internalName) {
        return new File(PLUGIN_DIR, internalName + ".jar").getPath().getBytes();
    }

    /**
     * Creates an OkHttpClient instance that does not use any proxy settings.
     *
     * @param base
     * @return
     */
    private static OkHttpClient noProxy(OkHttpClient base) {
        return base.newBuilder()
                .proxy(Proxy.NO_PROXY)
                .proxySelector(ProxySelector.of(null))
                .build();
    }

    /**
     * Verifies that the SHA-256 hash of a locally installed plugin matches the
     * authoritative hash from the manifest map.
     * <p>
     * This ensures the integrity of the plugin and detects tampering or corruption.
     *
     * @param internalName the internal name of the plugin to verify (must not be null or empty)
     * @return {@code true} if the plugin exists in both the local and authoritative manifests
     * and the SHA-256 hashes match, {@code false} otherwise
     * @throws IllegalArgumentException if {@code internalName} is null or empty
     */
    private boolean verifyHash(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            throw new IllegalArgumentException("Internal name is null/empty");
        }

        ProjectXPluginManifest authoritativeManifest = manifestMap.get(internalName);
        InstalledPluginVersion storedVersion = lookupInstalledPluginVersion(internalName).orElse(null);

        String localHash = calculateHash(internalName);
        String authoritativeHash = storedVersion != null ? storedVersion.getSha256()
                : authoritativeManifest != null ? authoritativeManifest.getSha256() : null;

        if (Strings.isNullOrEmpty(localHash) || Strings.isNullOrEmpty(authoritativeHash)) {
            return false;
        }

        var result = localHash.equals(authoritativeHash);

        if (!result) {
            log.warn("Hash mismatch for plugin {}: local={}, authoritative={}",
                    internalName, localHash, authoritativeHash);
        }
        return result;
    }

    /**
     * Makes sure the jar on disk is the one the site published, and refuses to
     * load it otherwise.
     *
     * <p>Previously a failed hash check logged a warning and loaded the plugin
     * anyway, which made the published SHA-256 decorative: a swapped or
     * tampered jar ran exactly like the reviewed one. For a marketplace whose
     * whole promise is "the code we read is the code you run", that was the
     * one place that promise had to be kept, and was not.
     *
     * <p>A mismatch is usually a stale local copy rather than an attack -- the
     * plugin was updated and this machine still has the old jar -- so the jar
     * is discarded and fetched again before any conclusion is drawn. Only a
     * freshly downloaded jar that still disagrees is treated as untrustworthy.
     *
     * @return true when the plugin may be loaded
     */
    private boolean ensureIntegrity(String internalName) {
        if (verifyHash(internalName)) {
            return true;
        }

        ProjectXPluginManifest manifest = manifestMap.get(internalName);
        if (manifest == null) {
            // Nothing to check against: a side-loaded jar the site has never
            // heard of. Left to the existing side-load path rather than blocked,
            // since nobody published a hash to disagree with.
            log.debug("No manifest for {}; skipping integrity check", internalName);
            return true;
        }

        log.warn("Plugin {} does not match its published hash; re-downloading", internalName);

        File pluginFile = getPluginJarFile(internalName);
        if (pluginFile.exists() && !pluginFile.delete()) {
            log.error("Could not remove the mismatched jar for {}; refusing to load it", internalName);
            return false;
        }
        // The stored record is about to be wrong either way, and leaving it
        // would let the re-downloaded jar be checked against a stale hash.
        clearInstalledPluginVersion(internalName);

        if (!downloadPlugin(internalName, null)) {
            log.error("Could not re-download {}; refusing to load it", internalName);
            return false;
        }

        if (!verifyHash(internalName)) {
            log.error(
                "Plugin {} still does not match its published hash after a fresh download. "
                    + "Refusing to load it -- this is a corrupted or tampered jar.",
                internalName);
            return false;
        }

        log.info("Plugin {} re-downloaded and verified", internalName);
        return true;
    }

    /**
     * Calculates the SHA-256 hash of the given JAR data.
     *
     * @param interalName
     * @return
     */
    private String calculateHash(String interalName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            File file = getPluginJarFile(interalName);
            try (InputStream inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error calculating plugin hash", e);
            return "";
        }
    }

    public static File[] createSideloadingFolder() {
        try {
            Files.createParentDirs(PLUGIN_DIR);

            if (!PLUGIN_DIR.exists() && PLUGIN_DIR.mkdir()) {
                log.debug("Directory for sideloading was created successfully.");
            }
        } catch (IOException e) {
            log.trace("Error creating directory for projectx-plugins!", e);
        }

        return PLUGIN_DIR.listFiles();
    }

    /** The internal names of the external plugins currently registered. */
    private Set<String> loadedExternalNames() {
        return pluginManager.getPlugins().stream()
                .filter(p -> p.getClass().isAnnotationPresent(PluginDescriptor.class))
                .filter(p -> p.getClass().getAnnotation(PluginDescriptor.class).isExternal())
                .map(p -> p.getClass().getSimpleName())
                .collect(Collectors.toSet());
    }

    /**
     * What identifies the current bytes of a plugin, for the unloadable memo.
     *
     * <p>The published hash is preferred over the local one: it is what changes
     * when the author releases a fix, and it is known before the jar is even
     * fetched.
     */
    @Nullable
    private String jarFingerprint(String internalName) {
        ProjectXPluginManifest manifest = manifestMap.get(internalName);
        if (manifest != null && !Strings.isNullOrEmpty(manifest.getSha256())) {
            return manifest.getSha256();
        }
        String local = calculateHash(internalName);
        return Strings.isNullOrEmpty(local) ? null : local;
    }

    /** True when this exact jar has already been tried and produced no plugin. */
    private boolean isKnownUnloadable(String internalName) {
        String recorded = unloadableJars.get(internalName);
        if (recorded == null) {
            return false;
        }
        if (recorded.equals(jarFingerprint(internalName))) {
            return true;
        }
        // Different bytes: a new build deserves a fresh attempt.
        unloadableJars.remove(internalName);
        return false;
    }

    /**
     * Loads a single plugin from the sideload folder if not already loaded.
     */
    private void loadSideLoadPlugin(String internalName) {
        File pluginFile = getPluginJarFile(internalName);
        if (!pluginFile.exists()) {
            log.debug("Plugin file {} does not exist", pluginFile);
            return;
        }
        if (isKnownUnloadable(internalName)) {
            log.debug("Skipping {}: this build has already failed to load", internalName);
            return;
        }
        Set<String> loadedInternalNames = loadedExternalNames();
        if (loadedInternalNames.contains(internalName)) {
            return;
        }
        try {
            if (!ensureIntegrity(internalName)) {
                return;
            }
            List<Class<?>> plugins = new ArrayList<>();
            PluginJarClassLoader classLoader = new PluginJarClassLoader(pluginFile, getClass().getClassLoader());
            loaders.put(internalName, classLoader);
            for (ClassPath.ClassInfo classInfo : ClassPath.from(classLoader).getAllClasses()) {
                try {
                    Class<?> clazz = classLoader.loadClass(classInfo.getName());
                    plugins.add(clazz);
                } catch (ClassNotFoundException e) {
                    log.trace("Class not found during sideloading: {}", classInfo.getName(), e);
                } catch(Throwable t) {
                    log.error("Incompatible plugin found: " + internalName);
                }
            }
            loadPlugins(plugins, null);

            if (!loadedExternalNames().contains(internalName)) {
                String fingerprint = jarFingerprint(internalName);
                if (fingerprint != null) {
                    unloadableJars.put(internalName, fingerprint);
                }
                log.warn(
                    "{} downloaded but registered no plugin -- it has no external "
                        + "@PluginDescriptor class, or its classes could not be linked. "
                        + "Not retrying this build.",
                    internalName);
            }
        } catch (PluginInstantiationException | IOException e) {
            log.trace("Error loading side-loaded plugin!", e);
        }
    }

    public void loadSideLoadPlugins() {
        if (safeMode) {
            log.warn("Safe mode is enabled, skipping loading of sideloaded plugins.");
            return;
        }
        File[] files = createSideloadingFolder();
        if (files == null) {
            return;
        }
        Set<String> loadedInternalNames = pluginManager.getPlugins().stream()
                .filter(p -> p.getClass().isAnnotationPresent(PluginDescriptor.class))
                .filter(p -> p.getClass().getAnnotation(PluginDescriptor.class).isExternal())
                .map(p -> p.getClass().getSimpleName())
                .collect(Collectors.toSet());
        for (File f : files) {
            if (!f.getName().endsWith(".jar")) {
                continue;
            }
            String internalName = f.getName().replace(".jar", "");
            if (loadedInternalNames.contains(internalName)) {
                continue;
            }
            try {
                loadSideLoadPlugin(internalName);
            } catch (Exception exception) {
                System.out.println("Error loading side-loaded plugin: " + internalName);
            }
        }
        eventBus.post(new ExternalPluginsChanged());
    }

    /**
     * Topologically sort a graph. Uses Kahn's algorithm.
     *
     * @param graph - A directed graph
     * @param <T>   - The type of the item contained in the nodes of the graph
     * @return - A topologically sorted list corresponding to graph.
     * <p>
     * Multiple invocations with the same arguments may return lists that are not equal.
     */
    @VisibleForTesting
    static <T> List<T> topologicalSort(Graph<T> graph) {
        MutableGraph<T> graphCopy = Graphs.copyOf(graph);
        List<T> l = new ArrayList<>();
        Set<T> s = graphCopy.nodes().stream()
                .filter(node -> graphCopy.inDegree(node) == 0)
                .collect(Collectors.toSet());
        while (!s.isEmpty()) {
            Iterator<T> it = s.iterator();
            T n = it.next();
            it.remove();

            l.add(n);

            for (T m : new HashSet<>(graphCopy.successors(n))) {
                graphCopy.removeEdge(n, m);
                if (graphCopy.inDegree(m) == 0) {
                    s.add(m);
                }
            }
        }
        if (!graphCopy.edges().isEmpty()) {
            throw new RuntimeException("Graph has at least one cycle");
        }
        return l;
    }

    private List<Plugin> loadPlugins(List<Class<?>> plugins, BiConsumer<Integer, Integer> onPluginLoaded) throws PluginInstantiationException {
        MutableGraph<Class<? extends Plugin>> graph = GraphBuilder
                .directed()
                .build();

        Set<Class<?>> alreadyLoaded = pluginManager.getPlugins().stream()
                .map(Object::getClass)
                .collect(Collectors.toSet());

        for (Class<?> clazz : plugins) {
            if (alreadyLoaded.contains(clazz)) {
                log.debug("Plugin {} is already loaded, skipping duplicate.", clazz.getSimpleName());
                continue;
            }
            PluginDescriptor pluginDescriptor = clazz.getAnnotation(PluginDescriptor.class);

            if (pluginDescriptor == null) {
                if (clazz.getSuperclass() == Plugin.class) {
                    log.error("Class {} is a plugin, but has no plugin descriptor", clazz);
                }
                continue;
            }

            if (clazz.getSuperclass() != Plugin.class) {
                log.error("Class {} has plugin descriptor, but is not a plugin", clazz);
                continue;
            }

            if (pluginDescriptor.isExternal() && !Rs2UiHelper.isClientVersionCompatible(pluginDescriptor.minClientVersion())) {
                log.error("Plugin {} needs plugin API {} or higher; this client provides {}. Skipping plugin loading.",
                        clazz.getSimpleName(), pluginDescriptor.minClientVersion(), RuneLiteProperties.getProjectXPluginApiVersion());
                continue;
            }

            if (pluginDescriptor.disable()) {
                log.error("Plugin {} has been disabled upstream", clazz.getSimpleName());
                continue;
            }

            graph.addNode((Class<Plugin>) clazz);
        }

        for (Class<? extends Plugin> pluginClazz : graph.nodes()) {
            PluginDependency[] pluginDependencies = pluginClazz.getAnnotationsByType(PluginDependency.class);

            for (PluginDependency pluginDependency : pluginDependencies) {
                if (graph.nodes().contains(pluginDependency.value())) {
                    graph.putEdge(pluginDependency.value(), pluginClazz);
                }
            }
        }

        if (Graphs.hasCycle(graph)) {
            throw new PluginInstantiationException("Plugin dependency graph contains a cycle!");
        }

        List<Class<? extends Plugin>> sortedPlugins = topologicalSort(graph);

        int loaded = 0;
        List<Plugin> newPlugins = new ArrayList<>();
        for (Class<? extends Plugin> pluginClazz : sortedPlugins) {
            Plugin plugin;
            try {
                plugin = instantiate(pluginManager.getPlugins(), (Class<Plugin>) pluginClazz);
                log.info("Plugin loaded {}", plugin.getClass().getSimpleName());
                newPlugins.add(plugin);
                pluginManager.addPlugin(plugin);
                loaded++;
            } catch (PluginInstantiationException ex) {
                log.error("Error instantiating plugin!", ex);
            }

            if (onPluginLoaded != null) {
                onPluginLoaded.accept(loaded, sortedPlugins.size());
            }
        }

        return newPlugins;
    }

    private Plugin instantiate(Collection<Plugin> scannedPlugins, Class<Plugin> clazz) throws PluginInstantiationException {
        PluginDependency[] pluginDependencies = clazz.getAnnotationsByType(PluginDependency.class);
        List<Plugin> deps = new ArrayList<>();
        for (PluginDependency pluginDependency : pluginDependencies) {
            Optional<Plugin> dependency = scannedPlugins.stream().filter(p -> p.getClass() == pluginDependency.value()).findFirst();
            if (!dependency.isPresent()) {
                throw new PluginInstantiationException("Unmet dependency for " + clazz.getSimpleName() + ": " + pluginDependency.value().getSimpleName());
            }
            deps.add(dependency.get());
        }

        Plugin plugin;
        try {
            plugin = clazz.getDeclaredConstructor().newInstance();
        } catch (ThreadDeath e) {
            throw e;
        } catch (Throwable ex) {
            throw new PluginInstantiationException(ex);
        }

        try {
            Injector parent = ProjectX.getInjector();

            if (deps.size() > 1) {
                List<com.google.inject.Module> modules = new ArrayList<>(deps.size());
                for (Plugin p : deps) {
                    com.google.inject.Module module = (Binder binder) ->
                    {
                        binder.bind((Class<Plugin>) p.getClass()).toInstance(p);
                        binder.install(p);
                    };
                    modules.add(module);
                }

                parent = parent.createChildInjector(modules);
            } else if (!deps.isEmpty()) {
                parent = deps.get(0).getInjector();
            }

            Module pluginModule = (Binder binder) ->
            {
                binder.bind(clazz).toInstance(plugin);
                binder.install(plugin);
            };
            Injector pluginInjector = parent.createChildInjector(pluginModule);
            System.out.println(pluginInjector.getClass().getSimpleName());
            plugin.setInjector(pluginInjector);
        } catch (com.google.common.util.concurrent.ExecutionError e) {
            // Guice/Guava wraps NoClassDefFoundError here
            Throwable cause = e.getCause();
            if (cause instanceof NoClassDefFoundError) {
                log.error("Missing class while loading plugin {}: {}", clazz.getSimpleName(), cause.toString());
            } else {
                log.error("Error while loading plugin {}: {}", clazz.getSimpleName(), e.toString(), e);
            }

            File jar = getPluginJarFile(plugin.getClass().getSimpleName());
            if (jar != null) {
                jar.delete();
            }
        } catch (Exception ex) {
            log.error("Incompatible plugin found: " + clazz.getSimpleName());
            File jar = getPluginJarFile(plugin.getClass().getSimpleName());
            jar.delete();
        }

        log.debug("Loaded plugin {}", clazz.getSimpleName());
        return plugin;
    }

    /**
     * Determines if a class is a ProjectX-related Plugin that should be loaded.
     * This includes plugins from utility packages, UI components, and specific ProjectX systems.
     *
     * @param clazz the class to check
     * @return true if the class should be included in ProjectX plugin loading
     */
    private static boolean isProjectXRelatedPlugin(Class<?> clazz) {
        if (clazz == null || clazz.getPackage() == null) {
            return false;
        }

        if (!Plugin.class.isAssignableFrom(clazz) || clazz == Plugin.class) {
            return false;
        }

        PluginDescriptor descriptor = clazz.getAnnotation(PluginDescriptor.class);
        if (descriptor == null) {
            return false;
        }

        String pkg = clazz.getPackage().getName();

        if (pkg.startsWith(PLUGIN_PACKAGE)) {
            return pkg.equals(PLUGIN_PACKAGE)
                    || pkg.contains(".ui")
                    || pkg.contains(".util")
                    || pkg.contains(".shortestpath")
                    || pkg.contains(".questhelper")
                    || pkg.contains("inventorysetups")
                    || pkg.contains("breakhandler");
        }

        return false;
    }

    /**
     * Scans the classpath for ProjectX-related Plugin classes and returns them.
     * This includes plugin classes from utility packages, UI components, and specific ProjectX systems.
     *
     * @return list of ProjectX-related Plugin classes found on the classpath
     */
    private List<Class<?>> scanForProjectXPlugins() {
        List<Class<?>> projectxPlugins = new ArrayList<>();

        try {
            ClassPath classPath = ClassPath.from(getClass().getClassLoader());

            for (ClassPath.ClassInfo classInfo : classPath.getAllClasses()) {
                if (!classInfo.getPackageName().startsWith(PLUGIN_PACKAGE)) {
                    continue;
                }

                try {
                    Class<?> clazz = classInfo.load();
                    if (isProjectXRelatedPlugin(clazz)) {
                        projectxPlugins.add(clazz);
                        log.debug("Found ProjectX plugin class: {}", clazz.getName());
                    }
                } catch (Throwable e) {
                    log.trace("Could not load class during ProjectX scan: {}", classInfo.getName(), e);
                }
            }

            log.info("Found {} additional ProjectX plugin classes during classpath scan", projectxPlugins.size());
        } catch (IOException e) {
            log.error("Failed to scan classpath for ProjectX plugin classes", e);
        }

        return projectxPlugins;
    }

    public void loadCorePlugins(List<Class<?>> plugins) throws PluginInstantiationException {
        SplashScreen.stage(.59, null, "Loading plugins");
        List<Class<?>> combinedPlugins = new ArrayList<>(plugins);

        List<Class<?>> additionalProjectXPlugins = scanForProjectXPlugins();

        Set<Class<?>> existingPlugins = new HashSet<>(plugins);

        List<Class<?>> newProjectXPlugins = additionalProjectXPlugins.stream()
                .filter(clazz -> !existingPlugins.contains(clazz))
                .collect(Collectors.toList());

        combinedPlugins.addAll(newProjectXPlugins);

        log.info("Loading core plugins: {} passed in + {} core ProjectX plugins = {} total",
                plugins.size(), newProjectXPlugins.size(), combinedPlugins.size());

        if (!combinedPlugins.isEmpty()) {
            loadPlugins(combinedPlugins, (loaded, total) ->
                    SplashScreen.stage(.60, .70, null, "Loading Project X plugins", loaded, total, false));
        }
    }

    @Subscribe
    public void onClientShutdown(ClientShutdown shutdown) {
        log.info("Client shutdown detected, stopping all ProjectX plugins");
        shutdown();
    }

    /**
     * Refreshes plugins
     */
    private void refresh() {
        if (safeMode) {
            log.warn("Safe mode is enabled, skipping loading of sideloaded plugins.");
            return;
        }

        if (isShuttingDown.get()) {
            return;
        }

        synchronized (this) {
            if (profileRefreshInProgress) {
                return;
            }
            profileRefreshInProgress = true;
        }

        try {
            log.debug("Starting plugin refresh");

            List<Plugin> installedPlugins = getInstalledPlugins();

            List<Plugin> disabledPlugins = installedPlugins.stream()
                    .filter(plugin -> {
                        ProjectXPluginManifest upstreamManifest = manifestMap.get(plugin.getClass().getSimpleName());
                        return upstreamManifest != null && upstreamManifest.isDisable();
                    })
                    .collect(Collectors.toList());

            if (!disabledPlugins.isEmpty()) {
                log.warn("Found {} disabled plugin(s) that have been disabled upstream:", disabledPlugins.size());
                for (Plugin disabledPlugin : disabledPlugins) {
                    log.warn("  - Plugin '{}' has been disabled upstream and will be removed from your installed plugins",
                            disabledPlugin.getClass().getSimpleName());
                }

                List<Plugin> enabledPlugins = installedPlugins.stream()
                        .filter(plugin -> {
                            ProjectXPluginManifest upstreamManifest = manifestMap.get(plugin.getClass().getSimpleName());
                            return upstreamManifest == null || !upstreamManifest.isDisable();
                        })
                        .collect(Collectors.toList());

                installedPlugins = enabledPlugins;

                log.info("Automatically removed {} disabled plugin(s) from your installed plugins list", disabledPlugins.size());
            }

            Set<String> installedNames = installedPlugins.stream()
                    .map(plugin -> plugin.getClass().getSimpleName())
                    .collect(Collectors.toSet());

            List<Plugin> allLoadedPlugins = new ArrayList<>(pluginManager.getPlugins());

            List<Plugin> loadedExternalPlugins = allLoadedPlugins.stream()
                    .filter(plugin -> {
                        PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
                        if (descriptor == null || !descriptor.isExternal()) {
                            return false;
                        }
                        String packageName = plugin.getClass().getPackage().getName();
                        return packageName.contains("projectx");
                    })
                    .collect(Collectors.toList());

            Set<String> loadedByInternalName = loadedExternalPlugins.stream()
                    .map(this::getPluginManifest) // your helper
                    .filter(Objects::nonNull)
                    .map(ProjectXPluginManifest::getInternalName)
                    .collect(Collectors.toSet());

            log.info("Profile refresh - Installed plugins: {}, Currently loaded ProjectX plugins: {}",
                    installedNames, loadedByInternalName);

            log.debug("All loaded plugins ({}):", allLoadedPlugins.size());
            for (Plugin plugin : allLoadedPlugins) {
                PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
                boolean isExternal = descriptor != null && descriptor.isExternal();
                ProjectXPluginManifest manifest = getPluginManifest(plugin);
                log.debug("  - {} (external: {}, has manifest: {})",
                        plugin.getClass().getSimpleName(), isExternal, manifest != null);
            }

            // TODO: current code will redownload all plugins, because manifestMap is a manifest of all the plugins on the hub
            // we need to filter it down to only the ones the user has installed
            // what i think we need to do:
            // create a filtered down list of manifestmap that contains only  the installed plugins
            // add your "install" plugin to this list, then use this list here
            var userManifestMap = manifestMap.entrySet().stream()
                    .filter(e -> installedNames.contains(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


            /*
             * A build already known not to load is left alone entirely -- not
             * fetched, not retried. Downloading it again would cost the site a
             * request every refresh and still produce nothing.
             */
            Set<String> needsDownload = userManifestMap.keySet().stream()
                    .filter(name -> !isKnownUnloadable(name))
                    .filter(projectxPluginManifest -> !getPluginJarFile(projectxPluginManifest).exists())
                    .collect(Collectors.toSet());

            Set<String> needsRedownload = userManifestMap.keySet().stream()
                    .filter(name -> !isKnownUnloadable(name))
                    .filter(pluginName -> {
                        File pluginFile = getPluginJarFile(pluginName);
                        if (!pluginFile.exists()) {
                            return false;
                        }
                        if (!verifyHash(pluginName)) {
                            log.info("Hash verification failed for plugin: {}. Marking for redownload.", pluginName);
                            if (pluginFile.delete()) {
                                log.info("Deleted outdated plugin file: {}", pluginFile.getClass().getSimpleName());
                            } else {
                                log.warn("Failed to delete outdated plugin file: {}", pluginFile.getAbsolutePath());
                            }
                            return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toSet());

            needsDownload.addAll(needsRedownload);

            Set<String> needsReload = new HashSet<>(needsDownload);

            Set<File> keepFiles = userManifestMap.keySet().stream()
                    .map(this::getPluginJarFile)
                    .filter(File::exists)
                    .collect(Collectors.toSet());

            Instant now = Instant.now();
            Instant keepAfter = now.minus(3, ChronoUnit.DAYS);

            Optional.ofNullable(PLUGIN_DIR.listFiles((dir, name) -> name.endsWith(".jar"))).stream()
                    .flatMap(Arrays::stream)
                    .filter(file -> !keepFiles.contains(file) && file.lastModified() < keepAfter.toEpochMilli())
                    .forEach(file -> {
                        log.info("Cleaning up old plugin file (>3 days): {}", file.getName());
                        if (!file.delete()) {
                            log.warn("Failed to delete old plugin file: {}", file.getAbsolutePath());
                        }
                    });

            for (String pluginName : needsDownload) {
                log.info("Downloading missing plugin: {}", pluginName);
                String desiredVersion = getInstalledPluginVersion(pluginName)
                        .orElse(null);
                downloadPlugin(pluginName, desiredVersion);
            }

            Set<String> installedPluginNames = userManifestMap.values().stream()
                    .map(ProjectXPluginManifest::getInternalName)
                    .collect(Collectors.toSet());

            Set<ProjectXPluginManifest> toAdd = userManifestMap.values().stream()
                    .filter(m -> !isKnownUnloadable(m.getInternalName()))
                    .filter(m -> needsReload.contains(m.getInternalName())
                            || !loadedByInternalName.contains(m.getInternalName()))
                    .collect(Collectors.toSet());

            List<Plugin> toRemove = loadedExternalPlugins.stream()
                    .filter(p -> {
                        ProjectXPluginManifest m = getPluginManifest(p);
                        if (m == null) return true; // unknown → remove
                        String name = m.getInternalName();
                        return !installedPluginNames.contains(name) || needsReload.contains(name);
                    })
                    .collect(Collectors.toList());

            log.info("Plugin refresh - Will add: {} plugins, Will remove: {} plugins",
                    toAdd.stream().map(ProjectXPluginManifest::getInternalName).collect(Collectors.toSet()),
                    toRemove.stream().map(p -> p.getClass().getSimpleName()).collect(Collectors.toSet()));

            for (Plugin plugin : toRemove) {
                String simple = plugin.getClass().getSimpleName();
                log.info("Stopping plugin \"{}\"", simple);
                try {
                    SwingUtilities.invokeAndWait(() -> stopPlugin(plugin));
                } catch (InterruptedException | InvocationTargetException e) {
                    log.warn("Failed to stop plugin {}", simple, e);
                }
            }


            for (ProjectXPluginManifest manifest : toAdd) {
                String pluginName = manifest.getInternalName();
                File pluginFile = getPluginJarFile(pluginName);
                if (!pluginFile.exists()) {
                    log.warn("Plugin file missing for {}, skipping load", pluginName);
                    continue;
                }

                log.info("Loading plugin \"{}\"", pluginName);
                List<Plugin> newPlugins = null;
                PluginJarClassLoader classLoader = null;
                try {
                    if (!verifyHash(pluginName)) {
                        log.warn("Plugin hash verification failed for: {}. The installed version may be outdated or from a different source.", pluginName);
                        continue;
                    }

                    List<Class<?>> pluginClasses = new ArrayList<>();
                    classLoader = new PluginJarClassLoader(pluginFile, getClass().getClassLoader());

                    for (ClassPath.ClassInfo classInfo : ClassPath.from(classLoader).getAllClasses()) {
                        try {
                            Class<?> clazz = classLoader.loadClass(classInfo.getName());
                            pluginClasses.add(clazz);
                        } catch (ClassNotFoundException e) {
                            log.trace("Class not found during plugin loading: {}", classInfo.getName(), e);
                        } catch(Throwable t) {
                            log.error("Incompatible plugin found: " + pluginName);
                        }
                    }

                    newPlugins = loadPlugins(pluginClasses, null);

                    boolean startup = SplashScreen.isOpen();
                    if (!startup && !newPlugins.isEmpty()) {
                        pluginManager.loadDefaultPluginConfiguration(newPlugins);
                        final List<Plugin> pluginsToStart = newPlugins;
                        SwingUtilities.invokeAndWait(() -> {
                            try {
                                for (Plugin p : pluginsToStart) {
                                    pluginManager.startPlugin(p);
                                }
                            } catch (PluginInstantiationException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    log.info("Successfully loaded plugin: {}", pluginName);
                } catch (ThreadDeath e) {
                    throw e;
                } catch (Throwable e) {
                    log.warn("Unable to load or start plugin \"{}\"", pluginName, e);
                }
            }

            if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
                eventBus.post(new ExternalPluginsChanged());
            }
            log.info("Completed plugin refresh - Added: {}, Removed: {}", toAdd.size(), toRemove.size());
        } catch (Exception e) {
            log.error("Error during plugin refresh", e);
        } finally {
            profileRefreshInProgress = false;
        }
    }

    /**
     * Downloads a plugin JAR file from the remote server.
     *
     * @param internalName the internal name of the plugin to download
     * @return true if the plugin was successfully downloaded, false otherwise
     */
    private boolean downloadPlugin(String internalName, @Nullable String versionOverride) {
        ProjectXPluginManifest manifest = manifestMap.get(internalName);
        if (manifest == null) {
            log.error("Cannot download plugin {}: manifest not found", internalName);
            return false;
        }

        try {
            File pluginFile = getPluginJarFile(internalName);

            String versionToDownload = !Strings.isNullOrEmpty(versionOverride) ? versionOverride : manifest.getVersion();
            if (Strings.isNullOrEmpty(versionToDownload)) {
                log.error("Cannot determine version to download for {}", internalName);
                return false;
            }

            HttpUrl jarUrl = projectxPluginClient.getJarURL(manifest, versionToDownload);
            if (!ProjectXSite.isAllowedDownload(jarUrl)) {
                log.error("Invalid JAR URL for plugin {}", internalName);
                return false;
            }

            OkHttpClient clientWithoutProxy = noProxy(okHttpClient);
            Request.Builder requestBuilder = new Request.Builder().url(jarUrl);
            // Paid jars need the user's token. It only ever goes to our own site.
            String token = ProjectXAccount.token(configManager);
            if (manifest.isPaid() && ProjectXSite.isSite(jarUrl) && !Strings.isNullOrEmpty(token)) {
                requestBuilder.header("Authorization", "Bearer " + token.trim());
            }
            Request request = requestBuilder.build();

            log.info("from url : " + jarUrl);

            try (Response response = clientWithoutProxy.newCall(request).execute()) {
                if (response.code() == 401 || response.code() == 403) {
                    log.warn("Plugin {} is a marketplace script and needs active access to download", internalName);
                    ProjectX.showMessage(manifest.getDisplayName() + " is a marketplace script.\n\n"
                            + (Strings.isNullOrEmpty(token)
                                ? "Add your Project X API token in the Project X settings, then install it again."
                                : "Buy access at " + manifest.getStoreUrl() + " and then install it again."));
                    return false;
                }
                if (!response.isSuccessful()) {
                    log.error("Failed to download plugin {}: HTTP {}", internalName, response.code());
                    return false;
                }

                byte[] jarData = response.body().bytes();

                Files.write(jarData, pluginFile);
                log.info("Plugin {} (version {}) downloaded to {}", internalName, versionToDownload, pluginFile.getAbsolutePath());

                String authoritativeHash = versionToDownload.equals(manifest.getVersion()) ? manifest.getSha256() : null;
                if (Strings.isNullOrEmpty(authoritativeHash)) {
                    authoritativeHash = calculateHash(internalName);
                }
                if (!Strings.isNullOrEmpty(authoritativeHash)) {
                    rememberInstalledPluginVersion(internalName, versionToDownload, authoritativeHash);
                }

                return true;
            }

        } catch (Exception e) {
            log.error("Failed to download plugin {}", internalName, e);

            File pluginFile = getPluginJarFile(internalName);
            if (pluginFile.exists() && !pluginFile.delete()) {
                log.warn("Failed to delete corrupted plugin file: {}", pluginFile.getAbsolutePath());
            }
            return false;
        }
    }

    /**
     * Installs a plugin and triggers UI refresh.
     *
     * @param manifest the manifest of the plugin to install
     */
    public void installPlugin(ProjectXPluginManifest manifest, @Nullable String versionOverride) {
        executor.submit(() -> install(manifest, versionOverride));
    }

    /**
     * Removes a plugin and triggers UI refresh.
     *
     * @param manifest the manifest of the plugin to remove
     */
    public void removePlugin(ProjectXPluginManifest manifest) {
        executor.submit(() -> remove(manifest));
    }

    /**
     * Installs a plugin by adding it to the installed plugins list in config.
     *
     * @param manifest the manifest of the plugin to install
     */
    public void install(ProjectXPluginManifest manifest, @Nullable String versionOverride) {
        if (manifest == null || !manifestMap.containsValue(manifest)) {
            log.error("Can't install plugin: unable to identify manifest");
            return;
        }

        final String internalName = manifest.getInternalName();
        if (internalName == null || internalName.isEmpty()) {
            log.error("Cannot install plugin: internal name is null or empty");
            return;
        }

        if (manifest.isDisable()) {
            log.warn("Cannot install plugin '{}' ({}): This plugin has been disabled upstream by the developers. " +
                            "This usually means the plugin is no longer functional, has security issues, or has been deprecated.",
                    manifest.getDisplayName(), internalName);
            return;
        }

        /*
         * Installing is reached on every entitlement sync, not just from a
         * click, so a build that cannot load would be fetched again on each
         * pass -- the download the memo is meant to prevent, arriving by a
         * different route.
         */
        if (isKnownUnloadable(internalName)) {
            log.info("Not reinstalling {}: this build has already failed to load", internalName);
            eventBus.post(new ExternalPluginsChanged());
            return;
        }

        var result = downloadPlugin(internalName, versionOverride);
        if (result) {
            //verifiy hash inside loadSidePlugin doesn't work
            loadSideLoadPlugin(internalName);
            sendPluginInstallTelemetry(manifest, versionOverride);
        }

        log.info("Added plugin {} to installed list", manifest.getDisplayName());
        eventBus.post(new ExternalPluginsChanged());
    }

    /**
     * Removes a plugin by removing it from the installed plugins list in config.
     *
     * @param manifest the manifest of the plugin to remove
     */
    /**
     * Stops a side-loaded plugin and releases its classloader, leaving its jar
     * on disk.
     *
     * <p>Separated from {@link #remove} so a plugin can be swapped for a newer
     * build without the old one being deleted first -- which would leave nothing
     * installed if the replacement then failed to arrive.
     *
     * @return false when the plugin is loaded but its classloader is not known,
     *     in which case nothing was unloaded and the old classes are still live.
     */
    private boolean unloadPlugin(String internalName) {
        var pluginToRemove = pluginManager.getPlugins().stream()
                .filter(x -> x.getClass().getSimpleName().equalsIgnoreCase(internalName))
                .findFirst();

        if (pluginToRemove.isEmpty()) {
            log.warn("Plugin to remove not found in plugin manager: {}", internalName);
            return true;
        }

        URLClassLoader cl = loaders.remove(internalName);
        if (cl == null) {
            return false;
        }

        var plugin = pluginToRemove.get();
        try {
            SwingUtilities.invokeAndWait(() ->
            {
                try {
                    pluginManager.stopPlugin(plugin);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        pluginManager.remove(plugin);

        try {
            cl.close();
        } catch (Exception ignored) {
        }

        return true;
    }

    public void remove(ProjectXPluginManifest manifest) {
        if (manifest == null || !manifestMap.containsValue(manifest)) {
            log.error("Can't install plugin: unable to identify manifest");
            return;
        }

        final String internalName = manifest.getInternalName();
        if (internalName == null || internalName.isEmpty()) {
            log.error("Cannot install plugin: internal name is null or empty");
            return;
        }

        if (manifest.isDisable()) {
            log.warn("Cannot install plugin '{}' ({}): This plugin has been disabled upstream by the developers. " +
                            "This usually means the plugin is no longer functional, has security issues, or has been deprecated.",
                    manifest.getDisplayName(), internalName);
            return;
        }

        File jar = getPluginJarFile(internalName);
        if (!unloadPlugin(internalName)) {
            return;
        }

        if (jar.exists() && !jar.delete()) {
            log.warn("Failed to delete plugin jar {}", jar.getAbsolutePath());
        }
        clearInstalledPluginVersion(internalName);

        log.info("Removed plugin {} from installed list", manifest.getDisplayName());
        eventBus.post(new ExternalPluginsChanged());
    }

    private void sendPluginInstallTelemetry(ProjectXPluginManifest manifest, @Nullable String versionOverride)
    {
        if (manifest == null) {
            return;
        }

        String version = Strings.isNullOrEmpty(versionOverride) ? manifest.getVersion() : versionOverride;
        projectxApi.increasePluginInstall(manifest.getInternalName(), manifest.getDisplayName(), version);
    }

    /**
     * Updates a plugin by redownloading and reloading it.
     *
     * @param manifest
     */
    public void update(ProjectXPluginManifest manifest, @Nullable String versionOverride) {
        // remove and download the new one
        remove(manifest);
        install(manifest, versionOverride);
    }

    /**
     * @return
     * @apiNote
     */
    public List<Plugin> getInstalledPlugins() {
        Predicate<Plugin> isExternalPluginPredicate = plugin ->
                plugin.getClass().getAnnotation(PluginDescriptor.class).isExternal();

        List<Plugin> loadedPlugins = pluginManager.getPlugins()
                .stream()
                .filter(plugin -> !plugin.getClass().getAnnotation(PluginDescriptor.class).hidden())
                .filter(isExternalPluginPredicate)
                .collect(Collectors.toList());

        return loadedPlugins;
    }

    /**
     * Submits a plugin refresh task to the executor.
     * This will reload plugins based on the current profile's installed plugins list.
     */
    public void updatePlugin(ProjectXPluginManifest manifest, @Nullable String versionOverride) {
        executor.submit(() -> {
            update(manifest, versionOverride);
        });
    }

    /**
     * Gets the manifest for a given plugin, this pulls from the global manifest map.
     *
     * @param plugin the plugin to get the manifest for
     * @return the manifest for the plugin, or null if not found or not an external plugin
     */
    @Nullable
    private ProjectXPluginManifest getPluginManifest(Plugin plugin) {
        PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
        if (descriptor == null || !descriptor.isExternal()) {
            return null;
        }

        String internalName = plugin.getClass().getSimpleName();

        return manifestMap.get(internalName);
    }

    /**
     * Gracefully stops a plugin
     */
    private void stopPlugin(Plugin plugin) {
        String pluginName = plugin.getClass().getSimpleName();

        try {
            if (pluginManager.isPluginActive(plugin)) {
                if (SwingUtilities.isEventDispatchThread()) {
                    pluginManager.stopPlugin(plugin);
                    return;
                } else  {
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            pluginManager.stopPlugin(plugin);
                        } catch (PluginInstantiationException e) {
                            log.warn("Error stopping plugin {}: {}", pluginName, e.getMessage());
                        }
                    });
                }
            }
            pluginManager.remove(plugin);
        } catch (Exception e) {
            log.warn("Error during plugin stop for {}: {}", pluginName, e.getMessage());
        }
    }

    /**
     * Gracefully shuts down the plugin manager and performs final cleanup.
     */
    private void shutdown() {
        if (!isShuttingDown.compareAndSet(false, true)) {
            return;
        }

        log.info("Shutting down ProjectXPluginManager");

        try {
            List<Plugin> externalPlugins = pluginManager.getPlugins().stream()
                    .filter(plugin -> {
                        PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
                        return descriptor != null && descriptor.isExternal();
                    })
                    .collect(Collectors.toList());

            for (Plugin plugin : externalPlugins) {
                stopPlugin(plugin);
            }

            log.info("ProjectXPluginManager shutdown complete");
        } catch (Exception e) {
            log.error("Error during ProjectXPluginManager shutdown", e);
        }
    }

    private Optional<InstalledPluginVersion> lookupInstalledPluginVersion(String internalName) {
        if (Strings.isNullOrEmpty(internalName)) {
            return Optional.empty();
        }
        String key = INSTALLED_VERSION_KEY_PREFIX + internalName;
        String value = configManager.getConfiguration(INSTALLED_VERSION_GROUP, key);
        if (Strings.isNullOrEmpty(value)) {
            return Optional.empty();
        }

        String[] parts = value.split(":", 2);
        if (parts.length < 2 || Strings.isNullOrEmpty(parts[0]) || Strings.isNullOrEmpty(parts[1])) {
            return Optional.empty();
        }

        return Optional.of(new InstalledPluginVersion(parts[0], parts[1]));
    }

    public Optional<String> getInstalledPluginVersion(String internalName) {
        return lookupInstalledPluginVersion(internalName).map(InstalledPluginVersion::getVersion);
    }

    public Optional<OutdatedPluginUpdate> getOutdatedPluginUpdate(Plugin plugin) {
        ProjectXPluginManifest manifest = getPluginManifest(plugin);
        if (manifest == null) {
            return Optional.empty();
        }

        String internalName = manifest.getInternalName();
        String latestVersion = manifest.getVersion();
        String installedVersion = getInstalledPluginVersion(internalName)
                .orElseGet(() -> {
                    PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
                    return descriptor == null ? null : descriptor.version();
                });

        if (Strings.isNullOrEmpty(internalName)
                || Strings.isNullOrEmpty(installedVersion)
                || Strings.isNullOrEmpty(latestVersion)
                || latestVersion.equals(installedVersion)) {
            return Optional.empty();
        }

        String notificationKey = UPDATE_NOTIFICATION_KEY_PREFIX + internalName;
        String notifiedVersion = configManager.getConfiguration(UPDATE_NOTIFICATION_GROUP, notificationKey);
        if (latestVersion.equals(notifiedVersion)) {
            return Optional.empty();
        }

        return Optional.of(new OutdatedPluginUpdate(
                internalName,
                Strings.isNullOrEmpty(manifest.getDisplayName()) ? internalName : manifest.getDisplayName(),
                installedVersion,
                latestVersion
        ));
    }

    public void rememberOutdatedPluginUpdateNotification(OutdatedPluginUpdate outdatedPluginUpdate) {
        if (outdatedPluginUpdate == null || Strings.isNullOrEmpty(outdatedPluginUpdate.getInternalName())
                || Strings.isNullOrEmpty(outdatedPluginUpdate.getLatestVersion())) {
            return;
        }

        configManager.setConfiguration(
                UPDATE_NOTIFICATION_GROUP,
                UPDATE_NOTIFICATION_KEY_PREFIX + outdatedPluginUpdate.getInternalName(),
                outdatedPluginUpdate.getLatestVersion()
        );
    }

    private void rememberInstalledPluginVersion(String internalName, String version, String sha256) {
        if (Strings.isNullOrEmpty(internalName) || Strings.isNullOrEmpty(version) || Strings.isNullOrEmpty(sha256)) {
            return;
        }

        configManager.setConfiguration(
                INSTALLED_VERSION_GROUP,
                INSTALLED_VERSION_KEY_PREFIX + internalName,
                version + ":" + sha256
        );
    }

    private void clearInstalledPluginVersion(String internalName) {
        if (Strings.isNullOrEmpty(internalName)) {
            return;
        }
        configManager.unsetConfiguration(INSTALLED_VERSION_GROUP, INSTALLED_VERSION_KEY_PREFIX + internalName);
    }

    private static final class InstalledPluginVersion {
        private final String version;
        private final String sha256;

        private InstalledPluginVersion(String version, String sha256) {
            this.version = version;
            this.sha256 = sha256;
        }

        public String getVersion() {
            return version;
        }

        public String getSha256() {
            return sha256;
        }
    }

    public static final class OutdatedPluginUpdate {
        private final String internalName;
        private final String displayName;
        private final String installedVersion;
        private final String latestVersion;

        private OutdatedPluginUpdate(String internalName, String displayName, String installedVersion, String latestVersion) {
            this.internalName = internalName;
            this.displayName = displayName;
            this.installedVersion = installedVersion;
            this.latestVersion = latestVersion;
        }

        public String getInternalName() {
            return internalName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getInstalledVersion() {
            return installedVersion;
        }

        public String getLatestVersion() {
            return latestVersion;
        }
    }
}
