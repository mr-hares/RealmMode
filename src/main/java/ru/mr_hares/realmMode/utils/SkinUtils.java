package ru.mr_hares.realmMode.utils;

import java.net.URL;
import java.util.UUID;

import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

public final class SkinUtils {
    private static Plugin plugin;
    private static SkinsRestorer skinsRestorer;

    private SkinUtils() {
    }

    public static void init(Plugin plugin, SkinsRestorer skinsRestorer) {
        SkinUtils.plugin = plugin;
        SkinUtils.skinsRestorer = skinsRestorer;
    }

    public static SkinProperty getPlayerSkin(UUID uuid, String name) {
        if (skinsRestorer == null) return null;

        try {
            return skinsRestorer.getPlayerStorage().getSkinForPlayer(uuid, name).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getTextureURL(SkinProperty property) {
        return PropertyUtils.getSkinTextureUrl(property);
    }

    public static ItemStack getHead(OfflinePlayer player) {
        return getSkull(player, player.getName() != null
                ? getPlayerSkin(player.getUniqueId(), player.getName())
                : null);
    }

    public static ItemStack getSkull(OfflinePlayer player, SkinProperty property) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);

        String texture = null;
        try {
            texture = property != null ? getTextureURL(property) : null;
        } catch (Exception ignored) {}

        if (texture == null || texture.isEmpty()) {
            head.editMeta(SkullMeta.class, meta -> meta.setOwnerProfile(player.getPlayerProfile()));
            return head;
        }

        final String skin = texture;
        head.editMeta(SkullMeta.class, meta -> {
            PlayerProfile profile = player.getName() != null
                    ? Bukkit.createPlayerProfile(player.getUniqueId(), player.getName())
                    : Bukkit.createPlayerProfile(player.getUniqueId());
            PlayerTextures textures = profile.getTextures();
            try {
                textures.setSkin(new URL(skin));
            } catch (Exception ignored) {}
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        });
        return head;
    }
}