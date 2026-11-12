package ru.mr_hares.realmMode.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.gui.ModeGUI;

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
    }
}
