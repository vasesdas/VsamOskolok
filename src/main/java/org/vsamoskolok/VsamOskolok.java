package org.vsamoskolok;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class VsamOskolok extends JavaPlugin {

    private ShardManager shardManager;

    @Override
    public void onEnable() {
        this.shardManager = new ShardManager(this);
        this.shardManager.loadShards();

        getCommand("oskolok").setExecutor(new ShardCommand(this));
        getServer().getPluginManager().registerEvents(new AnomalyListener(this), this);

        // ИСПРАВЛЕНИЕ: Таймер теперь выполняется раз в секунду (20 тиков) для обновления голограмм
        Bukkit.getScheduler().runTaskTimer(this, () -> shardManager.updateAndCheckShards(), 20L, 20L);

        getLogger().info("Плагин VsamOskolok успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (shardManager != null) {
            shardManager.cleanAllHolograms(); // Удаляем голограммы перед рестартом
            shardManager.saveShards();
        }
        getLogger().info("Плагин VsamOskolok выключен.");
    }

    public ShardManager getShardManager() {
        return shardManager;
    }
}