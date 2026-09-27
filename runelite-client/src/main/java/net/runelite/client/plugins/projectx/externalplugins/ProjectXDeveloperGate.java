/*
 * Copyright (c) 2026 ProjectX
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

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Plugins only script authors get to see.
 *
 * <p>Developer Tools exposes the widget inspector, the scene dumper and the
 * script console -- the things someone writing a plugin needs and nobody else
 * has a reason to open. Rather than shipping it to everyone and hoping, the
 * class is filtered out before {@code PluginManager} ever loads it, so there is
 * no entry in the plugin list, no sidebar button and no config to enable.
 *
 * <p>The decision is made once, at start-up, from the role the site reported for
 * the signed-in user. It fails closed: an unreachable site, an expired token or
 * nobody signed in all mean "not a developer", which is the right way round for
 * something that is hidden by default.
 *
 * <p>This is a UI decision, not a security control. The user owns the machine
 * and the jar; anything that must genuinely be enforced is enforced by the API.
 */
@Slf4j
public final class ProjectXDeveloperGate
{
    /**
     * Fully qualified names rather than class literals: naming the class here
     * would load it, which is most of what we are trying to avoid.
     */
    private static final Set<String> DEVELOPER_ONLY = ImmutableSet.of(
        "net.runelite.client.plugins.devtools.DevToolsPlugin"
    );

    /**
     * Set once during start-up, before plugins are loaded. Null in tests and in
     * any path that never initialised Project X, where nothing is gated.
     */
    private static volatile ProjectXAccount account;

    private ProjectXDeveloperGate()
    {
    }

    /**
     * Binds the account whose role decides the gate, and logs the outcome once
     * so a developer wondering where their tools went has somewhere to look.
     */
    public static void bind(ProjectXAccount boundAccount)
    {
        account = boundAccount;

        if (boundAccount == null)
        {
            return;
        }

        ProjectXAccount.Identity identity = boundAccount.getIdentity();
        if (identity == null)
        {
            log.info("Not signed in to Project X; developer tooling is hidden");
        }
        else
        {
            log.info("Signed in to Project X as {} ({})", identity.displayName(), identity.role());
        }
    }

    /** True while the signed-in user may see developer tooling. */
    public static boolean developerToolsAllowed()
    {
        ProjectXAccount current = account;
        return current != null && current.isDeveloper();
    }

    /** True when this plugin class must not be loaded for the current user. */
    public static boolean isHidden(Class<?> pluginClass)
    {
        return pluginClass != null && isHidden(pluginClass.getName());
    }

    public static boolean isHidden(String className)
    {
        if (className == null || !DEVELOPER_ONLY.contains(className))
        {
            return false;
        }

        if (developerToolsAllowed())
        {
            return false;
        }

        // Said out loud, once per gated plugin: a developer whose tools have
        // vanished because their token expired has no other way to find out.
        log.info("Hiding {}: Project X developer role required", className);
        return true;
    }
}
