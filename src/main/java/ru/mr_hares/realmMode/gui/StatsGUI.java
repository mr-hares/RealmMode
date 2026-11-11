package ru.mr_hares.realmMode.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.objects.Mode;
import ru.mr_hares.realmMode.utils.ColorUtil;
import ru.mr_hares.realmMode.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.mr_hares.realmMode.RealmMode.*;

public class StatsGUI implements InventoryHolder {

    private static final int CONTENT_SIZE = 21;
    private static final int[] CONTENT_SLOTS = buildContentSlots();
    private static final int PREV_SLOT = 39;
    private static final int INFO_SLOT = 40;
    private static final int NEXT_SLOT = 41;

    private static final NamespacedKey PLAYER_KEY =
            new NamespacedKey(getInstance(), "player_info");
    private static final NamespacedKey NAV_KEY =
            new NamespacedKey(getInstance(), "stats_nav");

    private final int page;
    private Inventory inventory;

    public StatsGUI(int page) {
        this.page = Math.max(0, page);
    }

    public StatsGUI() {
        this(0);
    }

    public int getPage() { return page; }

    private static int[] buildContentSlots() {
        int[] slots = new int[CONTENT_SIZE];
        int idx = 0;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 7; col++)
                slots[idx++] = 10 + col + row * 9;
        return slots;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory != null) return inventory;

        String title = MessageUtils.getRaw("gui.stats.title")
                .replace("{page}", String.valueOf(page + 1));
        inventory = Bukkit.createInventory(this, 45, ColorUtil.colorize(title));

        Map<UUID, Mode> list = DataBase.getList();
        List<UUID> uuids = new ArrayList<>(list.keySet());

        int from = page * CONTENT_SIZE;
        int to = Math.min(from + CONTENT_SIZE, uuids.size());
        for (int i = from; i < to; i++) {
            UUID uuid = uuids.get(i);
            inventory.setItem(CONTENT_SLOTS[i - from], getPlayerInfo(uuid, list.get(uuid)));
        }

        int totalPages = Math.max(1, (uuids.size() + CONTENT_SIZE - 1) / CONTENT_SIZE);
        if (page > 0 && page != totalPages)
            inventory.setItem(PREV_SLOT, getNav("prev", Material.ARROW,
                    MessageUtils.getRaw("gui.stats.prev")));
        if (page < totalPages - 1)
            inventory.setItem(NEXT_SLOT, getNav("next", Material.ARROW,
                    MessageUtils.getRaw("gui.stats.next")));

        inventory.setItem(INFO_SLOT, getGeneralInfo(list));

        fillBorder(inventory);

        return inventory;
    }


    private ItemStack getNav(String action, Material mat, String rawName) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(rawName));
        meta.getPersistentDataContainer().set(NAV_KEY, PersistentDataType.STRING, action);
        hideFlags(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getGeneralInfo(Map<UUID, Mode> list) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ColorUtil.colorize(
                MessageUtils.getRaw("gui.stats.general-info.name")));

        int total = list.size();

        Map<Mode, Long> counts = list.values().stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        long pve = counts.getOrDefault(Mode.PVE, 0L);
        long pvp = counts.getOrDefault(Mode.PVP, 0L);

        List<String> lore = MessageUtils.getList("gui.stats.general-info.lore").stream()
                .map(line -> line
                        .replace("{total}", String.valueOf(total))
                        .replace("{pve}", String.valueOf(pve))
                        .replace("{pvp}", String.valueOf(pvp)))
                .toList();
        meta.setLore(ColorUtil.colorize(lore));

        meta.getPersistentDataContainer().set(NAV_KEY, PersistentDataType.STRING, "info");

        hideFlags(meta);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getPlayerInfo(UUID uuid, Mode mode) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = op.hasPlayedBefore()
                ? op.getName()
                : MessageUtils.getRaw("gui.stats.player-info.unknown-name");

        meta.setDisplayName(ColorUtil.colorize(
                MessageUtils.getRaw("gui.stats.player-info.name").replace("{name}", name)));

        List<String> lore = MessageUtils.getList("gui.stats.player-info.lore").stream()
                .map(line -> line.replace("{mode}", mode.getValue().toUpperCase()))
                .toList();
        meta.setLore(ColorUtil.colorize(lore));

        meta.getPersistentDataContainer().set(PLAYER_KEY, PersistentDataType.STRING, uuid.toString());

        hideFlags(meta);
        item.setItemMeta(meta);
        return item;
    }

    public static void fillBorder(Inventory inv) {
        ItemStack filler = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        hideFlags(meta);
        filler.setItemMeta(meta);

        int size = inv.getSize();
        int rows = size / 9;

        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int col = slot % 9;

            boolean isBorder = row == 0 || row == rows - 1 || col == 0 || col == 8;

            if (!isBorder) continue;

            ItemStack cur = inv.getItem(slot);
            if (cur == null || cur.getType() == Material.AIR)
                inv.setItem(slot, filler.clone());
        }
    }

    private static void hideFlags(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DYE, ItemFlag.HIDE_UNBREAKABLE);
    }

    public static boolean isPrev(ItemStack item) {
        return navAction(item) != null && navAction(item).equals("prev");
    }

    public static boolean isNext(ItemStack item) {
        return navAction(item) != null && navAction(item).equals("next");
    }

    public static boolean isInfo(ItemStack item) {
        return "info".equals(navAction(item));
    }

    public static UUID getPlayer(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer()
                .get(PLAYER_KEY, PersistentDataType.STRING);
        return s == null ? null : UUID.fromString(s);
    }

    private static String navAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(NAV_KEY, PersistentDataType.STRING);
    }
}