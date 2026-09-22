# ProjectX Client-Thread Manifest

Generated: 2026-04-09  
Source: `runelite-client/src/test/java/net/runelite/client/plugins/projectx/threadsafety/ClientThreadScannerTest.java`

> Manually regenerate with `./gradlew :client:runClientThreadScanner`. Commit the diff to track how RuneLite's client-thread surface evolves between revisions.

## Summary

| Category | Count |
|---|---:|
| Classes scanned | 5445 |
| Methods scanned | 35573 |
| `REQUIRES_CLIENT_THREAD` (asserts) | 12 |
| `CHECKS_THREAD_GUARD` (branches) | 26 |
| `SELF_MARSHALLING` (wraps invoke) | 474 |
| `EVENT_HANDLER` (`@Subscribe`) | 937 |
| `CONFIRMED_LAMBDA` (passed to invoke) | 457 |
| RuneLite API methods inferred client-thread-only | 650 |

## Legend

- **REQUIRES_CLIENT_THREAD** — `assert client.isClientThread()` in body. Throws `AssertionError` off-thread when `-ea` is enabled.
- **CHECKS_THREAD_GUARD** — Reads `isClientThread()` to branch. Often a sleep/wait guard or a hybrid helper.
- **SELF_MARSHALLING** — Calls `ClientThread.invoke*()` / `runOnClientThreadOptional()`. Safe to call from any thread.
- **EVENT_HANDLER** — Annotated `@Subscribe`. RuneLite's event bus dispatches these on the client thread.
- **CONFIRMED_LAMBDA** — Synthetic lambda body that was passed to `ClientThread.invoke*()`. Reached transitively, including nested lambdas.

## Methods that ASSERT client thread

<details><summary>12 method(s) across 10 class(es)</summary>

**`net.runelite.client.callback.ClientThread`**

- `invokeList(ConcurrentLinkedQueue): void`

**`net.runelite.client.game.ChatIconManager`**

- `reserveChatIcon(): int`

**`net.runelite.client.game.SpriteManager`**

- `getSprite(int, int): BufferedImage`

**`net.runelite.client.party.PartyService`**

- `generatePassphrase(): String`

**`net.runelite.client.plugins.projectx.inventorysetups.MInventorySetupsPlugin`**

- `getCurrentSpellbook(): int`
- `getNormalizedContainer(int): List`

**`net.runelite.client.plugins.projectx.util.bank.Rs2Bank`**

- `updateLocalBank(ItemContainerChanged): void`

**`net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment`**

- `storeEquipmentItemsInMemory(ItemContainerChanged): void`

**`net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory`**

- `storeInventoryItemsInMemory(ItemContainerChanged): void`

**`net.runelite.client.plugins.projectx.util.inventory.Rs2RunePouch`**

- `onVarbitChanged(VarbitChanged): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.worldhopper.WorldHopperPlugin`**

- `hop(World): void`

</details>

## Methods that GUARD on client thread

<details><summary>26 method(s) across 10 class(es)</summary>

**`net.runelite.client.callback.ClientThread`**

- `invoke(BooleanSupplier): void`
- `invoke(Supplier): Object`
- `runOnClientThreadOptional(Callable): Optional`

**`net.runelite.client.plugins.projectx.ProjectX`**

- `click(Rectangle): void`
- `click(Rectangle, NewMenuEntry): void`
- `drag(Rectangle, Rectangle): void`

**`net.runelite.client.plugins.projectx.questhelper.managers.QuestManager`**

- `startUpQuest(QuestHelper, boolean): void`

**`net.runelite.client.plugins.projectx.questhelper.util.Utils`**

- `addChatMessage(Client, String): void`

**`net.runelite.client.plugins.projectx.util.Global`**

- `sleep(int): void`
- `sleepUntil(BooleanSupplier, Runnable, long, int): boolean`
- `sleepUntil(BooleanSupplier, int): boolean`
- `sleepUntilNotNull(Callable, int, int): Object`
- `sleepUntilOnClientThread(BooleanSupplier, int): void`
- `sleepUntilTrue(BooleanSupplier): boolean`
- `sleepUntilTrue(BooleanSupplier, BooleanSupplier, int, int): boolean`
- `sleepUntilTrue(BooleanSupplier, int, int): boolean`

**`net.runelite.client.plugins.projectx.util.mouse.VirtualMouse`**

- `click(Point, boolean): Mouse`
- `click(Point, boolean, NewMenuEntry): Mouse`

**`net.runelite.client.plugins.projectx.util.mouse.naturalmouse.NaturalMouse`**

- `moveTo(int, int): void`

**`net.runelite.client.plugins.projectx.util.walker.Rs2Walker`**

- `restartPathfinding(WorldPoint, Set): boolean`
- `setStart(WorldPoint): void`
- `setTarget(WorldPoint): void`
- `walkWithBankedTransportsAndState(WorldPoint, int, boolean): WalkerState`
- `walkWithStateInternal(WorldPoint, int): WalkerState`

**`net.runelite.client.ui.overlay.WidgetOverlay`**

- `getParentBounds(): Rectangle`

**`net.runelite.client.ui.overlay.WidgetOverlay$XpTrackerWidgetOverlay`**

- `getPosition(): OverlayPosition`

</details>

## Self-marshalling helpers

<details><summary>474 method(s) across 171 class(es)</summary>

**`net.runelite.client.callback.ClientThread`**

- `invoke(): void`
- `invoke(BooleanSupplier): void`
- `invoke(Runnable): void`
- `invoke(Supplier): Object`
- `invokeLater(Runnable): void`
- `invokeTickEnd(): void`
- `runOnClientThreadOptional(Callable): Optional`

**`net.runelite.client.callback.Hooks`**

- `tick(): void`
- `tickEnd(): void`

**`net.runelite.client.chat.ChatInputManager`**

- `lambda$handleInput$1(String, int, int): void`
- `lambda$handlePrivateMessage$3(String, String): void`

**`net.runelite.client.chat.ChatMessageManager`**

