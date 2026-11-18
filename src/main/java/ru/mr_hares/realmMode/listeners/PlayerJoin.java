package ru.mr_hares.realmMode.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.gui.ModeGUI;
import ru.mr_hares.realmMode.utils.ColorUtil;
import ru.mr_hares.realmMode.utils.MessageUtils;
import ru.mr_hares.realmMode.utils.UpdateChecker;

import static ru.mr_hares.realmMode.RealmMode.*;

public class PlayerJoin implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (DataBase.getMode(player.getUniqueId()) == null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(getInstance(), () -> {
                player.openInventory(new ModeGUI().getInventory());
            }, 20L);
        }

        if ((player.isOp() || player.hasPermission("realm-mode.admin.reload")) && getInstance().getConfig().getBoolean("update", true)) {
            new UpdateChecker(getInstance(), "realm-mode").check(result -> {
                if (result.hasFailed()) return;
                if (!result.isUpdateAvailable()) return;

                player.sendMessage(ColorUtil.parse(String.join("\n", MessageUtils.getList("update"))
                        .replace("{current}", result.getCurrentVersion())
                        .replace("{latest}", result.getLatestVersion())
                        .replace("{url}", result.getPageUrl())
                ));
            });
        }
    }
}
