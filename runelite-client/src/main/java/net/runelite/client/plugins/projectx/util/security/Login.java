package net.runelite.client.plugins.projectx.util.security;

import net.runelite.client.config.ConfigProfile;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.keyboard.Rs2Keyboard;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.projectx.util.Global.sleep;

@Deprecated
public class Login {

    private static final int MAX_PLAYER_COUNT = 1950;

    public Login() {
        this(ProjectX.getClient().getWorld() > 300 ? ProjectX.getClient().getWorld() : getRandomWorld(LoginManager.getActiveProfile().isMember()));
    }

    public Login(int world) {
        this(LoginManager.getActiveProfile().getName(), LoginManager.getActiveProfile().getPassword(), world);
    }

    public Login(String username, String password) {
        this(username, password, 360);
    }

    public Login(String username, String password, int world) {
        if(ProjectX.isLoggedIn())
            return;
        if (ProjectX.getClient().getLoginIndex() == 3 || ProjectX.getClient().getLoginIndex() == 24) { // you were disconnected from the server.
            int loginScreenWidth = 804;
            int startingWidth = (ProjectX.getClient().getCanvasWidth() / 2) - (loginScreenWidth / 2);
            ProjectX.getMouse().click(365 + startingWidth, 308); //clicks a button "OK" when you've been disconnected
            sleep(600);
        }
        Rs2Keyboard.keyPress(KeyEvent.VK_ENTER);
        sleep(600);
        try {
            setWorld(world);
        } catch (Exception e) {
            System.out.println("Changing world failed");
        }
        ProjectX.getClient().setUsername(username);
        try {
            ProjectX.getClient().setPassword(Encryption.decrypt(password));
        } catch (Exception e) {
            System.out.println("no password has been set in the profile");
        }
        sleep(300);
        Rs2Keyboard.keyPress(KeyEvent.VK_ENTER);
        sleep(300);
        Rs2Keyboard.keyPress(KeyEvent.VK_ENTER);
        if (ProjectX.getClient().getLoginIndex() == 10) {
            int loginScreenWidth = 804;
            int startingWidth = (ProjectX.getClient().getCanvasWidth() / 2) - (loginScreenWidth / 2);
            ProjectX.getMouse().click(365 + startingWidth, 250); //clicks a button "OK" when you've been disconnected
        } else if (ProjectX.getClient().getLoginIndex() == 9) {
            int loginScreenWidth = 804;
            int startingWidth = (ProjectX.getClient().getCanvasWidth() / 2) - (loginScreenWidth / 2);
            ProjectX.getMouse().click(365 + startingWidth, 300); //clicks a button "OK" when you've been disconnected
        }
    }

    public static int getRandomWorld(boolean isMembers, WorldRegion region) {
        WorldResult worldResult = ProjectX.getWorldService().getWorlds();

        if (worldResult == null) {
            return isMembers ? 360 : 383;
        }

        List<World> worlds = worldResult.getWorlds();
        boolean isInSeasonalWorld = ProjectX.getClient().getWorldType().contains(WorldType.SEASONAL);

        List<World> filteredWorlds = worlds.stream()
                .filter(x -> !x.getTypes().contains(WorldType.PVP) &&
                        !x.getTypes().contains(WorldType.HIGH_RISK) &&
                        !x.getTypes().contains(WorldType.BOUNTY) &&
                        !x.getTypes().contains(WorldType.SKILL_TOTAL) &&
                        !x.getTypes().contains(WorldType.LAST_MAN_STANDING) &&
                        !x.getTypes().contains(WorldType.QUEST_SPEEDRUNNING) &&
                        !x.getTypes().contains(WorldType.BETA_WORLD) &&
                        !x.getTypes().contains(WorldType.DEADMAN) &&
                        !x.getTypes().contains(WorldType.PVP_ARENA) &&
                        !x.getTypes().contains(WorldType.TOURNAMENT) &&
                        !x.getTypes().contains(WorldType.NOSAVE_MODE) &&
                        !x.getTypes().contains(WorldType.LEGACY_ONLY) &&
                        !x.getTypes().contains(WorldType.EOC_ONLY) &&
                        !x.getTypes().contains(WorldType.FRESH_START_WORLD) &&
                        x.getPlayers() < MAX_PLAYER_COUNT &&
                        x.getPlayers() >= 0)
                .filter(x -> isInSeasonalWorld == x.getTypes().contains(WorldType.SEASONAL)) // seasonal filter
                .collect(Collectors.toList());

        filteredWorlds = isMembers
                ? filteredWorlds.stream().filter(x -> x.getTypes().contains(WorldType.MEMBERS)).collect(Collectors.toList())
                : filteredWorlds.stream().filter(x -> !x.getTypes().contains(WorldType.MEMBERS)).collect(Collectors.toList());

        if (region != null) {
            filteredWorlds = filteredWorlds.stream()
                    .filter(x -> x.getRegion() == region)
                    .collect(Collectors.toList());
        }

        Random random = new Random();
        World world = filteredWorlds.stream()
                .skip(random.nextInt(filteredWorlds.size()))
                .findFirst()
                .orElse(null);

        return (world != null) ? world.getId() : (isMembers ? 360 : 383);
    }

