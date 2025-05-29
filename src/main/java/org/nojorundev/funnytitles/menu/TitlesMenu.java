package org.nojorundev.funnytitles.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nojorundev.funnytitles.FunnyTitles;
import org.nojorundev.funnytitles.Title;
import org.nojorundev.funnytitles.utils.ChatColorUtil;

import java.util.*;
import java.util.stream.Collectors;

public class TitlesMenu implements InventoryHolder {

    private final FunnyTitles plugin;
    private final Player player;
    private Inventory inventory;
    private int currentPage = 0;
    private static final int TITLES_PER_PAGE = 28;

    public TitlesMenu(FunnyTitles plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        int rows = plugin.getMenuConfig().getInt("menu.rows", 6);
        int inventorySize = rows * 9;
        String menuTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getMenuConfig().getString("menu.title", "Выбор титула"));
        this.inventory = Bukkit.createInventory(this, inventorySize, menuTitle);
        openPage(0);
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void openPage(int page) {
        inventory.clear();
        currentPage = page;

        fillFrame();
        addInfoItem();
        addNavigationItems();
        addRemoveTitleButton();
        addTitles();
    }

    private void fillFrame() {
        ConfigurationSection frameSection = plugin.getMenuConfig().getConfigurationSection("menu.frame");
        if (frameSection == null) return;

        ItemStack frameItem = createItem(
                frameSection.getString("material"),
                frameSection.getString("name"),
                frameSection.getStringList("lore")
        );

        for (int slot : frameSection.getIntegerList("slots")) {
            inventory.setItem(slot, frameItem);
        }
    }

    private void addInfoItem() {
        ConfigurationSection infoSection = plugin.getMenuConfig().getConfigurationSection("menu.info_item");
        if (infoSection == null) return;

        String name = infoSection.getString("name");
        List<String> lore = infoSection.getStringList("lore");

        name = name.replace("{current_page}", String.valueOf(currentPage + 1))
                .replace("{total_pages}", String.valueOf(getTotalPages()));

        List<String> processedLore = new ArrayList<>();
        for (String line : lore) {
            processedLore.add(line.replace("{current_page}", String.valueOf(currentPage + 1))
                    .replace("{total_pages}", String.valueOf(getTotalPages())));
        }

        ItemStack item = createItem(
                infoSection.getString("material"),
                name,
                processedLore
        );

        inventory.setItem(infoSection.getInt("slot"), item);
    }

    private void addNavigationItems() {
        if (hasNextPage(currentPage)) {
            ConfigurationSection nextSection = plugin.getMenuConfig().getConfigurationSection("menu.navigation.next");
            if (nextSection != null) {
                ItemStack nextItem = createItem(
                        nextSection.getString("material"),
                        nextSection.getString("name"),
                        nextSection.getStringList("lore")
                );
                inventory.setItem(nextSection.getInt("slot"), nextItem);
            }
        }

        if (hasPreviousPage(currentPage)) {
            ConfigurationSection prevSection = plugin.getMenuConfig().getConfigurationSection("menu.navigation.previous");
            if (prevSection != null) {
                ItemStack prevItem = createItem(
                        prevSection.getString("material"),
                        prevSection.getString("name"),
                        prevSection.getStringList("lore")
                );
                inventory.setItem(prevSection.getInt("slot"), prevItem);
            }
        }
    }

    private void addTitles() {
        ConfigurationSection titleItemSection = plugin.getMenuConfig().getConfigurationSection("menu.title_item");
        if (titleItemSection == null) return;

        List<Integer> slots = plugin.getMenuConfig().getIntegerList("menu.title_slots");
        if (slots.isEmpty()) return;

        List<Map.Entry<String, Title>> sortedTitles = getSortedTitles();

        int startIndex = currentPage * slots.size();
        int endIndex = Math.min(startIndex + slots.size(), sortedTitles.size());

        for (int i = startIndex; i < endIndex; i++) {
            if (i - startIndex >= slots.size()) break;

            Map.Entry<String, Title> entry = sortedTitles.get(i);
            String titleId = entry.getKey();
            Title title = entry.getValue();
            boolean hasTitle = plugin.hasTitle(player, titleId);
            boolean canAfford = plugin.getPlayerPointsAPI().look(player.getUniqueId()) >= title.getCost();

            ConfigurationSection itemSection;
            if (hasTitle) {
                itemSection = titleItemSection.getConfigurationSection("purchased");
            } else if (canAfford) {
                itemSection = titleItemSection.getConfigurationSection("available");
            } else {
                itemSection = titleItemSection.getConfigurationSection("unavailable");
            }

            if (itemSection != null) {
                String name = itemSection.getString("name").replace("{title_name}", title.getDisplayName());

                List<String> lore = new ArrayList<>();
                for (String line : itemSection.getStringList("lore")) {
                    lore.add(line.replace("{title_name}", title.getDisplayName())
                            .replace("{title_cost}", String.valueOf(title.getCost()))
                            .replace("{title_suffix}", title.getSuffix()));
                }

                ItemStack item = createItem(
                        itemSection.getString("material"),
                        name,
                        lore
                );

                inventory.setItem(slots.get(i - startIndex), item);
            }
        }
    }

