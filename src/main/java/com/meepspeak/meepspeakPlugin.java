package com.meepspeak;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@PluginDescriptor(name = "meepspeak", description = "Changes NPC/item/widget names to meepspeak", tags = {
        "meep"}, enabledByDefault = true, hidden = false)
public class meepspeakPlugin extends Plugin {

    private static final Set<MenuAction> NPC_MENU_ACTIONS = ImmutableSet.of(
            MenuAction.NPC_FIRST_OPTION, MenuAction.NPC_SECOND_OPTION,
            MenuAction.NPC_THIRD_OPTION, MenuAction.NPC_FOURTH_OPTION,
            MenuAction.NPC_FIFTH_OPTION, MenuAction.WIDGET_TARGET_ON_NPC,
            MenuAction.EXAMINE_NPC, MenuAction.EXAMINE_OBJECT);

    private static final Set<MenuAction> ITEM_MENU_ACTIONS = ImmutableSet.of(
            MenuAction.GROUND_ITEM_FIRST_OPTION, MenuAction.GROUND_ITEM_SECOND_OPTION,
            MenuAction.GROUND_ITEM_THIRD_OPTION, MenuAction.GROUND_ITEM_FOURTH_OPTION,
            MenuAction.GROUND_ITEM_FIFTH_OPTION, MenuAction.EXAMINE_ITEM_GROUND,
            MenuAction.CC_OP, MenuAction.CC_OP_LOW_PRIORITY, MenuAction.WIDGET_TARGET,
            MenuAction.WIDGET_TARGET_ON_PLAYER, MenuAction.WIDGET_TARGET_ON_NPC,
            MenuAction.WIDGET_TARGET_ON_GAME_OBJECT, MenuAction.WIDGET_TARGET_ON_GROUND_ITEM,
            MenuAction.WIDGET_TARGET_ON_WIDGET);

    private static final ImmutableMap<String, String> ItemNameRemap = ImmutableMap.<String, String>builder()
            .put("Dragon warhammer", "bonky hammer")
            .put("Dinh's bulwark", "stompy eye ball shield")
            .put("Zaryte crossbow", "kitty ear crossbow")
            .build();

