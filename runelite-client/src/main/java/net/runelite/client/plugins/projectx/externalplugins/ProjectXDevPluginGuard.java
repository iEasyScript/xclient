package net.runelite.client.plugins.projectx.externalplugins;

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Decides whether a jar from the local dev folder may be loaded.
 *
 * <p>A developer's own script is welcome; a store script copied there to keep it running after the
 * subscription ended is not. Checked against the whole published catalogue, not the user's own
 * subscriptions.
 */
final class ProjectXDevPluginGuard
{
	static final String STORE_PACKAGE_PREFIX = "net.runelite.client.plugins.projectx.";

	private ProjectXDevPluginGuard()
	{
	}

	/**
	 * @return why the plugin must not load, or null when it may
	 */
	@Nullable
	static String refusal(String internalName, @Nullable String packageName, @Nullable String jarSha256,
		Collection<ProjectXPluginManifest> catalogue)
	{
		for (ProjectXPluginManifest m : catalogue)
		{
			String storeName = m.getInternalName();
			if (Strings.isNullOrEmpty(storeName))
			{
				continue;
			}
			if (storeName.equalsIgnoreCase(internalName))
			{
				return "its name matches the store script " + storeName;
			}
			if (!Strings.isNullOrEmpty(jarSha256) && jarSha256.equalsIgnoreCase(m.getSha256()))
			{
				return "this jar is identical to the published build of " + storeName;
			}
			if (packageName != null && packageName.startsWith(STORE_PACKAGE_PREFIX))
			{
				String folder = packageName.substring(STORE_PACKAGE_PREFIX.length());
				int dot = folder.indexOf('.');
				if (dot >= 0)
				{
					folder = folder.substring(0, dot);
				}
				if (storeFolders(m).contains(folder.toLowerCase(Locale.ROOT)))
				{
					return "it uses the store package " + STORE_PACKAGE_PREFIX + folder + " of " + storeName;
				}
			}
		}
		return null;
	}

	private static java.util.Set<String> storeFolders(ProjectXPluginManifest m)
	{
		java.util.Set<String> out = new java.util.HashSet<>();
		String name = m.getInternalName().toLowerCase(Locale.ROOT);
		out.add(name);
		if (name.endsWith("plugin") && name.length() > "plugin".length())
		{
			out.add(name.substring(0, name.length() - "plugin".length()));
		}
		for (String s : new String[]{m.getArtifactId(), m.getFileName()})
		{
			if (!Strings.isNullOrEmpty(s))
			{
				String stem = s.toLowerCase(Locale.ROOT).replaceAll("[.]jar$", "");
				out.add(stem);
			}
		}
		return out;
	}
}
