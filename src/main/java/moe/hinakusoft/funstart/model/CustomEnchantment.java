package moe.hinakusoft.funstart.model;

import java.util.List;

public enum CustomEnchantment {
    MINER_AGILITY("矿工之敏", "挖掘矿石概率双倍掉落"),
    LIFE_STEAL("生命恢复", "击杀生物概率恢复生命"),
    FROST_TRAIL("霜痕", "行走概率减速附近生物"),
    ENDER_MOVE("龟壳移动", "受伤后获得抗性提升"),
    EXPLOSIVE_THROW("爆裂投掷", "投掷物击中产生爆炸"),
    LEAF_HIDDEN("叶隐", "树叶下隐身并缓慢回复"),
    TIME_SLOW("时间减缓", "攻击概率减缓目标"),
    SHIKI_NATSUME("四季夏目", "四季夏目的力量");

    private final String displayName;
    private final String description;

    CustomEnchantment(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getKey() { return name().toLowerCase(); }

    public static final List<CustomEnchantment> VALUES = List.of(values());
    public static final List<CustomEnchantment> GUI_VALUES = List.of(
        MINER_AGILITY, LIFE_STEAL, FROST_TRAIL, ENDER_MOVE,
        EXPLOSIVE_THROW, LEAF_HIDDEN, TIME_SLOW
    );
}
