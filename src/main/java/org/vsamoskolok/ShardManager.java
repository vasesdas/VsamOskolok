package org.vsamoskolok;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShardManager {
    private final VsamOskolok plugin;
    private final Map<Location, Long> activeShards = new HashMap<>();
    private final NamespacedKey holoKey;
    private final NamespacedKey holoTimerKey; // НОВЫЙ ключ для идентификации строки таймера
    private File file;
    private FileConfiguration config;

    public ShardManager(VsamOskolok plugin) {
        this.plugin = plugin;
        this.holoKey = new NamespacedKey(plugin, "shard_holo");
        this.holoTimerKey = new NamespacedKey(plugin, "shard_holo_timer");
        this.file = new File(plugin.getDataFolder(), "shards.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isInAnomalyZone(Location loc) {
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;

        for (Location shardLoc : activeShards.keySet()) {
            if (!shardLoc.getWorld().equals(loc.getWorld())) continue;
            int sChunkX = shardLoc.getBlockX() >> 4;
            int sChunkZ = shardLoc.getBlockZ() >> 4;
            if (Math.abs(chunkX - sChunkX) <= 1 && Math.abs(chunkZ - sChunkZ) <= 1) return true;
        }
        return false;
    }

    public boolean isShardBlock(Location loc) {
        return activeShards.containsKey(loc);
    }

    public void addShard(Location loc, Player p) {
        long expireTime = System.currentTimeMillis() + (24L * 60L * 60L * 1000L); // 24 часа
        activeShards.put(loc, expireTime);
        saveShards();
        spawnHologram(loc);
        if (p != null) showBorderToPlayer(loc, p);
    }

    public void showBorderToPlayer(Location loc, Player p) {
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;

        int minX = (chunkX - 1) * 16;
        int minZ = (chunkZ - 1) * 16;
        int maxX = (chunkX + 1) * 16 + 15;
        int maxZ = (chunkZ + 1) * 16 + 15;

        new BukkitRunnable() {
            int time = 0;
            @Override
            public void run() {
                if (time > 30 || !p.isOnline()) { // Отрисовка длится 15 секунд
                    this.cancel();
                    return;
                }
                time++;

                double y = p.getLocation().getY() + 1.0;
                Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.5f);

                for (int x = minX; x <= maxX; x += 2) {
                    p.spawnParticle(Particle.REDSTONE, x + 0.5, y, minZ, 1, dust);
                    p.spawnParticle(Particle.REDSTONE, x + 0.5, y, maxZ + 1, 1, dust);
                }
                for (int z = minZ; z <= maxZ; z += 2) {
                    p.spawnParticle(Particle.REDSTONE, minX, y, z + 0.5, 1, dust);
                    p.spawnParticle(Particle.REDSTONE, maxX + 1, y, z + 0.5, 1, dust);
                }

                for (double py = loc.getBlockY(); py < loc.getBlockY() + 15; py += 1.5) {
                    p.spawnParticle(Particle.FLAME, minX, py, minZ, 1, 0, 0, 0, 0);
                    p.spawnParticle(Particle.FLAME, maxX + 1, py, minZ, 1, 0, 0, 0, 0);
                    p.spawnParticle(Particle.FLAME, minX, py, maxZ + 1, 1, 0, 0, 0, 0);
                    p.spawnParticle(Particle.FLAME, maxX + 1, py, maxZ + 1, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 10L); // ИСПРАВЛЕНИЕ: Теперь частицы спавнятся асинхронно
    }

    public void spawnHologram(Location loc) {
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;

        Location holo1 = loc.clone().add(0.5, 1.5, 0.5); // Подняли чуть выше
        Location holo2 = loc.clone().add(0.5, 1.2, 0.5);
        Location holo3 = loc.clone().add(0.5, 0.9, 0.5); // НОВАЯ: строка для таймера

        loc.getWorld().spawn(holo1, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setCustomNameVisible(true);
            as.setCustomName("§4§lАНОМАЛЬНАЯ ЗОНА");
            as.setGravity(false);
            as.setMarker(true);
            as.getPersistentDataContainer().set(holoKey, PersistentDataType.BYTE, (byte) 1);
        });

        loc.getWorld().spawn(holo2, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setCustomNameVisible(true);
            as.setCustomName("§cРадиус: 3x3 чанка");
            as.setGravity(false);
            as.setMarker(true);
            as.getPersistentDataContainer().set(holoKey, PersistentDataType.BYTE, (byte) 1);
        });

        loc.getWorld().spawn(holo3, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setCustomNameVisible(true);
            as.setCustomName("§eОсталось: §fВычисление...");
            as.setGravity(false);
            as.setMarker(true);
            as.getPersistentDataContainer().set(holoKey, PersistentDataType.BYTE, (byte) 1);
            as.getPersistentDataContainer().set(holoTimerKey, PersistentDataType.BYTE, (byte) 1); // Метка таймера
        });
    }

    public void removeHologram(Location loc) {
        for (Entity ent : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 1.0, 0.5), 1.0, 2.0, 1.0)) {
            if (ent instanceof ArmorStand && ent.getPersistentDataContainer().has(holoKey, PersistentDataType.BYTE)) {
                ent.remove();
            }
        }
    }

    public void handleChunkLoad(Chunk chunk) {
        // Очищаем старые голограммы, чтобы не дублировались
        for (Entity ent : chunk.getEntities()) {
            if (ent instanceof ArmorStand && ent.getPersistentDataContainer().has(holoKey, PersistentDataType.BYTE)) {
                ent.remove();
            }
        }

        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        World chunkWorld = chunk.getWorld();

        for (Location loc : activeShards.keySet()) {
            // Проверяем, в том ли мире находится осколок
            if (!loc.getWorld().equals(chunkWorld)) continue;

            // Вычисляем координаты чанка у осколка (без метода getChunk!)
            int shardChunkX = loc.getBlockX() >> 4;
            int shardChunkZ = loc.getBlockZ() >> 4;

            // Если координаты совпадают — спавним голограмму
            if (shardChunkX == chunkX && shardChunkZ == chunkZ) {
                spawnHologram(loc);
            }
        }
    }

    public void cleanAllHolograms() {
        for (Location loc : activeShards.keySet()) {
            if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                removeHologram(loc);
            }
        }
    }

    // НОВЫЙ МЕТОД: Одновременно проверяет таймеры (каждую секунду) и обновляет голограммы
    public void updateAndCheckShards() {
        long now = System.currentTimeMillis();
        boolean removed = activeShards.entrySet().removeIf(entry -> {
            Location loc = entry.getKey();
            long expire = entry.getValue();

            // Если время вышло — удаляем Осколок
            if (now >= expire) {
                loc.getBlock().setType(Material.AIR);
                if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    removeHologram(loc);
                }
                Bukkit.broadcastMessage("§8[§cBleedCube§8] §fАномальная зона по координатам §c" +
                        loc.getBlockX() + " " + loc.getBlockZ() + " §fрассеялась!");
                return true;
            }

            // Если не вышло — обновляем текст таймера
            if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                String timeStr = formatTime(expire - now);
                for (Entity ent : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 1.0, 0.5), 1.0, 2.0, 1.0)) {
                    if (ent instanceof ArmorStand && ent.getPersistentDataContainer().has(holoTimerKey, PersistentDataType.BYTE)) {
                        ent.setCustomName("§eОсталось: §f" + timeStr);
                    }
                }
            }
            return false;
        });

        // Сохраняем конфиг, только если какая-то зона пропала
        if (removed) {
            saveShards();
        }
    }

    // Вспомогательный метод для красивого формата времени (ЧЧ:ММ:СС)
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public void loadShards() {
        if (config.contains("shards")) {
            for (String key : config.getConfigurationSection("shards").getKeys(false)) {
                World world = Bukkit.getWorld(UUID.fromString(config.getString("shards." + key + ".world")));
                if (world == null) continue;
                int x = config.getInt("shards." + key + ".x");
                int y = config.getInt("shards." + key + ".y");
                int z = config.getInt("shards." + key + ".z");
                long expire = config.getLong("shards." + key + ".expire");

                Location loc = new Location(world, x, y, z);
                activeShards.put(loc, expire);

                if (world.isChunkLoaded(x >> 4, z >> 4)) {
                    spawnHologram(loc);
                }
            }
        }
    }

    public void saveShards() {
        config.set("shards", null);
        int i = 0;
        for (Map.Entry<Location, Long> entry : activeShards.entrySet()) {
            Location loc = entry.getKey();
            String path = "shards." + i;
            config.set(path + ".world", loc.getWorld().getUID().toString());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".expire", entry.getValue());
            i++;
        }
        try { config.save(file); } catch (IOException ignored) {}
    }
}