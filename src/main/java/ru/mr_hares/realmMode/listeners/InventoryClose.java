package ru.mr_hares.realmMode.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.gui.ConfirmGUI;
import ru.mr_hares.realmMode.gui.ModeGUI;
import ru.mr_hares.realmMode.utils.GuiSwitchFlag;

import static ru.mr_hares.realmMode.RealmMode.getInstance;

public class InventoryClose implements Listener {

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (GuiSwitchFlag.isMarked(event.getPlayer().getUniqueId())) {
            GuiSwitchFlag.clear(event.getPlayer().getUniqueId());
            return;
        }

        if (event.getInventory().getHolder() instanceof ModeGUI) {
            if (DataBase.getMode(event.getPlayer().getUniqueId()) == null) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(getInstance(), () -> {
                    event.getPlayer().openInventory(new ModeGUI().getInventory());
                }, 10L);
            }
        } else if (event.getInventory().getHolder() instanceof ConfirmGUI) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(getInstance(), () -> {
                event.getPlayer().openInventory(new ModeGUI().getInventory());
            }, 10L);
        }
    }
}
