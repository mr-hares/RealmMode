package ru.mr_hares.realmMode.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.gui.StatsGUI;
import ru.mr_hares.realmMode.objects.Mode;
import ru.mr_hares.realmMode.utils.ColorUtil;
import ru.mr_hares.realmMode.utils.CommandTemplate;
import ru.mr_hares.realmMode.utils.MessageUtils;

import java.util.List;

public class RmCommand extends CommandTemplate {
    private JavaPlugin plugin;

    public RmCommand(JavaPlugin plugin) {
        super("realmmode", plugin);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("realm-mode.admin.reload") && !sender.hasPermission("realm-mode.admin.change") && !sender.hasPermission("realm-mode.admin.stats")) {
            sender.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("not-permission")));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(String.join("\n", ColorUtil.colorize(MessageUtils.getList("help.command-list"))));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("realm-mode.admin.reload")) {
                sender.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("not-permission")));
                return;
            }

            plugin.reloadConfig();
            MessageUtils.reload();

            sender.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("reload")
                    .replace("{lang}", MessageUtils.getLanguage())
            ));
        } else if (args[0].equalsIgnoreCase("change")) {
            if (!sender.hasPermission("realm-mode.admin.change")) {
                sender.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("not-permission")));
                return;
            }

            if (args.length < 3 || (!args[2].equalsIgnoreCase("PVP") && ! args[2].equalsIgnoreCase("PVE"))) {
                sender.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("help.use-change")));
                return;
            }

            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            if (!op.hasPlayedBefore()) {
                sender.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("not-found-player")));
                return;
            }

            Mode mode = Mode.fromValue(args[2].toLowerCase());
            DataBase.updateMode(op.getUniqueId(), mode);

            sender.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("change-mode.initiator")
                    .replace("{player}", op.getName())
                    .replace("{mode}", mode.getValue().toUpperCase())
            ));

            if (op.isOnline()) {
                op.getPlayer().sendMessage(ColorUtil.colorize(MessageUtils.getRaw("change-mode.target")
                        .replace("{player}", sender.getName())
                        .replace("{mode}", mode.getValue().toUpperCase())
                ));
            }
        } else if (args[0].equalsIgnoreCase("stats")) {
            if (!sender.hasPermission("realm-mode.admin.stats")) {
                sender.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("not-permission")));
                return;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("only-player")));
                return;
            }

            player.openInventory(new StatsGUI().getInventory());
        } else {
            sender.sendMessage(String.join("\n", ColorUtil.colorize(MessageUtils.getList("help.command-list"))));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "change", "stats");
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("change")) {
                return null;
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("change")) {
                return List.of("PVE", "PVP");
            }
        }

        return List.of();
    }
}
