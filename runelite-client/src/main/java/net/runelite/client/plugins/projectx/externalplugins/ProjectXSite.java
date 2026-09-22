package net.runelite.client.plugins.projectx.externalplugins;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import okhttp3.HttpUrl;

import java.util.Objects;

/**
 * Where the Project X website lives. It is both the plugin hub and the
 * entitlement authority, so everything that talks to "our servers" starts here.
 *
 * <p>Resolved in order: {@code -Dprojectx.siteUrl}, {@code PROJECTX_SITE_URL},
 * the "Site URL" setting in the Project X config, then the default below. The
 * default is still localhost because {@value #PRODUCTION_SITE_URL} is not hosted
 * yet -- when it is, change {@link #DEFAULT_SITE_URL} to it and nothing else
 * needs touching.
 */
@Slf4j
public final class ProjectXSite
{
    /** The domain we own. Not hosted yet. */
    public static final String PRODUCTION_SITE_URL = "https://xclient.dev/";

    /** Until the site is hosted, a local {@code npm run dev}. */
    private static final String DEFAULT_SITE_URL = "http://localhost:3000/";

    private static volatile HttpUrl cachedUrl;
    private static volatile String cachedSource;

    private ProjectXSite()
    {
    }

    /**
     * The site's base URL. Re-read when the setting changes, so pointing the
     * client at a different site takes effect without a restart.
     */
    public static HttpUrl baseUrl()
    {
        String configured = configuredUrl();

        HttpUrl current = cachedUrl;
        if (current != null && Objects.equals(cachedSource, configured))
        {
            return current;
        }

        HttpUrl url = Strings.isNullOrEmpty(configured) ? null : HttpUrl.parse(configured);
        if (url == null)
        {
            if (!Strings.isNullOrEmpty(configured))
            {
                log.warn("Ignoring unusable Project X site URL '{}'; falling back to {}", configured, DEFAULT_SITE_URL);
            }
            url = HttpUrl.get(DEFAULT_SITE_URL);
        }

        cachedSource = configured;
        cachedUrl = url;
        return url;
    }

    private static String configuredUrl()
    {
        String configured = System.getProperty("projectx.siteUrl");
        if (Strings.isNullOrEmpty(configured))
        {
            configured = System.getenv("PROJECTX_SITE_URL");
        }
        if (Strings.isNullOrEmpty(configured))
        {
            configured = fromConfig();
        }
        return Strings.isNullOrEmpty(configured) ? null : configured.trim();
    }

    /** The config is not loaded during early start-up, so this stays best-effort. */
    private static String fromConfig()
    {
        try
        {
            ConfigManager configManager = ProjectX.getConfigManager();
            return configManager == null
                ? null
                : configManager.getConfiguration(ProjectXConfig.configGroup, ProjectXConfig.keySiteUrl);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /** {@code /api/v1/<segments...>} on the site. */
    public static HttpUrl api(String... segments)
    {
        HttpUrl.Builder builder = baseUrl().newBuilder().encodedPath("/api/v1");
        for (String segment : segments)
        {
            builder.addPathSegment(segment);
        }
        return builder.build();
    }

    /** True when the URL points at the site itself, so it may receive the user's API token. */
    public static boolean isSite(HttpUrl url)
    {
        HttpUrl base = baseUrl();
        return url != null
            && url.scheme().equals(base.scheme())
            && url.host().equalsIgnoreCase(base.host())
            && url.port() == base.port();
    }

    /**
     * Plugin jars are executable code, so they only come over HTTPS -- except from
     * this machine, which is how the site runs until it is hosted.
     */
    public static boolean isAllowedDownload(HttpUrl url)
    {
        if (url == null)
        {
            return false;
        }
        if (url.isHttps())
        {
            return true;
        }
        String host = url.host();
        return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1");
    }
}
