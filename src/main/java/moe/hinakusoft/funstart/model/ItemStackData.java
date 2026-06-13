package moe.hinakusoft.funstart.model;

import org.bukkit.Material;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ItemStackData {

    private final String serialized;

    public ItemStackData(ItemStack item) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("i", item);
        this.serialized = config.saveToString();
    }

    public ItemStackData(String serialized) {
        this.serialized = serialized;
    }

    public ItemStack toItemStack() {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(serialized);
            return config.getItemStack("i");
        } catch (Exception e) {
            return new ItemStack(Material.STONE);
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("s", serialized);
        return map;
    }

    public static ItemStackData deserialize(Map<String, Object> map) {
        return new ItemStackData((String) map.get("s"));
    }
}
