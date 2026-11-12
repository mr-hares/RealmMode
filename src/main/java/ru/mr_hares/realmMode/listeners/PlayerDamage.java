package ru.mr_hares.realmMode.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.objects.Mode;
import ru.mr_hares.realmMode.utils.ColorUtil;
import ru.mr_hares.realmMode.utils.MessageUtils;

public class PlayerDamage implements Listener {

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player damageTaker && event.getDamager() instanceof Player damagerPlayer) {
            Mode modeTaker = DataBase.getMode(damageTaker.getUniqueId());
            Mode modePlayer =  DataBase.getMode(damagerPlayer.getUniqueId());

            if (modePlayer == null || modeTaker == null) {
                event.setCancelled(true);
                return;
            }

            if (modePlayer == Mode.PVP && modeTaker == Mode.PVE) {
                damagerPlayer.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("not-damage.pvp")));
                event.setCancelled(true);
            } else if (modePlayer == Mode.PVE) {
                damagerPlayer.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("not-damage.pve")));
                event.setCancelled(true);
            }
        }
    }
}