- `onConfigChanged(ConfigChanged): void`
- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.game.ChatIconManager`**

- `<init>(Client, SpriteManager, ClientThread, EventBus): void`
- `registerChatIcon(BufferedImage): int`

**`net.runelite.client.game.ItemManager`**

- `loadImage(int, int, boolean): AsyncBufferedImage`

**`net.runelite.client.game.SpriteManager`**

- `addSpriteOverrides(SpriteOverride[]): void`
- `getSpriteAsync(int, int, Consumer): void`
- `removeSpriteOverrides(SpriteOverride[]): void`

**`net.runelite.client.game.chatbox.ChatboxItemSearch`**

- `keyPressed(KeyEvent): void`
- `lambda$new$1(ClientThread, String): void`

**`net.runelite.client.game.chatbox.ChatboxPanelManager`**

- `close(): void`
- `openInput(ChatboxInput): void`

**`net.runelite.client.game.chatbox.ChatboxTextInput`**

- `cursorAt(int, int): ChatboxTextInput`
- `lines(int): ChatboxTextInput`
- `prompt(String): ChatboxTextInput`

**`net.runelite.client.game.npcoverlay.NpcOverlayService`**

- `rebuild(): void`

**`net.runelite.client.plugins.achievementdiary.DiaryRequirementsPlugin`**

- `showDiaryRequirements(): void`

**`net.runelite.client.plugins.ammo.AmmoPlugin`**

- `startUp(): void`

**`net.runelite.client.plugins.antidrag.AntiDragPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onFocusChanged(FocusChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.attackstyles.AttackStylesPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.bank.BankPlugin`**

- `onScriptPostFired(ScriptPostFired): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onWidgetLoaded(WidgetLoaded): void`
- `shutDown(): void`

**`net.runelite.client.plugins.bank.BankPlugin$1`**

- `keyPressed(KeyEvent): void`

**`net.runelite.client.plugins.bank.BankSearch`**

- `initSearch(): void`
- `reset(boolean): void`

**`net.runelite.client.plugins.banktags.BankTagsPlugin`**

- `lambda$editTags$11(int, String): void`
- `onConfigChanged(ConfigChanged): void`
- `resetConfiguration(): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.banktags.tabs.TabInterface`**

- `lambda$handleDeposit$7(List, String): void`
- `lambda$handleNewTab$9(String): void`
- `lambda$opTagTab$11(String, Integer): void`
- `lambda$opTagTab$13(String): void`
- `lambda$opTagTab$15(String): void`
- `lambda$renameTab$18(String, String): void`
- `lambda$renameTab$20(String): void`
- `lambda$renameTab$22(String, String): void`
- `onScriptPreFired(ScriptPreFired): void`

**`net.runelite.client.plugins.camera.CameraPlugin`**

- `keyReleased(KeyEvent): void`
- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.cannon.CannonPlugin`**

- `onConfigChanged(ConfigChanged): void`

**`net.runelite.client.plugins.chatchannel.ChatChannelPlugin`**

- `lambda$confirmKickPlayer$7(String): void`
- `onConfigChanged(ConfigChanged): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `rebuildClanTitle(): void`
- `rebuildFriendsChat(): void`
- `shutDown(): void`
- `startUp(): void`
- `timeoutMessages(): void`

**`net.runelite.client.plugins.chatcommands.ChatCommandsPlugin`**

- `startUp(): void`

**`net.runelite.client.plugins.chatcommands.ChatKeyboardListener`**

- `keyPressed(KeyEvent): void`

**`net.runelite.client.plugins.chathistory.ChatHistoryPlugin`**

- `clearChatboxHistory(ChatboxTab): void`
- `keyPressed(KeyEvent): void`

**`net.runelite.client.plugins.cluescrolls.ClueScrollPlugin`**

- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.crowdsourcing.music.CrowdsourcingMusic`**

- `onChatMessage(ChatMessage): void`

**`net.runelite.client.plugins.customcursor.CustomCursorPlugin`**

- `updateCursor(): void`

**`net.runelite.client.plugins.defaultworld.DefaultWorldPlugin`**

- `startUp(): void`

**`net.runelite.client.plugins.devtools.DevToolsPanel`**

- `lambda$createOptionsPanel$7(ActionEvent): void`

**`net.runelite.client.plugins.devtools.InventoryInspector`**

- `lambda$new$1(ClientThread, TreeSelectionEvent): void`

**`net.runelite.client.plugins.devtools.ShellFrame$1`**

- `invokeOnClientThread(Runnable): void`

**`net.runelite.client.plugins.devtools.VarInspector`**

- `open(): void`

**`net.runelite.client.plugins.devtools.WidgetInfoTableModel`**

- `setValueAt(Object, int, int): void`
- `setWidget(Widget): void`

**`net.runelite.client.plugins.devtools.WidgetInspector`**

- `close(): void`
- `lambda$new$10(ClientThread, ActionEvent): void`
- `open(): void`
- `refreshWidgets(): void`
- `searchWidgets(String): void`
- `setSelectedWidget(Widget, boolean): void`

**`net.runelite.client.plugins.fairyring.FairyRingPlugin`**

- `lambda$setTagMenuOpen$1(): void`
- `lambda$setTagMenuOpen$2(String, String): void`

**`net.runelite.client.plugins.friendnotes.FriendNotesPlugin`**

- `rebuildFriendsList(): void`
- `rebuildIgnoreList(): void`

**`net.runelite.client.plugins.gpu.GpuPlugin`**

- `loadScene(WorldView, Scene): void`
- `loadSubScene(WorldView, Scene): void`
- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.grandexchange.GrandExchangePlugin`**

- `startUp(): void`

**`net.runelite.client.plugins.grandexchange.GrandExchangeSearchPanel`**

- `priceLookup(boolean): void`

**`net.runelite.client.plugins.grounditems.GroundItemsPlugin`**

- `reset(): void`
- `shutDown(): void`

**`net.runelite.client.plugins.grounditems.Lootbeam`**

- `update(): void`

**`net.runelite.client.plugins.herbiboars.HerbiboarPlugin`**

- `startUp(): void`

**`net.runelite.client.plugins.interfacestyles.InterfaceStylesPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `overrideHealthBars(): void`
- `queueUpdateAllOverrides(): void`
- `restoreHealthBars(): void`
- `shutDown(): void`

**`net.runelite.client.plugins.itemcharges.ItemChargePlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onWidgetLoaded(WidgetLoaded): void`
- `startUp(): void`

**`net.runelite.client.plugins.itemstats.ItemStatPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`

**`net.runelite.client.plugins.keyremapping.KeyRemappingListener`**

- `keyPressed(KeyEvent): void`

**`net.runelite.client.plugins.keyremapping.KeyRemappingPlugin`**

- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.kourendlibrary.KourendLibraryPlugin`**

- `startUp(): void`

**`net.runelite.client.plugins.loginscreen.LoginScreenPlugin`**

- `lambda$overrideLoginScreen$4(BufferedImage): void`
- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.loottracker.LootTrackerPlugin`**

- `lambda$switchProfile$2(String): void`

**`net.runelite.client.plugins.lowmemory.LowMemoryPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.projectx.GameChatAppender`**

- `append(ILoggingEvent): void`

**`net.runelite.client.plugins.projectx.ProjectX`**

- `getDBRowsByValue(int, int, int, Object): List`
- `getDBTableField(int, int, int): Object[]`
- `getDBTableRows(int): List`
- `getEnum(int): EnumComposition`
- `getStructComposition(int): StructComposition`
- `hopToWorld(int): boolean`
- `lambda$openPopUp$16(String, String): void`
- `openPopUp(String, String): void`

**`net.runelite.client.plugins.projectx.ProjectXPlugin`**

- `hasWidgetOverlapWithBounds(Rectangle): boolean`

**`net.runelite.client.plugins.projectx.agentserver.handler.SkillsHandler`**

- `handleRequest(HttpExchange): void`

**`net.runelite.client.plugins.projectx.agentserver.handler.StateHandler`**

- `handleRequest(HttpExchange): void`

**`net.runelite.client.plugins.projectx.agentserver.handler.WidgetInvokeHandler`**

- `handleRequest(HttpExchange): void`

**`net.runelite.client.plugins.projectx.api.AbstractEntityQueryable`**

- `firstOnClientThread(): IEntity`
- `nearestOnClientThread(): IEntity`
- `nearestOnClientThread(WorldPoint, int): IEntity`
- `nearestOnClientThread(int): IEntity`
- `toListOnClientThread(): List`

**`net.runelite.client.plugins.projectx.api.actor.Rs2ActorModel`**

- `getCameraFocus(): LocalPoint`
- `getCombatLevel(): int`
- `getHealthRatio(): int`
- `getHealthScale(): int`
- `getInteracting(): Actor`
- `getName(): String`
- `getWorldLocation(): WorldPoint`
- `getWorldView(): WorldView`

**`net.runelite.client.plugins.projectx.api.boat.Rs2BoatCache`**

- `getBoat(Rs2PlayerModel): Rs2BoatModel`
- `getLocalBoat(): Rs2BoatModel`

**`net.runelite.client.plugins.projectx.api.boat.models.Rs2BoatModel`**

- `getCameraFocus(): LocalPoint`
- `getConfig(): WorldEntityConfig`
- `getLocalLocation(): LocalPoint`
- `getOrientation(): int`
- `getOwnerType(): int`
- `getPlayerBoatLocation(): WorldPoint`
- `getTargetLocation(): LocalPoint`
- `getTargetOrientation(): int`
- `getWorldLocation(): WorldPoint`
- `getWorldView(): WorldView`
- `isHiddenForOverlap(): boolean`
- `lambda$getPlayerBoatLocation$4(): WorldPoint`
- `setHeading(Heading): void`
- `transformToMainWorld(LocalPoint): LocalPoint`

**`net.runelite.client.plugins.projectx.api.npc.models.Rs2NpcModel`**

- `click(String): boolean`
- `getDistanceFromPlayer(): int`
- `isInteractingWithPlayer(): boolean`
- `isMoving(): boolean`
- `isWithinDistanceFromPlayer(int): boolean`

**`net.runelite.client.plugins.projectx.api.player.models.Rs2PlayerModel`**

- `getOverheadIcon(): HeadIcon`
- `getSkullIcon(): int`
- `isClanMember(): boolean`
- `isFriend(): boolean`
- `isFriendsChatMember(): boolean`

**`net.runelite.client.plugins.projectx.api.playerstate.Rs2PlayerStateCache`**

- `populateQuests(): void`
- `updateVarbitValue(int): int`
- `updateVarpValue(int): int`

**`net.runelite.client.plugins.projectx.api.tileitem.models.Rs2TileItemModel`**

- `click(String): boolean`
- `getName(): String`
- `getTotalGeValue(): int`
- `getTotalValue(): int`
- `isMembers(): boolean`
- `isNoted(): boolean`
- `isProfitableToHighAlch(): boolean`
- `isStackable(): boolean`
- `isTradeable(): boolean`

**`net.runelite.client.plugins.projectx.api.tileobject.models.Rs2TileObjectModel`**

- `getName(): String`
- `getObjectComposition(): ObjectComposition`
- `isReachable(): boolean`

**`net.runelite.client.plugins.projectx.example.ExampleScript`**

- `lambda$checkEquipment$31(): void`
- `lambda$checkWorldViewAndThreading$13(): void`
- `lambda$checkWorldViewAndThreading$15(): void`

**`net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsAmmoHandler`**

- `handleSpecialHighlighting(InventorySetup, List, List): void`

**`net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsBankSearch`**

- `initSearch(): void`
- `reset(boolean): void`

**`net.runelite.client.plugins.projectx.inventorysetups.MInventorySetupsPlugin`**

- `addAdditionalFilteredItem(int, InventorySetup, Map): void`
- `addInventorySetup(): void`
- `addInventorySetup(InventorySetup): void`
- `addInventorySetup(String): void`
- `doBankSearch(): void`
- `importSetup(): void`
- `lambda$previewNewLayout$22(InventorySetup, InventorySetupLayoutType, Layout): void`
- `lambda$previewNewLayout$25(Layout): void`
- `lambda$startUp$5(): boolean`
- `lambda$updateCurrentSetup$46(InventorySetup): void`
- `lambda$updateSlotFromSearch$53(InventorySetupsSlot, boolean, boolean, Integer): void`
- `massImportSetups(): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWidgetClosed(WidgetClosed): void`
- `previewNewLayout(InventorySetup, InventorySetupLayoutType): void`
- `removeInventorySetup(InventorySetup): void`
- `removeItemFromSlot(InventorySetupsSlot): void`
- `resetBankSearch(): void`
- `startUp(): void`
- `switchProfile(): void`
- `toggleFuzzyOnSlot(InventorySetupsSlot): void`
- `toggleLockOnSlot(InventorySetupsSlot): void`
- `triggerBankSearchFromHotKey(): void`
- `updateCurrentSetup(InventorySetup, boolean): void`
- `updateExisting(InventorySetup): void`
- `updateNotesInSetup(InventorySetup, String): void`
- `updateSetupName(InventorySetup, String): void`
- `updateSlotFromContainer(InventorySetupsSlot, boolean): void`
- `updateSlotFromSearchHelper(InventorySetupsSlot, InventorySetupsItem, InventorySetupsItem, List, boolean): void`
- `updateSpellbookInSetup(int): void`

**`net.runelite.client.plugins.projectx.inventorysetups.ui.InventorySetupsPluginPanel`**

- `setCurrentInventorySetup(InventorySetup, boolean): void`

**`net.runelite.client.plugins.projectx.inventorysetups.ui.InventorySetupsSpellbookPanel`**

- `<init>(ItemManager, MInventorySetupsPlugin): void`
- `highlightSlots(List, InventorySetup): void`

**`net.runelite.client.plugins.projectx.inventorysetups.ui.InventorySetupsStandardPanel`**

- `lambda$new$1(MInventorySetupsPlugin, InventorySetupsPluginPanel, ActionEvent): void`

**`net.runelite.client.plugins.projectx.questhelper.QuestHelperPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `refreshBank(): void`
- `startUp(): void`

**`net.runelite.client.plugins.projectx.questhelper.QuestScript`**

- `chooseCorrectNPCOption(QuestStep, Rs2NpcModel): String`
- `chooseCorrectObjectOption(QuestStep, Rs2TileObjectModel): String`
- `lambda$run$8(QuestHelperConfig, QuestHelperPlugin): void`

**`net.runelite.client.plugins.projectx.questhelper.bank.banktab.QuestBankTab`**

- `onScriptPostFired(ScriptPostFired): void`
- `shutDown(): void`
- `sortBankTabItems(Widget, Widget[], List): void`
- `startUp(): void`

**`net.runelite.client.plugins.projectx.questhelper.bank.banktab.QuestBankTabInterface`**

- `init(): void`

**`net.runelite.client.plugins.projectx.questhelper.bank.banktab.QuestGrandExchangeInterface`**

- `activateTab(): void`
- `closeOptions(): void`
- `onceOffActivateTab(Widget): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.mischelpers.farmruns.TreeRun`**

- `onConfigChanged(ConfigChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.akingdomdivided.StonePuzzleStep`**

- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.demonslayer.IncantationStep`**

- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.shadowofthestorm.IncantationStep`**

- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.songoftheelves.BaxtorianPuzzle`**

- `onGraphicsObjectCreated(GraphicsObjectCreated): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.managers.QuestManager`**

- `getAllItemRequirements(): void`
- `handleConfigChanged(): void`
- `handleVarbitChanged(): void`
- `lambda$initializeNewQuest$6(QuestHelper): void`
- `startUpBackgroundQuest(String): void`
- `startUpQuest(QuestHelper, boolean): void`
- `updateAllItemsHelper(): void`

**`net.runelite.client.plugins.projectx.questhelper.questhelpers.BasicQuestHelper`**

- `setSelectedStateOverride(Integer): void`

**`net.runelite.client.plugins.projectx.questhelper.runeliteobjects.extendedruneliteobjects.ExtendedRuneliteObject`**

- `update(): void`

**`net.runelite.client.plugins.projectx.questhelper.runeliteobjects.extendedruneliteobjects.FakeNpc`**

- `update(): void`

**`net.runelite.client.plugins.projectx.questhelper.runeliteobjects.extendedruneliteobjects.RuneliteObjectManager`**

- `lambda$onWidgetLoaded$26(WidgetLoaded, String, ExtendedRuneliteObjects): void`
- `removeGroupAndSubgroups(String): void`
- `replaceWidgetsForReplacedNpcs(ReplacedNpc, WidgetLoaded): void`
- `shutDown(): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.QuestStep`**

- `onWidgetLoaded(WidgetLoaded): void`
- `startUp(): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.playermadesteps.RuneliteObjectStep`**

- `shutDown(): void`

**`net.runelite.client.plugins.projectx.shortestpath.ShortestPathPlugin`**

- `restartPathfinding(WorldPoint, Set, boolean): void`

**`net.runelite.client.plugins.projectx.util.ActorModel`**

- `getCameraFocus(): LocalPoint`
- `getCombatLevel(): int`
- `getHealthRatio(): int`
- `getHealthScale(): int`
- `getInteracting(): Actor`
- `getLocalLocation(): LocalPoint`
- `getName(): String`
- `getWorldArea(): WorldArea`
- `getWorldLocation(): WorldPoint`

**`net.runelite.client.plugins.projectx.util.Global`**

- `sleepUntilOnClientThread(BooleanSupplier, int): void`

**`net.runelite.client.plugins.projectx.util.bank.Rs2Bank`**

- `getItems(): List`
- `getTabs(): List`
- `scrollBankToSlot(int): boolean`
- `withdrawLootItems(String, List): boolean`

**`net.runelite.client.plugins.projectx.util.bank.Rs2BankData`**

- `rebuildBankItemsList(): void`

**`net.runelite.client.plugins.projectx.util.camera.Rs2Camera`**

- `setZoom(int): void`

**`net.runelite.client.plugins.projectx.util.combat.Rs2Combat`**

- `inCombat(): boolean`

**`net.runelite.client.plugins.projectx.util.combat.models.Rs2DropSource`**

- `getItemComposition(): ItemComposition`

**`net.runelite.client.plugins.projectx.util.farming.Rs2Farming`**

- `getPatchByRegionAndVarbit(String, int): Optional`
- `getPatchesByTab(Tab): List`
- `isFarmingSystemReady(): boolean`
- `predictPatchState(FarmingPatch): CropState`

**`net.runelite.client.plugins.projectx.util.gameobject.Rs2Cannon`**

- `lambda$refill$2(): boolean`
- `refill(int): boolean`

**`net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject`**

- `clickObject(TileObject, String): boolean`
- `convertToObjectCompositionInternal(int, boolean): ObjectComposition`
- `getObjectComposition(int): ObjectComposition`
- `getSceneObjects(Function): Stream`

**`net.runelite.client.plugins.projectx.util.gameobject.Rs2ObjectModel`**

- `getDistanceFromPlayer(): int`
- `getObjectComposition(): ObjectComposition`
- `getTicksSinceCreation(): int`

**`net.runelite.client.plugins.projectx.util.grandexchange.Rs2GrandExchange`**

- `getSearchResultWidget(String, boolean): Pair`
- `setChatboxValue(int): void`

**`net.runelite.client.plugins.projectx.util.grounditem.Rs2GroundItem`**

- `getAllAt(int, int): RS2Item[]`
- `getAllFromWorldPoint(int, WorldPoint): RS2Item[]`
- `interact(InteractModel, String): boolean`
- `lambda$isItemBasedOnValueOnGround$25(int, RS2Item): boolean`
- `lambda$lootItemBasedOnValue$10(int, RS2Item): boolean`
- `lootAllItemBasedOnValue(int, int): boolean`

**`net.runelite.client.plugins.projectx.util.grounditem.models.Rs2SpawnLocation`**

- `getItemComposition(): ItemComposition`

**`net.runelite.client.plugins.projectx.util.inventory.Rs2FuzzyItem`**

- `getItemName(int): String`

**`net.runelite.client.plugins.projectx.util.inventory.Rs2Gembag`**

- `getGemItemModel(String): Rs2ItemModel`

**`net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory`**

- `getInventory(): Widget`
- `invokeMenu(Rs2ItemModel, String): void`
- `items(): Stream`
- `lambda$dropAllExcept$24(int, Rs2ItemModel): boolean`

**`net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel`**

- `<init>(int, int, int): void`
- `ensureCompositionLoaded(): void`
- `getNotedId(int): int`
- `getPrice(): int`
- `getUnNotedId(int): int`
- `initializeFromComposition(ItemComposition): void`
- `isHaProfitable(): boolean`

**`net.runelite.client.plugins.projectx.util.item.Rs2EnsouledHead`**

- `lambda$reanimate$1(): boolean`

**`net.runelite.client.plugins.projectx.util.item.Rs2ItemManager`**

- `getItemComposition(int): ItemComposition`
- `searchItem(String): List`

**`net.runelite.client.plugins.projectx.util.magic.Rs2Magic`**

- `lambda$alch$10(): boolean`
- `lambda$alch$8(): boolean`
- `lambda$superHeat$12(): boolean`

**`net.runelite.client.plugins.projectx.util.misc.Rs2UiHelper`**

- `getActorClickbox(Actor): Rectangle`
- `getObjectClickbox(TileObject): Rectangle`

**`net.runelite.client.plugins.projectx.util.npc.Rs2Npc`**

- `getAvailableAction(Rs2NpcModel, List): String`
- `getNpcs(Predicate): Stream`
- `hasAction(int, String): boolean`
- `interact(Rs2NpcModel, String): boolean`
- `isMoving(NPC): boolean`

**`net.runelite.client.plugins.projectx.util.npc.Rs2NpcModel`**

- `getDistanceFromPlayer(): int`
- `isInteractingWithPlayer(): boolean`
- `isMoving(): boolean`
- `isWithinDistanceFromPlayer(int): boolean`

**`net.runelite.client.plugins.projectx.util.player.Rs2Player`**

- `getAnimation(): int`
- `getBoostedSkillLevel(Skill): int`
- `getCombatLevel(): int`
- `getInteracting(): Actor`
- `getPlayerEquipmentNames(Rs2PlayerModel): Map`
- `getPoseAnimation(): int`
- `getRealSkillLevel(Skill): int`
- `getWorldLocation_Internal(): WorldPoint`
- `getWorldView_Internal(): WorldView`
- `isMoving(): boolean`
- `isMoving(Rs2PlayerModel): boolean`
- `updateCombatTime(): void`

**`net.runelite.client.plugins.projectx.util.reachable.Rs2Reachable`**

- `getReachableTiles(WorldPoint): IntSet`

**`net.runelite.client.plugins.projectx.util.reflection.Rs2Reflection`**

- `invokeMenu(int, int, int, int, int, int, String, String, int, int): void`

**`net.runelite.client.plugins.projectx.util.settings.Rs2Settings`**

- `findSettingsSearchClickable(String[]): Widget`

**`net.runelite.client.plugins.projectx.util.shop.Rs2Shop`**

- `invokeMenu(Rs2ItemModel, String): void`

**`net.runelite.client.plugins.projectx.util.shop.models.Rs2ShopItem`**

- `getItemComposition(): ItemComposition`

**`net.runelite.client.plugins.projectx.util.skills.slayer.Rs2Slayer`**

- `getSlayerTaskWeaknessName(): String`

**`net.runelite.client.plugins.projectx.util.tileobject.Rs2TileObjectModel`**

- `getName(): String`
- `getObjectComposition(): ObjectComposition`

**`net.runelite.client.plugins.projectx.util.walker.Rs2MiniMap`**

- `getMinimapClipArea(double): Shape`
- `localToMinimap(LocalPoint): Point`
- `worldToMinimap(WorldPoint): Point`

**`net.runelite.client.plugins.projectx.util.walker.Rs2Walker`**

- `handleCharterShip(Transport): boolean`

**`net.runelite.client.plugins.projectx.util.widget.Rs2Widget`**

- `clickChildWidget(int, int): boolean`
- `clickWidget(String, Optional, int, boolean): boolean`
- `clickWidget(int): boolean`
- `enableQuantityOption(String): boolean`
- `findWidget(String, List, boolean): Widget`
- `findWidget(int, List): Widget`
- `getChildWidgetSpriteID(int, int): int`
- `getWidget(int): Widget`
- `getWidget(int, int): Widget`
- `hasWidgetText(String, int, boolean): boolean`
- `hasWidgetText(String, int, int, boolean): boolean`
- `isHidden(int): boolean`
- `isHidden(int, int): boolean`
- `isWidgetVisible(int): boolean`
- `isWidgetVisible(int, int): boolean`
- `lambda$waitForWidget$27(String): boolean`

**`net.runelite.client.plugins.projectx.util.widget.Rs2WidgetInspector`**

- `describeWidget(int, int, int): List`
- `getVisibleInterfaces(): List`
- `getWidgetPath(Widget): String`
- `search(String[]): List`

**`net.runelite.client.plugins.minimap.MinimapPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.motherlode.MotherlodePlugin`**

- `startUp(): void`

**`net.runelite.client.plugins.music.MusicPlugin`**

- `lambda$openSearch$11(): void`
- `lambda$openSearch$7(String): void`
- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.npchighlight.NpcIndicatorsPlugin`**

- `lambda$createTagColorMenu$5(NPC, Color, MenuEntry): void`
- `lambda$createTagColorMenu$6(NPC, Color): void`
- `lambda$createTagColorMenu$9(NPC, MenuEntry): void`
- `lambda$createTagStyleMenu$10(NPC, String, MenuEntry): void`
- `lambda$createTagStyleMenu$11(NPC, MenuEntry): void`
- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.npcunaggroarea.NpcAggroAreaPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `startUp(): void`

**`net.runelite.client.plugins.objectindicators.ObjectIndicatorsPlugin`**

- `lambda$createTagBorderColorMenu$12(TileObject, Color): void`
- `lambda$createTagFillColorMenu$19(TileObject, Color): void`
- `onProfileChanged(ProfileChanged): void`
- `startUp(): void`

**`net.runelite.client.plugins.party.PartyPanel`**

- `lambda$new$1(PartyService, PartyPlugin, ClientThread, ActionEvent): void`

**`net.runelite.client.plugins.party.PartyPlugin`**

- `onTilePing(TilePing): void`
- `onUserSync(UserSync): void`

**`net.runelite.client.plugins.playerindicators.PlayerIndicatorsPlugin`**

- `onScriptPostFired(ScriptPostFired): void`

**`net.runelite.client.plugins.poison.PoisonPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.prayer.PrayerReorder`**

- `onProfileChanged(ProfileChanged): void`
- `reset(): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.pyramidplunder.PyramidPlunderPlugin`**

- `shutDown(): void`

**`net.runelite.client.plugins.questlist.QuestListPlugin`**

- `redrawQuests(): void`
- `startUp(): void`

**`net.runelite.client.plugins.raids.RaidsPlugin`**

- `startUp(): void`

**`net.runelite.client.plugins.raids.RaidsPlugin$1`**

- `hotkeyPressed(): void`

**`net.runelite.client.plugins.reportbutton.ReportButtonPlugin`**

- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.roofremoval.RoofRemovalPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.rsnhider.RsnHiderPlugin`**

- `shutDown(): void`

**`net.runelite.client.plugins.screenshot.ScreenshotPlugin`**

- `lambda$takeScreenshot$3(String, String, Image): void`

**`net.runelite.client.plugins.skillcalculator.SkillCalculator`**

- `renderActionSlots(): void`

**`net.runelite.client.plugins.skillcalculator.UIActionSlot`**

- `<init>(SkillAction, ClientThread, ItemManager, JLabel): void`

**`net.runelite.client.plugins.slayer.SlayerPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onVarbitChanged(VarbitChanged): void`
- `startUp(): void`

**`net.runelite.client.plugins.specialcounter.SpecialCounterPlugin`**

- `onSpecialCounterUpdate(SpecialCounterUpdate): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.spellbook.SpellbookPlugin`**

- `onProfileChanged(ProfileChanged): void`
- `onScriptPreFired(ScriptPreFired): void`
- `resetConfiguration(): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.statusbars.StatusBarsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `startUp(): void`

**`net.runelite.client.plugins.team.TeamPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `startUp(): void`

**`net.runelite.client.plugins.timestamp.TimestampPlugin`**

- `onConfigChanged(ConfigChanged): void`

**`net.runelite.client.plugins.virtuallevels.VirtualLevelsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onPluginChanged(PluginChanged): void`
- `shutDown(): void`

**`net.runelite.client.plugins.wiki.WikiDpsManager`**

- `onScriptPreFired(ScriptPreFired): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.wiki.WikiPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `shutDown(): void`
- `startUp(): void`

**`net.runelite.client.plugins.wiki.WikiSearchChatboxTextInput`**

- `lambda$new$3(ClientThread, ScheduledExecutorService, boolean, OkHttpClient, Gson, String): void`

**`net.runelite.client.plugins.wiki.WikiSearchChatboxTextInput$1`**

- `onResponse(Call, Response): void`

**`net.runelite.client.plugins.woodcutting.WoodcuttingPlugin`**

- `onConfigChanged(ConfigChanged): void`

**`net.runelite.client.plugins.worldhopper.WorldHopperPlugin`**

- `hopTo(World): void`
- `updateList(): void`

**`net.runelite.client.plugins.worldhopper.WorldHopperPlugin$1`**

- `hotkeyPressed(): void`

**`net.runelite.client.plugins.worldhopper.WorldHopperPlugin$2`**

- `hotkeyPressed(): void`

**`net.runelite.client.plugins.xptracker.XpTrackerPlugin`**

- `startUp(): void`

**`net.runelite.client.plugins.zalcano.ZalcanoPlugin`**

- `startUp(): void`

**`net.runelite.client.ui.ClientUI`**

- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.ui.overlay.components.ButtonComponent$1`**

- `mousePressed(MouseEvent): MouseEvent`

**`net.runelite.client.util.AsyncBufferedImage`**

- `onLoaded(Runnable): void`

**`net.runelite.client.util.GameEventManager`**

- `simulateGameEvents(Object): void`

</details>

## Event handlers (@Subscribe)

<details><summary>937 method(s) across 278 class(es)</summary>

**`net.runelite.client.ClientSessionManager`**

- `onClientShutdown(ClientShutdown): void`

**`net.runelite.client.RuntimeConfigRefresher`**

- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.callback.Hooks`**

- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.chat.ChatCommandManager`**

- `onChatMessage(ChatMessage): void`
- `onChatboxInput(ChatboxInput): void`
- `onPrivateMessageInput(PrivateMessageInput): void`

**`net.runelite.client.chat.ChatInputManager`**

- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.chat.ChatMessageManager`**

- `onConfigChanged(ConfigChanged): void`
- `onProfileChanged(ProfileChanged): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.config.ConfigManager`**

- `onAccountHashChanged(AccountHashChanged): void`
- `onClientShutdown(ClientShutdown): void`
- `onPlayerChanged(PlayerChanged): void`
- `onRuneScapeProfileChanged(RuneScapeProfileChanged): void`
- `onSessionClose(SessionClose): void`
- `onSessionOpen(SessionOpen): void`
- `onWorldChanged(WorldChanged): void`

**`net.runelite.client.externalplugins.ExternalPluginManager`**

- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.game.ChatIconManager`**

- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.game.ItemManager`**

- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.game.LootManager`**

- `onAnimationChanged(AnimationChanged): void`
- `onGameTick(GameTick): void`
- `onItemDespawned(ItemDespawned): void`
- `onItemSpawned(ItemSpawned): void`
- `onNpcChanged(NpcChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onPlayerDespawned(PlayerDespawned): void`
- `onScriptPreFired(ScriptPreFired): void`

**`net.runelite.client.game.NpcUtil`**

- `onAnimationChanged(AnimationChanged): void`
- `onNpcChanged(NpcChanged): void`

**`net.runelite.client.game.chatbox.ChatboxPanelManager`**

- `onGameStateChanged(GameStateChanged): void`
- `onScriptPreFired(ScriptPreFired): void`

**`net.runelite.client.game.npcoverlay.NpcOverlayService`**

- `onGameStateChanged(GameStateChanged): void`
- `onNpcChanged(NpcChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`

**`net.runelite.client.input.KeyManager`**

- `onFocusChanged(FocusChanged): void`

**`net.runelite.client.menus.MenuManager`**

- `onMenuEntryAdded(MenuEntryAdded): void`
- `onPlayerMenuOptionsChanged(PlayerMenuOptionsChanged): void`

**`net.runelite.client.party.PartyService`**

- `onPartyChatMessage(PartyChatMessage): void`
- `onUserJoin(UserJoin): void`
- `onUserPart(UserPart): void`

**`net.runelite.client.plugins.PluginManager`**

- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.plugins.achievementdiary.DiaryRequirementsPlugin`**

- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.agility.AgilityPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onDecorativeObjectDespawned(DecorativeObjectDespawned): void`
- `onDecorativeObjectSpawned(DecorativeObjectSpawned): void`
- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onGroundObjectDespawned(GroundObjectDespawned): void`
- `onGroundObjectSpawned(GroundObjectSpawned): void`
- `onItemDespawned(ItemDespawned): void`
- `onItemSpawned(ItemSpawned): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`
- `onStatChanged(StatChanged): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWallObjectDespawned(WallObjectDespawned): void`
- `onWallObjectSpawned(WallObjectSpawned): void`

**`net.runelite.client.plugins.ammo.AmmoPlugin`**

- `onItemContainerChanged(ItemContainerChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.antidrag.AntiDragPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onFocusChanged(FocusChanged): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.attackstyles.AttackStylesPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.bank.BankPlugin`**

- `onItemContainerChanged(ItemContainerChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onMenuShouldLeftClick(MenuShouldLeftClick): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.banktags.BankTagsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGrandExchangeSearched(GrandExchangeSearched): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.plugins.banktags.tabs.LayoutManager`**

- `onScriptPreFired(ScriptPreFired): void`

**`net.runelite.client.plugins.banktags.tabs.PotionStorage`**

- `onClientTick(ClientTick): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWidgetClosed(WidgetClosed): void`

**`net.runelite.client.plugins.banktags.tabs.TabInterface`**

- `onMenuEntryAdded(MenuEntryAdded): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onWidgetClosed(WidgetClosed): void`
- `onWidgetDrag(WidgetDrag): void`

**`net.runelite.client.plugins.barbarianassault.BarbarianAssaultPlugin`**

- `onChatMessage(ChatMessage): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.barrows.BarrowsPlugin`**

- `onBeforeRender(BeforeRender): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onWidgetClosed(WidgetClosed): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.blastfurnace.BlastFurnacePlugin`**

- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.blastmine.BlastMinePlugin`**

- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.boosts.BoostsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onStatChanged(StatChanged): void`

**`net.runelite.client.plugins.bosstimer.BossTimersPlugin`**

- `onChatMessage(ChatMessage): void`
- `onNpcChanged(NpcChanged): void`
- `onNpcDespawned(NpcDespawned): void`

**`net.runelite.client.plugins.camera.CameraPlugin`**

- `onBeforeRender(BeforeRender): void`
- `onClientTick(ClientTick): void`
- `onConfigChanged(ConfigChanged): void`
- `onFocusChanged(FocusChanged): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.cannon.CannonPlugin`**

- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.chatchannel.ChatChannelPlugin`**

- `onClanMemberJoined(ClanMemberJoined): void`
- `onClanMemberLeft(ClanMemberLeft): void`
- `onConfigChanged(ConfigChanged): void`
- `onFriendsChatChanged(FriendsChatChanged): void`
- `onFriendsChatMemberJoined(FriendsChatMemberJoined): void`
- `onFriendsChatMemberLeft(FriendsChatMemberLeft): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onVarClientStrChanged(VarClientStrChanged): void`

**`net.runelite.client.plugins.chatcommands.ChatCommandsPlugin`**

- `onChatMessage(ChatMessage): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.chatfilter.ChatFilterPlugin`**

- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onOverheadTextChanged(OverheadTextChanged): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.plugins.chathistory.ChatHistoryPlugin`**

- `onChatMessage(ChatMessage): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onMenuOptionClicked(MenuOptionClicked): void`

**`net.runelite.client.plugins.chatnotifications.ChatNotificationsPlugin`**

- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.plugins.cluescrolls.ClueScrollPlugin`**

- `onChatMessage(ChatMessage): void`
- `onCommandExecuted(CommandExecuted): void`
- `onConfigChanged(ConfigChanged): void`
- `onDecorativeObjectDespawned(DecorativeObjectDespawned): void`
- `onDecorativeObjectSpawned(DecorativeObjectSpawned): void`
- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onGroundObjectDespawned(GroundObjectDespawned): void`
- `onGroundObjectSpawned(GroundObjectSpawned): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`
- `onWallObjectDespawned(WallObjectDespawned): void`
- `onWallObjectSpawned(WallObjectSpawned): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.combatlevel.CombatLevelPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onStatChanged(StatChanged): void`

**`net.runelite.client.plugins.config.ConfigPanel`**

- `onExternalPluginsChanged(ExternalPluginsChanged): void`
- `onPluginChanged(PluginChanged): void`
- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.plugins.config.ConfigPlugin`**

- `onOverlayMenuClicked(OverlayMenuClicked): void`

**`net.runelite.client.plugins.config.PluginHubPanel`**

- `onExternalPluginsChanged(ExternalPluginsChanged): void`

**`net.runelite.client.plugins.config.PluginListPanel`**

- `onExternalPluginsChanged(ExternalPluginsChanged): void`
- `onPluginChanged(PluginChanged): void`
- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.plugins.config.ProfilePanel`**

- `onProfileChanged(ProfileChanged): void`
- `onRuneScapeProfileChanged(RuneScapeProfileChanged): void`
- `onSessionClose(SessionClose): void`
- `onSessionOpen(SessionOpen): void`

**`net.runelite.client.plugins.cooking.CookingPlugin`**

- `onChatMessage(ChatMessage): void`
- `onGameTick(GameTick): void`
- `onGraphicChanged(GraphicChanged): void`

**`net.runelite.client.plugins.corp.CorpPlugin`**

- `onGameStateChanged(GameStateChanged): void`
- `onHitsplatApplied(HitsplatApplied): void`
- `onInteractingChanged(InteractingChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.crowdsourcing.cooking.CrowdsourcingCooking`**

- `onChatMessage(ChatMessage): void`
- `onMenuOptionClicked(MenuOptionClicked): void`

**`net.runelite.client.plugins.crowdsourcing.dialogue.CrowdsourcingDialogue`**

- `onChatMessage(ChatMessage): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.crowdsourcing.music.CrowdsourcingMusic`**

- `onChatMessage(ChatMessage): void`

**`net.runelite.client.plugins.crowdsourcing.thieving.CrowdsourcingThieving`**

- `onChatMessage(ChatMessage): void`
- `onMenuOptionClicked(MenuOptionClicked): void`

**`net.runelite.client.plugins.crowdsourcing.woodcutting.CrowdsourcingWoodcutting`**

- `onChatMessage(ChatMessage): void`
- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameTick(GameTick): void`
- `onMenuOptionClicked(MenuOptionClicked): void`

**`net.runelite.client.plugins.crowdsourcing.zmi.CrowdsourcingZMI`**

- `onChatMessage(ChatMessage): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onStatChanged(StatChanged): void`

**`net.runelite.client.plugins.customcursor.CustomCursorPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onItemContainerChanged(ItemContainerChanged): void`

**`net.runelite.client.plugins.dailytaskindicators.DailyTasksPlugin`**

- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.defaultworld.DefaultWorldPlugin`**

- `onWorldChanged(WorldChanged): void`

**`net.runelite.client.plugins.devtools.DevToolsPlugin`**

- `onClientTick(ClientTick): void`
- `onCommandExecuted(CommandExecuted): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.plugins.devtools.InventoryInspector`**

- `onItemContainerChanged(ItemContainerChanged): void`

**`net.runelite.client.plugins.devtools.ScriptInspector`**

- `onScriptPostFired(ScriptPostFired): void`
- `onScriptPreFired(ScriptPreFired): void`

**`net.runelite.client.plugins.devtools.SoundEffectOverlay`**

- `onAreaSoundEffectPlayed(AreaSoundEffectPlayed): void`
- `onSoundEffectPlayed(SoundEffectPlayed): void`

**`net.runelite.client.plugins.devtools.VarInspector`**

- `onVarClientIntChanged(VarClientIntChanged): void`
- `onVarClientStrChanged(VarClientStrChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.devtools.WidgetInspector`**

- `onConfigChanged(ConfigChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onMenuOptionClicked(MenuOptionClicked): void`

**`net.runelite.client.plugins.discord.DiscordPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onDiscordUserInfo(DiscordUserInfo): void`
- `onGameStateChanged(GameStateChanged): void`
- `onStatChanged(StatChanged): void`
- `onUserSync(UserSync): void`

**`net.runelite.client.plugins.dpscounter.DpsCounterPlugin`**

- `onDpsUpdate(DpsUpdate): void`
- `onHitsplatApplied(HitsplatApplied): void`
- `onNpcDespawned(NpcDespawned): void`
- `onPartyChanged(PartyChanged): void`

**`net.runelite.client.plugins.emojis.EmojiPlugin`**

- `onChatMessage(ChatMessage): void`
- `onOverheadTextChanged(OverheadTextChanged): void`

**`net.runelite.client.plugins.entityhider.EntityHiderPlugin`**

- `onConfigChanged(ConfigChanged): void`

**`net.runelite.client.plugins.examine.ExaminePlugin`**

- `onChatMessage(ChatMessage): void`
- `onGameStateChanged(GameStateChanged): void`
- `onMenuOptionClicked(MenuOptionClicked): void`

**`net.runelite.client.plugins.fairyring.FairyRingPlugin`**

- `onGameTick(GameTick): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.fishing.FishingPlugin`**

- `onChatMessage(ChatMessage): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onInteractingChanged(InteractingChanged): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.fps.FpsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onFocusChanged(FocusChanged): void`

**`net.runelite.client.plugins.friendlist.FriendListPlugin`**

- `onChatMessage(ChatMessage): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onScriptPostFired(ScriptPostFired): void`

**`net.runelite.client.plugins.friendnotes.FriendNotesPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onNameableNameChanged(NameableNameChanged): void`
- `onRemovedFriend(RemovedFriend): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.plugins.gpu.GpuPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onPostClientTick(PostClientTick): void`

**`net.runelite.client.plugins.grandexchange.GrandExchangePlugin`**

- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onFocusChanged(FocusChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGrandExchangeOfferChanged(GrandExchangeOfferChanged): void`
- `onGrandExchangeSearched(GrandExchangeSearched): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onSessionClose(SessionClose): void`
- `onSessionOpen(SessionOpen): void`

**`net.runelite.client.plugins.grounditems.GroundItemsPlugin`**

- `onClientTick(ClientTick): void`
- `onConfigChanged(ConfigChanged): void`
- `onFocusChanged(FocusChanged): void`
- `onItemDespawned(ItemDespawned): void`
- `onItemQuantityChanged(ItemQuantityChanged): void`
- `onItemSpawned(ItemSpawned): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onProfileChanged(ProfileChanged): void`
- `onWorldViewUnloaded(WorldViewUnloaded): void`

**`net.runelite.client.plugins.groundmarkers.GroundMarkerPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onProfileChanged(ProfileChanged): void`
- `onWorldViewLoaded(WorldViewLoaded): void`
- `onWorldViewUnloaded(WorldViewUnloaded): void`

**`net.runelite.client.plugins.herbiboars.HerbiboarPlugin`**

- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGroundObjectDespawned(GroundObjectDespawned): void`
- `onGroundObjectSpawned(GroundObjectSpawned): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.hiscore.HiscorePlugin`**

- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onMenuOpened(MenuOpened): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.hunter.HunterPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.idlenotifier.IdleNotifierPlugin`**

- `onAnimationChanged(AnimationChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onGraphicChanged(GraphicChanged): void`
- `onHitsplatApplied(HitsplatApplied): void`
- `onInteractingChanged(InteractingChanged): void`
- `onNpcChanged(NpcChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.implings.ImplingsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onNpcChanged(NpcChanged): void`
- `onNpcSpawned(NpcSpawned): void`

**`net.runelite.client.plugins.info.InfoPanel`**

- `onSessionClose(SessionClose): void`
- `onSessionOpen(SessionOpen): void`

**`net.runelite.client.plugins.instancemap.InstanceMapPlugin`**

- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.plugins.interacthighlight.InteractHighlightPlugin`**

- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onInteractingChanged(InteractingChanged): void`
- `onItemDespawned(ItemDespawned): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onNpcDespawned(NpcDespawned): void`
- `onPlayerDespawned(PlayerDespawned): void`

**`net.runelite.client.plugins.interfacestyles.InterfaceStylesPlugin`**

- `onBeforeMenuRender(BeforeMenuRender): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onMenuOpened(MenuOpened): void`
- `onPostClientTick(PostClientTick): void`
- `onPostHealthBarConfig(PostHealthBarConfig): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.plugins.inventorytags.InventoryTagsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onMenuOpened(MenuOpened): void`

**`net.runelite.client.plugins.itemcharges.ItemChargePlugin`**

- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.itemprices.ItemPricesPlugin`**

- `onBeforeRender(BeforeRender): void`

**`net.runelite.client.plugins.itemstats.ItemStatPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.keyremapping.KeyRemappingPlugin`**

- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.plugins.kingdomofmiscellania.KingdomPlugin`**

- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.kourendlibrary.KourendLibraryPlugin`**

- `onAnimationChanged(AnimationChanged): void`
- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`

**`net.runelite.client.plugins.loginscreen.LoginScreenPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.plugins.logouttimer.LogoutTimerPlugin`**

- `onConfigChanged(ConfigChanged): void`

**`net.runelite.client.plugins.loottracker.LootTrackerPlugin`**

- `onChatMessage(ChatMessage): void`
- `onClientShutdown(ClientShutdown): void`
- `onConfigChanged(ConfigChanged): void`
- `onConfigSync(ConfigSync): void`
- `onGameStateChanged(GameStateChanged): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onPlayerLootReceived(PlayerLootReceived): void`
- `onPluginLootReceived(PluginLootReceived): void`
- `onPostClientTick(PostClientTick): void`
- `onRuneScapeProfileChanged(RuneScapeProfileChanged): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onServerNpcLoot(ServerNpcLoot): void`
- `onSessionClose(SessionClose): void`
- `onSessionOpen(SessionOpen): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.lowmemory.LowMemoryPlugin`**

- `onBeforeRender(BeforeRender): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.plugins.menuentryswapper.MenuEntrySwapperPlugin`**

- `onClientTick(ClientTick): void`
- `onMenuOpened(MenuOpened): void`
- `onPostMenuSort(PostMenuSort): void`

**`net.runelite.client.plugins.metronome.MetronomePlugin`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.ProjectXPlugin`**

- `onAnimationChanged(AnimationChanged): void`
- `onChatMessage(ChatMessage): void`
- `onClientShutdown(ClientShutdown): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onHitsplatApplied(HitsplatApplied): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onOverlayMenuClicked(OverlayMenuClicked): void`
- `onRuneScapeProfileChanged(RuneScapeProfileChanged): void`
- `onStatChanged(StatChanged): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWidgetClosed(WidgetClosed): void`
- `onWidgetLoaded(WidgetLoaded): void`
- `onWorldViewLoaded(WorldViewLoaded): void`
- `onWorldViewUnloaded(WorldViewUnloaded): void`

**`net.runelite.client.plugins.projectx.api.playerstate.Rs2PlayerStateCache`**

- `onGameStateChanged(GameStateChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.breakhandler.BreakHandlerPlugin`**

- `onConfigChanged(ConfigChanged): void`

**`net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2Plugin`**

- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.plugins.projectx.example.ExamplePlugin`**

- `onConfigChanged(ConfigChanged): void`

**`net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManager`**

- `onClientShutdown(ClientShutdown): void`

**`net.runelite.client.plugins.projectx.inventorysetups.MInventorySetupsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onPluginChanged(PluginChanged): void`
- `onPostMenuSort(PostMenuSort): void`
- `onProfileChanged(ProfileChanged): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWidgetClosed(WidgetClosed): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.mouserecorder.MouseMacroRecorderPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onMenuOptionClicked(MenuOptionClicked): void`

**`net.runelite.client.plugins.projectx.questhelper.QuestHelperPlugin`**

- `onChatMessage(ChatMessage): void`
- `onClientShutdown(ClientShutdown): void`
- `onCommandExecuted(CommandExecuted): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onRuneScapeProfileChanged(RuneScapeProfileChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.bank.banktab.PotionStorage`**

- `onClientTick(ClientTick): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.bank.banktab.QuestBankTab`**

- `onGrandExchangeSearched(GrandExchangeSearched): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.achievementdiaries.kourend.KourendMedium`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.miniquests.curseoftheemptylord.CurseOfTheEmptyLord`**

- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.miniquests.enchantedkey.EnchantedKeyDigStep`**

- `onChatMessage(ChatMessage): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.miniquests.hisfaithfulservants.BarrowsHelper`**

- `onChatMessage(ChatMessage): void`
- `onGameTick(GameTick): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.miniquests.hisfaithfulservants.HisFaithfulServants`**

- `onChatMessage(ChatMessage): void`
- `onGameTick(GameTick): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.miniquests.themagearenaii.MageArenaBossStep`**

- `onChatMessage(ChatMessage): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.mischelpers.farmruns.HerbRun`**

- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.mischelpers.farmruns.TreeRun`**

- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.akingdomdivided.StatuePuzzle`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.akingdomdivided.StonePuzzleStep`**

- `onGameTick(GameTick): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.beneathcursedsands.TombRiddle`**

- `onGameTick(GameTick): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.betweenarock.PuzzleStep`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.biohazard.GiveIngredientsToHelpersStep`**

- `onGameTick(GameTick): void`
- `onInteractingChanged(InteractingChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.deserttreasureii.ChestCodeStep`**

- `onVarClientIntChanged(VarClientIntChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.deserttreasureii.GolemPuzzleStep`**

- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.dragonslayerii.CryptPuzzle`**

- `onGameTick(GameTick): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.dragonslayerii.MapPuzzle`**

- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.dreammentor.SelectingCombatGear`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.enlightenedjourney.BalloonFlightStep`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.enlightenedjourney.GiveAugusteItems`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.ghostsahoy.DyeShipSteps`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.hazeelcult.HazeelValves`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.icthlarinslittlehelper.DoorPuzzleStep`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.inaidofthemyreque.FillBurghCrate`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.insearchofknowledge.FeedingAimeri`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.kingsransom.LockpickPuzzle`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.lunardiplomacy.ChanceChallenge`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.lunardiplomacy.MemoryChallenge`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.monkeymadnessii.AgilityDungeonSteps`**

- `onChatMessage(ChatMessage): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.myarmsbigadventure.AddCompost`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.myarmsbigadventure.AddDung`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.olafsquest.PaintingWall`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.priestinperil.BringDrezelPureEssenceStep`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.ragandboneman.RagAndBoneManI`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.ragandboneman.RagAndBoneManII`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.ratcatchers.RatCharming`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.recipefordisaster.AskAboutFishCake`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.recipefordisaster.GetRohakDrunk`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.recipefordisaster.MakeEvilStew`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.recipefordisaster.QuizSteps`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.recruitmentdrive.DoorPuzzle`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.rumdeal.SlugSteps`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.secretsofthenorth.ArrowChestPuzzleStep`**

- `onVarClientIntChanged(VarClientIntChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.secretsofthenorth.AskAboutRitual`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.secretsofthenorth.SolveChestCode`**

- `onVarClientIntChanged(VarClientIntChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.secretsofthenorth.SolveDoorCode`**

- `onVarClientIntChanged(VarClientIntChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.secretsofthenorth.TellAboutMurder`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.shadowofthestorm.IncantationStep`**

- `onMenuOptionClicked(MenuOptionClicked): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.shadowofthestorm.SearchKilns`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.sheepshearer.SheepShearer`**

- `onItemContainerChanged(ItemContainerChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.sinsofthefather.DoorPuzzleStep`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.sinsofthefather.ValveStep`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.songoftheelves.BaxtorianPuzzle`**

- `onGameTick(GameTick): void`
- `onGraphicsObjectCreated(GraphicsObjectCreated): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.swansong.FishMonkfish`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.swansong.FixWall`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.templeoftheeye.RuneEnergyStep`**

- `onChatMessage(ChatMessage): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.thecurseofarrav.MetalDoorSolver`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.thecurseofarrav.TilePuzzleSolver`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.thecurseofarrav.rubblesolvers.RubbleSolver`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.theeyesofglouphrie.PuzzleStep`**

- `onGameTick(GameTick): void`
- `onItemContainerChanged(ItemContainerChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.thefinaldawn.TheFinalDawn`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.theforsakentower.AltarPuzzle`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.theforsakentower.JugPuzzle`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.theforsakentower.PotionPuzzle`**

- `onGameTick(GameTick): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.theforsakentower.PowerPuzzle`**

- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.thefremennikisles.KillTrolls`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.thefremenniktrials.CombinationPuzzle`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.thegreatbrainrobbery.TheGreatBrainRobbery`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.theheartofdarkness.ChestCodeStep`**

- `onVarClientIntChanged(VarClientIntChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.theheartofdarkness.LockedChestPuzzle`**

- `onGameTick(GameTick): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.theheartofdarkness.TheHeartOfDarkness`**

- `onChatMessage(ChatMessage): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.thepathofglouphrie.MonolithPuzzle`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.thepathofglouphrie.YewnocksPuzzle`**

- `onGameTick(GameTick): void`
- `onItemContainerChanged(ItemContainerChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.theslugmenace.PuzzleStep`**

- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.tribaltotem.PuzzleStep`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.troubledtortugans.RepairTown`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.watchtower.SkavidChoice`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.whileguthixsleeps.WeightStep`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.requirements.RequirementValidator`**

- `onChatMessage(ChatMessage): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onNpcChanged(NpcChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`

**`net.runelite.client.plugins.projectx.questhelper.runeliteobjects.extendedruneliteobjects.RuneliteObjectManager`**

- `onClientTick(ClientTick): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.statemanagement.AchievementDiaryStepManager`**

- `onNpcLootReceived(NpcLootReceived): void`

**`net.runelite.client.plugins.projectx.questhelper.statemanagement.PlayerStateManager`**

- `onChatMessage(ChatMessage): void`
- `onGameTick(GameTick): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.ConditionalStep`**

- `onChatMessage(ChatMessage): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onNpcChanged(NpcChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.DetailedQuestStep`**

- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onItemDespawned(ItemDespawned): void`
- `onItemSpawned(ItemSpawned): void`
- `onWorldMapAreaChanged(WorldMapAreaChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.DigStep`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.NpcStep`**

- `onNpcChanged(NpcChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.ObjectStep`**

- `onDecorativeObjectDespawned(DecorativeObjectDespawned): void`
- `onDecorativeObjectSpawned(DecorativeObjectSpawned): void`
- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameTick(GameTick): void`
- `onGroundObjectDespawned(GroundObjectDespawned): void`
- `onGroundObjectSpawned(GroundObjectSpawned): void`
- `onWallObjectDespawned(WallObjectDespawned): void`
- `onWallObjectSpawned(WallObjectSpawned): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.PuzzleStep`**

- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.QuestStep`**

- `onChatMessage(ChatMessage): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.projectx.questhelper.util.worldmap.WorldMapAreaManager`**

- `onScriptPostFired(ScriptPostFired): void`
- `onScriptPreFired(ScriptPreFired): void`

**`net.runelite.client.plugins.projectx.shortestpath.ShortestPathPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onMenuOpened(MenuOpened): void`
- `onPluginMessage(PluginMessage): void`

**`net.runelite.client.plugins.projectx.testing.TestRunnerPlugin`**

- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.plugins.projectx.ui.ProjectXConfigPanel`**

- `onExternalPluginsChanged(ExternalPluginsChanged): void`
- `onPluginChanged(PluginChanged): void`
- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.plugins.projectx.ui.ProjectXPluginHubPanel`**

- `onExternalPluginsChanged(ExternalPluginsChanged): void`

**`net.runelite.client.plugins.projectx.ui.ProjectXPluginListPanel`**

- `onExternalPluginsChanged(ExternalPluginsChanged): void`
- `onPluginChanged(PluginChanged): void`
- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.plugins.projectx.ui.ProjectXProfilePanel`**

- `onProfileChanged(ProfileChanged): void`
- `onRuneScapeProfileChanged(RuneScapeProfileChanged): void`
- `onSessionClose(SessionClose): void`
- `onSessionOpen(SessionOpen): void`

**`net.runelite.client.plugins.projectx.util.antiban.AntibanPlugin`**

- `onChatMessage(ChatMessage): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onProfileChanged(ProfileChanged): void`
- `onStatChanged(StatChanged): void`

**`net.runelite.client.plugins.minimap.MinimapPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onScriptPostFired(ScriptPostFired): void`

**`net.runelite.client.plugins.mining.MiningPlugin`**

- `onAnimationChanged(AnimationChanged): void`
- `onChatMessage(ChatMessage): void`
- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onScriptPreFired(ScriptPreFired): void`

**`net.runelite.client.plugins.motherlode.MotherlodePlugin`**

- `onChatMessage(ChatMessage): void`
- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWallObjectDespawned(WallObjectDespawned): void`
- `onWallObjectSpawned(WallObjectSpawned): void`

**`net.runelite.client.plugins.mta.alchemy.AlchemyRoom`**

- `onChatMessage(ChatMessage): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.mta.enchantment.EnchantmentRoom`**

- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onItemDespawned(ItemDespawned): void`
- `onItemSpawned(ItemSpawned): void`

**`net.runelite.client.plugins.mta.graveyard.GraveyardRoom`**

- `onGameTick(GameTick): void`
- `onItemContainerChanged(ItemContainerChanged): void`

**`net.runelite.client.plugins.mta.telekinetic.TelekineticRoom`**

- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onGroundObjectSpawned(GroundObjectSpawned): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`
- `onWallObjectSpawned(WallObjectSpawned): void`

**`net.runelite.client.plugins.music.MusicPlugin`**

- `onAmbientSoundEffectCreated(AmbientSoundEffectCreated): void`
- `onAreaSoundEffectPlayed(AreaSoundEffectPlayed): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onSoundEffectPlayed(SoundEffectPlayed): void`
- `onVarClientIntChanged(VarClientIntChanged): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.nightmarezone.NightmareZonePlugin`**

- `onBeforeRender(BeforeRender): void`
- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.notes.NotesPlugin`**

- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.plugins.npchighlight.NpcIndicatorsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onGraphicsObjectCreated(GraphicsObjectCreated): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onNpcChanged(NpcChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`

**`net.runelite.client.plugins.npcunaggroarea.NpcAggroAreaPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onNpcSpawned(NpcSpawned): void`

**`net.runelite.client.plugins.objectindicators.ObjectIndicatorsPlugin`**

- `onDecorativeObjectDespawned(DecorativeObjectDespawned): void`
- `onDecorativeObjectSpawned(DecorativeObjectSpawned): void`
- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGroundObjectDespawned(GroundObjectDespawned): void`
- `onGroundObjectSpawned(GroundObjectSpawned): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onProfileChanged(ProfileChanged): void`
- `onWallObjectDespawned(WallObjectDespawned): void`
- `onWallObjectSpawned(WallObjectSpawned): void`
- `onWorldViewLoaded(WorldViewLoaded): void`
- `onWorldViewUnloaded(WorldViewUnloaded): void`

**`net.runelite.client.plugins.opponentinfo.OpponentInfoPlugin`**

- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onInteractingChanged(InteractingChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onScriptPostFired(ScriptPostFired): void`

**`net.runelite.client.plugins.party.PartyPlugin`**

- `onCommandExecuted(CommandExecuted): void`
- `onConfigChanged(ConfigChanged): void`
- `onFocusChanged(FocusChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onLocationUpdate(LocationUpdate): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onPartyChanged(PartyChanged): void`
- `onPartyMemberAvatar(PartyMemberAvatar): void`
- `onStatusUpdate(StatusUpdate): void`
- `onTilePing(TilePing): void`
- `onUserJoin(UserJoin): void`
- `onUserPart(UserPart): void`
- `onUserSync(UserSync): void`

**`net.runelite.client.plugins.pestcontrol.PestControlPlugin`**

- `onChatMessage(ChatMessage): void`
- `onGameStateChanged(GameStateChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`

**`net.runelite.client.plugins.playerindicators.PlayerIndicatorsPlugin`**

- `onClientTick(ClientTick): void`
- `onProfileChanged(ProfileChanged): void`
- `onScriptPostFired(ScriptPostFired): void`

**`net.runelite.client.plugins.poh.PohPlugin`**

- `onAnimationChanged(AnimationChanged): void`
- `onConfigChanged(ConfigChanged): void`
- `onDecorativeObjectDespawned(DecorativeObjectDespawned): void`
- `onDecorativeObjectSpawned(DecorativeObjectSpawned): void`
- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.plugins.poison.PoisonPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.prayer.PrayerPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`
- `onItemContainerChanged(ItemContainerChanged): void`

**`net.runelite.client.plugins.prayer.PrayerReorder`**

- `onMenuOptionClicked(MenuOptionClicked): void`
- `onProfileChanged(ProfileChanged): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onWidgetDrag(WidgetDrag): void`

**`net.runelite.client.plugins.puzzlesolver.PuzzleSolverPlugin`**

- `onGameTick(GameTick): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.pyramidplunder.PyramidPlunderPlugin`**

- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onWallObjectSpawned(WallObjectSpawned): void`

**`net.runelite.client.plugins.questlist.QuestListPlugin`**

- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onVarClientIntChanged(VarClientIntChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.raids.RaidsPlugin`**

- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.randomevents.RandomEventPlugin`**

- `onInteractingChanged(InteractingChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onNpcDespawned(NpcDespawned): void`

**`net.runelite.client.plugins.regenmeter.RegenMeterPlugin`**

- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.reportbutton.ReportButtonPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.roofremoval.RoofRemovalPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onPreMapLoad(PreMapLoad): void`

**`net.runelite.client.plugins.rsnhider.RsnHiderPlugin`**

- `onBeforeRender(BeforeRender): void`
- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onOverheadTextChanged(OverheadTextChanged): void`

**`net.runelite.client.plugins.runecraft.RunecraftPlugin`**

- `onChatMessage(ChatMessage): void`
- `onDecorativeObjectDespawned(DecorativeObjectDespawned): void`
- `onDecorativeObjectSpawned(DecorativeObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onItemContainerChanged(ItemContainerChanged): void`

**`net.runelite.client.plugins.runenergy.RunEnergyPlugin`**

- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onScriptPostFired(ScriptPostFired): void`

**`net.runelite.client.plugins.screenmarkers.ScreenMarkerPlugin`**

- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.plugins.screenshot.ScreenshotPlugin`**

- `onActorDeath(ActorDeath): void`
- `onAnimationChanged(AnimationChanged): void`
- `onChatMessage(ChatMessage): void`
- `onGameTick(GameTick): void`
- `onPlayerLootReceived(PlayerLootReceived): void`
- `onPostClientTick(PostClientTick): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.skillcalculator.SkillCalculatorPlugin`**

- `onWorldChanged(WorldChanged): void`

**`net.runelite.client.plugins.skybox.SkyboxPlugin`**

- `onBeforeRender(BeforeRender): void`
- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.plugins.slayer.SlayerPlugin`**

- `onChatMessage(ChatMessage): void`
- `onCommandExecuted(CommandExecuted): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.smelting.SmeltingPlugin`**

- `onChatMessage(ChatMessage): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.specialcounter.SpecialCounterPlugin`**

- `onCommandExecuted(CommandExecuted): void`
- `onFakeXpDrop(FakeXpDrop): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onHitsplatApplied(HitsplatApplied): void`
- `onNpcChanged(NpcChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onSpecialCounterUpdate(SpecialCounterUpdate): void`
- `onStatChanged(StatChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.spellbook.SpellbookPlugin`**

- `onProfileChanged(ProfileChanged): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onWidgetDrag(WidgetDrag): void`

**`net.runelite.client.plugins.statusbars.StatusBarsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.stretchedmode.StretchedModePlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onResizeableChanged(ResizeableChanged): void`

**`net.runelite.client.plugins.team.TeamPlugin`**

- `onClanChannelChanged(ClanChannelChanged): void`
- `onClanMemberJoined(ClanMemberJoined): void`
- `onClanMemberLeft(ClanMemberLeft): void`
- `onConfigChanged(ConfigChanged): void`
- `onFriendsChatChanged(FriendsChatChanged): void`
- `onFriendsChatMemberJoined(FriendsChatMemberJoined): void`
- `onFriendsChatMemberLeft(FriendsChatMemberLeft): void`
- `onGameStateChanged(GameStateChanged): void`
- `onPlayerChanged(PlayerChanged): void`
- `onPlayerDespawned(PlayerDespawned): void`
- `onPlayerSpawned(PlayerSpawned): void`

**`net.runelite.client.plugins.tearsofguthix.TearsOfGuthixPlugin`**

- `onDecorativeObjectDespawned(DecorativeObjectDespawned): void`
- `onDecorativeObjectSpawned(DecorativeObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.plugins.timersandbuffs.TimersAndBuffsPlugin`**

- `onActorDeath(ActorDeath): void`
- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onGraphicChanged(GraphicChanged): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onNpcChanged(NpcChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.timestamp.TimestampPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.plugins.timetracking.TimeTrackingPlugin`**

- `onChatMessage(ChatMessage): void`
- `onCommandExecuted(CommandExecuted): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameTick(GameTick): void`
- `onRuneScapeProfileChanged(RuneScapeProfileChanged): void`
- `onWidgetClosed(WidgetClosed): void`

**`net.runelite.client.plugins.timetracking.farming.CompostTracker`**

- `onChatMessage(ChatMessage): void`
- `onGameStateChanged(GameStateChanged): void`
- `onMenuOptionClicked(MenuOptionClicked): void`

**`net.runelite.client.plugins.timetracking.farming.PaymentTracker`**

- `onChatMessage(ChatMessage): void`
- `onGameTick(GameTick): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onScriptPreFired(ScriptPreFired): void`

**`net.runelite.client.plugins.tithefarm.TitheFarmPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.twitch.TwitchPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.plugins.virtuallevels.VirtualLevelsPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onPluginChanged(PluginChanged): void`
- `onScriptCallbackEvent(ScriptCallbackEvent): void`

**`net.runelite.client.plugins.wiki.WikiDpsManager`**

- `onScriptPreFired(ScriptPreFired): void`

**`net.runelite.client.plugins.wiki.WikiPlugin`**

- `onConfigChanged(ConfigChanged): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onMenuOptionClicked(MenuOptionClicked): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onWidgetLoaded(WidgetLoaded): void`

**`net.runelite.client.plugins.wintertodt.WintertodtPlugin`**

- `onAnimationChanged(AnimationChanged): void`
- `onChatMessage(ChatMessage): void`
- `onGameTick(GameTick): void`
- `onItemContainerChanged(ItemContainerChanged): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.woodcutting.WoodcuttingPlugin`**

- `onAnimationChanged(AnimationChanged): void`
- `onChatMessage(ChatMessage): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameObjectDespawned(GameObjectDespawned): void`
- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onInteractingChanged(InteractingChanged): void`
- `onItemSpawned(ItemSpawned): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.plugins.worldhopper.WorldHopperPlugin`**

- `onChatMessage(ChatMessage): void`
- `onCommandExecuted(CommandExecuted): void`
- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onVarbitChanged(VarbitChanged): void`
- `onWorldListLoad(WorldListLoad): void`
- `onWorldsFetch(WorldsFetch): void`

**`net.runelite.client.plugins.worldmap.WorldMapPlugin`**

- `onClientTick(ClientTick): void`
- `onConfigChanged(ConfigChanged): void`
- `onScriptPostFired(ScriptPostFired): void`
- `onStatChanged(StatChanged): void`

**`net.runelite.client.plugins.xpdrop.XpDropPlugin`**

- `onGameTick(GameTick): void`
- `onScriptPreFired(ScriptPreFired): void`
- `onStatChanged(StatChanged): void`

**`net.runelite.client.plugins.xpglobes.XpGlobesPlugin`**

- `onGameStateChanged(GameStateChanged): void`
- `onStatChanged(StatChanged): void`

**`net.runelite.client.plugins.xptracker.XpTrackerPlugin`**

- `onClientShutdown(ClientShutdown): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`
- `onMenuEntryAdded(MenuEntryAdded): void`
- `onRuneScapeProfileChanged(RuneScapeProfileChanged): void`
- `onStatChanged(StatChanged): void`

**`net.runelite.client.plugins.xpupdater.XpUpdaterPlugin`**

- `onGameStateChanged(GameStateChanged): void`
- `onGameTick(GameTick): void`

**`net.runelite.client.plugins.xtea.XteaPlugin`**

- `onWorldViewLoaded(WorldViewLoaded): void`

**`net.runelite.client.plugins.zalcano.ZalcanoPlugin`**

- `onGameObjectSpawned(GameObjectSpawned): void`
- `onGameStateChanged(GameStateChanged): void`
- `onGraphicsObjectCreated(GraphicsObjectCreated): void`
- `onHitsplatApplied(HitsplatApplied): void`
- `onNpcChanged(NpcChanged): void`
- `onNpcDespawned(NpcDespawned): void`
- `onNpcSpawned(NpcSpawned): void`
- `onProjectileMoved(ProjectileMoved): void`
- `onVarbitChanged(VarbitChanged): void`

**`net.runelite.client.ui.ClientUI`**

- `onConfigChanged(ConfigChanged): void`
- `onGameStateChanged(GameStateChanged): void`

**`net.runelite.client.ui.overlay.OverlayManager`**

- `onConfigChanged(ConfigChanged): void`
- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.ui.overlay.OverlayRenderer`**

- `onBeforeRender(BeforeRender): void`
- `onClientTick(ClientTick): void`
- `onFocusChanged(FocusChanged): void`

**`net.runelite.client.ui.overlay.infobox.InfoBoxManager`**

- `onConfigChanged(ConfigChanged): void`
- `onInfoBoxMenuClicked(InfoBoxMenuClicked): void`
- `onProfileChanged(ProfileChanged): void`

**`net.runelite.client.ui.overlay.infobox.InfoBoxOverlay`**

- `onMenuOptionClicked(MenuOptionClicked): void`

</details>

## Confirmed lambda bodies (reached via invoke)

<details><summary>457 method(s) across 171 class(es)</summary>

**`net.runelite.api.Actor`**

- `getCombatLevel(): int`
- `getHealthRatio(): int`
- `getHealthScale(): int`
- `getInteracting(): Actor`
- `getLocalLocation(): LocalPoint`
- `getName(): String`
- `getWorldArea(): WorldArea`
- `getWorldLocation(): WorldPoint`
- `getWorldView(): WorldView`

**`net.runelite.api.CameraFocusableEntity`**

- `getCameraFocus(): LocalPoint`

**`net.runelite.api.Client`**

- `refreshChat(): void`
- `resetHealthBarCaches(): void`

**`net.runelite.api.ItemComposition`**

- `getSubops(): String[][]`

**`net.runelite.api.ObjectComposition`**

- `getImpostor(): ObjectComposition`

**`net.runelite.api.Player`**

- `getOverheadIcon(): HeadIcon`
- `getSkullIcon(): int`
- `isClanMember(): boolean`
- `isFriend(): boolean`
- `isFriendsChatMember(): boolean`

**`net.runelite.api.TileObject`**

- `getClickbox(): Shape`

**`net.runelite.api.WorldEntity`**

- `getConfig(): WorldEntityConfig`
- `getLocalLocation(): LocalPoint`
- `getOrientation(): int`
- `getOwnerType(): int`
- `getTargetLocation(): LocalPoint`
- `getTargetOrientation(): int`
- `getWorldView(): WorldView`
- `isHiddenForOverlap(): boolean`

**`net.runelite.client.callback.ClientThread`**

- `lambda$invoke$0(Runnable): boolean`
- `lambda$invoke$2(CompletableFuture, Supplier): void`
- `lambda$invokeLater$3(Runnable): boolean`

**`net.runelite.client.chat.ChatInputManager`**

- `lambda$handleInput$0(String, int, int): void`
- `lambda$handlePrivateMessage$2(String, String): void`

**`net.runelite.client.game.ChatIconManager`**

- `lambda$new$0(Client): void`
- `refreshIcons(): void`

**`net.runelite.client.game.ItemManager`**

- `lambda$loadImage$0(int, int, boolean, AsyncBufferedImage): boolean`

**`net.runelite.client.game.SpriteManager`**

- `lambda$addSpriteOverrides$6(SpriteOverride[]): void`
- `lambda$getSpriteAsync$0(int, int, Consumer): boolean`
- `lambda$removeSpriteOverrides$7(SpriteOverride[]): void`

**`net.runelite.client.game.chatbox.ChatboxItemSearch`**

- `lambda$new$0(): void`
- `update(): void`

**`net.runelite.client.game.chatbox.ChatboxPanelManager`**

- `lambda$openInput$1(ChatboxInput): void`
- `unsafeCloseInput(): void`

**`net.runelite.client.game.chatbox.ChatboxTextInput`**

- `update(): void`

**`net.runelite.client.game.npcoverlay.NpcOverlayService`**

- `lambda$rebuild$0(): void`

**`net.runelite.client.plugins.achievementdiary.DiaryRequirementsPlugin`**

- `lambda$showDiaryRequirements$0(int): void`

**`net.runelite.client.plugins.ammo.AmmoPlugin`**

- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.antidrag.AntiDragPlugin`**

- `lambda$startUp$0(): void`
- `resetDragDelay(): void`
- `setDragDelay(): void`

**`net.runelite.client.plugins.attackstyles.AttackStylesPlugin`**

- `lambda$onConfigChanged$2(ConfigChanged, boolean): void`
- `lambda$shutDown$1(): void`
- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.bank.BankPlugin`**

- `lambda$onScriptPreFired$2(WidgetNode): void`
- `lambda$shutDown$0(): void`
- `updateSeedVaultTotal(): void`

**`net.runelite.client.plugins.bank.BankPlugin$1`**

- `lambda$keyPressed$0(): void`
- `lambda$keyPressed$1(): void`

**`net.runelite.client.plugins.bank.BankSearch`**

- `lambda$initSearch$0(): void`
- `lambda$reset$1(boolean): void`
- `layoutBank(): void`

**`net.runelite.client.plugins.banktags.BankTagsPlugin`**

- `lambda$editTags$10(String, int): void`
- `lambda$shutDown$0(): void`
- `reinitBank(): void`

**`net.runelite.client.plugins.banktags.tabs.TabInterface`**

- `lambda$handleDeposit$6(String, List): void`
- `lambda$handleNewTab$8(String): void`
- `lambda$onScriptPreFired$1(): void`
- `lambda$opTagTab$10(): void`
- `lambda$opTagTab$12(String): void`
- `lambda$opTagTab$14(String): void`
- `lambda$renameTab$17(String, String): void`
- `lambda$renameTab$19(String): void`
- `lambda$renameTab$21(String, String): void`

**`net.runelite.client.plugins.camera.CameraPlugin`**

- `lambda$keyReleased$3(int): void`
- `lambda$onConfigChanged$2(): void`
- `lambda$shutDown$1(): void`
- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.cannon.CannonPlugin`**

- `addCounter(): void`

**`net.runelite.client.plugins.chatchannel.ChatChannelPlugin`**

- `lambda$confirmKickPlayer$6(String): void`
- `lambda$onConfigChanged$2(Color): void`
- `lambda$onScriptCallbackEvent$4(String): void`
- `lambda$rebuildClanTitle$8(): void`
- `lambda$rebuildClanTitle$9(): void`
- `lambda$rebuildFriendsChat$5(Object[]): void`
- `lambda$shutDown$1(): void`
- `lambda$startUp$0(): void`
- `lambda$timeoutMessages$3(): void`

**`net.runelite.client.plugins.chatcommands.ChatCommandsPlugin`**

- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.chatcommands.ChatKeyboardListener`**

- `lambda$keyPressed$0(int, String): void`
- `lambda$keyPressed$1(int): void`

**`net.runelite.client.plugins.chathistory.ChatHistoryPlugin`**

- `lambda$clearChatboxHistory$2(): void`
- `lambda$keyPressed$3(): void`

**`net.runelite.client.plugins.cluescrolls.ClueScrollPlugin`**

- `lambda$onWidgetLoaded$1(): void`

**`net.runelite.client.plugins.crowdsourcing.music.CrowdsourcingMusic`**

- `lambda$onChatMessage$0(String): void`

**`net.runelite.client.plugins.customcursor.CustomCursorPlugin`**

- `lambda$updateCursor$0(CustomCursor): void`

**`net.runelite.client.plugins.defaultworld.DefaultWorldPlugin`**

- `lambda$startUp$0(): boolean`

**`net.runelite.client.plugins.devtools.DevToolsPanel`**

- `lambda$createOptionsPanel$6(): void`

**`net.runelite.client.plugins.devtools.InventoryInspector`**

- `lambda$new$0(Object): void`

**`net.runelite.client.plugins.devtools.VarInspector`**

- `lambda$open$2(): void`

**`net.runelite.client.plugins.devtools.WidgetInfoTableModel`**

- `lambda$setValueAt$3(WidgetField, Object): void`
- `lambda$setWidget$2(Widget): void`

**`net.runelite.client.plugins.devtools.WidgetInspector`**

- `addPickerWidget(): void`
- `lambda$new$9(): void`
- `lambda$refreshWidgets$12(): void`
- `lambda$searchWidgets$20(String): void`
- `lambda$setSelectedWidget$14(Widget): void`
- `removePickerWidget(): void`

**`net.runelite.client.plugins.fairyring.FairyRingPlugin`**

- `lambda$setTagMenuOpen$1(): void`
- `openSearch(): void`

**`net.runelite.client.plugins.friendnotes.FriendNotesPlugin`**

- `lambda$rebuildFriendsList$2(): void`
- `lambda$rebuildIgnoreList$3(): void`

**`net.runelite.client.plugins.gpu.GpuPlugin`**

- `lambda$loadScene$7(CountDownLatch): void`
- `lambda$loadScene$8(Zone[][], CountDownLatch): void`
- `lambda$loadSubScene$9(GpuPlugin$SceneContext, CountDownLatch): void`
- `lambda$onConfigChanged$3(): void`
- `lambda$onConfigChanged$4(): void`
- `lambda$shutDown$2(): void`
- `lambda$startUp$1(): boolean`
- `setupSyncMode(): void`

**`net.runelite.client.plugins.grandexchange.GrandExchangePlugin`**

- `lambda$startUp$0(int, GrandExchangeOffer[]): void`

**`net.runelite.client.plugins.grandexchange.GrandExchangeSearchPanel`**

- `lambda$priceLookup$3(List, boolean): void`

**`net.runelite.client.plugins.grounditems.GroundItemsPlugin`**

- `handleLootbeams(): void`
- `lambda$reset$4(): void`
- `removeAllLootbeams(): void`

**`net.runelite.client.plugins.grounditems.Lootbeam`**

- `lambda$update$1(): boolean`

**`net.runelite.client.plugins.herbiboars.HerbiboarPlugin`**

- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.interfacestyles.InterfaceStylesPlugin`**

- `lambda$queueUpdateAllOverrides$1(): boolean`
- `lambda$shutDown$0(): void`
- `updateAllOverrides(): void`

**`net.runelite.client.plugins.itemcharges.ItemChargePlugin`**

- `lambda$onWidgetLoaded$1(): void`
- `lambda$startUp$0(): void`
- `updateInfoboxes(): void`

**`net.runelite.client.plugins.itemstats.ItemStatPlugin`**

- `resetGEInventory(): void`

**`net.runelite.client.plugins.keyremapping.KeyRemappingListener`**

- `lambda$keyPressed$0(): void`

**`net.runelite.client.plugins.keyremapping.KeyRemappingPlugin`**

- `lambda$shutDown$1(): void`
- `lambda$startUp$0(): void`
- `lockChat(): void`
- `unlockChat(): void`

**`net.runelite.client.plugins.kourendlibrary.KourendLibraryPlugin`**

- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.loginscreen.LoginScreenPlugin`**

- `lambda$overrideLoginScreen$3(BufferedImage): void`
- `lambda$shutDown$0(): void`
- `overrideLoginScreen(): void`

**`net.runelite.client.plugins.loottracker.LootTrackerPlugin`**

- `lambda$switchProfile$1(List): boolean`

**`net.runelite.client.plugins.lowmemory.LowMemoryPlugin`**

- `lambda$onConfigChanged$2(): void`
- `lambda$shutDown$1(): void`
- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.projectx.GameChatAppender`**

- `lambda$append$0(String): MessageNode`

**`net.runelite.client.plugins.projectx.ProjectX`**

- `lambda$getDBRowsByValue$4(int, int, int, Object): List`
- `lambda$getDBTableField$3(int, int, int): Object[]`
- `lambda$getDBTableRows$2(int): List`
- `lambda$getEnum$0(int): EnumComposition`
- `lambda$getStructComposition$1(int): StructComposition`
- `lambda$hopToWorld$7(int): Boolean`
- `lambda$openPopUp$15(WidgetNode): boolean`
- `lambda$openPopUp$16(String, String): void`

**`net.runelite.client.plugins.projectx.ProjectXPlugin`**

- `lambda$hasWidgetOverlapWithBounds$4(Rectangle, int, int): Boolean`

**`net.runelite.client.plugins.projectx.agentserver.handler.SkillsHandler`**

- `lambda$handleRequest$0(String): List`

**`net.runelite.client.plugins.projectx.agentserver.handler.StateHandler`**

- `lambda$handleRequest$0(Map): Object`

**`net.runelite.client.plugins.projectx.agentserver.handler.WidgetInvokeHandler`**

- `lambda$handleRequest$0(int, int): Rectangle`

**`net.runelite.client.plugins.projectx.api.AbstractEntityQueryable`**

- `lambda$firstOnClientThread$16(): IEntity`
- `lambda$nearestOnClientThread$17(): IEntity`
- `lambda$nearestOnClientThread$18(int): IEntity`
- `lambda$nearestOnClientThread$19(WorldPoint, int): IEntity`
- `lambda$toListOnClientThread$20(): List`

**`net.runelite.client.plugins.projectx.api.actor.Rs2ActorModel`**

- `lambda$getWorldLocation$0(): WorldPoint`

**`net.runelite.client.plugins.projectx.api.boat.Rs2BoatCache`**

- `lambda$getBoat$1(Rs2PlayerModel): WorldEntity`
- `lambda$getLocalBoat$0(): WorldEntity`

**`net.runelite.client.plugins.projectx.api.boat.models.Rs2BoatModel`**

- `lambda$getPlayerBoatLocation$3(float[]): WorldPoint`
- `lambda$getPlayerBoatLocation$4(): WorldPoint`
- `lambda$getWorldLocation$0(): WorldPoint`
- `lambda$setHeading$9(): WorldView`
- `lambda$transformToMainWorld$1(LocalPoint): LocalPoint`

**`net.runelite.client.plugins.projectx.api.npc.models.Rs2NpcModel`**

- `lambda$click$6(): NPCComposition`
- `lambda$getDistanceFromPlayer$1(): Integer`
- `lambda$isInteractingWithPlayer$2(): Boolean`
- `lambda$isMoving$3(): Boolean`
- `lambda$isWithinDistanceFromPlayer$0(int): Boolean`

**`net.runelite.client.plugins.projectx.api.playerstate.Rs2PlayerStateCache`**

- `lambda$populateQuests$2(): void`
- `lambda$updateVarbitValue$3(int): Integer`
- `lambda$updateVarpValue$4(int): Integer`

**`net.runelite.client.plugins.projectx.api.tileitem.models.Rs2TileItemModel`**

- `lambda$click$8(): ItemComposition`
- `lambda$getName$0(): String`
- `lambda$getTotalGeValue$4(): Integer`
- `lambda$getTotalValue$7(): Integer`
- `lambda$isMembers$6(): Boolean`
- `lambda$isNoted$1(): Boolean`
- `lambda$isProfitableToHighAlch$3(): Boolean`
- `lambda$isStackable$2(): Boolean`
- `lambda$isTradeable$5(): Boolean`

**`net.runelite.client.plugins.projectx.api.tileobject.models.Rs2TileObjectModel`**

- `lambda$getName$0(): String`
- `lambda$getObjectComposition$1(): ObjectComposition`
- `lambda$isReachable$2(): WorldView`

**`net.runelite.client.plugins.projectx.example.ExampleScript`**

- `lambda$checkEquipment$30(): ItemContainer`
- `lambda$checkWorldViewAndThreading$12(Player): WorldView`
- `lambda$checkWorldViewAndThreading$14(): String`

**`net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsAmmoHandler`**

- `lambda$handleSpecialHighlighting$10(InventorySetup): void`
- `lambda$handleSpecialHighlighting$11(InventorySetup, Set): void`
- `lambda$handleSpecialHighlighting$12(InventorySetup, boolean): void`
- `lambda$handleSpecialHighlighting$8(InventorySetup): void`
- `lambda$handleSpecialHighlighting$9(InventorySetup): void`

**`net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsBankSearch`**

- `lambda$initSearch$0(): void`
- `lambda$reset$1(boolean): void`

**`net.runelite.client.plugins.projectx.inventorysetups.MInventorySetupsPlugin`**

- `lambda$addAdditionalFilteredItem$43(int, Map, InventorySetup): void`
- `lambda$addInventorySetup$31(String): void`
- `lambda$addInventorySetup$34(String): void`
- `lambda$addInventorySetup$36(InventorySetup): void`
- `lambda$doBankSearch$41(): void`
- `lambda$importSetup$67(InventorySetup, InventorySetupPortable): void`
- `lambda$massImportSetups$69(ArrayList, ArrayList): void`
- `lambda$onConfigChanged$7(): void`
- `lambda$onWidgetClosed$29(): void`
- `lambda$previewNewLayout$21(InventorySetup, InventorySetupLayoutType, Layout): void`
- `lambda$previewNewLayout$24(Layout): void`
- `lambda$previewNewLayout$26(InventorySetup, InventorySetupLayoutType): void`
- `lambda$removeInventorySetup$63(InventorySetup): void`
- `lambda$removeItemFromSlot$58(InventorySetupsSlot): void`
- `lambda$resetBankSearch$44(): void`
- `lambda$startUp$4(): void`
- `lambda$startUp$5(): boolean`
- `lambda$switchProfile$65(): boolean`
- `lambda$toggleFuzzyOnSlot$59(int, InventorySetupsSlot): void`
- `lambda$toggleLockOnSlot$60(int, InventorySetupsSlot): void`
- `lambda$triggerBankSearchFromHotKey$42(int): boolean`
- `lambda$updateCurrentSetup$45(InventorySetup): void`
- `lambda$updateCurrentSetup$46(InventorySetup): void`
- `lambda$updateExisting$48(InventorySetup, InventorySetup): void`
- `lambda$updateNotesInSetup$62(InventorySetup, String): void`
- `lambda$updateSetupName$73(InventorySetup, String): void`
- `lambda$updateSlotFromContainer$50(InventorySetupsSlot, boolean, InventorySetupsStackCompareID, boolean, InventorySetupsItem): void`
- `lambda$updateSlotFromSearch$52(Integer, InventorySetupsSlot, boolean, boolean): void`
- `lambda$updateSlotFromSearchHelper$55(boolean, InventorySetupsItem, InventorySetupsItem, InventorySetupsSlot, List): void`
- `lambda$updateSpellbookInSetup$61(int): void`

**`net.runelite.client.plugins.projectx.inventorysetups.ui.InventorySetupsPluginPanel`**

- `doHighlighting(): void`

**`net.runelite.client.plugins.projectx.inventorysetups.ui.InventorySetupsSpellbookPanel`**

- `lambda$highlightSlots$2(InventorySetup): void`
- `lambda$new$0(MInventorySetupsPlugin): boolean`

**`net.runelite.client.plugins.projectx.inventorysetups.ui.InventorySetupsStandardPanel`**

- `lambda$new$0(MInventorySetupsPlugin, InventorySetupsPluginPanel): void`

**`net.runelite.client.plugins.projectx.questhelper.QuestHelperPlugin`**

- `lambda$onGameStateChanged$4(): void`
- `lambda$refreshBank$5(): void`
- `lambda$startUp$2(): void`

**`net.runelite.client.plugins.projectx.questhelper.QuestScript`**

- `lambda$chooseCorrectNPCOption$31(Rs2NpcModel): NPCComposition`
- `lambda$chooseCorrectObjectOption$29(Rs2TileObjectModel): ObjectComposition`
- `lambda$run$3(): Boolean`

**`net.runelite.client.plugins.projectx.questhelper.bank.banktab.QuestBankTab`**

- `lambda$onScriptPostFired$1(Widget, Widget[]): void`
- `lambda$sortBankTabItems$2(int): void`
- `removeAddedWidgets(): void`

**`net.runelite.client.plugins.projectx.questhelper.bank.banktab.QuestBankTabInterface`**

- `destroy(): void`
- `init(): void`
- `lambda$init$0(boolean): void`

**`net.runelite.client.plugins.projectx.questhelper.bank.banktab.QuestGrandExchangeInterface`**

- `destroy(): void`
- `init(): void`
- `lambda$activateTab$1(): void`
- `lambda$closeOptions$0(): void`
- `lambda$onceOffActivateTab$2(): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.mischelpers.farmruns.TreeRun`**

- `lambda$onConfigChanged$2(ConfigChanged): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.akingdomdivided.StonePuzzleStep`**

- `lambda$onWidgetLoaded$0(): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.demonslayer.IncantationStep`**

- `resetIncarnationIfRequired(): void`
- `updateChoiceIfRequired(): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.shadowofthestorm.IncantationStep`**

- `updateChoiceIfRequired(): void`

**`net.runelite.client.plugins.projectx.questhelper.helpers.quests.songoftheelves.BaxtorianPuzzle`**

- `lambda$onGraphicsObjectCreated$0(GraphicsObject): void`
- `lambda$onWidgetLoaded$1(Widget): void`

**`net.runelite.client.plugins.projectx.questhelper.managers.QuestManager`**

- `lambda$getAllItemRequirements$10(): void`
- `lambda$handleConfigChanged$1(): void`
- `lambda$handleVarbitChanged$0(): void`
- `lambda$initializeNewQuest$5(): void`
- `lambda$startUpBackgroundQuest$7(QuestHelper, String): void`
- `lambda$startUpQuest$4(QuestHelper, boolean): void`
- `lambda$updateAllItemsHelper$11(): void`
- `updateQuestList(): void`

**`net.runelite.client.plugins.projectx.questhelper.questhelpers.BasicQuestHelper`**

- `updateQuest(): boolean`

**`net.runelite.client.plugins.projectx.questhelper.runeliteobjects.extendedruneliteobjects.ExtendedRuneliteObject`**

- `lambda$update$0(): boolean`

**`net.runelite.client.plugins.projectx.questhelper.runeliteobjects.extendedruneliteobjects.FakeNpc`**

- `lambda$update$0(): boolean`

**`net.runelite.client.plugins.projectx.questhelper.runeliteobjects.extendedruneliteobjects.RuneliteObjectManager`**

- `lambda$onWidgetLoaded$25(ExtendedRuneliteObject, WidgetLoaded): void`
- `lambda$removeGroupAndSubgroups$11(String): void`
- `lambda$replaceWidgetsForReplacedNpcs$27(Widget, Widget, ReplacedNpc): void`
- `removeRuneliteObjects(): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.QuestStep`**

- `highlightChoice(): void`
- `highlightWidgetChoice(): void`

**`net.runelite.client.plugins.projectx.questhelper.steps.playermadesteps.RuneliteObjectStep`**

- `removeRuneliteNpcs(): void`

**`net.runelite.client.plugins.projectx.shortestpath.ShortestPathPlugin`**

- `lambda$restartPathfinding$0(Set, boolean, WorldPoint, ExecutorService): void`

**`net.runelite.client.plugins.projectx.util.bank.Rs2Bank`**

- `lambda$getItems$54(): List`
- `lambda$getTabs$53(): List`
- `lambda$scrollBankToSlot$55(int): void`
- `lambda$withdrawLootItems$48(LootTrackerItem): ItemComposition`
- `lambda$withdrawLootItems$50(int): ItemComposition`

**`net.runelite.client.plugins.projectx.util.bank.Rs2BankData`**

- `lambda$rebuildBankItemsList$0(): Boolean`

**`net.runelite.client.plugins.projectx.util.camera.Rs2Camera`**

- `lambda$setZoom$4(int): void`

**`net.runelite.client.plugins.projectx.util.combat.Rs2Combat`**

- `lambda$inCombat$5(Actor, Player): Boolean`

**`net.runelite.client.plugins.projectx.util.combat.models.Rs2DropSource`**

- `lambda$getItemComposition$0(): ItemComposition`

**`net.runelite.client.plugins.projectx.util.farming.Rs2Farming`**

- `lambda$getPatchByRegionAndVarbit$12(int, String): Optional`
- `lambda$getPatchesByTab$0(Tab): List`
- `lambda$isFarmingSystemReady$14(): Boolean`
- `lambda$predictPatchState$4(FarmingPatch): CropState`

**`net.runelite.client.plugins.projectx.util.gameobject.Rs2Cannon`**

- `lambda$refill$0(): Integer`
- `lambda$refill$1(): Integer`

**`net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject`**

- `lambda$clickObject$92(): WorldView`
- `lambda$convertToObjectCompositionInternal$90(int, boolean): ObjectComposition`
- `lambda$getObjectComposition$93(int): ObjectComposition`
- `lambda$getSceneObjects$84(): Triple`

**`net.runelite.client.plugins.projectx.util.gameobject.Rs2ObjectModel`**

- `lambda$getDistanceFromPlayer$1(): Integer`
- `lambda$getObjectComposition$2(): ObjectComposition`
- `lambda$getTicksSinceCreation$0(): Integer`

**`net.runelite.client.plugins.projectx.util.grandexchange.Rs2GrandExchange`**

- `lambda$getSearchResultWidget$21(): Widget`
- `lambda$setChatboxValue$32(int): Object`

**`net.runelite.client.plugins.projectx.util.grounditem.Rs2GroundItem`**

- `lambda$getAllAt$3(int, int): RS2Item[]`
- `lambda$getAllFromWorldPoint$6(WorldPoint, int): RS2Item[]`
- `lambda$interact$0(InteractModel): ItemComposition`
- `lambda$isItemBasedOnValueOnGround$24(RS2Item): Integer`
- `lambda$lootAllItemBasedOnValue$26(int): RS2Item[]`
- `lambda$lootAllItemBasedOnValue$27(RS2Item): Integer`
- `lambda$lootItemBasedOnValue$9(RS2Item): Integer`

**`net.runelite.client.plugins.projectx.util.grounditem.models.Rs2SpawnLocation`**

- `lambda$getItemComposition$0(): ItemComposition`

**`net.runelite.client.plugins.projectx.util.inventory.Rs2FuzzyItem`**

- `lambda$getItemName$2(int): String`

**`net.runelite.client.plugins.projectx.util.inventory.Rs2Gembag`**

- `lambda$getGemItemModel$8(int, String): Rs2ItemModel`

**`net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory`**

- `lambda$dropAllExcept$23(Rs2ItemModel): Integer`
- `lambda$getInventory$69(): Widget`
- `lambda$invokeMenu$66(): Boolean`
- `lambda$items$0(): Object`

**`net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel`**

- `lambda$ensureCompositionLoaded$1(): ItemComposition`
- `lambda$getNotedId$12(int): ItemComposition`
- `lambda$getPrice$3(): Integer`
- `lambda$getUnNotedId$13(int): ItemComposition`
- `lambda$initializeFromComposition$14(ItemComposition): ItemComposition`
- `lambda$initializeFromComposition$16(ItemComposition): Boolean`
- `lambda$isHaProfitable$4(): Integer`
- `lambda$new$0(int): ItemComposition`

**`net.runelite.client.plugins.projectx.util.item.Rs2EnsouledHead`**

- `lambda$reanimate$0(): Boolean`

**`net.runelite.client.plugins.projectx.util.item.Rs2ItemManager`**

- `lambda$getItemComposition$1(int): ItemComposition`
- `lambda$searchItem$0(String): List`

**`net.runelite.client.plugins.projectx.util.magic.Rs2Magic`**

- `lambda$alch$7(): Boolean`
- `lambda$alch$9(): Boolean`
- `lambda$superHeat$11(): Boolean`

**`net.runelite.client.plugins.projectx.util.misc.Rs2UiHelper`**

- `lambda$getActorClickbox$0(Actor, LocalPoint): Shape`

**`net.runelite.client.plugins.projectx.util.npc.Rs2Npc`**

- `lambda$getAvailableAction$43(Rs2NpcModel): NPCComposition`
- `lambda$getNpcs$13(Predicate): Rs2NpcModel[]`
- `lambda$hasAction$32(int): NPCComposition`
- `lambda$interact$34(Rs2NpcModel): NPCComposition`
- `lambda$isMoving$1(NPC): Boolean`

**`net.runelite.client.plugins.projectx.util.npc.Rs2NpcModel`**

- `lambda$getDistanceFromPlayer$1(): Integer`
- `lambda$isInteractingWithPlayer$2(): Boolean`
- `lambda$isMoving$3(): Boolean`
- `lambda$isWithinDistanceFromPlayer$0(int): Boolean`

**`net.runelite.client.plugins.projectx.util.player.Rs2Player`**

- `lambda$getAnimation$41(): Integer`
- `lambda$getBoostedSkillLevel$44(Skill): Integer`
- `lambda$getCombatLevel$25(): Integer`
- `lambda$getInteracting$45(): Actor`
- `lambda$getPlayerEquipmentNames$19(Rs2PlayerModel): Map`
- `lambda$getPoseAnimation$42(): Integer`
- `lambda$getRealSkillLevel$43(Skill): Integer`
- `lambda$getWorldLocation_Internal$29(): WorldPoint`
- `lambda$getWorldView_Internal$30(): WorldView`
- `lambda$isMoving$6(): Boolean`
- `lambda$isMoving$7(Rs2PlayerModel): Boolean`
- `lambda$updateCombatTime$26(): Object`

**`net.runelite.client.plugins.projectx.util.reachable.Rs2Reachable`**

- `lambda$getReachableTiles$0(int, WorldPoint): IntSet`

**`net.runelite.client.plugins.projectx.util.reflection.Rs2Reflection`**

- `lambda$invokeMenu$2(int, int, int, int, int, String, String): void`
- `lambda$invokeMenu$3(int, int, int, int, int, int, String, String, int, int): Object`

**`net.runelite.client.plugins.projectx.util.settings.Rs2Settings`**

- `lambda$findSettingsSearchClickable$6(String[]): Widget`

**`net.runelite.client.plugins.projectx.util.shop.Rs2Shop`**

- `lambda$invokeMenu$8(Rs2ItemModel): ItemComposition`

**`net.runelite.client.plugins.projectx.util.shop.models.Rs2ShopItem`**

- `lambda$getItemComposition$0(): ItemComposition`

**`net.runelite.client.plugins.projectx.util.skills.slayer.Rs2Slayer`**

- `lambda$getSlayerTaskWeaknessName$1(int): String`

**`net.runelite.client.plugins.projectx.util.tileobject.Rs2TileObjectModel`**

- `lambda$getName$0(): String`
- `lambda$getObjectComposition$1(): ObjectComposition`

**`net.runelite.client.plugins.projectx.util.walker.Rs2MiniMap`**

- `lambda$getMinimapClipArea$2(boolean): BufferedImage`
- `lambda$localToMinimap$0(LocalPoint): Point`
- `lambda$worldToMinimap$1(LocalPoint): Point`

**`net.runelite.client.plugins.projectx.util.walker.Rs2Walker`**

- `lambda$handleCharterShip$103(Widget): Boolean`

**`net.runelite.client.plugins.projectx.util.widget.Rs2Widget`**

- `lambda$clickChildWidget$12(int): Widget`
- `lambda$clickWidget$11(int): Widget`
- `lambda$clickWidget$3(Optional, String, boolean, int): Boolean`
- `lambda$enableQuantityOption$24(String): Boolean`
- `lambda$findWidget$16(List, String, boolean): Widget`
- `lambda$findWidget$19(List, int): Widget`
- `lambda$getChildWidgetSpriteID$10(int, int): Integer`
- `lambda$getWidget$6(int): Widget`
- `lambda$getWidget$9(int, int): Widget`
- `lambda$hasWidgetText$13(int, String, boolean): Boolean`
- `lambda$hasWidgetText$14(int, int, String, boolean): Boolean`
- `lambda$isHidden$7(int, int): Boolean`
- `lambda$isHidden$8(int): Boolean`
- `lambda$isWidgetVisible$4(int): Boolean`
- `lambda$isWidgetVisible$5(int, int): Boolean`
- `lambda$waitForWidget$26(String): Boolean`

**`net.runelite.client.plugins.projectx.util.widget.Rs2WidgetInspector`**

- `lambda$describeWidget$1(int, int, int): List`
- `lambda$getVisibleInterfaces$0(): List`
- `lambda$getWidgetPath$5(Widget): String`
- `lambda$search$4(String[]): List`

**`net.runelite.client.plugins.minimap.MinimapPlugin`**

- `lambda$onConfigChanged$2(): void`
- `lambda$shutDown$1(): void`
- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.motherlode.MotherlodePlugin`**

- `refreshSackValues(): void`

**`net.runelite.client.plugins.music.MusicPlugin`**

- `lambda$onConfigChanged$4(ConfigChanged): void`
- `lambda$openSearch$6(String): void`
- `lambda$openSearch$9(): void`
- `lambda$shutDown$1(): void`
- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.npchighlight.NpcIndicatorsPlugin`**

- `lambda$shutDown$1(): void`
- `lambda$startUp$0(): void`
- `rebuild(): void`

**`net.runelite.client.plugins.npcunaggroarea.NpcAggroAreaPlugin`**

- `scanNpcs(): void`

**`net.runelite.client.plugins.objectindicators.ObjectIndicatorsPlugin`**

- `lambda$createTagBorderColorMenu$11(TileObject, Color): void`
- `lambda$createTagFillColorMenu$18(TileObject, Color): void`
- `lambda$onProfileChanged$1(): void`
- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.party.PartyPanel`**

- `lambda$new$0(PartyService): void`

**`net.runelite.client.plugins.party.PartyPlugin`**

- `lambda$onTilePing$1(): void`
- `lambda$onUserSync$3(): void`

**`net.runelite.client.plugins.playerindicators.PlayerIndicatorsPlugin`**

- `lambda$onScriptPostFired$0(): void`

**`net.runelite.client.plugins.poison.PoisonPlugin`**

- `checkHealthIcon(): void`
- `resetHealthIcon(): void`

**`net.runelite.client.plugins.prayer.PrayerReorder`**

- `lambda$shutDown$0(): void`
- `redrawPrayers(): void`

**`net.runelite.client.plugins.pyramidplunder.PyramidPlunderPlugin`**

- `lambda$shutDown$0(): void`

**`net.runelite.client.plugins.questlist.QuestListPlugin`**

- `addQuestButtons(): void`
- `lambda$redrawQuests$6(Object[]): void`

**`net.runelite.client.plugins.raids.RaidsPlugin`**

- `scoutRaid(): void`
- `screenshotScoutOverlay(): void`

**`net.runelite.client.plugins.reportbutton.ReportButtonPlugin`**

- `lambda$shutDown$0(): void`
- `updateReportButtonTime(): void`

**`net.runelite.client.plugins.roofremoval.RoofRemovalPlugin`**

- `lambda$onConfigChanged$2(): void`
- `lambda$shutDown$1(): void`
- `lambda$startUp$0(): boolean`

**`net.runelite.client.plugins.rsnhider.RsnHiderPlugin`**

- `lambda$shutDown$0(): void`

**`net.runelite.client.plugins.screenshot.ScreenshotPlugin`**

- `lambda$takeScreenshot$2(): void`

**`net.runelite.client.plugins.skillcalculator.SkillCalculator`**

- `lambda$renderActionSlots$13(): void`

**`net.runelite.client.plugins.skillcalculator.UIActionSlot`**

- `lambda$new$1(SkillAction, ItemManager, JShadowedLabel): void`

**`net.runelite.client.plugins.slayer.SlayerPlugin`**

- `addCounter(): void`
- `lambda$startUp$3(): boolean`
- `updateTask(): void`

**`net.runelite.client.plugins.specialcounter.SpecialCounterPlugin`**

- `lambda$onSpecialCounterUpdate$1(SpecialCounterUpdate, String): void`
- `lambda$onVarbitChanged$0(int, int): void`

**`net.runelite.client.plugins.spellbook.SpellbookPlugin`**

- `lambda$onScriptPreFired$6(int): void`
- `redrawSpellbook(): void`
- `reinitializeSpellbook(): void`

**`net.runelite.client.plugins.statusbars.StatusBarsPlugin`**

- `checkStatusBars(): void`

**`net.runelite.client.plugins.team.TeamPlugin`**

- `addClanChatCounter(): void`
- `addFriendsChatCounter(): void`
- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.timestamp.TimestampPlugin`**

- `lambda$onConfigChanged$0(): void`

**`net.runelite.client.plugins.virtuallevels.VirtualLevelsPlugin`**

- `simulateSkillChange(): void`

**`net.runelite.client.plugins.wiki.WikiDpsManager`**

- `lambda$onScriptPreFired$1(): void`
- `lambda$startUp$0(): void`
- `removeButton(): void`

**`net.runelite.client.plugins.wiki.WikiPlugin`**

- `addWidgets(): void`
- `lambda$onConfigChanged$4(): void`
- `lambda$shutDown$0(): void`

**`net.runelite.client.plugins.wiki.WikiSearchChatboxTextInput`**

- `lambda$new$1(): void`

**`net.runelite.client.plugins.wiki.WikiSearchChatboxTextInput$1`**

- `lambda$onResponse$0(List): void`

**`net.runelite.client.plugins.woodcutting.WoodcuttingPlugin`**

- `updateLeprechaunsLuck(): void`

**`net.runelite.client.plugins.worldhopper.WorldHopperPlugin`**

- `lambda$hopTo$8(World): void`
- `lambda$updateList$6(WorldResult): boolean`

**`net.runelite.client.plugins.worldhopper.WorldHopperPlugin$1`**

- `lambda$hotkeyPressed$0(): void`

**`net.runelite.client.plugins.worldhopper.WorldHopperPlugin$2`**

- `lambda$hotkeyPressed$0(): void`

**`net.runelite.client.plugins.xptracker.XpTrackerPlugin`**

- `lambda$startUp$0(): void`

**`net.runelite.client.plugins.zalcano.ZalcanoPlugin`**

- `lambda$startUp$0(): void`

**`net.runelite.client.ui.ClientUI`**

- `lambda$onGameStateChanged$3(Client): boolean`

**`net.runelite.client.ui.overlay.components.ButtonComponent$1`**

- `lambda$mousePressed$0(MouseEvent): void`

**`net.runelite.client.util.GameEventManager`**

- `lambda$simulateGameEvents$0(Object): void`

</details>

## Inferred RuneLite API methods needing client thread

These `net.runelite.api.*` methods are invoked from inside methods that are guaranteed to run on the client thread. Each entry is tagged with the strength of the evidence:

- **`ASSERT`** — caller has `assert client.isClientThread()` (highest confidence)
- **`SUBSCRIBE`** — caller is `@Subscribe`-annotated, dispatched on the client thread by the event bus
- **`LAMBDA`** — caller is a lambda body that was passed (transitively) to `ClientThread.invoke*()`

> This list is derived, not exhaustive. It catches API methods reached from known-on-thread callers in this repo. Many other API methods are also unsafe off-thread but call no asserting/subscribing/marshalling wrapper here.

### `net.runelite.api.Actor`

| Method | Evidence | Caller count |
|---|---|---:|
| `getAnimation(): int` | SUBSCRIBE | 5 |
| `getCombatLevel(): int` | LAMBDA, SUBSCRIBE | 3 |
| `getCurrentOrientation(): int` | LAMBDA | 1 |
| `getGraphic(): int` | SUBSCRIBE | 2 |
| `getHealthRatio(): int` | LAMBDA | 1 |
| `getHealthScale(): int` | LAMBDA | 1 |
| `getInteracting(): Actor` | LAMBDA, SUBSCRIBE | 2 |
| `getLocalLocation(): LocalPoint` | LAMBDA, SUBSCRIBE | 2 |
| `getModel(): Model` | LAMBDA | 1 |
| `getName(): String` | LAMBDA, SUBSCRIBE | 5 |
| `getWorldArea(): WorldArea` | LAMBDA | 1 |
| `getWorldLocation(): WorldPoint` | LAMBDA | 3 |
| `getWorldView(): WorldView` | LAMBDA | 2 |
| `setDead(boolean): void` | SUBSCRIBE | 1 |
| `setOverheadText(String): void` | SUBSCRIBE | 3 |

### `net.runelite.api.CameraFocusableEntity`

| Method | Evidence | Caller count |
|---|---|---:|
| `getCameraFocus(): LocalPoint` | LAMBDA | 1 |

### `net.runelite.api.ChatMessageType`

| Method | Evidence | Caller count |
|---|---|---:|
| `of(int): ChatMessageType` | SUBSCRIBE | 1 |
| `ordinal(): int` | SUBSCRIBE | 7 |

### `net.runelite.api.ChatPlayer`

| Method | Evidence | Caller count |
|---|---|---:|
| `getWorld(): int` | SUBSCRIBE | 2 |

### `net.runelite.api.Client`

| Method | Evidence | Caller count |
|---|---|---:|
| `addChatMessage(ChatMessageType, String, String, String): MessageNode` | SUBSCRIBE | 1 |
| `addChatMessage(ChatMessageType, String, String, String, boolean): MessageNode` | LAMBDA, SUBSCRIBE | 2 |
| `changeMemoryMode(boolean): void` | LAMBDA, SUBSCRIBE | 4 |
| `changeWorld(World): void` | ASSERT | 1 |
| `clearHintArrow(): void` | SUBSCRIBE | 10 |
| `closeInterface(WidgetNode, boolean): void` | LAMBDA | 2 |
| `createItemSprite(int, int, int, int, int, boolean, int): SpritePixels` | LAMBDA | 1 |
| `createMenuEntry(int): MenuEntry` | SUBSCRIBE | 17 |
| `createScriptEventBuilder(Object[]): ScriptEventBuilder` | LAMBDA | 7 |
| `createWorld(): World` | ASSERT, LAMBDA | 2 |
| `draw2010Menu(int): void` | SUBSCRIBE | 1 |
| `drawOriginalMenu(int): void` | SUBSCRIBE | 1 |
| `getAccountHash(): long` | LAMBDA, SUBSCRIBE | 3 |
| `getAmbientSoundEffects(): Deque` | SUBSCRIBE | 2 |
| `getArray(int): int[]` | SUBSCRIBE | 1 |
| `getBaseX(): int` | SUBSCRIBE | 1 |
| `getBaseY(): int` | SUBSCRIBE | 1 |
| `getBoostedSkillLevel(Skill): int` | LAMBDA, SUBSCRIBE | 11 |
| `getBoostedSkillLevels(): int[]` | SUBSCRIBE | 1 |
| `getCameraX(): int` | SUBSCRIBE | 1 |
| `getCameraY(): int` | SUBSCRIBE | 1 |
| `getCanvas(): Canvas` | LAMBDA | 1 |
| `getClanChannel(): ClanChannel` | SUBSCRIBE | 1 |
| `getComponentTable(): HashTable` | SUBSCRIBE | 1 |
| `getDBRowsByValue(int, int, int, Object): List` | LAMBDA, SUBSCRIBE | 3 |
| `getDBTableField(int, int, int): Object[]` | LAMBDA, SUBSCRIBE | 6 |
| `getDBTableRows(int): List` | LAMBDA | 2 |
| `getDraggedOnWidget(): Widget` | SUBSCRIBE | 3 |
| `getDraggedWidget(): Widget` | SUBSCRIBE | 3 |
| `getEnum(int): EnumComposition` | LAMBDA, SUBSCRIBE | 4 |
| `getFriendContainer(): FriendContainer` | SUBSCRIBE | 1 |
| `getFriendsChatManager(): FriendsChatManager` | SUBSCRIBE | 2 |
| `getGameCycle(): int` | LAMBDA, SUBSCRIBE | 5 |
| `getGameState(): GameState` | ASSERT, LAMBDA, SUBSCRIBE | 51 |
| `getGuestClanChannel(): ClanChannel` | SUBSCRIBE | 1 |
| `getHintArrowNpc(): NPC` | SUBSCRIBE | 1 |
| `getHintArrowPoint(): WorldPoint` | SUBSCRIBE | 1 |
| `getIgnoreContainer(): NameableContainer` | SUBSCRIBE | 1 |
| `getIndexConfig(): IndexDataBase` | LAMBDA | 1 |
| `getIndexScripts(): IndexDataBase` | SUBSCRIBE | 1 |
| `getIndexSprites(): IndexDataBase` | ASSERT | 1 |
| `getIntStack(): int[]` | SUBSCRIBE | 25 |
| `getIntStackSize(): int` | SUBSCRIBE | 24 |
| `getItemContainer(InventoryID): ItemContainer` | SUBSCRIBE | 1 |
| `getItemContainer(int): ItemContainer` | ASSERT, LAMBDA, SUBSCRIBE | 24 |
| `getItemContainers(): HashTable` | LAMBDA | 1 |
| `getItemCount(): int` | ASSERT, SUBSCRIBE | 2 |
| `getItemDefinition(int): ItemComposition` | ASSERT, LAMBDA, SUBSCRIBE | 27 |
| `getKeyboardIdleTicks(): int` | SUBSCRIBE | 1 |
| `getLocalDestinationLocation(): LocalPoint` | SUBSCRIBE | 2 |
| `getLocalPlayer(): Player` | LAMBDA, SUBSCRIBE | 96 |
| `getMapElementConfig(int): MapElementConfig` | SUBSCRIBE | 1 |
| `getMenu(): Menu` | SUBSCRIBE | 6 |
| `getMenuEntries(): MenuEntry[]` | SUBSCRIBE | 13 |
| `getMessages(): IterableHashTable` | SUBSCRIBE | 3 |
| `getModIcons(): IndexedSprite[]` | ASSERT, LAMBDA, SUBSCRIBE | 3 |
| `getMouseCanvasPosition(): Point` | SUBSCRIBE | 3 |
| `getMouseCurrentButton(): int` | SUBSCRIBE | 3 |
| `getMouseLastPressedMillis(): long` | SUBSCRIBE | 1 |
| `getNpcDefinition(int): NPCComposition` | LAMBDA, SUBSCRIBE | 7 |
| `getObjectDefinition(int): ObjectComposition` | LAMBDA, SUBSCRIBE | 11 |
| `getObjectStack(): Object[]` | SUBSCRIBE | 11 |
| `getObjectStackSize(): int` | SUBSCRIBE | 11 |
| `getOculusOrbFocalPointX(): int` | SUBSCRIBE | 1 |
| `getOculusOrbFocalPointY(): int` | SUBSCRIBE | 1 |
| `getOculusOrbState(): int` | SUBSCRIBE | 1 |
| `getOverallExperience(): long` | SUBSCRIBE | 5 |
| `getPlane(): int` | SUBSCRIBE | 10 |
| `getPlayers(): List` | LAMBDA | 1 |
| `getPreferences(): Preferences` | SUBSCRIBE | 2 |
| `getRealSkillLevel(Skill): int` | LAMBDA, SUBSCRIBE | 9 |
| `getRealSkillLevels(): int[]` | SUBSCRIBE | 1 |
| `getRevision(): int` | SUBSCRIBE | 1 |
| `getScene(): Scene` | LAMBDA, SUBSCRIBE | 4 |
| `getScriptActiveWidget(): Widget` | SUBSCRIBE | 2 |
| `getSelectedSceneTile(): Tile` | SUBSCRIBE | 1 |
| `getSelectedWidget(): Widget` | SUBSCRIBE | 2 |
| `getSkillExperience(Skill): int` | LAMBDA, SUBSCRIBE | 5 |
| `getSkillExperiences(): int[]` | SUBSCRIBE | 1 |
| `getSpriteOverrides(): Map` | LAMBDA | 4 |
| `getSprites(IndexDataBase, int, int): SpritePixels[]` | ASSERT | 1 |
| `getStructComposition(int): StructComposition` | LAMBDA | 1 |
| `getTextureProvider(): TextureProvider` | SUBSCRIBE | 1 |
| `getTickCount(): int` | LAMBDA, SUBSCRIBE | 36 |
| `getTopLevelWorldView(): WorldView` | LAMBDA, SUBSCRIBE | 17 |
| `getUsername(): String` | SUBSCRIBE | 1 |
| `getVarbit(int): VarbitComposition` | LAMBDA, SUBSCRIBE | 2 |
| `getVarbitValue(int): int` | ASSERT, LAMBDA, SUBSCRIBE | 36 |
| `getVarbitValue(int[], int): int` | SUBSCRIBE | 2 |
| `getVarcIntValue(int): int` | SUBSCRIBE | 1 |
| `getVarcMap(): Map` | SUBSCRIBE | 2 |
| `getVarcStrValue(int): String` | LAMBDA, SUBSCRIBE | 10 |
| `getVarpValue(int): int` | LAMBDA, SUBSCRIBE | 14 |
| `getVarps(): int[]` | SUBSCRIBE | 2 |
| `getWidget(WidgetInfo): Widget` | LAMBDA | 1 |
| `getWidget(int): Widget` | LAMBDA, SUBSCRIBE | 98 |
| `getWidget(int, int): Widget` | ASSERT, LAMBDA, SUBSCRIBE | 10 |
| `getWidgetRoots(): Widget[]` | LAMBDA, SUBSCRIBE | 8 |
| `getWidgetSpriteCache(): NodeCache` | LAMBDA | 2 |
| `getWorld(): int` | LAMBDA, SUBSCRIBE | 14 |
| `getWorldMap(): WorldMap` | SUBSCRIBE | 1 |
| `getWorldType(): EnumSet` | LAMBDA, SUBSCRIBE | 5 |
| `getWorldView(int): WorldView` | SUBSCRIBE | 3 |
| `hopToWorld(World): void` | LAMBDA, SUBSCRIBE | 2 |
| `invalidateStretching(boolean): void` | SUBSCRIBE | 1 |
| `isClientThread(): boolean` | ASSERT | 12 |
| `isInInstancedRegion(): boolean` | LAMBDA, SUBSCRIBE | 5 |
| `isKeyPressed(int): boolean` | SUBSCRIBE | 9 |
| `isMenuOpen(): boolean` | SUBSCRIBE | 8 |
| `isPrayerActive(Prayer): boolean` | SUBSCRIBE | 1 |
| `isResized(): boolean` | LAMBDA, SUBSCRIBE | 3 |
| `isWidgetSelected(): boolean` | SUBSCRIBE | 1 |
| `loadAnimation(int): Animation` | LAMBDA | 2 |
| `menuAction(int, int, MenuAction, int, int, String, String): void` | LAMBDA | 1 |
| `openInterface(int, int, int): WidgetNode` | LAMBDA | 1 |
| `openWorldHopper(): void` | LAMBDA, SUBSCRIBE | 2 |
| `playSoundEffect(int): void` | LAMBDA, SUBSCRIBE | 2 |
| `playSoundEffect(int, int): void` | SUBSCRIBE | 1 |
| `queueChangedSkill(Skill): void` | LAMBDA, SUBSCRIBE | 2 |
| `queueChangedVarp(int): void` | SUBSCRIBE | 1 |
| `refreshChat(): void` | LAMBDA, SUBSCRIBE | 3 |
| `resetHealthBarCaches(): void` | LAMBDA | 1 |
| `resizeCanvas(): void` | LAMBDA | 2 |
| `runScript(Object[]): void` | LAMBDA, SUBSCRIBE | 27 |
| `setDraggedOnWidget(Widget): void` | SUBSCRIBE | 2 |
| `setDrawCallbacks(DrawCallbacks): void` | LAMBDA | 2 |
| `setExpandedMapLoading(int): void` | LAMBDA | 3 |
| `setGameState(GameState): void` | LAMBDA | 8 |
| `setGeSearchResultCount(int): void` | SUBSCRIBE | 2 |
| `setGeSearchResultIds(short[]): void` | SUBSCRIBE | 2 |
| `setGeSearchResultIndex(int): void` | SUBSCRIBE | 2 |
| `setGpuFlags(int): void` | LAMBDA, SUBSCRIBE | 3 |
| `setHintArrow(NPC): void` | SUBSCRIBE | 3 |
| `setHintArrow(WorldPoint): void` | SUBSCRIBE | 6 |
| `setIdleTimeout(int): void` | SUBSCRIBE | 1 |
| `setInventoryDragDelay(int): void` | LAMBDA | 2 |
| `setLoginScreen(SpritePixels): void` | LAMBDA | 2 |
| `setMenuEntries(MenuEntry[]): void` | SUBSCRIBE | 3 |
| `setMinimapZoom(boolean): void` | SUBSCRIBE | 1 |
| `setModIcons(IndexedSprite[]): void` | ASSERT, LAMBDA | 2 |
| `setShouldRenderLoginScreenFire(boolean): void` | LAMBDA | 2 |
| `setSkyboxColor(int): void` | SUBSCRIBE | 2 |
| `setUnlockedFps(boolean): void` | LAMBDA | 2 |
| `setUnlockedFpsTarget(int): void` | LAMBDA | 1 |
| `setVarbitValue(int[], int, int): void` | SUBSCRIBE | 2 |
| `setVarcIntValue(int, int): void` | LAMBDA | 3 |
| `setVarcStrValue(int, String): void` | LAMBDA | 7 |
| `setWidgetSelected(boolean): void` | SUBSCRIBE | 2 |

### `net.runelite.api.CollisionData`

| Method | Evidence | Caller count |
|---|---|---:|
| `getFlags(): int[][]` | LAMBDA | 1 |

### `net.runelite.api.DecorativeObject`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | SUBSCRIBE | 3 |

### `net.runelite.api.Deque`

| Method | Evidence | Caller count |
|---|---|---:|
| `clear(): void` | SUBSCRIBE | 2 |

### `net.runelite.api.EnumComposition`

| Method | Evidence | Caller count |
|---|---|---:|
| `getIntValue(int): int` | SUBSCRIBE | 2 |

### `net.runelite.api.EquipmentInventorySlot`

| Method | Evidence | Caller count |
|---|---|---:|
| `getSlotIdx(): int` | LAMBDA, SUBSCRIBE | 3 |
| `values(): EquipmentInventorySlot[]` | ASSERT, LAMBDA | 2 |

### `net.runelite.api.Experience`

| Method | Evidence | Caller count |
|---|---|---:|
| `getCombatLevelPrecise(int, int, int, int, int, int, int): double` | SUBSCRIBE | 2 |
| `getLevelForXp(int): int` | SUBSCRIBE | 4 |
| `getXpForLevel(int): int` | SUBSCRIBE | 1 |

### `net.runelite.api.FriendsChatManager`

| Method | Evidence | Caller count |
|---|---|---:|
| `getCount(): int` | SUBSCRIBE | 2 |
| `getSize(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.FriendsChatMember`

| Method | Evidence | Caller count |
|---|---|---:|
| `getName(): String` | SUBSCRIBE | 2 |
| `getRank(): FriendsChatRank` | SUBSCRIBE | 2 |
| `getWorld(): int` | SUBSCRIBE | 2 |

### `net.runelite.api.FriendsChatRank`

| Method | Evidence | Caller count |
|---|---|---:|
| `getValue(): int` | SUBSCRIBE | 2 |

### `net.runelite.api.GameObject`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | SUBSCRIBE | 15 |
| `getOrientation(): int` | SUBSCRIBE | 1 |
| `getWorldLocation(): WorldPoint` | SUBSCRIBE | 5 |
| `sizeX(): int` | SUBSCRIBE | 1 |
| `sizeY(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.GameState`

| Method | Evidence | Caller count |
|---|---|---:|
| `getState(): int` | ASSERT, LAMBDA, SUBSCRIBE | 13 |
| `ordinal(): int` | ASSERT, LAMBDA, SUBSCRIBE | 22 |

### `net.runelite.api.GrandExchangeOffer`

| Method | Evidence | Caller count |
|---|---|---:|
| `getItemId(): int` | SUBSCRIBE | 1 |
| `getQuantitySold(): int` | SUBSCRIBE | 1 |
| `getState(): GrandExchangeOfferState` | SUBSCRIBE | 1 |

### `net.runelite.api.GraphicsObject`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | SUBSCRIBE | 3 |
| `getLocation(): LocalPoint` | LAMBDA, SUBSCRIBE | 2 |

### `net.runelite.api.GroundObject`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | SUBSCRIBE | 1 |
| `getWorldLocation(): WorldPoint` | SUBSCRIBE | 1 |

### `net.runelite.api.HashTable`

| Method | Evidence | Caller count |
|---|---|---:|
| `get(long): Node` | SUBSCRIBE | 1 |
| `iterator(): Iterator` | LAMBDA | 1 |

### `net.runelite.api.HealthBarConfig`

| Method | Evidence | Caller count |
|---|---|---:|
| `getHealthBarFrontSpriteId(): int` | SUBSCRIBE | 1 |
| `setPadding(int): void` | SUBSCRIBE | 1 |

### `net.runelite.api.Hitsplat`

| Method | Evidence | Caller count |
|---|---|---:|
| `getAmount(): int` | SUBSCRIBE | 4 |
| `getHitsplatType(): int` | SUBSCRIBE | 1 |
| `isMine(): boolean` | SUBSCRIBE | 4 |
| `isOthers(): boolean` | SUBSCRIBE | 2 |

### `net.runelite.api.IndexDataBase`

| Method | Evidence | Caller count |
|---|---|---:|
| `getFileIds(int): int[]` | LAMBDA | 1 |
| `isOverlayOutdated(): boolean` | SUBSCRIBE | 1 |

### `net.runelite.api.IndexedObjectSet`

| Method | Evidence | Caller count |
|---|---|---:|
| `byIndex(int): Object` | LAMBDA | 3 |
| `iterator(): Iterator` | LAMBDA, SUBSCRIBE | 2 |
| `stream(): Stream` | LAMBDA | 1 |

### `net.runelite.api.Item`

| Method | Evidence | Caller count |
|---|---|---:|
| `<init>(int, int): void` | LAMBDA, SUBSCRIBE | 2 |
| `getId(): int` | ASSERT, LAMBDA, SUBSCRIBE | 13 |
| `getQuantity(): int` | ASSERT, LAMBDA, SUBSCRIBE | 4 |

### `net.runelite.api.ItemComposition`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | LAMBDA, SUBSCRIBE | 9 |
| `getIntValue(int): int` | SUBSCRIBE | 1 |
| `getLinkedNoteId(): int` | LAMBDA | 1 |
| `getMembersName(): String` | ASSERT, SUBSCRIBE | 3 |
| `getName(): String` | ASSERT, LAMBDA, SUBSCRIBE | 11 |
| `getNote(): int` | LAMBDA | 1 |
| `getPlaceholderTemplateId(): int` | ASSERT | 1 |
| `getPrice(): int` | LAMBDA | 2 |
| `getStringValue(int): String` | SUBSCRIBE | 1 |
| `getSubops(): String[][]` | LAMBDA | 1 |
| `isMembers(): boolean` | LAMBDA | 1 |
| `isStackable(): boolean` | LAMBDA | 2 |
| `isTradeable(): boolean` | LAMBDA | 1 |

### `net.runelite.api.ItemContainer`

| Method | Evidence | Caller count |
|---|---|---:|
| `contains(int): boolean` | SUBSCRIBE | 5 |
| `count(): int` | SUBSCRIBE | 2 |
| `find(int): int` | SUBSCRIBE | 1 |
| `getId(): int` | LAMBDA | 1 |
| `getItem(int): Item` | LAMBDA, SUBSCRIBE | 2 |
| `getItems(): Item[]` | ASSERT, LAMBDA, SUBSCRIBE | 20 |

### `net.runelite.api.ItemLayer`

| Method | Evidence | Caller count |
|---|---|---:|
| `getHeight(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.IterableHashTable`

| Method | Evidence | Caller count |
|---|---|---:|
| `get(long): Node` | SUBSCRIBE | 3 |

### `net.runelite.api.Menu`

| Method | Evidence | Caller count |
|---|---|---:|
| `createMenuEntry(int): MenuEntry` | SUBSCRIBE | 7 |
| `getMenuEntries(): MenuEntry[]` | SUBSCRIBE | 4 |
| `removeMenuEntry(MenuEntry): void` | SUBSCRIBE | 1 |
| `setMenuEntries(MenuEntry[]): void` | SUBSCRIBE | 2 |

### `net.runelite.api.MenuAction`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | SUBSCRIBE | 9 |
| `of(int): MenuAction` | LAMBDA, SUBSCRIBE | 3 |
| `ordinal(): int` | SUBSCRIBE | 5 |

### `net.runelite.api.MenuEntry`

| Method | Evidence | Caller count |
|---|---|---:|
| `createSubMenu(): Menu` | SUBSCRIBE | 4 |
| `getIdentifier(): int` | SUBSCRIBE | 5 |
| `getItemId(): int` | SUBSCRIBE | 1 |
| `getNpc(): NPC` | SUBSCRIBE | 10 |
| `getOption(): String` | SUBSCRIBE | 10 |
| `getParam0(): int` | SUBSCRIBE | 4 |
| `getParam1(): int` | SUBSCRIBE | 6 |
| `getPlayer(): Player` | SUBSCRIBE | 4 |
| `getTarget(): String` | SUBSCRIBE | 10 |
| `getType(): MenuAction` | SUBSCRIBE | 12 |
| `getWidget(): Widget` | SUBSCRIBE | 3 |
| `getWorldViewId(): int` | SUBSCRIBE | 6 |
| `onClick(Consumer): MenuEntry` | SUBSCRIBE | 18 |
| `setDeprioritized(boolean): MenuEntry` | SUBSCRIBE | 3 |
| `setForceLeftClick(boolean): MenuEntry` | SUBSCRIBE | 1 |
| `setIdentifier(int): MenuEntry` | SUBSCRIBE | 7 |
| `setItemId(int): MenuEntry` | SUBSCRIBE | 3 |
| `setOption(String): MenuEntry` | SUBSCRIBE | 21 |
| `setParam0(int): MenuEntry` | SUBSCRIBE | 6 |
| `setParam1(int): MenuEntry` | SUBSCRIBE | 8 |
| `setTarget(String): MenuEntry` | SUBSCRIBE | 25 |
| `setType(MenuAction): MenuEntry` | SUBSCRIBE | 20 |
| `setWorldViewId(int): MenuEntry` | SUBSCRIBE | 3 |

### `net.runelite.api.MessageNode`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | SUBSCRIBE | 1 |
| `getName(): String` | SUBSCRIBE | 3 |
| `getRuneLiteFormatMessage(): String` | SUBSCRIBE | 1 |
| `getSender(): String` | SUBSCRIBE | 1 |
| `getTimestamp(): int` | SUBSCRIBE | 2 |
| `getType(): ChatMessageType` | SUBSCRIBE | 2 |
| `getValue(): String` | SUBSCRIBE | 6 |
| `setName(String): void` | SUBSCRIBE | 1 |
| `setRuneLiteFormatMessage(String): void` | SUBSCRIBE | 3 |
| `setTimestamp(int): void` | SUBSCRIBE | 1 |
| `setValue(String): void` | SUBSCRIBE | 4 |

### `net.runelite.api.NPC`

| Method | Evidence | Caller count |
|---|---|---:|
| `getAnimation(): int` | SUBSCRIBE | 1 |
| `getComposition(): NPCComposition` | LAMBDA | 1 |
| `getId(): int` | LAMBDA, SUBSCRIBE | 39 |
| `getIdlePoseAnimation(): int` | LAMBDA | 1 |
| `getIndex(): int` | LAMBDA, SUBSCRIBE | 8 |
| `getInteracting(): Actor` | SUBSCRIBE | 1 |
| `getName(): String` | SUBSCRIBE | 5 |
| `getPoseAnimation(): int` | LAMBDA | 1 |
| `getTransformedComposition(): NPCComposition` | SUBSCRIBE | 2 |
| `getWorldLocation(): WorldPoint` | SUBSCRIBE | 4 |
| `isDead(): boolean` | SUBSCRIBE | 4 |
| `setDead(boolean): void` | SUBSCRIBE | 1 |

### `net.runelite.api.NPCComposition`

| Method | Evidence | Caller count |
|---|---|---:|
| `getActions(): String[]` | LAMBDA | 1 |
| `getCombatLevel(): int` | SUBSCRIBE | 1 |
| `getId(): int` | SUBSCRIBE | 2 |
| `getName(): String` | LAMBDA, SUBSCRIBE | 4 |

### `net.runelite.api.Nameable`

| Method | Evidence | Caller count |
|---|---|---:|
| `getName(): String` | SUBSCRIBE | 2 |
| `getPrevName(): String` | SUBSCRIBE | 1 |

### `net.runelite.api.NameableContainer`

| Method | Evidence | Caller count |
|---|---|---:|
| `getCount(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.NodeCache`

| Method | Evidence | Caller count |
|---|---|---:|
| `reset(): void` | LAMBDA | 2 |

### `net.runelite.api.ObjectComposition`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | SUBSCRIBE | 2 |
| `getImpostor(): ObjectComposition` | LAMBDA, SUBSCRIBE | 8 |
| `getImpostorIds(): int[]` | LAMBDA, SUBSCRIBE | 7 |
| `getName(): String` | LAMBDA, SUBSCRIBE | 3 |

### `net.runelite.api.Perspective`

| Method | Evidence | Caller count |
|---|---|---:|
| `getClickbox(Client, WorldView, Model, int, int, int, int): Shape` | LAMBDA | 1 |
| `getTileHeight(Client, LocalPoint, int): int` | LAMBDA | 1 |
| `localToMinimap(Client, LocalPoint): Point` | LAMBDA | 2 |

### `net.runelite.api.Player`

| Method | Evidence | Caller count |
|---|---|---:|
| `getAnimation(): int` | LAMBDA, SUBSCRIBE | 9 |
| `getCanvasTilePoly(): Polygon` | SUBSCRIBE | 1 |
| `getCombatLevel(): int` | LAMBDA, SUBSCRIBE | 4 |
| `getGraphic(): int` | SUBSCRIBE | 2 |
| `getHealthRatio(): int` | LAMBDA, SUBSCRIBE | 2 |
| `getHealthScale(): int` | LAMBDA | 1 |
| `getId(): int` | SUBSCRIBE | 1 |
| `getIdlePoseAnimation(): int` | LAMBDA | 2 |
| `getInteracting(): Actor` | LAMBDA, SUBSCRIBE | 7 |
| `getLocalLocation(): LocalPoint` | LAMBDA, SUBSCRIBE | 11 |
| `getName(): String` | LAMBDA, SUBSCRIBE | 18 |
| `getOverheadIcon(): HeadIcon` | LAMBDA | 1 |
| `getPlayerComposition(): PlayerComposition` | SUBSCRIBE | 1 |
| `getPoseAnimation(): int` | LAMBDA | 3 |
| `getSkullIcon(): int` | LAMBDA | 1 |
| `getWorldLocation(): WorldPoint` | LAMBDA, SUBSCRIBE | 25 |
| `getWorldView(): WorldView` | LAMBDA | 8 |
| `isClanMember(): boolean` | LAMBDA, SUBSCRIBE | 4 |
| `isFriend(): boolean` | LAMBDA, SUBSCRIBE | 2 |
| `isFriendsChatMember(): boolean` | LAMBDA, SUBSCRIBE | 4 |
| `isInteracting(): boolean` | LAMBDA | 3 |
| `setAnimation(int): void` | SUBSCRIBE | 1 |
| `setAnimationFrame(int): void` | SUBSCRIBE | 1 |
| `setGraphic(int): void` | SUBSCRIBE | 1 |
| `setIdlePoseAnimation(int): void` | SUBSCRIBE | 1 |
| `setPoseAnimation(int): void` | SUBSCRIBE | 1 |
| `setSpotAnimFrame(int): void` | SUBSCRIBE | 1 |

### `net.runelite.api.PlayerComposition`

| Method | Evidence | Caller count |
|---|---|---:|
| `getEquipmentId(KitType): int` | LAMBDA | 1 |
| `getEquipmentIds(): int[]` | SUBSCRIBE | 1 |
| `setHash(): void` | SUBSCRIBE | 1 |
| `setTransformedNpcId(int): void` | SUBSCRIBE | 1 |

### `net.runelite.api.Point`

| Method | Evidence | Caller count |
|---|---|---:|
| `getX(): int` | SUBSCRIBE | 2 |
| `getY(): int` | SUBSCRIBE | 2 |

### `net.runelite.api.Preferences`

| Method | Evidence | Caller count |
|---|---|---:|
| `getRememberedUsername(): String` | SUBSCRIBE | 1 |
| `getSoundEffectVolume(): int` | SUBSCRIBE | 1 |
| `setSoundEffectVolume(int): void` | SUBSCRIBE | 1 |

### `net.runelite.api.Projectile`

| Method | Evidence | Caller count |
|---|---|---:|
| `getEndCycle(): int` | SUBSCRIBE | 1 |
| `getId(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.Projection`

| Method | Evidence | Caller count |
|---|---|---:|
| `project(float, float, float): float[]` | LAMBDA | 1 |

### `net.runelite.api.Quest`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | LAMBDA | 1 |
| `getState(Client): QuestState` | LAMBDA, SUBSCRIBE | 2 |
| `values(): Quest[]` | LAMBDA, SUBSCRIBE | 2 |

### `net.runelite.api.RuneLiteObject`

| Method | Evidence | Caller count |
|---|---|---:|
| `setAnimation(Animation): void` | LAMBDA | 3 |
| `setModel(Model): void` | LAMBDA | 3 |
| `setShouldLoop(boolean): void` | LAMBDA | 1 |

### `net.runelite.api.Scene`

| Method | Evidence | Caller count |
|---|---|---:|
| `getTiles(): Tile[][][]` | LAMBDA, SUBSCRIBE | 2 |
| `isInstance(): boolean` | LAMBDA | 1 |
| `setMinLevel(int): void` | SUBSCRIBE | 1 |
| `setRoofRemovalMode(int): void` | LAMBDA, SUBSCRIBE | 3 |

### `net.runelite.api.ScriptEvent`

| Method | Evidence | Caller count |
|---|---|---:|
| `getArguments(): Object[]` | SUBSCRIBE | 6 |
| `getSource(): Widget` | SUBSCRIBE | 2 |
| `run(): void` | LAMBDA | 7 |

### `net.runelite.api.ScriptEventBuilder`

| Method | Evidence | Caller count |
|---|---|---:|
| `build(): ScriptEvent` | LAMBDA | 7 |
| `setOp(int): ScriptEventBuilder` | LAMBDA | 2 |
| `setSource(Widget): ScriptEventBuilder` | LAMBDA | 5 |

### `net.runelite.api.Skill`

| Method | Evidence | Caller count |
|---|---|---:|
| `equals(Object): boolean` | SUBSCRIBE | 1 |
| `getName(): String` | LAMBDA, SUBSCRIBE | 2 |
| `ordinal(): int` | SUBSCRIBE | 5 |
| `valueOf(String): Skill` | SUBSCRIBE | 2 |
| `values(): Skill[]` | LAMBDA, SUBSCRIBE | 5 |

### `net.runelite.api.SpritePixels`

| Method | Evidence | Caller count |
|---|---|---:|
| `toBufferedImage(): BufferedImage` | ASSERT | 1 |
| `toBufferedImage(BufferedImage): void` | LAMBDA | 1 |

### `net.runelite.api.TextureProvider`

| Method | Evidence | Caller count |
|---|---|---:|
| `getBrightness(): double` | SUBSCRIBE | 1 |

### `net.runelite.api.Tile`

| Method | Evidence | Caller count |
|---|---|---:|
| `getGameObjects(): GameObject[]` | SUBSCRIBE | 1 |
| `getGroundItems(): List` | LAMBDA | 1 |
| `getItemLayer(): ItemLayer` | SUBSCRIBE | 1 |
| `getLocalLocation(): LocalPoint` | SUBSCRIBE | 4 |
| `getWorldLocation(): WorldPoint` | SUBSCRIBE | 7 |

### `net.runelite.api.TileItem`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | LAMBDA, SUBSCRIBE | 20 |
| `getQuantity(): int` | LAMBDA, SUBSCRIBE | 8 |

### `net.runelite.api.TileObject`

| Method | Evidence | Caller count |
|---|---|---:|
| `getClickbox(): Shape` | LAMBDA | 1 |
| `getId(): int` | LAMBDA | 5 |

### `net.runelite.api.VarbitComposition`

| Method | Evidence | Caller count |
|---|---|---:|
| `getIndex(): int` | LAMBDA, SUBSCRIBE | 2 |

### `net.runelite.api.WallObject`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | SUBSCRIBE | 3 |

### `net.runelite.api.World`

| Method | Evidence | Caller count |
|---|---|---:|
| `getId(): int` | SUBSCRIBE | 1 |
| `getPlayerCount(): int` | SUBSCRIBE | 1 |
| `setActivity(String): void` | ASSERT, LAMBDA | 2 |
| `setAddress(String): void` | ASSERT, LAMBDA | 2 |
| `setId(int): void` | ASSERT, LAMBDA | 2 |
| `setLocation(int): void` | ASSERT, LAMBDA | 2 |
| `setPlayerCount(int): void` | ASSERT, LAMBDA | 2 |
| `setTypes(EnumSet): void` | ASSERT, LAMBDA | 2 |

### `net.runelite.api.WorldEntity`

| Method | Evidence | Caller count |
|---|---|---:|
| `getConfig(): WorldEntityConfig` | LAMBDA | 1 |
| `getLocalLocation(): LocalPoint` | LAMBDA | 2 |
| `getOrientation(): int` | LAMBDA | 1 |
| `getOwnerType(): int` | LAMBDA | 1 |
| `getTargetLocation(): LocalPoint` | LAMBDA | 1 |
| `getTargetOrientation(): int` | LAMBDA | 1 |
| `getWorldView(): WorldView` | LAMBDA, SUBSCRIBE | 3 |
| `isHiddenForOverlap(): boolean` | LAMBDA | 1 |
| `transformToMainWorld(LocalPoint): LocalPoint` | LAMBDA | 1 |

### `net.runelite.api.WorldView`

| Method | Evidence | Caller count |
|---|---|---:|
| `getBaseX(): int` | LAMBDA | 1 |
| `getBaseY(): int` | LAMBDA | 1 |
| `getCollisionMaps(): CollisionData[]` | LAMBDA | 1 |
| `getId(): int` | SUBSCRIBE | 3 |
| `getMainWorldProjection(): Projection` | LAMBDA | 1 |
| `getMapRegions(): int[]` | SUBSCRIBE | 3 |
| `getPlane(): int` | LAMBDA, SUBSCRIBE | 6 |
| `getScene(): Scene` | LAMBDA, SUBSCRIBE | 3 |
| `getSelectedSceneTile(): Tile` | SUBSCRIBE | 1 |
| `getXteaKeys(): int[][]` | SUBSCRIBE | 1 |
| `isTopLevel(): boolean` | LAMBDA, SUBSCRIBE | 4 |
| `npcs(): IndexedObjectSet` | LAMBDA, SUBSCRIBE | 4 |
| `worldEntities(): IndexedObjectSet` | LAMBDA, SUBSCRIBE | 3 |

### `net.runelite.api.clan.ClanChannel`

| Method | Evidence | Caller count |
|---|---|---:|
| `getMembers(): List` | SUBSCRIBE | 1 |

### `net.runelite.api.clan.ClanChannelMember`

| Method | Evidence | Caller count |
|---|---|---:|
| `getName(): String` | SUBSCRIBE | 3 |
| `getWorld(): int` | SUBSCRIBE | 2 |

### `net.runelite.api.coords.Angle`

| Method | Evidence | Caller count |
|---|---|---:|
| `<init>(int): void` | SUBSCRIBE | 1 |
| `getNearestDirection(): Direction` | SUBSCRIBE | 1 |

### `net.runelite.api.coords.Direction`

| Method | Evidence | Caller count |
|---|---|---:|
| `ordinal(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.coords.LocalPoint`

| Method | Evidence | Caller count |
|---|---|---:|
| `distanceTo(LocalPoint): int` | LAMBDA | 4 |
| `fromWorld(Client, WorldPoint): LocalPoint` | LAMBDA, SUBSCRIBE | 3 |
| `fromWorld(WorldView, WorldPoint): LocalPoint` | LAMBDA | 2 |
| `getSceneX(): int` | SUBSCRIBE | 4 |
| `getSceneY(): int` | SUBSCRIBE | 4 |
| `getWorldView(): int` | LAMBDA | 2 |
| `getX(): int` | LAMBDA, SUBSCRIBE | 4 |
| `getY(): int` | LAMBDA, SUBSCRIBE | 4 |

### `net.runelite.api.coords.WorldPoint`

| Method | Evidence | Caller count |
|---|---|---:|
| `<init>(int, int, int): void` | SUBSCRIBE | 3 |
| `distanceTo(WorldPoint): int` | LAMBDA, SUBSCRIBE | 7 |
| `distanceTo2D(WorldPoint): int` | SUBSCRIBE | 1 |
| `dx(int): WorldPoint` | SUBSCRIBE | 1 |
| `dy(int): WorldPoint` | SUBSCRIBE | 1 |
| `equals(Object): boolean` | LAMBDA, SUBSCRIBE | 7 |
| `fromCoord(int): WorldPoint` | SUBSCRIBE | 3 |
| `fromLocal(Client, LocalPoint): WorldPoint` | LAMBDA, SUBSCRIBE | 4 |
| `fromLocal(WorldView, int, int, int): WorldPoint` | LAMBDA | 2 |
| `fromLocalInstance(Client, LocalPoint): WorldPoint` | LAMBDA, SUBSCRIBE | 5 |
| `fromScene(Client, int, int, int): WorldPoint` | SUBSCRIBE | 6 |
| `fromScene(WorldView, int, int, int): WorldPoint` | SUBSCRIBE | 1 |
| `getMirrorPoint(WorldPoint, boolean): WorldPoint` | SUBSCRIBE | 1 |
| `getPlane(): int` | LAMBDA, SUBSCRIBE | 7 |
| `getRegionID(): int` | SUBSCRIBE | 11 |
| `getX(): int` | LAMBDA, SUBSCRIBE | 8 |
| `getY(): int` | LAMBDA, SUBSCRIBE | 9 |
| `isInScene(Client): boolean` | SUBSCRIBE | 2 |
| `isInScene(Client, int, int): boolean` | SUBSCRIBE | 2 |

### `net.runelite.api.events.ActorDeath`

| Method | Evidence | Caller count |
|---|---|---:|
| `getActor(): Actor` | SUBSCRIBE | 2 |

### `net.runelite.api.events.AnimationChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getActor(): Actor` | SUBSCRIBE | 9 |

### `net.runelite.api.events.AreaSoundEffectPlayed`

| Method | Evidence | Caller count |
|---|---|---:|
| `consume(): void` | SUBSCRIBE | 1 |
| `getDelay(): int` | SUBSCRIBE | 1 |
| `getRange(): int` | SUBSCRIBE | 1 |
| `getSceneX(): int` | SUBSCRIBE | 1 |
| `getSceneY(): int` | SUBSCRIBE | 1 |
| `getSoundId(): int` | SUBSCRIBE | 2 |
| `getSource(): Actor` | SUBSCRIBE | 2 |

### `net.runelite.api.events.BeforeMenuRender`

| Method | Evidence | Caller count |
|---|---|---:|
| `consume(): void` | SUBSCRIBE | 1 |

### `net.runelite.api.events.ChatMessage`

| Method | Evidence | Caller count |
|---|---|---:|
| `getMessage(): String` | SUBSCRIBE | 50 |
| `getMessageNode(): MessageNode` | SUBSCRIBE | 7 |
| `getName(): String` | SUBSCRIBE | 3 |
| `getTimestamp(): int` | SUBSCRIBE | 1 |
| `getType(): ChatMessageType` | SUBSCRIBE | 53 |
| `setMessage(String): void` | SUBSCRIBE | 1 |
| `setName(String): void` | SUBSCRIBE | 1 |

### `net.runelite.api.events.ClanChannelChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getClanChannel(): ClanChannel` | SUBSCRIBE | 1 |
| `getClanId(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.events.ClanMemberJoined`

| Method | Evidence | Caller count |
|---|---|---:|
| `getClanChannel(): ClanChannel` | SUBSCRIBE | 1 |
| `getClanMember(): ClanChannelMember` | SUBSCRIBE | 2 |

### `net.runelite.api.events.ClanMemberLeft`

| Method | Evidence | Caller count |
|---|---|---:|
| `getClanChannel(): ClanChannel` | SUBSCRIBE | 1 |
| `getClanMember(): ClanChannelMember` | SUBSCRIBE | 2 |

### `net.runelite.api.events.CommandExecuted`

| Method | Evidence | Caller count |
|---|---|---:|
| `getArguments(): String[]` | SUBSCRIBE | 5 |
| `getCommand(): String` | SUBSCRIBE | 8 |

### `net.runelite.api.events.DecorativeObjectDespawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getDecorativeObject(): DecorativeObject` | SUBSCRIBE | 6 |
| `getTile(): Tile` | SUBSCRIBE | 1 |

### `net.runelite.api.events.DecorativeObjectSpawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getDecorativeObject(): DecorativeObject` | SUBSCRIBE | 7 |
| `getTile(): Tile` | SUBSCRIBE | 2 |

### `net.runelite.api.events.FakeXpDrop`

| Method | Evidence | Caller count |
|---|---|---:|
| `<init>(Skill, int): void` | SUBSCRIBE | 1 |
| `getSkill(): Skill` | SUBSCRIBE | 1 |

### `net.runelite.api.events.FocusChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `isFocused(): boolean` | SUBSCRIBE | 7 |

### `net.runelite.api.events.FriendsChatChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `isJoined(): boolean` | SUBSCRIBE | 2 |

### `net.runelite.api.events.FriendsChatMemberJoined`

| Method | Evidence | Caller count |
|---|---|---:|
| `getMember(): FriendsChatMember` | SUBSCRIBE | 2 |

### `net.runelite.api.events.FriendsChatMemberLeft`

| Method | Evidence | Caller count |
|---|---|---:|
| `getMember(): FriendsChatMember` | SUBSCRIBE | 2 |

### `net.runelite.api.events.GameObjectDespawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getGameObject(): GameObject` | SUBSCRIBE | 10 |
| `getTile(): Tile` | SUBSCRIBE | 2 |

### `net.runelite.api.events.GameObjectSpawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getGameObject(): GameObject` | SUBSCRIBE | 16 |
| `getTile(): Tile` | SUBSCRIBE | 3 |

### `net.runelite.api.events.GameStateChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getGameState(): GameState` | SUBSCRIBE | 73 |

### `net.runelite.api.events.GrandExchangeOfferChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getOffer(): GrandExchangeOffer` | SUBSCRIBE | 1 |
| `getSlot(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.events.GrandExchangeSearched`

| Method | Evidence | Caller count |
|---|---|---:|
| `consume(): void` | SUBSCRIBE | 3 |
| `isConsumed(): boolean` | SUBSCRIBE | 1 |

### `net.runelite.api.events.GraphicChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getActor(): Actor` | SUBSCRIBE | 3 |

### `net.runelite.api.events.GraphicsObjectCreated`

| Method | Evidence | Caller count |
|---|---|---:|
| `getGraphicsObject(): GraphicsObject` | SUBSCRIBE | 3 |

### `net.runelite.api.events.GroundObjectDespawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getGroundObject(): GroundObject` | SUBSCRIBE | 4 |
| `getTile(): Tile` | SUBSCRIBE | 1 |

### `net.runelite.api.events.GroundObjectSpawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getGroundObject(): GroundObject` | SUBSCRIBE | 6 |
| `getTile(): Tile` | SUBSCRIBE | 1 |

### `net.runelite.api.events.HitsplatApplied`

| Method | Evidence | Caller count |
|---|---|---:|
| `getActor(): Actor` | SUBSCRIBE | 6 |
| `getHitsplat(): Hitsplat` | SUBSCRIBE | 6 |

### `net.runelite.api.events.InteractingChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getSource(): Actor` | SUBSCRIBE | 8 |
| `getTarget(): Actor` | SUBSCRIBE | 8 |

### `net.runelite.api.events.ItemContainerChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `<init>(int, ItemContainer): void` | LAMBDA | 1 |
| `getContainerId(): int` | ASSERT, SUBSCRIBE | 18 |
| `getItemContainer(): ItemContainer` | ASSERT, SUBSCRIBE | 21 |

### `net.runelite.api.events.ItemDespawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getItem(): TileItem` | SUBSCRIBE | 6 |
| `getTile(): Tile` | SUBSCRIBE | 5 |

### `net.runelite.api.events.ItemQuantityChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getItem(): TileItem` | SUBSCRIBE | 1 |
| `getNewQuantity(): int` | SUBSCRIBE | 1 |
| `getOldQuantity(): int` | SUBSCRIBE | 1 |
| `getTile(): Tile` | SUBSCRIBE | 1 |

### `net.runelite.api.events.ItemSpawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getItem(): TileItem` | SUBSCRIBE | 6 |
| `getTile(): Tile` | SUBSCRIBE | 5 |

### `net.runelite.api.events.MenuEntryAdded`

| Method | Evidence | Caller count |
|---|---|---:|
| `getActionParam0(): int` | SUBSCRIBE | 8 |
| `getActionParam1(): int` | SUBSCRIBE | 15 |
| `getIdentifier(): int` | SUBSCRIBE | 8 |
| `getItemId(): int` | SUBSCRIBE | 2 |
| `getMenuEntry(): MenuEntry` | SUBSCRIBE | 10 |
| `getOption(): String` | SUBSCRIBE | 15 |
| `getTarget(): String` | SUBSCRIBE | 13 |
| `getType(): int` | SUBSCRIBE | 11 |

### `net.runelite.api.events.MenuOpened`

| Method | Evidence | Caller count |
|---|---|---:|
| `getMenuEntries(): MenuEntry[]` | SUBSCRIBE | 2 |

### `net.runelite.api.events.MenuOptionClicked`

| Method | Evidence | Caller count |
|---|---|---:|
| `consume(): void` | SUBSCRIBE | 4 |
| `getId(): int` | SUBSCRIBE | 10 |
| `getItemId(): int` | SUBSCRIBE | 2 |
| `getMenuAction(): MenuAction` | SUBSCRIBE | 16 |
| `getMenuEntry(): MenuEntry` | SUBSCRIBE | 8 |
| `getMenuOption(): String` | SUBSCRIBE | 11 |
| `getMenuTarget(): String` | SUBSCRIBE | 3 |
| `getParam0(): int` | SUBSCRIBE | 9 |
| `getParam1(): int` | SUBSCRIBE | 13 |
| `getWidget(): Widget` | SUBSCRIBE | 5 |
| `isItemOp(): boolean` | SUBSCRIBE | 3 |

### `net.runelite.api.events.MenuShouldLeftClick`

| Method | Evidence | Caller count |
|---|---|---:|
| `setForceRightClick(boolean): void` | SUBSCRIBE | 1 |

### `net.runelite.api.events.NameableNameChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getNameable(): Nameable` | SUBSCRIBE | 1 |

### `net.runelite.api.events.NpcChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getNpc(): NPC` | SUBSCRIBE | 11 |

### `net.runelite.api.events.NpcDespawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getNpc(): NPC` | SUBSCRIBE | 20 |

### `net.runelite.api.events.NpcSpawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getNpc(): NPC` | SUBSCRIBE | 16 |

### `net.runelite.api.events.OverheadTextChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getActor(): Actor` | SUBSCRIBE | 3 |
| `getOverheadText(): String` | SUBSCRIBE | 3 |

### `net.runelite.api.events.PlayerChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getPlayer(): Player` | SUBSCRIBE | 2 |

### `net.runelite.api.events.PlayerDespawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getPlayer(): Player` | SUBSCRIBE | 3 |

### `net.runelite.api.events.PlayerMenuOptionsChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getIndex(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.events.PlayerSpawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getPlayer(): Player` | SUBSCRIBE | 1 |

### `net.runelite.api.events.PostHealthBarConfig`

| Method | Evidence | Caller count |
|---|---|---:|
| `getHealthBarConfig(): HealthBarConfig` | SUBSCRIBE | 1 |

### `net.runelite.api.events.PreMapLoad`

| Method | Evidence | Caller count |
|---|---|---:|
| `getScene(): Scene` | SUBSCRIBE | 1 |

### `net.runelite.api.events.ProjectileMoved`

| Method | Evidence | Caller count |
|---|---|---:|
| `getPosition(): LocalPoint` | SUBSCRIBE | 1 |
| `getProjectile(): Projectile` | SUBSCRIBE | 1 |

### `net.runelite.api.events.RemovedFriend`

| Method | Evidence | Caller count |
|---|---|---:|
| `getNameable(): Nameable` | SUBSCRIBE | 1 |

### `net.runelite.api.events.ScriptCallbackEvent`

| Method | Evidence | Caller count |
|---|---|---:|
| `getEventName(): String` | SUBSCRIBE | 23 |

### `net.runelite.api.events.ScriptPostFired`

| Method | Evidence | Caller count |
|---|---|---:|
| `getScriptId(): int` | SUBSCRIBE | 25 |

### `net.runelite.api.events.ScriptPreFired`

| Method | Evidence | Caller count |
|---|---|---:|
| `getScriptEvent(): ScriptEvent` | SUBSCRIBE | 7 |
| `getScriptId(): int` | SUBSCRIBE | 19 |

### `net.runelite.api.events.SoundEffectPlayed`

| Method | Evidence | Caller count |
|---|---|---:|
| `consume(): void` | SUBSCRIBE | 1 |
| `getDelay(): int` | SUBSCRIBE | 1 |
| `getSoundId(): int` | SUBSCRIBE | 2 |

### `net.runelite.api.events.StatChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `<init>(Skill, int, int, int): void` | SUBSCRIBE | 1 |
| `getBoostedLevel(): int` | SUBSCRIBE | 2 |
| `getLevel(): int` | SUBSCRIBE | 3 |
| `getSkill(): Skill` | SUBSCRIBE | 10 |
| `getXp(): int` | SUBSCRIBE | 8 |

### `net.runelite.api.events.VarClientIntChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getIndex(): int` | SUBSCRIBE | 2 |

### `net.runelite.api.events.VarClientStrChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `getIndex(): int` | SUBSCRIBE | 2 |

### `net.runelite.api.events.VarbitChanged`

| Method | Evidence | Caller count |
|---|---|---:|
| `<init>(): void` | SUBSCRIBE | 1 |
| `getIndex(): int` | SUBSCRIBE | 1 |
| `getValue(): int` | ASSERT, SUBSCRIBE | 19 |
| `getVarbitId(): int` | ASSERT, SUBSCRIBE | 22 |
| `getVarpId(): int` | SUBSCRIBE | 19 |
| `setValue(int): void` | SUBSCRIBE | 1 |
| `setVarbitId(int): void` | SUBSCRIBE | 1 |
| `setVarpId(int): void` | SUBSCRIBE | 1 |

### `net.runelite.api.events.WallObjectDespawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getTile(): Tile` | SUBSCRIBE | 1 |
| `getWallObject(): WallObject` | SUBSCRIBE | 4 |

### `net.runelite.api.events.WallObjectSpawned`

| Method | Evidence | Caller count |
|---|---|---:|
| `getTile(): Tile` | SUBSCRIBE | 2 |
| `getWallObject(): WallObject` | SUBSCRIBE | 7 |

### `net.runelite.api.events.WidgetClosed`

| Method | Evidence | Caller count |
|---|---|---:|
| `getGroupId(): int` | SUBSCRIBE | 5 |
| `getModalMode(): int` | SUBSCRIBE | 1 |
| `isUnload(): boolean` | SUBSCRIBE | 2 |

### `net.runelite.api.events.WidgetLoaded`

| Method | Evidence | Caller count |
|---|---|---:|
| `getGroupId(): int` | ASSERT, SUBSCRIBE | 27 |

### `net.runelite.api.events.WorldListLoad`

| Method | Evidence | Caller count |
|---|---|---:|
| `getWorlds(): World[]` | SUBSCRIBE | 1 |

### `net.runelite.api.events.WorldViewLoaded`

| Method | Evidence | Caller count |
|---|---|---:|
| `getWorldView(): WorldView` | SUBSCRIBE | 4 |

### `net.runelite.api.events.WorldViewUnloaded`

| Method | Evidence | Caller count |
|---|---|---:|
| `getWorldView(): WorldView` | SUBSCRIBE | 4 |

### `net.runelite.api.kit.KitType`

| Method | Evidence | Caller count |
|---|---|---:|
| `getIndex(): int` | SUBSCRIBE | 1 |
| `values(): KitType[]` | LAMBDA | 1 |

### `net.runelite.api.vars.InputType`

| Method | Evidence | Caller count |
|---|---|---:|
| `getType(): int` | LAMBDA | 2 |

### `net.runelite.api.widgets.Widget`

| Method | Evidence | Caller count |
|---|---|---:|
| `createChild(int, int): Widget` | LAMBDA | 5 |
| `deleteAllChildren(): void` | LAMBDA | 5 |
| `getActions(): String[]` | LAMBDA, SUBSCRIBE | 3 |
| `getBounds(): Rectangle` | LAMBDA, SUBSCRIBE | 2 |
| `getChild(int): Widget` | LAMBDA, SUBSCRIBE | 15 |
| `getChildren(): Widget[]` | LAMBDA, SUBSCRIBE | 7 |
| `getClickMask(): int` | SUBSCRIBE | 1 |
| `getDynamicChildren(): Widget[]` | ASSERT, LAMBDA, SUBSCRIBE | 9 |
| `getHeight(): int` | SUBSCRIBE | 1 |
| `getId(): int` | LAMBDA, SUBSCRIBE | 14 |
| `getIndex(): int` | ASSERT, LAMBDA, SUBSCRIBE | 7 |
| `getItemId(): int` | ASSERT, LAMBDA, SUBSCRIBE | 14 |
| `getItemQuantity(): int` | ASSERT | 1 |
| `getModelId(): int` | LAMBDA, SUBSCRIBE | 5 |
| `getModelType(): int` | SUBSCRIBE | 1 |
| `getName(): String` | SUBSCRIBE | 2 |
| `getOnInvTransmitListener(): Object[]` | LAMBDA | 4 |
| `getOnLoadListener(): Object[]` | LAMBDA | 4 |
| `getOnOpListener(): Object[]` | LAMBDA, SUBSCRIBE | 4 |
| `getOnVarTransmitListener(): Object[]` | LAMBDA | 3 |
| `getOpacity(): int` | SUBSCRIBE | 1 |
| `getParent(): Widget` | LAMBDA, SUBSCRIBE | 7 |
| `getParentId(): int` | SUBSCRIBE | 2 |
| `getSpriteId(): int` | LAMBDA | 1 |
| `getStaticChildren(): Widget[]` | SUBSCRIBE | 3 |
| `getText(): String` | LAMBDA, SUBSCRIBE | 37 |
| `getType(): int` | SUBSCRIBE | 2 |
| `getVarTransmitTrigger(): int[]` | SUBSCRIBE | 1 |
| `getWidth(): int` | LAMBDA, SUBSCRIBE | 4 |
| `isHidden(): boolean` | LAMBDA, SUBSCRIBE | 26 |
| `revalidate(): void` | LAMBDA | 8 |
| `setAction(int, String): void` | LAMBDA, SUBSCRIBE | 6 |
| `setBorderType(int): void` | LAMBDA | 1 |
| `setChildren(Widget[]): void` | LAMBDA | 1 |
| `setClickMask(int): Widget` | LAMBDA, SUBSCRIBE | 3 |
| `setDragDeadTime(int): void` | SUBSCRIBE | 1 |
| `setFontId(int): Widget` | LAMBDA | 2 |
| `setHasListener(boolean): Widget` | LAMBDA | 2 |
| `setHidden(boolean): Widget` | LAMBDA, SUBSCRIBE | 9 |
| `setItemId(int): Widget` | LAMBDA | 1 |
| `setItemQuantity(int): Widget` | LAMBDA | 1 |
| `setItemQuantityMode(int): Widget` | LAMBDA | 1 |
| `setModelId(int): Widget` | LAMBDA | 1 |
| `setName(String): Widget` | LAMBDA | 4 |
| `setNoClickThrough(boolean): void` | LAMBDA | 2 |
| `setOnKeyListener(Object[]): void` | SUBSCRIBE | 1 |
| `setOnMouseLeaveListener(Object[]): void` | LAMBDA | 1 |
| `setOnMouseOverListener(Object[]): void` | LAMBDA | 1 |
| `setOnMouseRepeatListener(Object[]): void` | LAMBDA, SUBSCRIBE | 2 |
| `setOnOpListener(Object[]): void` | LAMBDA | 5 |
| `setOnTargetEnterListener(Object[]): void` | LAMBDA | 2 |
| `setOnTargetLeaveListener(Object[]): void` | LAMBDA | 2 |
| `setOpacity(int): Widget` | LAMBDA, SUBSCRIBE | 3 |
| `setOriginalHeight(int): Widget` | LAMBDA | 5 |
| `setOriginalWidth(int): Widget` | LAMBDA | 4 |
| `setOriginalX(int): Widget` | LAMBDA | 6 |
| `setOriginalY(int): Widget` | LAMBDA | 5 |
| `setSpriteId(int): Widget` | LAMBDA | 3 |
| `setTargetVerb(String): void` | LAMBDA | 2 |
| `setText(String): Widget` | LAMBDA, SUBSCRIBE | 17 |
| `setTextColor(int): Widget` | LAMBDA | 2 |
| `setWidthMode(int): Widget` | LAMBDA | 2 |
| `setXPositionMode(int): Widget` | LAMBDA | 4 |
| `setXTextAlignment(int): Widget` | LAMBDA | 2 |
| `setYPositionMode(int): Widget` | LAMBDA | 3 |
| `setYTextAlignment(int): Widget` | LAMBDA | 2 |

### `net.runelite.api.widgets.WidgetUtil`

| Method | Evidence | Caller count |
|---|---|---:|
| `componentToId(int): int` | LAMBDA, SUBSCRIBE | 4 |
| `componentToInterface(int): int` | LAMBDA, SUBSCRIBE | 20 |

### `net.runelite.api.worldmap.MapElementConfig`

| Method | Evidence | Caller count |
|---|---|---:|
| `getCategory(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.worldmap.WorldMap`

| Method | Evidence | Caller count |
|---|---|---:|
| `getWorldMapRenderer(): WorldMapRenderer` | SUBSCRIBE | 1 |

### `net.runelite.api.worldmap.WorldMapIcon`

| Method | Evidence | Caller count |
|---|---|---:|
| `getCoordinate(): WorldPoint` | SUBSCRIBE | 1 |
| `getType(): int` | SUBSCRIBE | 1 |

### `net.runelite.api.worldmap.WorldMapRegion`

| Method | Evidence | Caller count |
|---|---|---:|
| `getMapIcons(): Collection` | SUBSCRIBE | 1 |

### `net.runelite.api.worldmap.WorldMapRenderer`

| Method | Evidence | Caller count |
|---|---|---:|
| `getMapRegions(): WorldMapRegion[][]` | SUBSCRIBE | 1 |
| `isLoaded(): boolean` | SUBSCRIBE | 1 |

