package moe.hinakusoft.funstart.manager;

import moe.hinakusoft.funstart.model.FeatureFlag;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FeatureConfig {

    private final File file;
    private final YamlConfiguration config;
    private final Map<FeatureFlag, Boolean> cache = new HashMap<>();

    public FeatureConfig(File dataFolder) {
        this.file = new File(dataFolder, "enabled.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadDefaults();
    }

    private void loadDefaults() {
        boolean dirty = false;
        for (FeatureFlag flag : FeatureFlag.values()) {
            String key = flag.getKey();
            if (!config.contains(key)) {
                config.set(key, true);
                dirty = true;
            }
            cache.put(flag, config.getBoolean(key, true));
        }
        if (dirty) save();
    }

    public boolean isEnabled(FeatureFlag flag) {
        return cache.getOrDefault(flag, true);
    }

    public void setEnabled(FeatureFlag flag, boolean enabled) {
        cache.put(flag, enabled);
        config.set(flag.getKey(), enabled);
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            // silently ignore save failure
        }
    }

    public void reload() {
        for (FeatureFlag flag : FeatureFlag.values()) {
            cache.put(flag, config.getBoolean(flag.getKey(), true));
        }
    }
}
