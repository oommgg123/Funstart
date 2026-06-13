package moe.hinakusoft.funstart.command;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.listener.PanelClockListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FstGetCommand implements CommandExecutor {

    private final FunstartPlugin plugin;

    public FstGetCommand(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该指令仅限玩家使用");
            return true;
        }

        boolean hasSlot = false;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType().isAir()) {
                hasSlot = true;
                break;
            }
        }

        if (!hasSlot) {
            player.sendMessage("§c背包已满, 请清理出空格后再试");
            return true;
        }

        player.getInventory().addItem(PanelClockListener.createPanelClock());
        player.sendMessage("§e[Funstart] §a已获得面板钟, 右键打开功能面板");
        return true;
    }
}
