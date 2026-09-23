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

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class ProjectXPluginClient
{
    // Our own hub, served by the Project X website. The URL is resolved per call
    // rather than cached in a static, so changing the site in the settings takes
    // effect without restarting the client.
    //
    // The site serves one version per plugin (downloadUrl in the manifest), so
    // there is no release history to browse. Kept null rather than deleting the
    // version-picker plumbing, which the hub panel still calls.
    private static final HttpUrl PROJECTX_PLUGIN_RELEASES_URL = null;
    private static final HttpUrl PROJECTX_PLUGIN_RELEASES_API_URL = null;
    private static final String GITHUB_TOKEN_ENV = "GITHUB_TOKEN";
    private static final String ASSET_EXTENSION = ".jar";
    private static final String PLUGINS_JSON_PATH = "plugins.json";
    
    private final OkHttpClient okHttpClient;
    private final Gson gson;

    @Inject
    private ProjectXPluginClient(OkHttpClient okHttpClient, Gson gson)
    {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
    }

    /**
     * Downloads the plugin manifest from the ProjectX Hub
     */
    public List<ProjectXPluginManifest> downloadManifest() throws IOException
    {
        HttpUrl manifestUrl = ProjectXSite.api("hub").newBuilder()
            .addPathSegment(PLUGINS_JSON_PATH)
            .build();

        Request request = new Request.Builder()
            .url(manifestUrl)
            .header("Cache-Control", "no-cache")
            .build();

        try (Response res = okHttpClient.newCall(request).execute())
        {
            if (res.code() != 200)
            {
                throw new IOException("Non-OK response code: " + res.code());
            }

            return gson.fromJson(res.body().string(), 
                new TypeToken<List<ProjectXPluginManifest>>(){}.getType());
        }
        catch (JsonSyntaxException ex)
        {
            throw new IOException("Failed to parse plugin manifest", ex);
        }
    }

    /**
     * Downloads plugin icon from the ProjectX Hub
     */
    public BufferedImage downloadIcon(String iconUrl) throws IOException
    {
        HttpUrl url = HttpUrl.parse(iconUrl);
        if (url == null)
        {
            return null;
        }

        try (Response res = okHttpClient.newCall(new Request.Builder().url(url).build()).execute())
        {
            byte[] bytes = res.body().bytes();
            // We don't stream so the lock doesn't block the edt trying to load something at the same time
            synchronized (ImageIO.class)
            {
                return ImageIO.read(new ByteArrayInputStream(bytes));
            }
        }
    }

    /**
     * Returns the URL for downloading a plugin JAR
     */
    public HttpUrl getJarURL(ProjectXPluginManifest manifest, String versionOverride)
    {
        String artifactId = resolveArtifactId(manifest);
        String version = !Strings.isNullOrEmpty(versionOverride) ? versionOverride : manifest.getVersion();

        if (!Strings.isNullOrEmpty(manifest.getDownloadUrl()))
        {
            return HttpUrl.parse(manifest.getDownloadUrl());
        }

        if (PROJECTX_PLUGIN_RELEASES_URL != null && !Strings.isNullOrEmpty(artifactId) && !Strings.isNullOrEmpty(version))
        {
            String fileName = buildAssetFileName(artifactId, version);
            return PROJECTX_PLUGIN_RELEASES_URL.newBuilder()
                .addPathSegment(fileName)
                .build();
        }

        return Strings.isNullOrEmpty(manifest.getUrl()) ? null : HttpUrl.parse(manifest.getUrl());
    }

    /**
     * Gets download counts for plugins
     */
    public Map<String, Integer> getPluginCounts() throws IOException
    {
        // This would need to be implemented if your backend supports tracking download counts
        // For now, we'll return an empty map
        return Map.of();
    }

    /**
     * Fetches the list of published versions for the given plugin from GitHub releases.
     */
    public JsonArray fetchAllReleases() throws IOException
    {
        if (PROJECTX_PLUGIN_RELEASES_API_URL == null)
        {
            return new JsonArray();
        }

        HttpUrl releasesUrl = PROJECTX_PLUGIN_RELEASES_API_URL.newBuilder()
            .addQueryParameter("per_page", "100")
            .build();

        Request request = withGithubHeaders(new Request.Builder()
            .url(releasesUrl)
            .header("Cache-Control", "no-cache"))
            .build();

        try (Response res = okHttpClient.newCall(request).execute())
        {
            if (res.body() == null || res.code() != 200)
            {
                throw new IOException("Failed to fetch releases: HTTP " + res.code());
            }

            JsonArray releases = gson.fromJson(res.body().string(), JsonArray.class);
            return releases != null ? releases : new JsonArray();
        }
        catch (JsonSyntaxException ex)
        {
            throw new IOException("Unable to parse releases", ex);
        }
    }

    public List<String> parseVersionsFromReleases(ProjectXPluginManifest manifest, JsonArray releases) throws IOException
    {
        if (manifest == null || releases == null || releases.size() == 0)
        {
            return Collections.emptyList();
        }

        String artifactId = resolveArtifactId(manifest);
        if (Strings.isNullOrEmpty(artifactId))
        {
            return Collections.emptyList();
        }

        Set<String> versions = new LinkedHashSet<>();
        String normalizedArtifact = artifactId.toLowerCase(Locale.ROOT);

        for (JsonElement releaseElem : releases)
        {
            if (!releaseElem.isJsonObject())
            {
                continue;
            }

            JsonObject release = releaseElem.getAsJsonObject();
            String tagName = getString(release, "tag_name");
            JsonArray assets = release.getAsJsonArray("assets");
            if (assets == null)
            {
                continue;
            }

            for (JsonElement assetElem : assets)
            {
                if (!assetElem.isJsonObject())
                {
                    continue;
                }

                JsonObject asset = assetElem.getAsJsonObject();
                String assetName = getString(asset, "name");
                if (Strings.isNullOrEmpty(assetName))
                {
                    continue;
                }

                if (matchesArtifact(assetName, normalizedArtifact))
                {
                    String version = extractVersionFromAsset(assetName, normalizedArtifact);
                    if (!Strings.isNullOrEmpty(version))
                    {
                        versions.add(version);
                    }
                    else if (!Strings.isNullOrEmpty(tagName))
                    {
                        versions.add(normalizeTag(tagName));
                    }
                }
            }
        }

        return new ArrayList<>(versions);
    }

    public Optional<String> findFirstAssetCreatedAt(ProjectXPluginManifest manifest, JsonArray releases)
    {
        if (manifest == null || releases == null || releases.size() == 0)
        {
            return Optional.empty();
        }

        String artifactId = resolveArtifactId(manifest);
        if (Strings.isNullOrEmpty(artifactId))
        {
            return Optional.empty();
        }

        String normalizedArtifact = artifactId.toLowerCase(Locale.ROOT);
        String firstCreatedAt = null;

        for (JsonElement releaseElem : releases)
        {
            if (!releaseElem.isJsonObject())
            {
                continue;
            }

            JsonObject release = releaseElem.getAsJsonObject();
            JsonArray assets = release.getAsJsonArray("assets");
            if (assets == null)
            {
                continue;
            }

            for (JsonElement assetElem : assets)
            {
                if (!assetElem.isJsonObject())
                {
                    continue;
                }

                JsonObject asset = assetElem.getAsJsonObject();
                String assetName = getString(asset, "name");
                if (Strings.isNullOrEmpty(assetName) || !matchesArtifact(assetName, normalizedArtifact))
                {
                    continue;
                }

                String createdAt = getString(asset, "created_at");
                if (!Strings.isNullOrEmpty(createdAt)
                    && (firstCreatedAt == null || createdAt.compareTo(firstCreatedAt) < 0))
                {
                    firstCreatedAt = createdAt;
                }
            }
        }

        return Optional.ofNullable(firstCreatedAt);
    }

    public List<String> fetchAvailableVersions(ProjectXPluginManifest manifest) throws IOException
    {
        JsonArray releases = fetchAllReleases();
        return parseVersionsFromReleases(manifest, releases);
    }

    private Request.Builder withGithubHeaders(Request.Builder builder)
    {
        builder.header("Accept", "application/vnd.github+json")
            .header("User-Agent", "projectx-client");

        String token = System.getenv(GITHUB_TOKEN_ENV);
        if (!Strings.isNullOrEmpty(token))
        {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private String resolveArtifactId(ProjectXPluginManifest manifest)
    {
        if (manifest == null)
        {
            return "";
        }

        String artifactId = manifest.getArtifactId();
        if (!Strings.isNullOrEmpty(artifactId))
        {
            return artifactId;
        }

        return manifest.getInternalName();
    }

    private String buildAssetFileName(String artifactId, String version)
    {
        return artifactId + "-" + version + ASSET_EXTENSION;
    }

    private boolean matchesArtifact(String assetName, String normalizedArtifact)
    {
        String lower = assetName.toLowerCase(Locale.ROOT);
        return lower.startsWith(normalizedArtifact + "-") && lower.endsWith(ASSET_EXTENSION);
    }

    private String extractVersionFromAsset(String assetName, String normalizedArtifact)
    {
        String lower = assetName.toLowerCase(Locale.ROOT);
        int prefixLength = normalizedArtifact.length() + 1; // hyphen
        int suffixLength = ASSET_EXTENSION.length();
        if (lower.length() <= prefixLength + suffixLength)
        {
            return null;
        }

        String versionPart = assetName.substring(prefixLength, assetName.length() - suffixLength);
        return normalizeTag(versionPart);
    }

    private String normalizeTag(String tag)
    {
        if (Strings.isNullOrEmpty(tag))
        {
            return tag;
        }

        return tag.startsWith("v") && tag.length() > 1 ? tag.substring(1) : tag;
    }

    private String getString(JsonObject obj, String memberName)
    {
        if (obj == null || Strings.isNullOrEmpty(memberName) || !obj.has(memberName))
        {
            return null;
        }

        JsonElement elem = obj.get(memberName);
        return elem != null && !elem.isJsonNull() ? elem.getAsString() : null;
    }
}
