package org.vsamoskolok;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnomalyListener implements Listener {
    private final VsamOskolok plugin;
    private final NamespacedKey shardKey;
    private final Map<UUID, Long> particleCooldowns = new HashMap<>();

    public AnomalyListener(VsamOskolok plugin) {
        this.plugin = plugin;
        this.shardKey = new NamespacedKey(plugin, "bleed_shard");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getHand() == EquipmentSlot.HAND) {
            if (e.getClickedBlock() != null) {
                Location loc = e.getClickedBlock().getLocation();
                Player p = e.getPlayer();

                if (plugin.getShardManager().isShardBlock(loc)) {
                    e.setCancelled(true);

                    long current = System.currentTimeMillis();
                    if (particleCooldowns.getOrDefault(p.getUniqueId(), 0L) > current) {
                        p.sendMessage("§cПодождите немного перед повторной подсветкой!");
                        return;
                    }

                    particleCooldowns.put(p.getUniqueId(), current + 15000L);
                    plugin.getShardManager().showBorderToPlayer(loc, p);
                    p.sendMessage("§aВы подсветили границы Аномальной Зоны!");
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        plugin.getShardManager().handleChunkLoad(e.getChunk());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(shardKey, PersistentDataType.BYTE)) {
            plugin.getShardManager().addShard(e.getBlock().getLocation(), e.getPlayer());

            Player p = e.getPlayer();
            particleCooldowns.put(p.getUniqueId(), System.currentTimeMillis() + 15000L);

            p.sendMessage("§cВы создали Аномальную Зону! Она продержится 24 часа.");
            p.sendMessage("§7Граница зоны будет подсвечена красным в течение 15 секунд.");
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (plugin.getShardManager().isShardBlock(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cЭтот Осколок нельзя сломать! Он исчезнет сам.");
            return;
        }

        if (plugin.getShardManager().isInAnomalyZone(e.getBlock().getLocation())) {
            Material type = e.getBlock().getType();
            if (type.name().endsWith("_ORE") || type == Material.ANCIENT_DEBRIS) {

                ItemStack handItem = e.getPlayer().getInventory().getItemInMainHand();

                // ИСПРАВЛЕНИЕ: Защита от дюпа Шелковым касанием
                if (handItem.containsEnchantment(Enchantment.SILK_TOUCH)) {
                    return;
                }

                // ИСПРАВЛЕНИЕ: Отменяем ванильный дроп, чтобы не было х3 ресурсов
                e.setDropItems(false);

                for (ItemStack drop : e.getBlock().getDrops(handItem)) {
                    drop.setAmount(drop.getAmount() * 2);
                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
                }
            }
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.getShardManager().isShardBlock(b.getLocation()));
    }

    @EventHandler
    public void onTntPrime(ExplosionPrimeEvent e) {
        if (e.getEntity() instanceof TNTPrimed) {
            if (plugin.getShardManager().isInAnomalyZone(e.getEntity().getLocation())) {
                e.setRadius(e.getRadius() * 2.0f);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCrystalDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager().getType() == EntityType.ENDER_CRYSTAL) {
            if (plugin.getShardManager().isInAnomalyZone(e.getEntity().getLocation())) {
                e.setDamage(e.getDamage() * 1.3);
            }
        }
    }

    @EventHandler
    public void onCropGrow(BlockGrowEvent e) {
        if (plugin.getShardManager().isInAnomalyZone(e.getBlock().getLocation())) {
            if (e.getNewState().getBlockData() instanceof Ageable) {
                Ageable crop = (Ageable) e.getNewState().getBlockData();
                if (crop.getAge() < crop.getMaximumAge()) {
                    crop.setAge(Math.min(crop.getMaximumAge(), crop.getAge() + 2));
                    e.getNewState().setBlockData(crop);
                }
            }
        }
    }

    @EventHandler
    public void onSpawner(SpawnerSpawnEvent e) {
        if (plugin.getShardManager().isInAnomalyZone(e.getLocation())) {
            for (int i = 0; i < 2; i++) {
                e.getLocation().getWorld().spawnEntity(e.getLocation(), e.getEntityType());
            }
        }
    }
}