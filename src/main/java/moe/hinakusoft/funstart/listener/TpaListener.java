package moe.hinakusoft.funstart.listener;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.manager.TpaManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class TpaListener implements Listener {
    private final FunstartPlugin plugin;

    public TpaListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TpaManager tpaManager = this.plugin.getTpaManager();
        for (TpaManager.TpaRequest req : tpaManager.removeAllRequestsByPlayer(player.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(req.getTaskId());
            if (req.getRequester().equals(player.getUniqueId())) {
                Player target = Bukkit.getPlayer(req.getTarget());
                if (target == null || !target.isOnline()) continue;
                target.sendMessage("§e[Funstart] §b" + player.getName() + " §c已离线, 请求已取消");
                continue;
            }
            Player requester = Bukkit.getPlayer(req.getRequester());
            if (requester == null || !requester.isOnline()) continue;
            requester.sendMessage("§e[Funstart] §b" + player.getName() + " §c已离线, 请求已取消");
        }
    }
}