    private static final ImmutableMap<String, String> NPCNameRemap = ImmutableMap.<String, String>builder()
            .put("Tekton", "stompy rock man")
            .put("Tekton (enraged)", "stompy rock man (enraged)")
            .put("Vasa Nistirio", "mean stompy skele stone spider")
            .put("Vespula", "ouchies sting bug")
            .put("Abyssal Portal", "ouchies sting bug portal")
            .put("Skeletal Mystic", "ouchie skele mage")
            .put("Muttadile", "puppy dile")
            .put("Baby Muttadile", "puppy dile")
            .put("Big Muttadile", "puppy dile")
            .put("Vanguard", "crawlie ouchie trio")
            .put("Guardian", "rock throwing ouchie stone man")
            .put("TzKal-Zuk", "poops ouch balls")
            .put("Tzkal-Zuk", "poops ouch balls")
            .put("The Maiden of Sugadinti", "creepy ouchie blood lady")
            .put("Pestilent Bloat", "stinky stompy fat man")
            .put("The Nylocas", "confusing switchy ouchie spooders")
            .put("Nylocas Vasilias", "confusing switchy ouchie spooders")
            .put("Xarpus", "flappy poison throw up bat")
            .put("Sotetseg", "stompy rhino puppy wiff a death rune")
            .put("Verzik Vitur", "yuck youchie fat spider lady")
            .put("Verzik Vitur's Vault", "with a purple, imagine? po chew")
            .put("The Final Challenge", "yuck youchie fat spider lady")
            .put("Ice demon", "snowball throwing ouchie ice man")
            .put("Corrupted Hunllef", "baby stompy moose puppy")
            .put("Crystalline Hunllef", "baby stompy moose puppy")
            .put("The Corrupted Gauntlet", "The baby stompy moose puppy (Corrupted)")
            .put("The Gauntlet", "The baby stompy moose puppy")
            .put("Great Olm", "lizard goat")
            .put("Great Olm - Head", "lizard goat - Head")
            .put("Great Olm - Left Hand", "lizard goat - Left Hand")
            .put("Great Olm - Right Hand", "lizard goat - Right Hand")
            .put("Great Olm (Right claw)", "lizard goat (Right claw)")
            .put("Great Olm (Left claw)", "lizard goat (Left claw)")
            .put("Corrupted scavenger", "sleepy alien")
            .put("Zulrah", "punchy fire snake")
            .put("Vorkath", "blue dragon puppy wiff wiffle horns")
            .put("Kree'arra", "flappy ouchi birb man")
            .put("K'ril Tsutsaroth", "stompy spipey red demon wiff lil goat feet")
            .put("Commander Zilyana", "uglee godsword lady wiff wings")
            .put("General Graardor", "ouchi unicorn ogre man")
            .put("Nex", "yuck speedy winged demon lady thing wiff goat feet")
            .put("Black dragon", "down ear dragon puppy wiff spipey tail")
            .put("Blue dragon", "unicorn dragon wiff a ball on his tail")
            .put("Cerberus", "three headed awoo lava puppy")
            .put("Corporeal Beast", "giant mummy puppy")
            .put("The Nightmare", "yucky crouchie nasty sleep lady")
            .put("Phosani's Nightmare", "yucky crouchie nasty sleep lady")
            .put("Chaos Elemental", "cotton candy monster")
            .put("Giant Mole", "diggy scratchy feet baby wiff whiskers")
            .put("Kalphite Queen", "spipey crawly flying bug wiff pinchers")
            .put("Penance Queen", "one eyed creepy alien wiff a tummy bump")
            .put("Demonic gorilla", "angry monkee")
            .put("Brawler", "elephant man")
            .put("Torcher", "worm bat")
            .put("Ravager", "mole scratch man")
            .put("Defiler", "kitty meow")
            .put("Splatter", "bouncy ball")
            .put("Jelly", "squishy marshmallow")
            .put("King Black Dragon", "three headed uppy eardragon puppy")
            .put("Scorpia", "pokey poison ouch bug")
            .put("Abyssal Sire", "creepy whippy spider demon wiff tails on his back")
            .put("Kraken", "five legged yucky mouf octopus man")
            .put("Thermonuclear Smoke Devil", "baby squid")
            .put("Alchemical Hydra", "so muches heads ouchie slayer dragon (BUT EW ITS TOES)")
            .put("Tempoross", "fishy whirlpool man")
            .put("Zalcano", "sad angry stone lady wiff horse feet")
            .put("Mimic", "treasure chest octopus man wiff his tongue out")
            .put("Callisto and Artio", "stompy ouchie wildy bear")
            .put("Vet'ion and Calvar'ion", "purple tanky wildy boi who changes colors")
            .put("Venenatis and Spindel", "giant wildy spider wiff pointy teefs")
            .put("Sarachnis", "big red spider wiff a weird head")
            .put("Jal-MejRah", "big mouf stone bat")
            .put("Jal-Ak", "blobby man")
            .put("Jal-ImKot", "bat winged punchy worm")
            .put("Jal-Xil", "ranger wiff boxing glubs")
            .put("Jal-Zek", "flippy mager wiff lizard feets")
            .put("JalTok-Jad", "froggy stone jad")
            .put("Yt-Hurkot", "punchy healer mans")
            .put("Jal-MejJak", "lil tree branch mans")
            .put("Tz-Kih", "fiery bat man")
            .put("Tz-Kek", "stripey blob man")
            .put("Tok-Xil", "ranger wiff a yelling face")
            .put("Yt-MejKot", "super slow punchy man")
            .put("Ket-Zek", "spipey baby lizard mager")
            .put("TzTok-Jad", "stompy fire spipey man")
            .put("Inferno", "Inferno (poops ouch balls)")
            .put("The Inferno", "Inferno (poops ouch balls)")
            .put("The Fight Caves", "The Fight Caves (stompy fire spipey man)")
            .put("TzHaar Fight Cave", "TzHaar Fight Cave (stompy fire spipey man)")
            .put("Ba-Ba", "rock throwing skele monkee")
            .put("Kephri", "evil poopy feather bug")
            .put("Zebak", "angry poison croc wiff squishy feet")
            .put("Akkha", "switchy spear man")
            .build();

    private static final ImmutableMap<String, Set<Integer>> NPCFilterList = ImmutableMap.<String, Set<Integer>>builder()
            .put("Black dragon", ImmutableSet.of(NpcID.BLACK_DRAGON_254))
            .put("Blue dragon", ImmutableSet.of(NpcID.BLUE_DRAGON_268))
            .build();

    private static final int COLLECTION_LOG_GROUP = 621;
    private static final int COMBAT_ACHIEVEMENTS_GROUP = 716;
    private static final int TOB_PARTY_GROUP = 28;
    private static final int HEALTH_OVERLAY_GROUP = 303;

    private final HashMap<String, String> combinedNPCMap = new HashMap<>();
    private final HashMap<String, String> combinedItemMap = new HashMap<>();
    private final HashMap<String, String> CustomNPCRemap = new HashMap<>();
    private final HashMap<String, String> CustomItemRemap = new HashMap<>();

    @Inject
    private Client client;

    @Inject
    private meepspeakConfig meepspeakConfig;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ClientThread clientThread;

