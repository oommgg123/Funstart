package moe.hinakusoft.funstart.listener;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.manager.MarketManager;
import moe.hinakusoft.funstart.model.ItemStackData;
import moe.hinakusoft.funstart.model.MarketItem;
import moe.hinakusoft.funstart.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MarketGuiListener implements Listener {

    private final FunstartPlugin plugin;
    private final MarketManager marketManager;
    private final Map<UUID, ListingSession> listingSessions = new HashMap<>();
    private final Map<UUID, BuyingSession> buyingSessions = new HashMap<>();

    public MarketGuiListener(FunstartPlugin plugin) {
        this.plugin = plugin;
        this.marketManager = plugin.getMarketManager();
    }

    // ========== Open Market GUI ==========

    public static final int PAGE_SIZE = 45;

    public static void openMarket(Player player, FunstartPlugin plugin) {
        openMarket(player, plugin, 0);
    }

    private static void openMarket(Player player, FunstartPlugin plugin, int page) {
        MarketManager mm = plugin.getMarketManager();
        List<MarketItem> all = mm.getItems();
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        Inventory inv = Bukkit.createInventory(new MarketHolder(player, page), 54,
            "§6市场 (" + (page + 1) + "/" + totalPages + ")");

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < all.size(); i++) {
            MarketItem mi = all.get(start + i);
            ItemStack display = mi.getItemData().toItemStack().clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            lore.add("");
            if (mi.getType() == MarketItem.Type.OP_SHOP) {
                lore.add("§6§l[服务器商店]");
            } else {
                lore.add("§b§l[玩家出售] §7" + mi.getSellerName());
            }
            lore.add("§7价格: §e" + String.format("%.1f", mi.getCurrentPrice()) + " §7点数/个");
            lore.add("§7库存: §e" + mi.getStock() + " §7个");

            // Price history
            if (mi.getPriceUpCount() > 0 || mi.getPriceDownCount() > 0) {
                lore.add("§7↑涨: §c" + mi.getPriceUpCount() + "§7/3  ↓降: §a" + mi.getPriceDownCount() + "§7/3");
            }
            if (mi.getType() == MarketItem.Type.OP_SHOP) {
                lore.add("§7基础价: §e" + String.format("%.1f", mi.getBasePrice()));
            } else {
                long remaining = mi.getExpireTime() > 0 ? (mi.getExpireTime() - System.currentTimeMillis()) / 3600000L : -1;
                if (remaining > 0) lore.add("§7剩余: §e" + remaining + " §7小时");
                else lore.add("§7永久上架");
            }
            lore.add("§a点击购买");

            meta.setLore(lore);
            display.setItemMeta(meta);
            inv.setItem(i, display);
        }

        // Navigation
        if (page > 0) inv.setItem(45, makeItem(Material.ARROW, "§a上一页"));
        if (page < totalPages - 1) inv.setItem(53, makeItem(Material.ARROW, "§a下一页"));

        // List item button
        inv.setItem(49, makeItem(Material.HOPPER, "§e§l上架物品",
            "§7手持要出售的物品",
            "§7蹲下后按照提示操作"));

        // Claim button (slot 48)
        inv.setItem(48, makeItem(Material.CHEST, "§6§l领取奖励",
            "§7领取待领的点数和物品"));

        inv.setItem(50, makeItem(Material.BARRIER, "§c关闭"));
        player.openInventory(inv);
    }

    // ========== Market GUI Click ==========

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MarketHolder holder)) return;
        Player player = (Player) event.getWhoClicked();
        if (!holder.getPlayer().equals(player)) return;
        event.setCancelled(true);

        // Confirm GUI (page == -1)
        if (holder.getPage() == -1) {
            handleConfirmClick(player, event.getRawSlot());
            return;
        }

        // Claim GUI (page == -2)
        if (holder.getPage() == -2) {
            handleClaimClick(player, event.getRawSlot());
            return;
        }

        int slot = event.getRawSlot();
        int page = holder.getPage();

        if (slot >= 0 && slot < PAGE_SIZE) {
            handleItemClick(player, page, slot);
        } else if (slot == 45 && page > 0) {
            openMarket(player, plugin, page - 1);
        } else if (slot == 53) {
            openMarket(player, plugin, page + 1);
        } else if (slot == 48) {
            openClaimGui(player, plugin);
        } else if (slot == 49) {
            // Start listing flow
            player.closeInventory();
            plugin.addPendingChatAction(player.getUniqueId(),
                FunstartPlugin.PendingChatAction.Type.MARKET_LIST_QTY, null, 30000L);
            player.sendMessage("§6[市场] 请输入上架数量:");
        } else if (slot == 50) {
            player.closeInventory();
        }
    }

    private void handleConfirmClick(Player player, int slot) {
        if (slot == 11) {
            BuyingSession bs = buyingSessions.get(player.getUniqueId());
            if (bs == null) { player.closeInventory(); return; }
            player.closeInventory();
            executePurchase(player, bs);
        } else if (slot == 15 || slot == 26) {
            player.closeInventory();
            buyingSessions.remove(player.getUniqueId());
            player.sendMessage("§c已取消购买");
        }
    }

    private void handleItemClick(Player player, int page, int slot) {
        MarketManager mm = plugin.getMarketManager();
        List<MarketItem> all = mm.getItems();
        int index = page * PAGE_SIZE + slot;
        if (index >= all.size()) return;

        MarketItem mi = all.get(index);
        if (mi.getStock() <= 0) {
            player.sendMessage("§c该物品已售罄");
            return;
        }

        PlayerData pd = plugin.getPlayerDataManager().getPlayerData(player);
        double price = mi.getCurrentPrice();
        int maxBuy = Math.min(mi.getStock(), (int) (pd.getPoints() / price));
        if (maxBuy <= 0) {
            player.sendMessage("§c点数不足，无法购买该物品");
            return;
        }

        // Start buy flow
        buyingSessions.put(player.getUniqueId(), new BuyingSession(mi, index));
        player.closeInventory();
        plugin.addPendingChatAction(player.getUniqueId(),
            FunstartPlugin.PendingChatAction.Type.MARKET_BUY_QTY, null, 30000L);
        player.sendMessage("§6[市场] " + mi.getItemData().toItemStack().getType().name()
            + " §e" + String.format("%.1f", price) + " §6点数/个");
        player.sendMessage("§6请输入购买数量 (最多 §e" + maxBuy + " §6个, 0=取消):");
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MarketHolder) {
            event.setCancelled(true);
        }
    }

    // ========== Chat-based Listing Flow (handled via pendingChatActions) ==========

    public static class ListingSession {
        public ItemStack item;
        public int quantity;
        public double pricePerUnit;
        public long durationHours;
        public int step; // 0=qty, 1=price, 2=duration
    }

    public static class BuyingSession {
        public MarketItem marketItem;
        public int listIndex;
        public int quantity;
        public BuyingSession(MarketItem mi, int idx) { this.marketItem = mi; this.listIndex = idx; }
    }

    public void handleListingChat(Player player, String msg, FunstartPlugin.PendingChatAction action) {
        UUID uid = player.getUniqueId();
        ListingSession session = listingSessions.computeIfAbsent(uid, k -> new ListingSession());

        if (session.step == 0) {
            // Quantity
            try {
                int qty = Integer.parseInt(msg.trim());
                if (qty <= 0) { player.sendMessage("§c已取消上架"); cleanupListing(uid); return; }
                // Check player has enough items
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType() == Material.AIR) { player.sendMessage("§c请手持要出售的物品"); cleanupListing(uid); return; }
                if (held.getAmount() < qty) { player.sendMessage("§c你没有足够的物品"); cleanupListing(uid); return; }

                session.item = held.clone();
                session.item.setAmount(qty);
                session.quantity = qty;
                session.step = 1;

                // Show market price info
                String priceInfo = plugin.getMarketManager().getMarketPriceInfo((String) new ItemStackData(held).serialize().get("s"));
                player.sendMessage(priceInfo);
                player.sendMessage("§6请输入每个的价格 (点数):");
                plugin.addPendingChatAction(uid, FunstartPlugin.PendingChatAction.Type.MARKET_LIST_PRICE, null, 30000L);
            } catch (NumberFormatException e) {
                player.sendMessage("§c请输入有效数字");
            }
        } else if (session.step == 1) {
            // Price
            try {
                double price = Double.parseDouble(msg.trim());
                if (price <= 0) { player.sendMessage("§c已取消上架"); cleanupListing(uid); return; }
                session.pricePerUnit = price;
                session.step = 2;
                player.sendMessage("§6请输入挂售时长 (小时, 0=永久):");
                plugin.addPendingChatAction(uid, FunstartPlugin.PendingChatAction.Type.MARKET_LIST_DURATION, null, 30000L);
            } catch (NumberFormatException e) {
                player.sendMessage("§c请输入有效数字");
            }
        } else if (session.step == 2) {
            // Duration
            try {
                long hours = Long.parseLong(msg.trim());
                if (hours < 0) { player.sendMessage("§c已取消上架"); cleanupListing(uid); return; }
                session.durationHours = hours;

                // Create listing
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType() == Material.AIR || !held.isSimilar(session.item)) {
                    player.sendMessage("§c物品已变更，上架取消");
                    cleanupListing(uid);
                    return;
                }
                // Remove items from player
                held.setAmount(held.getAmount() - session.quantity);
                if (held.getAmount() <= 0) {
                    player.getInventory().setItemInMainHand(null);
                }

                MarketItem mi = MarketItem.createPlayerListing(
                    uid, player.getName(),
                    new ItemStackData(session.item),
                    session.pricePerUnit, session.quantity, hours);
                plugin.getMarketManager().addItem(mi);
                player.sendMessage("§a[市场] 上架成功! §e" + session.quantity + " §a个 "
                    + session.item.getType().name() + " §e" + String.format("%.1f", session.pricePerUnit) + " §a点数/个");
                cleanupListing(uid);
            } catch (NumberFormatException e) {
                player.sendMessage("§c请输入有效数字");
            }
        }
    }

    private void cleanupListing(UUID uid) {
        listingSessions.remove(uid);
        plugin.removePendingChatAction(uid);
    }

    public void handleBuyChat(Player player, String msg, FunstartPlugin.PendingChatAction action) {
        UUID uid = player.getUniqueId();
        BuyingSession bs = buyingSessions.get(uid);
        if (bs == null) return;

        try {
            int qty = Integer.parseInt(msg.trim());
            if (qty <= 0) { player.sendMessage("§c已取消购买"); buyingSessions.remove(uid); return; }

            List<MarketItem> all = plugin.getMarketManager().getItems();
            if (bs.listIndex >= all.size() || !all.get(bs.listIndex).equals(bs.marketItem)) {
                player.sendMessage("§c该物品已下架");
                buyingSessions.remove(uid);
                return;
            }

            MarketItem mi = all.get(bs.listIndex);
            if (qty > mi.getStock()) qty = mi.getStock();

            PlayerData pd = plugin.getPlayerDataManager().getPlayerData(player);
            double totalCost = mi.getCurrentPrice() * qty;
            if (pd.getPoints() < totalCost) {
                int maxCanAfford = (int) (pd.getPoints() / mi.getCurrentPrice());
                if (maxCanAfford <= 0) {
                    player.sendMessage("§c点数不足");
                    buyingSessions.remove(uid);
                    return;
                }
                qty = maxCanAfford;
                totalCost = mi.getCurrentPrice() * qty;
            }

            bs.quantity = qty;

            // Open confirm GUI
            openBuyConfirm(player, mi, qty, bs.listIndex, totalCost);
        } catch (NumberFormatException e) {
            player.sendMessage("§c请输入有效数字");
        }
    }

    private void openBuyConfirm(Player player, MarketItem mi, int qty, int index, double totalCost) {
        Inventory inv = Bukkit.createInventory(new MarketHolder(player, -1), 27, "§6确认购买");

        ItemStack display = mi.getItemData().toItemStack().clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add("§7数量: §e" + qty);
        lore.add("§7单价: §e" + String.format("%.1f", mi.getCurrentPrice()));
        lore.add("§7总价: §e" + String.format("%.1f", totalCost));
        meta.setLore(lore);
        display.setItemMeta(meta);
        inv.setItem(4, display);

        inv.setItem(11, makeItem(Material.LIME_DYE, "§a§l确认购买",
            "§7购买 §e" + qty + " §7个",
            "§7花费 §e" + String.format("%.1f", totalCost) + " §7点数"));
        inv.setItem(15, makeItem(Material.RED_DYE, "§c取消"));
        inv.setItem(22, makeItem(Material.PAPER, "§e购买详情",
            "§7物品: " + mi.getItemData().toItemStack().getType().name(),
            "§7卖家: " + mi.getSellerName(),
            "§7数量: §e" + qty,
            "§7总价: §e" + String.format("%.1f", totalCost)));

        player.openInventory(inv);
    }

    private boolean hasFreeSlots(Player player) {
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType().isAir()) {
                return true;
            }
        }
        return false;
    }

    private void executePurchase(Player player, BuyingSession bs) {
        List<MarketItem> all = plugin.getMarketManager().getItems();
        if (bs.listIndex >= all.size()) {
            player.sendMessage("§c该物品已下架");
            buyingSessions.remove(player.getUniqueId());
            return;
        }

        MarketItem current = all.get(bs.listIndex);
        PlayerData pd = plugin.getPlayerDataManager().getPlayerData(player);
        int qty = Math.min(bs.quantity, current.getStock());
        double maxAfford = pd.getPoints() / current.getCurrentPrice();
        if (qty > (int) maxAfford) qty = (int) maxAfford;
        if (qty <= 0) {
            player.sendMessage("§c库存不足或点数不足");
            buyingSessions.remove(player.getUniqueId());
            return;
        }

        // Block if buyer has no free inventory slot
        if (!hasFreeSlots(player)) {
            player.sendMessage("§c背包已满，无法购买");
            buyingSessions.remove(player.getUniqueId());
            return;
        }

        double totalCost = current.getCurrentPrice() * qty;
        pd.deductPoints(totalCost);

        ItemStack bought = current.getItemData().toItemStack().clone();
        bought.setAmount(qty);
        player.getInventory().addItem(bought).values().forEach(left ->
            player.getWorld().dropItemNaturally(player.getLocation(), left));

        current.setStock(current.getStock() - qty);
        current.setSoldQuantity(current.getSoldQuantity() + qty);

        // Pay seller (use pending if offline)
        if (current.getType() == MarketItem.Type.PLAYER_LISTING) {
            double sellerEarn = totalCost * 0.95;
            Player seller = Bukkit.getPlayer(current.getSellerId());
            if (seller != null && seller.isOnline()) {
                PlayerData sellerData = plugin.getPlayerDataManager().getPlayerData(current.getSellerId());
                if (sellerData != null) {
                    sellerData.addPoints(sellerEarn);
                    seller.sendMessage("§e[市场] §a你的物品已售出 §e" + qty + " §a个, 获得 §e"
                        + String.format("%.1f", sellerEarn) + " §a点数 (已扣5%手续费)");
                }
            } else {
                // Seller offline → store in pending
                plugin.getMarketManager().addPendingPoints(current.getSellerId(), sellerEarn);
            }
        }

        plugin.getMarketManager().save();
        player.sendMessage("§a[市场] 购买成功! §e" + qty + " §a个 "
            + current.getItemData().toItemStack().getType().name() + " §e"
            + String.format("%.1f", totalCost) + " §a点数");
        buyingSessions.remove(player.getUniqueId());
    }

    // ========== Cancel listing on sneak unsneak / item change ==========

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) return; // only on unsneak
        UUID uid = event.getPlayer().getUniqueId();
        if (listingSessions.containsKey(uid) && listingSessions.get(uid).step == 0) {
            cleanupListing(uid);
            event.getPlayer().sendMessage("§c已取消上架");
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        if (listingSessions.containsKey(uid) && listingSessions.get(uid).step == 0) {
            cleanupListing(uid);
            event.getPlayer().sendMessage("§c已取消上架 (切换物品)");
        }
    }

    @EventHandler
    public void onPlayerMoveInListing(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        ListingSession session = listingSessions.get(uid);
        if (session == null) return;
        if (session.step > 0) return; // already past item selection

        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
            || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            // Player moved without selecting item
            if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                cleanupListing(uid);
                player.sendMessage("§c已取消上架 (移动)");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        listingSessions.remove(uid);
        buyingSessions.remove(uid);
    }

    // ========== Claim GUI ==========

    public static void openClaimGui(Player player, FunstartPlugin plugin) {
        MarketManager mm = plugin.getMarketManager();
        UUID uid = player.getUniqueId();
        double points = mm.getPendingPointsTotal(uid);
        int itemCount = mm.getPendingItemCount(uid);

        Inventory inv = Bukkit.createInventory(new MarketHolder(player, -2), 27, "§6领取奖励");

        // Claim points
        inv.setItem(11, makeItem(Material.GOLD_INGOT, "§6§l领取点数",
            points > 0 ? "§7待领取: §e" + String.format("%.1f", points) + " §7点数"
                       : "§7暂无待领点数",
            points > 0 ? "§a点击领取" : "§7无"));

        // Claim items
        inv.setItem(13, makeItem(Material.CHEST, "§6§l领取物品",
            itemCount > 0 ? "§7待领取: §e" + itemCount + " §7个物品" : "§7暂无待领物品",
            itemCount > 0 ? "§a点击逐个领取" : "§7无"));

        // Info
        inv.setItem(22, makeItem(Material.PAPER, "§e领取说明",
            "§7- 出售物品所得点数(卖家离线时暂存)",
            "§7- 过期下架物品暂存于此",
            "§7- 领取物品时背包满则留在待领"));

        inv.setItem(26, makeItem(Material.ARROW, "§a返回市场"));
        inv.setItem(18, makeItem(Material.BARRIER, "§c关闭"));

        player.openInventory(inv);
    }

    private void handleClaimClick(Player player, int slot) {
        UUID uid = player.getUniqueId();
        MarketManager mm = plugin.getMarketManager();

        if (slot == 11) {
            // Claim points
            double amount = mm.claimPendingPoints(uid);
            if (amount > 0) {
                PlayerData pd = plugin.getPlayerDataManager().getPlayerData(player);
                pd.addPoints(amount);
                plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
                player.sendMessage("§a[市场] 已领取 §e" + String.format("%.1f", amount) + " §a点数");
            } else {
                player.sendMessage("§c没有待领取的点数");
            }
            openClaimGui(player, plugin);
        } else if (slot == 13) {
            // Claim one item
            ItemStackData data = mm.claimOnePendingItem(uid);
            if (data != null) {
                ItemStack item = data.toItemStack();
                // Check if inventory has space
                boolean hasSpace = false;
                for (int i = 0; i < 36; i++) {
                    if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType().isAir()) {
                        hasSpace = true;
                        break;
                    }
                }
                if (!hasSpace) {
                    // Put back if inventory full
                    mm.addPendingItem(uid, data);
                    player.sendMessage("§c背包已满，物品留在待领取中");
                } else {
                    player.getInventory().addItem(item).values().forEach(left ->
                        player.getWorld().dropItemNaturally(player.getLocation(), left));
                    player.sendMessage("§a[市场] 已领取 §e" + item.getType().name() + " §a×" + item.getAmount());
                }
            } else {
                player.sendMessage("§c没有待领取的物品");
            }
            openClaimGui(player, plugin);
        } else if (slot == 26) {
            openMarket(player, plugin);
        } else if (slot == 18) {
            player.closeInventory();
        }
    }

    // ========== Helpers ==========

    private static ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static class MarketHolder implements InventoryHolder {
        private final Player player;
        private final int page;
        public MarketHolder(Player player, int page) { this.player = player; this.page = page; }
        public Player getPlayer() { return player; }
        public int getPage() { return page; }
        @Override public Inventory getInventory() { return null; }
    }
}
