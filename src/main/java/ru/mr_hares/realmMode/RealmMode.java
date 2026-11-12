package ru.mr_hares.realmMode;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mr_hares.realmMode.commands.ModeCommand;
import ru.mr_hares.realmMode.commands.RmCommand;
import ru.mr_hares.realmMode.database.DataBase;
import ru.mr_hares.realmMode.listeners.InventoryClick;
import ru.mr_hares.realmMode.listeners.InventoryClose;
import ru.mr_hares.realmMode.listeners.PlayerDamage;
import ru.mr_hares.realmMode.listeners.PlayerJoin;
import ru.mr_hares.realmMode.utils.ExpansionUtils;
import ru.mr_hares.realmMode.utils.MessageUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class RealmMode extends JavaPlugin {
    private static RealmMode instance;
    public final Cache<UUID, Boolean> in_combat = Caffeine.newBuilder()
            .expireAfterWrite(getConfig().getInt("combat-cooldown", 15), TimeUnit.SECONDS)
            .build();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        DataBase.init(this);
        MessageUtils.init(this);

        initListeners(this,
                new PlayerJoin(),
                new InventoryClose(),
                new InventoryClick(),
                new PlayerDamage()
        );

        new RmCommand(this);
        new ModeCommand(this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ExpansionUtils().register();
        }
    }

    private void initListeners(Plugin p, Listener... listeners) {
        PluginManager pm = Bukkit.getPluginManager();
        long start = System.currentTimeMillis();

        for (Listener listener: listeners) {
            pm.registerEvents(listener, p);
        }

        getLogger().info("Registering Minecraft events in " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public void onDisable() {
        DataBase.close();
    }

    public static RealmMode getInstance() { return instance; }
}
