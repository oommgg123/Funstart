package moe.hinakusoft.funstart.manager;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.PlayerRank;
import moe.hinakusoft.funstart.model.PlayerRank.RankType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankManager {

    private final FunstartPlugin plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, PlayerRank> ranks = new HashMap<>();

    public RankManager(FunstartPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ranks.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    private void loadAll() {
        ranks.clear();
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String title = config.getString(key + ".title", "");
                String typeKey = config.getString(key + ".type", "default");
                RankType type = RankType.fromKey(typeKey);
                if (title != null && !title.isEmpty()) {
                    ranks.put(uuid, new PlayerRank(title, type));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveAll() {
        config.getKeys(false).forEach(k -> config.set(k, null));
        for (Map.Entry<UUID, PlayerRank> entry : ranks.entrySet()) {
            String path = entry.getKey().toString();
            config.set(path + ".title", entry.getValue().getTitle());
            config.set(path + ".type", entry.getValue().getType().getKey());
        }
        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }

    public PlayerRank getRank(UUID uuid) {
        return ranks.get(uuid);
    }

    public String getRankDisplay(UUID uuid) {
        PlayerRank rank = getRank(uuid);
        if (rank == null) return "";
        String formatted = rank.getFormatted();
        if (formatted.isEmpty()) return "";
        return "<" + formatted + "§f";
    }

    public PlayerRank.RankType getRankType(UUID uuid) {
        PlayerRank rank = getRank(uuid);
        return rank != null ? rank.getType() : RankType.DEFAULT;
    }

    public void setRank(UUID uuid, String title, RankType type) {
        if (title == null || title.trim().isEmpty()) {
            ranks.remove(uuid);
        } else {
            ranks.put(uuid, new PlayerRank(title.trim(), type));
        }
        saveAll();
    }

    public boolean hasRank(UUID uuid) {
        return ranks.containsKey(uuid);
    }

    public boolean hasRankType(RankType type) {
        return ranks.values().stream().anyMatch(r -> r.getType() == type);
    }

    public int getOnlineCountByType(RankType type) {
        return (int) ranks.entrySet().stream()
                .filter(e -> e.getValue().getType() == type)
                .filter(e -> org.bukkit.Bukkit.getPlayer(e.getKey()) != null)
                .count();
    }
}
