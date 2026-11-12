package ru.mr_hares.realmMode.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import ru.mr_hares.realmMode.objects.Mode;
import ru.mr_hares.realmMode.utils.ColorUtil;
import ru.mr_hares.realmMode.utils.MessageUtils;

import java.util.List;

import static ru.mr_hares.realmMode.RealmMode.getInstance;

public class ConfirmGUI implements InventoryHolder {
    private final Inventory inventory;
    private static final NamespacedKey key = new NamespacedKey(getInstance(), "mode");

    public ConfirmGUI(Mode mode) {
        String title = MessageUtils.get("gui.confirm-change.title");
        this.inventory = Bukkit.createInventory(this, 45, ColorUtil.colorize(title));

        inventory.setItem(22, get(mode));

        for (int i=0; i < 5; i++) {
            for (int y=0; y < 4; y++) {
                int slot = y + i*9;
                inventory.setItem(slot, getGlass(true, mode));
            }
        }

        for (int i=0; i < 5; i++) {
            for (int y=5; y < 9; y++) {
                int slot = y + i*9;
                inventory.setItem(slot, getGlass(false, mode));
            }
        }
    }

    private ItemStack getGlass(boolean accept, Mode mode) {
        Material material = accept ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String ket = accept ? "accept" : "decline";
        String DisplayName = MessageUtils.getRaw("gui.confirm-change." + ket);
        meta.setDisplayName(ColorUtil.colorize(DisplayName));
        meta.setLore(null);

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_UNBREAKABLE);

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, accept ? mode.getValue() : "back");

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack get(Mode mode) {
        Material fallback = mode.getValue().equals("pve") ? Material.SHIELD : Material.IRON_SWORD;
        Material material;
        try {
            material = Material.valueOf(getInstance().getConfig().getString(
                    "gui.mode-change." + mode.getValue() + "-item.material", fallback.name()));
        } catch (IllegalArgumentException e) {
            material = fallback;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String DisplayName = MessageUtils.getRaw("gui.mode-change." + mode.getValue() + "-item.name");
        meta.setDisplayName(ColorUtil.colorize(DisplayName));

        List<String> lore = MessageUtils.getList("gui.mode-change." + mode.getValue() + "-item.lore");
        meta.setLore(ColorUtil.colorize(lore));

        Boolean glowing = getInstance().getConfig().getBoolean("gui.mode-change." + mode.getValue() + "-item.glowing"
                , true);
        if (glowing) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);
        return item;
    }

    public static String get(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static boolean is(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(key);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
