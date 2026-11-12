package ru.mr_hares.realmMode.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.objects.Mode;

public class ExpansionUtils extends PlaceholderExpansion {
    @Override
    @NotNull
    public String getAuthor() {
        return "w1nsy"; //
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "realmmode"; //
    }

    @Override
    @NotNull
    public String getVersion() {
        return "0.1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equals("mode")) {
            Mode mode = DataBase.getMode(player.getUniqueId());
            if (mode == null) return null;

            return mode.getValue().toUpperCase();
        }

        return null;
    }
}
