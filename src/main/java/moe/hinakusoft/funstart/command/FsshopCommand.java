package moe.hinakusoft.funstart.command;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.ItemStackData;
import moe.hinakusoft.funstart.model.MarketItem;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FsshopCommand implements CommandExecutor {

    private final FunstartPlugin plugin;

    public FsshopCommand(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行");
            return true;
        }
        if (!player.isOp()) {
            player.sendMessage("§c你没有权限使用此命令");
            return true;
        }

        if (args.length < 6 || !args[0].equalsIgnoreCase("new")) {
            player.sendMessage("§c用法: /fsshop new <basevalue> <dailycount> <ifbuyallrestoreup%> <ifnotbuyallrestoredown%> <restoretime_h>");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            player.sendMessage("§c请手持要上架的商品");
            return true;
        }

        try {
            double baseValue = Double.parseDouble(args[1]);
            int dailyCount = Integer.parseInt(args[2]);
            double buyAllUp = Double.parseDouble(args[3]);
            double notBuyAllDown = Double.parseDouble(args[4]);
            long restoreTime = Long.parseLong(args[5]);

            if (baseValue <= 0 || dailyCount <= 0) {
                player.sendMessage("§c价格和数量必须大于0");
                return true;
            }

            MarketItem mi = MarketItem.createOpShop(
                new ItemStackData(held.clone()),
                baseValue, dailyCount, buyAllUp, notBuyAllDown, restoreTime
            );
            plugin.getMarketManager().addItem(mi);
            player.sendMessage("§a[市场] OP商店物品上架成功! §e" + held.getType().name());
            player.sendMessage("§7基础价: §e" + String.format("%.1f", baseValue)
                + " §7| 日补货: §e" + dailyCount
                + " §7| 涨: §e" + buyAllUp + "%"
                + " §7| 降: §e" + notBuyAllDown + "%"
                + " §7| 周期: §e" + restoreTime + "h");

        } catch (NumberFormatException e) {
            player.sendMessage("§c参数格式错误，请输入有效数字");
        }
        return true;
    }
}
