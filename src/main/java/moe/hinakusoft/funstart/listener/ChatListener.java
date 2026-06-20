package moe.hinakusoft.funstart.listener;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.FunstartPlugin.PendingChatAction;
import moe.hinakusoft.funstart.FunstartPlugin.ShareData;
import moe.hinakusoft.funstart.FunstartPlugin.TeleportData;
import moe.hinakusoft.funstart.FunstartPlugin.TpaResponseData;
import moe.hinakusoft.funstart.manager.TpaManager;
import moe.hinakusoft.funstart.manager.WarpManager.ShareRequest;
import moe.hinakusoft.funstart.model.PlayerData;
import moe.hinakusoft.funstart.model.WarpPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.UUID;

public class ChatListener implements Listener {

    private final FunstartPlugin plugin;

    public ChatListener(FunstartPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        String msg = event.getMessage().trim();

        // Check pending chat action first
        PendingChatAction action = plugin.removePendingChatAction(player.getUniqueId());

        if (action != null) {
            event.setCancelled(true);

            if (System.currentTimeMillis() > action.expireTime) {
                player.sendMessage("§c操作已超时");
                return;
            }

        if (action.type == FunstartPlugin.PendingChatAction.Type.TPA_RESPONSE) {
            if (!msg.equals("1") && !msg.equals("2")) {
                player.sendMessage("§c请输入 1 或 2");
                return;
            }
            if (!(action.data instanceof TpaResponseData trd)) return;
            boolean accept = msg.equals("1");
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                TpaManager.TpaRequest request = plugin.getTpaManager().getRequestByRequester(trd.requester());
                if (request == null) {
                    player.sendMessage("§c该传送请求已过期");
                    return;
                }
                Player requester = Bukkit.getPlayer(trd.requester());
                if (accept) {
                    if (requester == null || !requester.isOnline()) {
                        player.sendMessage("§c对方已离线");
                    } else if (request.getType() == TpaManager.TpaType.TPAH) {
                        player.teleportAsync(requester.getLocation());
                        player.sendMessage("§e[Funstart] §a已传送到 §b" + requester.getName() + " §a身边");
                        requester.sendMessage("§e[Funstart] §b" + player.getName() + " §a已接受邀请, 传送至你身边");
                    } else {
                        requester.teleportAsync(player.getLocation());
                        player.sendMessage("§e[Funstart] §a已同意传送请求");
                        requester.sendMessage("§e[Funstart] §a已传送至 §b" + player.getName() + " §a身边");
                    }
                } else {
                    player.sendMessage("§e[Funstart] §c已拒绝");
                    if (requester != null && requester.isOnline()) {
                        requester.sendMessage("§e[Funstart] §b" + player.getName() + " §c已拒绝");
                    }
                }
                if (request.getTaskId() >= 0) {
                    Bukkit.getScheduler().cancelTask(request.getTaskId());
                }
                plugin.getTpaManager().removeRequest(trd.requester());
            });
            return;
        }

