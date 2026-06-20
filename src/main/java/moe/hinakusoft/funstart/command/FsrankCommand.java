package moe.hinakusoft.funstart.command;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.PlayerRank.RankType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FsrankCommand implements CommandExecutor {

    private final FunstartPlugin plugin;

    public FsrankCommand(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /fsrank <玩家名> <头衔内容> [类型]");
            sender.sendMessage("§7类型: default(默认绿色), zanzhu(绿色+分红), custom(粉色)");
            sender.sendMessage("§7头衔内容为 null 则删除头衔");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String title = args[1];
        RankType type = RankType.DEFAULT;
        if (args.length >= 3) {
            type = RankType.fromKey(args[2]);
        }

        if (title.equalsIgnoreCase("null")) {
            plugin.getRankManager().setRank(target.getUniqueId(), null, type);
            sender.sendMessage("§e[Funstart] §a已删除 §b" + args[0] + " §a的头衔");
        } else {
            plugin.getRankManager().setRank(target.getUniqueId(), title, type);
            sender.sendMessage("§e[Funstart] §a已设置 §b" + args[0] + " §a的头衔为: " + type.getColor() + title);
        }
        return true;
    }
}