    private ItemStack createItem(String materialStr, String name, List<String> lore) {
        Material material = Material.matchMaterial(materialStr);
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(coloredLore);

        item.setItemMeta(meta);
        return item;
    }

    private int getTotalPages() {
        List<Integer> slots = plugin.getMenuConfig().getIntegerList("menu.title_slots");
        if (slots.isEmpty()) return 1;
        return (int) Math.ceil((double) getSortedTitles().size() / slots.size());
    }

    private List<Map.Entry<String, Title>> getSortedTitles() {
        List<Map.Entry<String, Title>> sortedTitles = new ArrayList<>(plugin.getTitles().entrySet());

        sortedTitles = sortedTitles.stream()
                .filter(entry -> entry.getValue().isShow() || plugin.hasTitle(player, entry.getKey()))
                .collect(Collectors.toList());

        sortedTitles.sort((entry1, entry2) -> {
            boolean hasTitle1 = plugin.hasTitle(player, entry1.getKey());
            boolean hasTitle2 = plugin.hasTitle(player, entry2.getKey());
            boolean canAfford1 = plugin.getPlayerPointsAPI().look(player.getUniqueId()) >= entry1.getValue().getCost();
            boolean canAfford2 = plugin.getPlayerPointsAPI().look(player.getUniqueId()) >= entry2.getValue().getCost();

            if (hasTitle1 && !hasTitle2) return -1;
            if (!hasTitle1 && hasTitle2) return 1;
            if (canAfford1 && !canAfford2) return -1;
            if (!canAfford1 && canAfford2) return 1;
            return entry1.getKey().compareTo(entry2.getKey());
        });

        return sortedTitles;
    }

    public void handleClick(int slot, ClickType clickType) {
        ConfigurationSection navSection = plugin.getMenuConfig().getConfigurationSection("menu.navigation");
        ConfigurationSection removeSection = plugin.getMenuConfig().getConfigurationSection("menu.remove_title");

        if (navSection != null) {
            int nextSlot = navSection.getInt("next.slot", 53);
            int prevSlot = navSection.getInt("previous.slot", 45);

            if (slot == nextSlot && hasNextPage(currentPage)) {
                openPage(currentPage + 1);
                return;
            } else if (slot == prevSlot && hasPreviousPage(currentPage)) {
                openPage(currentPage - 1);
                return;
            }
        }

        if (removeSection != null && slot == removeSection.getInt("slot")) {
            if (plugin.selectedTitles.containsKey(player.getUniqueId())) {
                plugin.selectedTitles.remove(player.getUniqueId());
                plugin.database.saveSelectedTitle(player.getUniqueId(), null);
                plugin.updatePlayerSuffix(player, "");
                player.sendMessage(ChatColorUtil.color(plugin.getlangConfig().getString("titul-removed")));
                openPage(currentPage);
            }
            return;
        }

        List<Integer> titleSlots = plugin.getMenuConfig().getIntegerList("menu.title_slots");
        if (titleSlots.contains(slot)) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) return;

            int slotsPerPage = titleSlots.size();
            List<Map.Entry<String, Title>> sortedTitles = getSortedTitles();
            int index = currentPage * titleSlots.size() + titleSlots.indexOf(slot);
            if (index >= sortedTitles.size()) return;

            String titleId = sortedTitles.get(index).getKey();
            Title title = sortedTitles.get(index).getValue();

            if (plugin.hasTitle(player, titleId)) {
                plugin.selectTitle(player, titleId);
                openPage(currentPage);
            } else {
                if (plugin.purchaseTitle(player, titleId)) {
                    openPage(currentPage);
                } else {
                    player.sendMessage(ChatColorUtil.color(plugin.getlangConfig().getString("not-enough-points")));
                }
            }
        }
    }

    private void addRemoveTitleButton() {
        ConfigurationSection removeSection = plugin.getMenuConfig().getConfigurationSection("menu.remove_title");
        ConfigurationSection removeSectionNull = plugin.getMenuConfig().getConfigurationSection("menu.remove_title_null");
        if (removeSection == null) return;
        if (removeSectionNull == null) return;

        String selectedTitleId = plugin.selectedTitles.get(player.getUniqueId());
        boolean hasSelectedTitle = selectedTitleId != null;

        ItemStack removeItem = createItem(
                removeSection.getString("material"),
                removeSection.getString("name"),
                removeSection.getStringList("lore")
        );

        if (!hasSelectedTitle) {
            ItemMeta meta = removeItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColorUtil.color(removeSectionNull.getString("name")));
                meta.setLore(removeSectionNull.getStringList("lore").stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .collect(Collectors.toList()));
                removeItem.setItemMeta(meta);
            }
        }

        inventory.setItem(removeSection.getInt("slot"), removeItem);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public boolean hasNextPage(int page) {
        List<Integer> slots = plugin.getMenuConfig().getIntegerList("menu.title_slots");
        if (slots.isEmpty()) return false;
        return (page + 1) * slots.size() < getSortedTitles().size();
    }

    public boolean hasPreviousPage(int page) {
        return page > 0;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}