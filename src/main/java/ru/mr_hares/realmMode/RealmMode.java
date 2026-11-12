package ru.mr_hares.realmMode;

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
import ru.mr_hares.realmMode.utils.MessageUtils;

public final class RealmMode extends JavaPlugin {
    private static RealmMode instance;

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
        // Plugin shutdown logic
    }

    public static RealmMode getInstance() { return instance; }
}
