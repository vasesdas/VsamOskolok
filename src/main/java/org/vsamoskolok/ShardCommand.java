package org.vsamoskolok;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ShardCommand implements CommandExecutor {

    private final VsamOskolok plugin;

    public ShardCommand(VsamOskolok plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vsamoskolok.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав!");
            return true;
        }

        if (sender instanceof Player) {
            Player p = (Player) sender;

            // В качестве блока используем Плачущий Обсидиан (Он круто выглядит и светится)
            ItemStack shard = new ItemStack(Material.CRYING_OBSIDIAN);
            ItemMeta meta = shard.getItemMeta();

            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4&lОсколок BleedCube"));

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Установите на землю, чтобы создать");
            lore.add(ChatColor.GRAY + "Аномальную Зону (3х3 чанка) на 24 часа.");
            lore.add("");
            lore.add(ChatColor.RED + "Внимание: " + ChatColor.GRAY + "В радиусе этой зоны");
            lore.add(ChatColor.GRAY + "взрывы ТНТ и кристаллов гораздо сильнее!");
            meta.setLore(lore);

            // Невидимая метка (защита от подделок)
            NamespacedKey key = new NamespacedKey(plugin, "bleed_shard");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            shard.setItemMeta(meta);
            p.getInventory().addItem(shard);
            p.sendMessage(ChatColor.GREEN + "Вы получили Осколок!");
        }

        return true;
    }
}