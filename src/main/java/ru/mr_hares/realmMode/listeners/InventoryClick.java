package ru.mr_hares.realmMode.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.gui.ConfirmGUI;
import ru.mr_hares.realmMode.gui.ModeGUI;
import ru.mr_hares.realmMode.gui.StatsGUI;
import ru.mr_hares.realmMode.objects.Mode;
import ru.mr_hares.realmMode.utils.ColorUtil;
import ru.mr_hares.realmMode.utils.GuiSwitchFlag;
import ru.mr_hares.realmMode.utils.MessageUtils;

import java.util.UUID;

import static ru.mr_hares.realmMode.RealmMode.*;

public class InventoryClick implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory().getHolder() instanceof ModeGUI) {
            event.setCancelled(true);

            if (!ModeGUI.is(event.getCurrentItem())) return;
            Mode mode = ModeGUI.get(event.getCurrentItem());

            GuiSwitchFlag.mark(player.getUniqueId());
            event.getInventory().close();
            player.openInventory(new ConfirmGUI(mode).getInventory());
        }

        if (event.getInventory().getHolder() instanceof ConfirmGUI) {
            event.setCancelled(true);

            if (!ConfirmGUI.is(event.getCurrentItem())) return;
            String type = ConfirmGUI.get(event.getCurrentItem());

            if (type.equals("back")) {
                event.getInventory().close();
            } else {
                GuiSwitchFlag.mark(player.getUniqueId());
                String key = DataBase.getMode(player.getUniqueId()) == null ? "first" : "again";
                DataBase.setMode(player.getUniqueId(), Mode.fromValue(type));

                player.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("set-mode")
                        .replace("{mode}", type.toUpperCase())
                ));

                if (getInstance().getConfig().getBoolean("titles", true)) {
                    player.sendTitle(
                            ColorUtil.colorize(MessageUtils.getRaw("titles." + type + ".title")),
                            ColorUtil.colorize(MessageUtils.getRaw("titles." + type + ".subtitle"))
                    );
                }

                if (getInstance().getConfig().getBoolean("broadcast", true)) {
                    String message = MessageUtils.getRaw("broadcast." + key)
                            .replace("{player}", player.getName())
                            .replace("{mode}", type.toUpperCase());

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p != player) p.sendMessage(ColorUtil.colorize(message));
                    }
                }

                event.getInventory().close();
            }
        }

        if (event.getInventory().getHolder() instanceof StatsGUI gui) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (StatsGUI.isPrev(clicked)) {
                GuiSwitchFlag.mark(player.getUniqueId());
                player.openInventory(new StatsGUI(gui.getPage() - 1).getInventory());
                return;
            }
            if (StatsGUI.isNext(clicked)) {
                GuiSwitchFlag.mark(player.getUniqueId());
                player.openInventory(new StatsGUI(gui.getPage() + 1).getInventory());
                return;
            }
            if (StatsGUI.isInfo(clicked)) return;

            UUID target = StatsGUI.getPlayer(clicked);
            if (target == null) return;

            Mode newMode = event.getClick() == ClickType.LEFT ? Mode.PVE : Mode.PVP;
            applyMode(player, target, newMode);
            GuiSwitchFlag.mark(player.getUniqueId());
            player.openInventory(new StatsGUI(gui.getPage()).getInventory());
        }
    }

    private void applyMode(Player initiator, UUID targetUuid, Mode mode) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
        DataBase.setMode(op.getUniqueId(), mode);

        initiator.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("change-mode.initiator")
                .replace("{player}", displayName(op))
                .replace("{mode}", mode.getValue().toUpperCase())
        ));

        if (op.isOnline()) {
            Player online = op.getPlayer();
            if (online != null) {
                online.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("change-mode.target")
                        .replace("{player}", initiator.getName())
                        .replace("{mode}", mode.getValue().toUpperCase())
                ));
            }
        }
    }

    private String displayName(OfflinePlayer op) {
        return op.getName() != null ? op.getName()
                : MessageUtils.getRaw("gui.stats.player-info.unknown-name");
    }
}