    @Provides
    meepspeakConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(meepspeakConfig.class);
    }

    @Override
    protected void startUp() {
        parseConfig();
        rebuildCombinedMaps();
    }

    @Override
    protected void shutDown() {
    }

    @Subscribe
    protected void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(meepspeakConfig.GROUP))
            return;

        parseConfig();
        rebuildCombinedMaps();
    }

    private void rebuildCombinedMaps() {
        combinedNPCMap.clear();
        combinedItemMap.clear();

        if (meepspeakConfig.npcNameToggle()) {
            combinedNPCMap.putAll(NPCNameRemap);
        }
        combinedNPCMap.putAll(CustomNPCRemap);

        if (meepspeakConfig.itemNameToggle()) {
            combinedItemMap.putAll(ItemNameRemap);
        }
        combinedItemMap.putAll(CustomItemRemap);
    }

    private void parseConfig() {
        CustomItemRemap.clear();
        CustomNPCRemap.clear();

        try {
            String customNPCs = meepspeakConfig.customNPCList();
            if (!customNPCs.isEmpty()) {
                String[] pairs = customNPCs.split("\n");
                for (String pair : pairs) {
                    String[] kv = pair.split(",");
                    if (kv.length == 2) {
                        CustomNPCRemap.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            String customItems = meepspeakConfig.customItemList();
            if (!customItems.isEmpty()) {
                String[] pairs = customItems.split("\n");
                for (String pair : pairs) {
                    String[] kv = pair.split(",");
                    if (kv.length == 2) {
                        CustomItemRemap.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Subscribe
    private void onWidgetLoaded(WidgetLoaded event) {
        int groupId = event.getGroupId();
        if (groupId == COLLECTION_LOG_GROUP ||
                groupId == COMBAT_ACHIEVEMENTS_GROUP ||
                groupId == TOB_PARTY_GROUP ||
                groupId == HEALTH_OVERLAY_GROUP) {
            clientThread.invokeLater(() -> processWidgetGroup(groupId));
        }
    }

    @Subscribe
    private void onScriptPostFired(ScriptPostFired event) {
        int scriptId = event.getScriptId();

        if (scriptId == ScriptID.COLLECTION_DRAW_LIST ||
                scriptId == 2730 ||
                scriptId == 2731 ||
                scriptId == 2732 ||
                scriptId == 2733) {
            processWidgetGroup(COLLECTION_LOG_GROUP);
            return;
        }

        if (scriptId == 4399 || scriptId == 4400 || scriptId == 4401) {
            processWidgetGroup(COMBAT_ACHIEVEMENTS_GROUP);
            return;
        }

        if (scriptId == ScriptID.HP_HUD_UPDATE) {
            processWidgetGroup(HEALTH_OVERLAY_GROUP);
        }
    }

    private void processWidgetGroup(int groupId) {
        for (int childId = 0; childId < 300; childId++) {
            int componentId = WidgetUtil.packComponentId(groupId, childId);
            Widget widget = client.getWidget(componentId);
            if (widget == null) {
                continue;
            }
            processWidget(widget);
        }
    }

    private void processWidget(Widget widget) {
        if (widget == null) {
            return;
        }

        String text = widget.getText();
        if (text != null && !text.isEmpty()) {
            String replacement = getReplacementText(text);
            if (replacement != null) {
                widget.setText(replacement);
            }
        }

        Widget[] children = widget.getDynamicChildren();
        if (children != null) {
            for (Widget child : children) {
                processWidget(child);
            }
        }

        children = widget.getStaticChildren();
        if (children != null) {
            for (Widget child : children) {
                processWidget(child);
            }
        }

        children = widget.getNestedChildren();
        if (children != null) {
            for (Widget child : children) {
                processWidget(child);
            }
        }
    }

    private String getReplacementText(String text) {
        String replacement = combinedNPCMap.get(text);
        if (replacement != null) {
            return replacement;
        }

        replacement = combinedItemMap.get(text);
        if (replacement != null) {
            return replacement;
        }

        for (Map.Entry<String, String> entry : combinedNPCMap.entrySet()) {
            if (text.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }

        for (Map.Entry<String, String> entry : combinedItemMap.entrySet()) {
            if (text.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    @Subscribe
    protected void onMenuEntryAdded(MenuEntryAdded event) {
        MenuEntry entry = event.getMenuEntry();
        if (NPC_MENU_ACTIONS.contains(entry.getType())) {
            if (shouldRemapName(entry)) {
                remapMenuEntryText(entry, combinedNPCMap);
            }
        } else if (ITEM_MENU_ACTIONS.contains(entry.getType())) {
            remapMenuEntryText(entry, combinedItemMap);
        }
    }

    private void remapMenuEntryText(MenuEntry menuEntry, Map<String, String> map) {
        String target = menuEntry.getTarget();
        NPC npc = menuEntry.getNpc();
        String cleanTarget;
        if (npc != null) {
            cleanTarget = Text.removeTags(npc.getName());
        } else {
            cleanTarget = Text.removeTags(target);
        }

        if (cleanTarget == null) {
            return;
        }

        String replacement = map.get(cleanTarget);
        if (replacement != null) {
            menuEntry.setTarget(target.replace(cleanTarget, replacement));
            return;
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (cleanTarget.equalsIgnoreCase(entry.getKey())) {
                menuEntry.setTarget(target.replace(entry.getKey(), entry.getValue()));
                return;
            }
        }
    }

    private boolean shouldRemapName(MenuEntry entry) {
        NPC npc = entry.getNpc();
        if (npc == null) {
            return true;
        }

        Set<Integer> ids = NPCFilterList.get(npc.getName());
        if (ids == null) {
            return true;
        }

        return ids.contains(npc.getId());
    }
}
