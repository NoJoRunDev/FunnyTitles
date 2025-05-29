package org.nojorundev.funnytitles.listeners;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.nojorundev.funnytitles.FunnyTitles;
import org.nojorundev.funnytitles.Title;
import org.nojorundev.funnytitles.menu.TitlesMenu;

import java.util.List;

public class TitlesListener implements Listener {

    private final FunnyTitles plugin;

    public TitlesListener(FunnyTitles plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TitlesMenu)) return;
        event.setCancelled(true);

        if (event.getClick().isKeyboardClick() || event.getClick().isCreativeAction()) {
            return;
        }

        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof TitlesMenu)) {
            return;
        }

        TitlesMenu menu = (TitlesMenu) event.getInventory().getHolder();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        menu.handleClick(event.getRawSlot(), event.getClick());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TitlesMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            String selectedTitleId = plugin.selectedTitles.get(event.getPlayer().getUniqueId());
            if (selectedTitleId != null) {
                Title title = plugin.getTitles().get(selectedTitleId);
                if (title != null) {
                    plugin.updatePlayerSuffix(event.getPlayer(), title.getSuffix());
                }
            }
        }, 20L);
    }
}