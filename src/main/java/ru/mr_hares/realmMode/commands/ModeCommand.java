package ru.mr_hares.realmMode.commands;

import com.github.benmanes.caffeine.cache.Policy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mr_hares.realmMode.RealmMode;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.gui.ModeGUI;
import ru.mr_hares.realmMode.utils.ColorUtil;
import ru.mr_hares.realmMode.utils.CommandTemplate;
import ru.mr_hares.realmMode.utils.MessageUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ModeCommand extends CommandTemplate {
    private RealmMode plugin;

    public ModeCommand(JavaPlugin plugin) {
        super("mode", plugin);
        this.plugin = (RealmMode) plugin;
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

        long RemainingSeconds = getRemainingSeconds(player.getUniqueId());
        if (RemainingSeconds > 0 && !player.hasPermission("realm-mode.bypass.combat")) {
            player.sendMessage(ColorUtil.colorize(MessageUtils.getRaw("in-combat")
                    .replace("{time}", String.valueOf(RemainingSeconds))
            ));
            return;
        }

        player.openInventory(new ModeGUI().getInventory());
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }

    private long getRemainingSeconds(UUID uuid) {
        if (plugin.in_combat.getIfPresent(uuid) == null) {
            return 0;
        }

        Optional<Policy.FixedExpiration<UUID, Boolean>> policeOpt = plugin.in_combat.policy().expireAfterWrite();
        if (policeOpt.isEmpty()) return 0;

        Policy.FixedExpiration<UUID, Boolean> policy = policeOpt.get();
        Optional<Duration> ageOpt = policy.ageOf(uuid);
        if (ageOpt.isEmpty()) return 0;

        long ageSec = ageOpt.get().toSeconds();
        long ttlSec = policy.getExpiresAfter().toSeconds();

        return Math.max(0, ttlSec - ageSec);
    }
}
