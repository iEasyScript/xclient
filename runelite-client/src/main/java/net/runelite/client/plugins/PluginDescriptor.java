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
package net.runelite.client.plugins;

import java.awt.*;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface PluginDescriptor
{
    /*
     * Author badges, all empty.
     *
     * Each of these was an HTML fragment -- a coloured letter in brackets --
     * glued to the front of a plugin's name. Across a list of a hundred and
     * sixty that is a column of tags nobody can read, in front of the words
     * people are actually looking for.
     *
     * Emptied rather than deleted, deliberately. The constants stay public and
     * keep their names, so every plugin that prefixes its name with one still
     * compiles, plugins are built from separate checkouts that would otherwise
     * break, and putting a badge back is a matter of putting a string in one of
     * these. Nothing has been taken away except what was on screen.
     */
    String Bee = "";
    String Nate = "";
    String Mocrosoft = "";
    String Default = "";
    String Bank = "";
    String Forn = "";
    String See1Duck = "";
    String TaFCat = "";
    String GMason = "";
    String Pumster = "";
    String Basche = "";
    String Vince = "";
    String Basm = "";
    String Geoff = "";
    String Bttqjs = "";
	String VOX = "";
    String Gabulhas = "";
    String zerozero ="" ;
    String LiftedMango = "";
    String eXioStorm = ""; Color stormColor = new Color(255, 0, 220);
    String Girdy = "";
    String Cicire = "";
    String ChillX = "";
    String Gage = "";
	String Bradley = "";
	String Frosty = "";
	String Maxxin = "";
	String Hal = "";
	String Funk = "";
  	String Cardew = "";
	String Bolado = "";
 	String Choken = "";

	String name();

	/**
	 * Internal name used in the config.
	 */
	String configName() default "";

	/**
	 * A short, one-line summary of the plugin.
	 */
	String description() default "";

	/**
	 * A list of plugin keywords, used (together with the name) when searching for plugins.
	 * Each tag should not contain any spaces, and should be fully lowercase.
	 */
	String[] tags() default {};

	/**
	 * A list of plugin names that are mutually exclusive with this plugin. Any plugins
	 * with a name or conflicts value that matches this will be disabled when this plugin
	 * is started
	 */
	String[] conflicts() default {};

	/**
	 * If this plugin should be defaulted to on. Plugin-Hub plugins should always
	 * have this set to true (the default), since having them off by defaults means
	 * the user has to install the plugin, then separately enable it, which is confusing.
	 */
	boolean enabledByDefault() default true;

    /**
     * always on
     */
    boolean alwaysOn() default false;
	/**
	 * If this plugin should be disabled on startup. This is used for plugins that
	 * are not needed in certain situations, like the cache debugger.
	 */
	boolean disableOnStartUp() default false; 
	/**
	 * disable the plugin on startup and not allow it to be enabled
	 * This is used for plugins that are not needed in certain situations
	 * @return
	 */
	boolean disable() default false;

	/**
	 * Whether or not plugin is hidden from configuration panel
	 */
	boolean hidden() default false;

	boolean developerPlugin() default false;

	boolean loadInSafeMode() default true;

	boolean priority() default false;

	/**
	 * The author of the plugin.
	 */
	String[] authors() default { "Unknown" };

	/**
	 * The version of the plugin.
	 */
	String version() default "1.0.0";

	/**
	 * The minimum client version required for this plugin to work.
	 */
	String minClientVersion() default "1.0.0";
	/**
	 * An optional URL to an icon for the plugin.
	 * This icon will be displayed in the plugin hub list
	 */
	String iconUrl() default "";

	/**
	 * An optional URL to an banner image for the plugin.
	 * this image will be displayed in the plugin card on the website.
	 */
	String cardUrl() default "";

	/**
	 * A flag to denote if a plugin is an external plugin (loaded from a JAR file) or a native plugin.
	 */
	boolean isExternal() default false;
}