    public static int getRandomWorld(boolean isMembers) {
        return getRandomWorld(isMembers, null);
    }

    public static int getNextWorld(boolean isMembers) {
        return getNextWorld(isMembers, null);
    }

    public static int getNextWorld(boolean isMembers, WorldRegion region) {
        WorldResult worldResult = ProjectX.getWorldService().getWorlds();

        if (worldResult == null) {
            return isMembers ? 360 : 383;
        }

        List<World> worlds = worldResult.getWorlds();
        boolean isInSeasonalWorld = ProjectX.getClient().getWorldType().contains(WorldType.SEASONAL);

        List<World> filteredWorlds = worlds.stream()
                .filter(x -> !x.getTypes().contains(WorldType.PVP) &&
                        !x.getTypes().contains(WorldType.HIGH_RISK) &&
                        !x.getTypes().contains(WorldType.BOUNTY) &&
                        !x.getTypes().contains(WorldType.SKILL_TOTAL) &&
                        !x.getTypes().contains(WorldType.LAST_MAN_STANDING) &&
                        !x.getTypes().contains(WorldType.QUEST_SPEEDRUNNING) &&
                        !x.getTypes().contains(WorldType.BETA_WORLD) &&
                        !x.getTypes().contains(WorldType.DEADMAN) &&
                        !x.getTypes().contains(WorldType.PVP_ARENA) &&
                        !x.getTypes().contains(WorldType.TOURNAMENT) &&
                        !x.getTypes().contains(WorldType.NOSAVE_MODE) &&
                        !x.getTypes().contains(WorldType.LEGACY_ONLY) &&
                        !x.getTypes().contains(WorldType.EOC_ONLY) &&
                        !x.getTypes().contains(WorldType.FRESH_START_WORLD) &&
                        x.getPlayers() < MAX_PLAYER_COUNT &&
                        x.getPlayers() >= 0)
                .filter(x -> isInSeasonalWorld == x.getTypes().contains(WorldType.SEASONAL)) // Strict seasonal filter
                .collect(Collectors.toList());

        filteredWorlds = isMembers
                ? filteredWorlds.stream().filter(x -> x.getTypes().contains(WorldType.MEMBERS)).collect(Collectors.toList())
                : filteredWorlds.stream().filter(x -> !x.getTypes().contains(WorldType.MEMBERS)).collect(Collectors.toList());

        if (region != null) {
            filteredWorlds = filteredWorlds.stream()
                    .filter(x -> x.getRegion() == region)
                    .collect(Collectors.toList());
        }

        int currentWorldId = ProjectX.getClient().getWorld();
        int currentIndex = -1;

        for (int i = 0; i < filteredWorlds.size(); i++) {
            if (filteredWorlds.get(i).getId() == currentWorldId) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1) {
            int nextIndex = (currentIndex + 1) % filteredWorlds.size();
            return filteredWorlds.get(nextIndex).getId();
        } else if (!filteredWorlds.isEmpty()) {
            // If current world is not found in the filtered list, pick the first in the list
            return filteredWorlds.get(0).getId();
        }

        return isMembers ? 360 : 383;
    }

    public void setWorld(int worldNumber) {
        try {
            net.runelite.http.api.worlds.World world = ProjectX.getWorldService().getWorlds().findWorld(worldNumber);
            final net.runelite.api.World rsWorld = ProjectX.getClient().createWorld();
            rsWorld.setActivity(world.getActivity());
            rsWorld.setAddress(world.getAddress());
            rsWorld.setId(world.getId());
            rsWorld.setPlayerCount(world.getPlayers());
            rsWorld.setLocation(world.getLocation());
            rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));
            ProjectX.getClient().changeWorld(rsWorld);
        } catch (Exception ex) {
            System.out.println("Failed to find world");
        }
    }
}
