package org.nojorundev.funnytitles;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.nojorundev.funnytitles.database.Database;
import org.nojorundev.funnytitles.listeners.TitlesListener;
import org.nojorundev.funnytitles.menu.TitlesMenu;
import org.nojorundev.funnytitles.utils.ChatColorUtil;

import java.io.File;
import java.util.*;

public class FunnyTitles extends JavaPlugin {

    private LuckPerms luckPerms;
    private PlayerPointsAPI playerPointsAPI;
    private Map<String, Title> titles = new HashMap<>();
    public Map<UUID, Set<String>> playerTitles = new HashMap<>();
    private FileConfiguration menuConfig;
    private FileConfiguration langConfig;
    public Database database;
    public Map<UUID, String> selectedTitles = new HashMap<>();

    @Override
    public void onEnable() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        }

        playerPointsAPI = PlayerPoints.getInstance().getAPI();

        database = new Database(this);
        database.connect();
        database.loadAllTitles(this);

        saveDefaultConfig();
        createMenuConfig();
        createLangConfig();
        loadTitles();

        Bukkit.getPluginManager().registerEvents(new TitlesListener(this), this);
        getCommand("titles").setExecutor(this);
        this.getCommand("titles").setTabCompleter((sender, command, alias, args) -> {
            if (!sender.hasPermission("titles.admin")) {
                return null;
            }

            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                completions.add("give");
                completions.add("remove");
                completions.add("reload");
                completions.add("help");
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove"))) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove"))) {
                completions.addAll(titles.keySet());
            }

            if (args.length > 0) {
                String input = args[args.length - 1].toLowerCase();
                completions.removeIf(s -> !s.toLowerCase().startsWith(input));
            }

            return completions;
        });
    }

    private void createMenuConfig() {
        File menuFile = new File(getDataFolder(), "menu.yml");
        if (!menuFile.exists()) {
            saveResource("menu.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(menuFile);
    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }

    private void createLangConfig() {
        File langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public FileConfiguration getlangConfig() {
        return langConfig;
    }

    private void loadTitles() {
        titles.clear();
        for (String key : getConfig().getConfigurationSection("titles").getKeys(false)) {
            String displayName = getConfig().getString("titles." + key + ".display");
            int cost = getConfig().getInt("titles." + key + ".cost");
            String suffix = getConfig().getString("titles." + key + ".suffix");
            boolean show = getConfig().getBoolean("titles." + key + ".show", true);
            titles.put(key, new Title(displayName, cost, suffix, show));
        }
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            new TitlesMenu(this, player).open();
            return true;
        }

        if (!player.hasPermission("titles.admin")) {
            new TitlesMenu(this, player).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                if (args.length < 3) {
                    player.sendMessage(ChatColorUtil.color("&d&lFunnyTitles &7- by &dNoJoRunDev"));
                    player.sendMessage(ChatColorUtil.color(getlangConfig().getString("help")));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColorUtil.color(getlangConfig().getString("player-not-found").replace("{player}", args[1])));
                    return true;
                }

                String titleId = args[2];
                if (!titles.containsKey(titleId)) {
                    player.sendMessage(ChatColorUtil.color(getlangConfig().getString("titul-not-found")));
                    return true;
                }

                giveTitle(target, titleId);
                player.sendMessage(ChatColorUtil.color(getlangConfig().getString("titul-gived").replace("{titul}", titles.get(titleId).getDisplayName()).replace("{player}", target.getName())));
                return true;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage(ChatColorUtil.color("&d&lFunnyTitles &7- by &dNoJoRunDev"));
                    player.sendMessage(ChatColorUtil.color(getlangConfig().getString("help")));
                    return true;
                }

                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColorUtil.color(getlangConfig().getString("player-not-found")));
                    return true;
                }

                if (args.length >= 3) {
                    titleId = args[2];
                    if (!titles.containsKey(titleId)) {
                        player.sendMessage(ChatColorUtil.color(getlangConfig().getString("titul-not-found")));
                        return true;
                    }
                    removeTitle(target, titleId);
                } else {
                    removeTitle(target, null);
                }
                return true;
            case "reload":
                reloadConfig();
                loadTitles();
                createMenuConfig();
                createLangConfig();
                player.sendMessage(ChatColorUtil.color(getlangConfig().getString("config-reload")));
                return true;
            case "help":
                player.sendMessage(ChatColorUtil.color("&d&lFunnyTitles &7- by &dNoJoRunDev"));
                player.sendMessage(ChatColorUtil.color(getlangConfig().getString("help")));
                return true;
            default:
                player.sendMessage(ChatColorUtil.color("&d&lFunnyTitles &7- by &dNoJoRunDev"));
                player.sendMessage(ChatColorUtil.color(getlangConfig().getString("help")));
                return true;
        }
    }

    public void selectTitle(Player player, String titleId) {
        if (titleId == null) {
            selectedTitles.remove(player.getUniqueId());
            database.saveSelectedTitle(player.getUniqueId(), null);
            updatePlayerSuffix(player, "");
            player.sendMessage(ChatColorUtil.color(getlangConfig().getString("titul-removed")));
            return;
        }

        Title title = titles.get(titleId);
        if (title == null) return;

        if (!hasTitle(player, titleId)) return;

        if (titleId.equals(selectedTitles.get(player.getUniqueId()))) {
            player.sendMessage(ChatColorUtil.color(getlangConfig().getString("titul-has-choosed")));
            return;
        }

        selectedTitles.put(player.getUniqueId(), titleId);
        database.saveSelectedTitle(player.getUniqueId(), titleId);
        updatePlayerSuffix(player, title.getSuffix());

        player.sendMessage(ChatColorUtil.color(getlangConfig().getString("titul-choosed").replace("{titul}", title.getDisplayName())));
    }

    public void giveTitle(Player player, String titleId) {
        Title title = titles.get(titleId);
        if (title == null) return;

        if (!playerTitles.containsKey(player.getUniqueId())) {
            playerTitles.put(player.getUniqueId(), new HashSet<>());
        }

        playerTitles.get(player.getUniqueId()).add(titleId);
        database.saveTitle(player.getUniqueId(), titleId);

        player.sendMessage(ChatColorUtil.color(getlangConfig().getString("titul-gived-player").replace("{titul}", title.getDisplayName())));
    }

    public boolean hasTitle(Player player, String titleId) {
        return playerTitles.containsKey(player.getUniqueId()) &&
                playerTitles.get(player.getUniqueId()).contains(titleId);
    }

    public void removeTitle(Player player, String titleId) {
        if (titleId != null) {
            if (playerTitles.containsKey(player.getUniqueId())) {
                playerTitles.get(player.getUniqueId()).remove(titleId);
                database.removeTitle(player.getUniqueId(), titleId);
                player.sendMessage(ChatColorUtil.color(getlangConfig().getString("titul-deleted").replace("{titul}", titles.get(titleId).getDisplayName())));
            }
        } else {
            if (playerTitles.containsKey(player.getUniqueId())) {
                playerTitles.remove(player.getUniqueId());
                database.removeAllTitles(player.getUniqueId());
                updatePlayerSuffix(player, "");
                player.sendMessage(ChatColorUtil.color(getlangConfig().getString("all-titul-deleted")));
            }
        }
    }

    public void updatePlayerSuffix(Player player, String suffix) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        user.data().clear(node -> node.getKey().startsWith("suffix."));

        if (suffix != null && !suffix.isEmpty()) {
            Node node = Node.builder("suffix.1. " + suffix.trim()).build();
            user.data().add(node);
        }

        luckPerms.getUserManager().saveUser(user);
    }

    public boolean purchaseTitle(Player player, String titleId) {
        Title title = titles.get(titleId);
        if (title == null) return false;

        if (playerPointsAPI.take(player.getUniqueId(), title.getCost())) {
            giveTitle(player, titleId);
            return true;
        }
        return false;
    }

    public Map<String, Title> getTitles() {
        return titles;
    }

    public PlayerPointsAPI getPlayerPointsAPI() {
        return playerPointsAPI;
    }
}

