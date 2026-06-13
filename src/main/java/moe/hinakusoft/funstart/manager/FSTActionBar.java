package moe.hinakusoft.funstart.manager;

import moe.hinakusoft.funstart.FunstartPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FSTActionBar {

    private static final int DISPLAY_TICKS = 40;
    private static final int INTERVAL = 5;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Map<UUID, List<BarMessage>> messageMap = new ConcurrentHashMap<>();

    public FSTActionBar(FunstartPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                List<BarMessage> list = messageMap.get(player.getUniqueId());
                if (list == null || list.isEmpty()) continue;

                Iterator<BarMessage> it = list.iterator();
                while (it.hasNext()) {
                    BarMessage msg = it.next();
                    msg.ticks -= INTERVAL;
                    if (msg.ticks <= 0) {
                        it.remove();
                    }
                }

                if (!list.isEmpty()) {
                    List<Component> parts = new ArrayList<>();
                    for (BarMessage msg : list) {
                        Component comp = LEGACY.deserialize(msg.text);
                        if (msg.count > 1) {
                            comp = comp.append(LEGACY.deserialize(" §7(x" + msg.count + ")"));
                        }
                        parts.add(comp);
                    }
                    player.sendActionBar(Component.join(JoinConfiguration.newlines(), parts));
                }
            }
        }, 0L, INTERVAL);
    }

    public void add(Player player, String text) {
        List<BarMessage> list = messageMap.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        for (BarMessage msg : list) {
            if (msg.text.equals(text)) {
                msg.ticks = DISPLAY_TICKS;
                msg.count++;
                return;
            }
        }
        list.add(new BarMessage(text, DISPLAY_TICKS));
    }

    private static class BarMessage {
        final String text;
        int ticks;
        int count = 1;

        BarMessage(String text, int ticks) {
            this.text = text;
            this.ticks = ticks;
        }
    }
}