        if (action.type == FunstartPlugin.PendingChatAction.Type.BANK_AMOUNT) {
            double amount;
            try {
                amount = Double.parseDouble(msg);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage("§c输入无效的数量");
                return;
            }
            if (!(action.data instanceof String targetUuidStr)) return;
            UUID targetUuid;
            try {
                targetUuid = UUID.fromString(targetUuidStr);
            } catch (IllegalArgumentException e) {
                player.sendMessage("§c目标无效");
                return;
            }
            Player target = Bukkit.getPlayer(targetUuid);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§c目标玩家已离线");
                return;
            }
            String targetName = target.getName();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                GuiListener.openBankConfirm(player, plugin, targetUuid, targetName, amount);
            });
            return;
        }

        if (action.type == FunstartPlugin.PendingChatAction.Type.ENCHANT_CONFIRM) {
            if (!msg.equalsIgnoreCase("f")) {
                player.sendMessage("§c请输入 §ef §c以确认附魔");
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                moe.hinakusoft.funstart.listener.EnchantGuiListener.openMaterialSelection(player, plugin);
            });
            return;
        }

        if (action.type == FunstartPlugin.PendingChatAction.Type.DISENCHANT_CONFIRM) {
            if (!msg.equalsIgnoreCase("f")) {
                player.sendMessage("§c请输入 §ef §c以确认驱魔");
                return;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType().isAir()) {
                player.sendMessage("§c请手持要驱魔的物品");
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                moe.hinakusoft.funstart.listener.PanelClockListener.openDisenchantFor(player, plugin);
            });
            return;
        }

        if (action.type == FunstartPlugin.PendingChatAction.Type.DATA_EDIT_CONFIRM) {
            if (!msg.equalsIgnoreCase("f")) {
                player.sendMessage("§c请输入 §ef §c以确认数据修改");
                return;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType().isAir()) {
                player.sendMessage("§c请手持要修改的物品");
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                moe.hinakusoft.funstart.listener.DataEditGuiListener.openDataEditor(player, plugin);
            });
            return;
        }

        if (action.type == FunstartPlugin.PendingChatAction.Type.NBT_EDIT_CONFIRM) {
            if (!msg.equalsIgnoreCase("f")) {
                player.sendMessage("§c请输入 §ef §c以确认NBT修改");
                return;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType().isAir()) {
                player.sendMessage("§c请手持要修改的物品");
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                moe.hinakusoft.funstart.listener.NbtEditGuiListener.openNbtEditor(player, plugin);
            });
            return;
        }

        if (action.type == FunstartPlugin.PendingChatAction.Type.NBT_ADD_KEY) {
            if (msg.length() < 1 || msg.length() > 32 || !msg.matches("[a-zA-Z0-9_]+")) {
                player.sendMessage("§c标签名必须为1-32个字母数字或下划线");
                return;
            }
            String key = msg.trim();
            moe.hinakusoft.funstart.listener.NbtEditGuiListener.getPendingTagKeys().put(player.getUniqueId(), key);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.addPendingChatAction(player.getUniqueId(),
                    FunstartPlugin.PendingChatAction.Type.NBT_ADD_VALUE, null);
                player.sendMessage("§b[NBT] §a请输入标签值 (文本):");
                player.sendMessage("§7输入任意内容作为值, 输入 §ec §7取消");
            });
            return;
        }

        if (action.type == FunstartPlugin.PendingChatAction.Type.NBT_ADD_VALUE) {
            if (msg.equalsIgnoreCase("c")) {
                moe.hinakusoft.funstart.listener.NbtEditGuiListener.getPendingTagKeys().remove(player.getUniqueId());
                player.sendMessage("§b[NBT] §c已取消添加标签");
                return;
            }
            String key = moe.hinakusoft.funstart.listener.NbtEditGuiListener.getPendingTagKeys().remove(player.getUniqueId());
            if (key == null) {
                player.sendMessage("§c操作已失效, 请重新开始");
                return;
            }
            String value = msg.trim();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage("§c手持物品不能为空");
                    return;
                }
                moe.hinakusoft.funstart.listener.NbtEditGuiListener.addTag(held, plugin, key, value);
                player.getInventory().setItemInMainHand(held);
                plugin.getLogger().info("[Admin] " + player.getName() + " 添加了NBT标签 " + key + "=" + value);
                player.sendMessage("§b[NBT] §a已添加标签: §e" + key + " §7= §f" + value);
                moe.hinakusoft.funstart.listener.NbtEditGuiListener.openNbtEditor(player, plugin);
            });
            return;
        }

        // Market listing flow
        if (action.type == FunstartPlugin.PendingChatAction.Type.MARKET_LIST_QTY
            || action.type == FunstartPlugin.PendingChatAction.Type.MARKET_LIST_PRICE
            || action.type == FunstartPlugin.PendingChatAction.Type.MARKET_LIST_DURATION) {
            MarketGuiListener listener = plugin.getMarketGuiListener();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                listener.handleListingChat(player, msg, action);
            });
            return;
        }

        if (action.type == FunstartPlugin.PendingChatAction.Type.MARKET_BUY_QTY) {
            MarketGuiListener listener = plugin.getMarketGuiListener();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                listener.handleBuyChat(player, msg, action);
            });
            return;
        }

        switch (action.type) {
            case ADD_WARP -> {
                plugin.getPendingWarpCreation().remove(player.getUniqueId());
                String name = msg;
                if (name.length() < 1 || name.length() > 20) {
                    player.sendMessage("§c名称长度需在1-20字之间");
                    return;
                }
                String finalName = name;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (plugin.getWarpManager().warpExists(finalName, player.getUniqueId())) {
                        player.sendMessage("§c已存在同名传送点");
                        return;
                    }
                    // Open confirm GUI instead of directly creating
                    moe.hinakusoft.funstart.listener.PanelClockListener.openWarpCreateConfirm(player, plugin, finalName);
                });
            }
            case TELEPORT_CONFIRM -> {
                if (!msg.equals("1")) {
                    player.sendMessage("§c已取消传送");
                    return;
                }
                if (!(action.data instanceof TeleportData td)) return;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    WarpPoint wp = plugin.getWarpManager().getWarp(td.warpId);
                    if (wp == null) {
                        player.sendMessage("§c该传送点已不存在");
                        return;
                    }
                    World world = Bukkit.getWorld(wp.getWorldName());
                    if (world == null) {
                        player.sendMessage("§c传送点所在世界不可用");
                        return;
                    }
                    Location loc = new Location(world, wp.getX(), wp.getY(), wp.getZ(), wp.getYaw(), wp.getPitch());
                    PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
                    if (data.getPoints() < td.cost) {
                        player.sendMessage("§c点数不足! 需要 §e" + PlayerData.fmt(td.cost) + " §c点 (当前: §e" + PlayerData.fmt(data.getPoints()) + "§c)");
                        return;
                    }
                    data.deductPoints(td.cost);
                    double remaining = data.getPoints();
                    player.teleportAsync(loc).thenAccept(success -> {
                        if (success) {
                            player.sendMessage("§e[Funstart] §a已传送到 §b" + wp.getName() + " §a, 消耗 §e" + PlayerData.fmt(td.cost) + " §a点, 剩余 §e" + PlayerData.fmt(remaining) + " §a点");
                        } else {
                            data.addPoints(td.cost);
                            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
                            player.sendMessage("§c传送失败，点数已退还");
                        }
                    });
                });
            }
            case SHARE_RESPONSE -> {
                if (!(action.data instanceof ShareData sd)) return;
                if (msg.equals("1")) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        ShareRequest sr = plugin.getWarpManager().acceptShare(player.getUniqueId(), sd.from);
                        if (sr == null) {
                            player.sendMessage("§c该分享请求已失效");
                            return;
                        }
                        WarpPoint wp = plugin.getWarpManager().getWarp(sr.getWarpId());
                        if (wp == null) {
                            player.sendMessage("§c该传送点已不存在");
                            return;
                        }
                        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
                        data.getAcceptedShares().add(sr.getWarpId());
                        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
                        player.sendMessage("§e[Funstart] §a已接受分享的传送点 §b" + wp.getName());
                        Player from = Bukkit.getPlayer(sd.from);
                        if (from != null && from.isOnline()) {
                            from.sendMessage("§e[Funstart] §b" + player.getName() + " §a已接受你分享的传送点 §b" + wp.getName());
                        }
                    });
                } else {
                    plugin.getWarpManager().denyShare(player.getUniqueId(), sd.from);
                    player.sendMessage("§c已拒绝分享请求");
                    Player from = Bukkit.getPlayer(sd.from);
                    if (from != null && from.isOnline()) {
                        from.sendMessage("§e[Funstart] §b" + player.getName() + " §c已拒绝你的分享请求");
                    }
                }
            }
            case DELETE_WARP -> {
                String name = msg.trim();
                if (name.length() < 1 || name.length() > 20) {
                    player.sendMessage("§c名称长度需在1-20字之间");
                    return;
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    String id = name.toLowerCase() + "_" + player.getUniqueId();
                    WarpPoint wp = plugin.getWarpManager().getWarp(id);
                    if (wp == null) {
                        player.sendMessage("§c未找到你的传送点 §e" + name);
                        return;
                    }
                    plugin.getWarpManager().deleteWarp(id, player.getUniqueId());
                    plugin.getPlayerDataManager().cleanAcceptedShares(wp.getId());
                    player.sendMessage("§e[Funstart] §a已删除传送点 §b" + wp.getName());
                });
            }
            case ALL_FIX_CONFIRM -> {
                if (!msg.equals("1")) {
                    player.sendMessage("§c已取消全部修复");
                    return;
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    ItemStack[] contents = player.getInventory().getContents();
                    double totalCost = 0.0;
                    int count = 0;
                    for (ItemStack item : contents) {
                        if (item == null || item.getType().isAir()) continue;
                        if (!(item.getItemMeta() instanceof Damageable damageable)) continue;
                        int maxDurability = item.getType().getMaxDurability();
                        if (maxDurability <= 0) continue;
                        int currentDurability = maxDurability - damageable.getDamage();
                        if ((double) currentDurability / maxDurability >= 0.9) continue;
                        totalCost += (double) (maxDurability - currentDurability) / 50.0;
                        count++;
                    }
                    if (count == 0) {
                        player.sendMessage("§c没有需要修复的物品");
                        return;
                    }
                    PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
                    if (data.getPoints() < totalCost) {
                        player.sendMessage("§c点数不足! 需要 " + PlayerData.fmt(totalCost) + " 点");
                        return;
                    }
                    for (ItemStack item : contents) {
                        if (item == null || item.getType().isAir()) continue;
                        if (!(item.getItemMeta() instanceof Damageable damageable)) continue;
                        int maxDurability = item.getType().getMaxDurability();
                        if (maxDurability <= 0) continue;
                        int currentDurability = maxDurability - damageable.getDamage();
                        if ((double) currentDurability / maxDurability >= 0.9) continue;
                        damageable.setDamage(0);
                        item.setItemMeta(damageable);
                    }
                    data.deductPoints(totalCost);
                    plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
                    player.sendMessage("§e[Funstart] §a已修复 §b" + count + " §a个物品, 消耗 §e" + PlayerData.fmt(totalCost) + " §a点, 剩余 §e" + PlayerData.fmt(data.getPoints()) + " §a点");
                });
            }
            case CLAIM_POSITION -> {
                String finalMsg = msg;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getClaimManager().handleClaimChat(player, finalMsg, action);
                });
            }
            case ADD_TRUSTED -> {
                String trustedName = msg.trim();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player target = Bukkit.getPlayerExact(trustedName);
                    if (target == null) {
                        player.sendMessage("§c未找到在线玩家: §e" + trustedName);
                        return;
                    }
                    UUID uid = player.getUniqueId();
                    if (plugin.getClaimManager().addTrusted(uid, target.getUniqueId())) {
                        player.sendMessage("§e[Funstart] §a已将 §b" + target.getName() + " §a添加为信任人");
                        target.sendMessage("§e[Funstart] §b" + player.getName() + " §a将你添加为领地信任人");
                    } else {
                        player.sendMessage("§c添加信任人失败 (可能你没有领地或已在信任列表中)");
                    }
                });
            }
            case CHANGE_PASSWORD -> {
                if (msg.length() < 4) {
                    player.sendMessage("§c密码长度不能少于4位, 请重新输入:");
                    return;
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    var authManager = plugin.getAuthManager();
                    if (authManager != null && authManager.isRegistered(player.getUniqueId())) {
                        authManager.changePassword(player.getUniqueId(), msg);
                        player.sendMessage("§a密码修改成功");
                    } else {
                        player.sendMessage("§c操作失败, 你尚未注册");
                    }
                });
            }
            case ADMIN_RESET_PASSWORD -> {
                if (msg.length() < 4) {
                    player.sendMessage("§c密码长度不能少于4位, 请重新输入:");
                    return;
                }
                if (!(action.data instanceof UUID targetUuid)) return;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    var authManager = plugin.getAuthManager();
                    if (authManager != null && authManager.resetPassword(targetUuid, msg)) {
                        String name = Bukkit.getOfflinePlayer(targetUuid).getName();
                        player.sendMessage("§c[管理] §a已重置 §b" + (name != null ? name : "未知") + " §a的密码");
                        plugin.getLogger().info("[Admin] " + player.getName() + " 重置了 " + name + " 的密码");
                    } else {
                        player.sendMessage("§c操作失败, 该玩家尚未注册");
                    }
                });
            }
            default -> {}
        }
        }

        // If no pending chat action, check claim session for 1/2
        if ((msg.equals("1") || msg.equals("2")) && plugin.getClaimManager().hasActiveSession(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getClaimManager().handleGlobalClaimChat(player, msg);
            });
        }

        // Prepend rank title to chat message (only if not cancelled)
        if (!event.isCancelled()) {
            String rankDisplay = plugin.getRankManager().getRankDisplay(player.getUniqueId());
            if (!rankDisplay.isEmpty()) {
                event.setFormat(rankDisplay + "§f" + player.getName() + "§7: §f%s");
            }
        }
    }

}
