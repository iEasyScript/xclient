package net.runelite.client.plugins.projectx.util.misc;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.projectx.util.math.Rs2Random;
import net.runelite.client.plugins.projectx.util.menu.NewMenuEntry;
import net.runelite.client.plugins.projectx.util.text.Rs2TextSanitizer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @deprecated Use {@link net.runelite.client.plugins.projectx.bot.UiHelper} instead: same methods, shorter name. This name keeps working.
 */
@Slf4j
@Deprecated
public class Rs2UiHelper {

    public static boolean isRectangleWithinViewport(Rectangle rectangle) {
        int viewportHeight = ProjectX.getClient().getViewportHeight();
        int viewportWidth = ProjectX.getClient().getViewportWidth();

        return !(rectangle.getX() > (double) viewportWidth) &&
                !(rectangle.getY() > (double) viewportHeight) &&
                !(rectangle.getX() < 0.0) &&
                !(rectangle.getY() < 0.0);
    }

    public static boolean isRectangleWithinCanvas(Rectangle rectangle) {
        if (rectangle == null) {
            return false;
        }
        int canvasHeight = ProjectX.getClient().getCanvasHeight();
        int canvasWidth = ProjectX.getClient().getCanvasWidth();

        return rectangle.getX() + rectangle.getWidth() <= (double) canvasWidth &&
                rectangle.getY() + rectangle.getHeight() <= (double) canvasHeight;
    }

    /**
     * Top-level containers drawn over the scene. In the resizable layouts the viewport covers the whole
     * canvas, so a scene point inside the viewport can still be under one of these.
     */
    private static final int[] SCENE_OVERLAY_COMPONENTS = {
            InterfaceID.ToplevelOsrsStretch.SIDE_MENU,
            InterfaceID.ToplevelOsrsStretch.CHAT_CONTAINER,
            InterfaceID.ToplevelOsrsStretch.MAP_CONTAINER,
            InterfaceID.ToplevelPreEoc.SIDE_CONTAINER,
            InterfaceID.ToplevelPreEoc.CHAT_CONTAINER,
            InterfaceID.ToplevelPreEoc.MAP_CONTAINER
    };

    /** Bounds of the visible side panel, chatbox and minimap containers. Safe from any thread. */
    public static List<Rectangle> visibleSceneOverlayBounds() {
        return ProjectX.getClientThread().runOnClientThreadOptional(() -> {
            Client client = ProjectX.getClient();
            List<Rectangle> bounds = new ArrayList<>();
            for (int componentId : SCENE_OVERLAY_COMPONENTS) {
                Widget widget = client.getWidget(componentId);
                if (widget != null && !widget.isHidden()) {
                    bounds.add(widget.getBounds());
                }
            }
            return bounds;
        }).orElseGet(ArrayList::new);
    }

    static final int INTERFACE_CLEARANCE_PX = 4;

