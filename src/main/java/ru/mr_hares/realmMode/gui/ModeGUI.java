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

import static ru.mr_hares.realmMode.RealmMode.*;

public class ModeGUI implements InventoryHolder {
    private final Inventory inventory;
    private static final NamespacedKey key = new NamespacedKey(getInstance(), "mode");
    
    public ModeGUI() {
        String title = MessageUtils.get("gui.mode-change.title");
        this.inventory = Bukkit.createInventory(this, 45, ColorUtil.colorize(title));

        inventory.setItem(20, get(Mode.PVE));
        inventory.setItem(24, get(Mode.PVP));

        fillEmptySlots(inventory);
    }

    public static void fillEmptySlots(Inventory inv) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(null);
        meta.setLore(null);

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, item.clone());
            }
        }
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

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, mode.getValue());

        item.setItemMeta(meta);
        return item;
    }

    public static Mode get(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return null;

        return Mode.fromValue(meta.getPersistentDataContainer().get(key, PersistentDataType.STRING));
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
