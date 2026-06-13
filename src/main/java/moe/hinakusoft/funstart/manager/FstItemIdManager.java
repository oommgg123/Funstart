package moe.hinakusoft.funstart.manager;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class FstItemIdManager {

    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    private final File fstItemDir;
    private final Map<String, String> itemHashToId = new HashMap<>();

    public FstItemIdManager(File dataFolder) {
        this.fstItemDir = new File(dataFolder, "fstitemid");
        fstItemDir.mkdirs();
        loadExisting();
    }

    private void loadExisting() {
        File[] files = fstItemDir.listFiles((d, n) -> n.endsWith(".fstid"));
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            int extIdx = name.lastIndexOf('.');
            if (extIdx <= 0) continue;
            String base = name.substring(0, extIdx);
            int usIdx = base.lastIndexOf('_');
            if (usIdx <= 0 || usIdx >= base.length() - 1) continue;
            String hash = base.substring(usIdx + 1);
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                String id = null;
                while ((line = r.readLine()) != null) {
                    if (line.startsWith("ID: ")) {
                        id = line.substring(4).trim();
                        break;
                    }
                }
                if (id != null) itemHashToId.put(hash, id);
            } catch (IOException ignored) {}
        }
    }

    private String now() {
        return TS_FMT.format(new Date());
    }

    private String computeHash(ItemStack item) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(item.getType().name().getBytes(StandardCharsets.UTF_8));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (meta.hasDisplayName())
                    md.update(meta.getDisplayName().getBytes(StandardCharsets.UTF_8));
                if (meta.hasLore())
                    for (String l : meta.getLore())
                        md.update(l.getBytes(StandardCharsets.UTF_8));
                if (meta.hasEnchants())
                    md.update(meta.getEnchants().toString().getBytes(StandardCharsets.UTF_8));
            }
            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.substring(0, 12);
        } catch (Exception e) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
    }

    public String getOrCreateItemId(ItemStack item) {
        try {
            String hash = computeHash(item);
            String existing = itemHashToId.get(hash);
            if (existing != null) return existing;

            String id = UUID.randomUUID().toString().substring(0, 8);
            String fileName = sanitize(item.getType().name()) + "_" + hash + ".fstid";
            File file = new File(fstItemDir, fileName);
            writeItemFile(file, id, item);
            itemHashToId.put(hash, id);
            return id;
        } catch (Exception e) {
            return "ERR";
        }
    }

    public String getOrCreateEntityId(EntityType type) {
        return getOrCreateEntityId(type, null);
    }

    public String getOrCreateEntityId(EntityType type, String extraData) {
        try {
            String input = type.name();
            if (extraData != null) input += "|" + extraData;
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            String hash = sb.substring(0, 12);

            String existing = itemHashToId.get("ent:" + hash);
            if (existing != null) return existing;

            String id = UUID.randomUUID().toString().substring(0, 8);
            String fileName = sanitize(type.name()) + "_" + hash + ".fstid";
            File file = new File(fstItemDir, fileName);
            writeEntityFile(file, id, type);
            itemHashToId.put("ent:" + hash, id);
            return id;
        } catch (Exception e) {
            return "ERR";
        }
    }

    private void writeItemFile(File file, String id, ItemStack item) {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write("ItemType: " + item.getType().name()); w.newLine();
            w.write("ID: " + id); w.newLine();
            w.write("记录时间: " + now()); w.newLine();
            w.write("名字: " + item.getType().name()); w.newLine();
            w.write("数量: " + item.getAmount()); w.newLine();
            w.newLine();

            w.write("=== 完整 NBT ==="); w.newLine();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                w.write("DisplayName: " + (meta.hasDisplayName() ? meta.getDisplayName() : "(无)")); w.newLine();
                w.write("Lore: " + (meta.hasLore() ? String.join(" | ", meta.getLore()) : "(无)")); w.newLine();
                w.write("Enchants: " + (meta.hasEnchants() ? meta.getEnchants().toString() : "(无)")); w.newLine();
                w.write("Unbreakable: " + meta.isUnbreakable()); w.newLine();
            } else {
                w.write("(无)"); w.newLine();
            }
            w.newLine();

            w.write("=== 完整物品组件 ==="); w.newLine();
            try {
                w.write(item.serialize().toString());
            } catch (Exception e) {
                w.write("(序列化失败: " + e.getMessage() + ")");
            }
            w.newLine();
            w.newLine();

            w.write("=== 完整标签 ==="); w.newLine();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (pdc.isEmpty()) {
                    w.write("(无)");
                } else {
                    for (NamespacedKey key : pdc.getKeys()) {
                        w.write(key.toString());
                        w.newLine();
                    }
                }
            } else {
                w.write("(无)");
            }
            w.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeEntityFile(File file, String id, EntityType type) {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write("EntityType: " + type.name()); w.newLine();
            w.write("ID: " + id); w.newLine();
            w.write("记录时间: " + now()); w.newLine();
            w.write("名字: " + type.name()); w.newLine();
            w.write("数量: 1"); w.newLine();
            w.newLine();
            w.write("=== 完整 NBT ==="); w.newLine();
            w.write("(实体 NBT 暂未实现)"); w.newLine();
            w.newLine();
            w.write("=== 完整物品组件 ==="); w.newLine();
            w.write("(无)"); w.newLine();
            w.newLine();
            w.write("=== 完整标签 ==="); w.newLine();
            w.write("(无)"); w.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}