    /**
     * The scene viewport. In the resizable layouts it is the whole canvas. Plain field reads, safe from
     * any thread; reading them inside a client-thread lambda would make the guardrail treat these
     * getters as client-thread-only for every other caller.
     */
    public static Rectangle sceneViewportBounds() {
        Client client = ProjectX.getClient();
        return new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
                client.getViewportWidth(), client.getViewportHeight());
    }

    /**
     * True for the stand-in rectangle {@link #getDefaultRectangle()} returns when there is no real
     * clickbox (it starts at -1, 0 or 1 on both axes).
     */
    public static boolean isFallbackRectangle(Rectangle rectangle) {
        return rectangle == null || (rectangle.x == rectangle.y && rectangle.x >= -1 && rectangle.x <= 1);
    }

    /**
     * The largest part of {@code clickbox} that is inside the viewport and at least
     * {@link #INTERFACE_CLEARANCE_PX} away from every interface rectangle, or null when nothing is
     * clear. Pure, so it can be tested without a client.
     */
    public static Rectangle clearOfInterfaces(Rectangle clickbox, Rectangle viewport, List<Rectangle> interfaceBounds) {
        if (clickbox == null || clickbox.isEmpty()) {
            return null;
        }
        Rectangle visible = viewport == null ? new Rectangle(clickbox) : clickbox.intersection(viewport);
        if (visible.isEmpty()) {
            return null;
        }
        List<Rectangle> clear = new ArrayList<>();
        clear.add(visible);
        if (interfaceBounds != null) {
            for (Rectangle bounds : interfaceBounds) {
                if (bounds == null || bounds.isEmpty()) {
                    continue;
                }
                Rectangle cover = new Rectangle(bounds);
                cover.grow(INTERFACE_CLEARANCE_PX, INTERFACE_CLEARANCE_PX);
                List<Rectangle> remaining = new ArrayList<>();
                for (Rectangle area : clear) {
                    if (!area.intersects(cover)) {
                        remaining.add(area);
                        continue;
                    }
                    addIfNotEmpty(remaining, new Rectangle(area.x, area.y, cover.x - area.x, area.height));
                    addIfNotEmpty(remaining, new Rectangle(cover.x + cover.width, area.y,
                            area.x + area.width - cover.x - cover.width, area.height));
                    addIfNotEmpty(remaining, new Rectangle(area.x, area.y, area.width, cover.y - area.y));
                    addIfNotEmpty(remaining, new Rectangle(area.x, cover.y + cover.height,
                            area.width, area.y + area.height - cover.y - cover.height));
                }
                clear = remaining;
            }
        }
        Rectangle largest = null;
        for (Rectangle area : clear) {
            Rectangle candidate = awayFromFallbackOrigin(area);
            if (candidate == null) {
                continue;
            }
            if (largest == null || (long) candidate.width * candidate.height > (long) largest.width * largest.height) {
                largest = candidate;
            }
        }
        return largest;
    }

    static final int FALLBACK_ORIGIN_INSET_PX = 2;

    /**
     * A clear area that starts at the canvas corner looks like the fallback rectangle, and a click on
     * it goes to the resting point instead; move it in by a couple of pixels.
     */
    private static Rectangle awayFromFallbackOrigin(Rectangle area) {
        if (!isFallbackRectangle(area)) {
            return area;
        }
        Rectangle moved = new Rectangle(FALLBACK_ORIGIN_INSET_PX, FALLBACK_ORIGIN_INSET_PX,
                area.x + area.width - FALLBACK_ORIGIN_INSET_PX, area.y + area.height - FALLBACK_ORIGIN_INSET_PX);
        return moved.isEmpty() ? null : moved;
    }

    static final long COVERED_WARN_INTERVAL_MS = 10_000;
    private static int lastCoveredWarnId = -1;
    private static long lastCoveredWarnAt;

    /** One warning per object every 10 s; the walker retries a covered object every pass. */
    static synchronized boolean shouldWarnCovered(int objectId, long now) {
        if (objectId == lastCoveredWarnId && now - lastCoveredWarnAt < COVERED_WARN_INTERVAL_MS) {
            return false;
        }
        lastCoveredWarnId = objectId;
        lastCoveredWarnAt = now;
        return true;
    }

    /** Logs that an object could not be clicked because interfaces cover it. */
    public static void warnObjectCoveredByInterface(int objectId, String objectName) {
        String name = objectName == null || objectName.isEmpty() || "null".equalsIgnoreCase(objectName)
                ? "object " + objectId : stripColTags(objectName);
        String message = "Can't click {} - it is behind the chatbox, side panel or minimap even after turning the camera."
                + " If this keeps happening, zoom out or move the camera so it is in view.";
        if (shouldWarnCovered(objectId, System.currentTimeMillis())) {
            log.warn(message, name);
        } else {
            log.debug(message, name);
        }
    }

    private static void addIfNotEmpty(List<Rectangle> areas, Rectangle area) {
        if (!area.isEmpty() && !areas.contains(area)) {
            areas.add(area);
        }
    }

    /**
     * Where to click an object: the part of its clickbox that is inside the viewport and clear of the
     * side panel, chatbox and minimap. A tile on screen does not mean the object is clickable: in the
     * resizable layouts a random point in the whole clickbox often landed on the chatbox. Returns the
     * fallback rectangle unchanged when the object has no clickbox, and null when no part is clear.
     */
    public static Rectangle clearObjectClickArea(TileObject object) {
        Rectangle clickbox = getObjectClickbox(object);
        if (isFallbackRectangle(clickbox)) {
            return clickbox;
        }
        return clearOfInterfaces(clickbox, sceneViewportBounds(), visibleSceneOverlayBounds());
    }

    public static boolean isRectangleWithinRectangle(Rectangle main, Rectangle sub) {
        return main.contains(sub);
    }

    private static final double CLICK_FORCE_MIN = 0.55;
    private static final double CLICK_FORCE_MAX = 0.95;
    private static volatile double sessionClickForce = 0.0;

    static double getSessionClickForce() {
        double cached = sessionClickForce;
        if (cached != 0.0) return cached;
        synchronized (Rs2UiHelper.class) {
            if (sessionClickForce != 0.0) return sessionClickForce;
            sessionClickForce = CLICK_FORCE_MIN + java.util.concurrent.ThreadLocalRandom.current().nextDouble()
                    * (CLICK_FORCE_MAX - CLICK_FORCE_MIN);
            return sessionClickForce;
        }
    }

    public static Point getClickingPoint(Rectangle rectangle, boolean randomize) {
        if (rectangle == null) return new Point(1, 1);
        if (rectangle.getX() == 1 && rectangle.getY() == 1) return new Point(1, 1);
        if (rectangle.getX() == 0 && rectangle.getY() == 0) return new Point(1, 1);

        if (!randomize) return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY());

        double force = getSessionClickForce();
        if (Rs2AntibanSettings.naturalMouse) {
            java.awt.Point mousePos = ProjectX.getMouse().getMousePosition();
            return Rs2Random.randomPointEx(new Point(mousePos.x, mousePos.y), rectangle, force);
        } else
            return Rs2Random.randomPointEx(ProjectX.getMouse().getLastClick(), rectangle, force);
    }

    //check if mouse is already within the rectangle
    public static boolean isMouseWithinRectangle(Rectangle rectangle) {
        java.awt.Point mousePos = ProjectX.getMouse().getMousePosition();
        if (rectangle.getX() == 1 && rectangle.getY() == 1) return true;
        if (rectangle.getX() == 0 && rectangle.getY() == 0) return true;
        return rectangle.contains(mousePos);
    }

    public static Rectangle getActorClickbox(Actor actor) {
        LocalPoint lp = actor.getLocalLocation();
        if (lp == null) {
            log.warn("LocalPoint is null");
            return getDefaultRectangle();
        }


        Shape clickbox = ProjectX.getClientThread().runOnClientThreadOptional(() -> Perspective.getClickbox(ProjectX.getClient(), ProjectX.getClient().getTopLevelWorldView(), actor.getModel(), actor.getCurrentOrientation(), lp.getX(), lp.getY(),
                Perspective.getTileHeight(ProjectX.getClient(), lp, actor.getWorldLocation().getPlane())))
                .orElse(null);

        if (clickbox == null) return getDefaultRectangle();  //return a small rectangle if clickbox is null
        

        return new Rectangle(clickbox.getBounds());
    }

    public static Rectangle getObjectClickbox(TileObject object) {

        if (object == null) return getDefaultRectangle();  //return a small rectangle if object is null
        Shape clickbox = ProjectX.getClientThread().runOnClientThreadOptional(object::getClickbox).orElse(null);
        if (clickbox == null) return getDefaultRectangle();  //return a small rectangle if clickbox is null
        if (clickbox.getBounds() == null) return getDefaultRectangle();


        return new Rectangle(clickbox.getBounds());
    }
    
    public static Rectangle getTileClickbox(Tile tile) {
        if (tile == null) return getDefaultRectangle();

        LocalPoint localPoint = tile.getLocalLocation();
        if (localPoint == null) return getDefaultRectangle();

        // Get the screen point of the tile center
        Point screenPoint = Perspective.localToCanvas(ProjectX.getClient(), localPoint, ProjectX.getClient().getPlane());

        if (screenPoint == null) return getDefaultRectangle();
        
        int tileSize = Perspective.LOCAL_TILE_SIZE;
        int halfSize = tileSize / 4;

        return new Rectangle(screenPoint.getX() - halfSize, screenPoint.getY() - halfSize, tileSize / 2, tileSize / 2);
    }

    // check if a menu entry is a actor
    public static boolean hasActor(NewMenuEntry entry) {
        return entry.getActor() != null;
    }

    // check if a menu entry is a game object
    public static boolean isGameObject(NewMenuEntry entry) {
        return entry.getGameObject() != null;
    }

    /**
     * Strips RuneLite/Jagex color tags from the provided text.
     *
     * @param text the text from which to strip color tags.
     * @return the text without color tags.
	 *
	 * @deprecated Use {@link #stripTags(String)} or {@link net.runelite.client.plugins.projectx.util.text.Rs2TextSanitizer#stripTags(String)}
	 * for full tag removal.
     */
	@Deprecated
    public static String stripColTags(String text) {
        return Rs2TextSanitizer.stripColorTags(text);
    }

	/** Strip RuneLite/Jagex markup tags from text. */
	public static String stripTags(String text)
	{
		return Rs2TextSanitizer.stripTags(text);
	}

	/** Strip tags and normalize whitespace to single spaces. */
	public static String stripTagsToSpace(String text)
	{
		return Rs2TextSanitizer.stripTagsToSpace(text);
	}

	public static Rectangle getDefaultRectangle() {
		int randomValue = ThreadLocalRandom.current().nextInt(3) - 1;
		return new Rectangle(randomValue, randomValue, ProjectX.getClient().getCanvasWidth(), ProjectX.getClient().getCanvasHeight());
	}

	/**
	 * Extracts the first integer number from a given string.
	 * For example: "+1%", "123abc", "Price: 99 dollars" will result in 1, 123, 99
	 *
	 * @param input The input string containing a number.
	 * @return The extracted number, or -1 if no number is found.
	 */
	public static int extractNumber(String input) {
		// Define a regex pattern to match one or more digits
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(input);

		// Find the first match and parse it as an integer
		if (matcher.find()) {
			return Integer.parseInt(matcher.group());
		}

		// Return -1 if no number is found
		return -1;
	}

	/**
	 * Check if the current client version is compatible with the required minimum version.
	 *
	 * <p>Compared against the plugin API level, not the release version. A
	 * plugin's {@code minClientVersion} says which API it was written against,
	 * and the catalogue was written against the 1.9-2.6 line this client
	 * inherited its API from. The release version is a separate, product-facing
	 * number that restarted at 1.0.0; comparing against that would declare
	 * every existing plugin incompatible purely because of a rename.
	 */
	public static boolean isClientVersionCompatible(String minClientVersion) {
		if (ProjectX.isDebug()) {
			return true; // return true if we are developing
		}
		if (minClientVersion == null || minClientVersion.isEmpty()) {
			return true;
		}

		String apiVersion = RuneLiteProperties.getProjectXPluginApiVersion();
		if (apiVersion == null || apiVersion.isEmpty()) {
			log.warn("Unable to determine the ProjectX plugin API version");
			return false;
		}

		return compareVersions(apiVersion, minClientVersion) >= 0;
	}

	/**
	 * Compare two version strings using semantic versioning with support for 4-part versions
	 * Supports formats like: 1.9.7, 1.9.7.1, 1.9.8, 1.9.8.1
	 * @param version1 The first version to compare
	 * @param version2 The second version to compare
	 * @return -1 if version1 < version2, 0 if equal, 1 if version1 > version2
	 */
	public static int compareVersions(String version1, String version2) {
		if (version1 == null && version2 == null) return 0;
		if (version1 == null) return -1;
		if (version2 == null) return 1;

		// Split versions by dots and handle up to 4 parts (major.minor.patch.build)
		String[] v1Parts = version1.split("\\.");
		String[] v2Parts = version2.split("\\.");

		int maxLength = Math.max(v1Parts.length, v2Parts.length);

		for (int i = 0; i < maxLength; i++) {
			int v1Part = i < v1Parts.length ? parseVersionPart(v1Parts[i]) : 0;
			int v2Part = i < v2Parts.length ? parseVersionPart(v2Parts[i]) : 0;

			if (v1Part < v2Part) return -1;
			if (v1Part > v2Part) return 1;
		}

		return 0;
	}

	/**
	 * Parse a version part, extracting only the numeric portion
	 */
	public static int parseVersionPart(String part) {
		if (part == null || part.isEmpty()) return 0;

		StringBuilder numericPart = new StringBuilder();
		for (char c : part.toCharArray()) {
			if (!Character.isDigit(c)) break;
			numericPart.append(c);
		}

		try {
			return numericPart.length() > 0 ? Integer.parseInt(numericPart.toString()) : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
