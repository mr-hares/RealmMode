package ru.mr_hares.realmMode.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.gui.ModeGUI;
import ru.mr_hares.realmMode.utils.ColorUtil;
import ru.mr_hares.realmMode.utils.CommandTemplate;
import ru.mr_hares.realmMode.utils.MessageUtils;

import java.util.List;

public class ModeCommand extends CommandTemplate {
    public ModeCommand(JavaPlugin plugin) {
        super("mode", plugin);
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("only-player")));
            return;
        }

        if (!DataBase.upTime(player.getUniqueId()) && !player.hasPermission("realm-mode.bypass.cooldown")) {
            player.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("not-up-cooldown")
                    .replace("{time}", String.valueOf(DataBase.getTime(player.getUniqueId())))
            ));
            return;
        }

        player.openInventory(new ModeGUI().getInventory());
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }
}
