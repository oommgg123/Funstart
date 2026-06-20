package moe.hinakusoft.funstart.model;

public enum FeatureFlag {
    ENCHANT("附魔"),
    DISENCHANT("驱魔"),
    WARP("传送点"),
    CLAIM("圈地"),
    MARKET("市场"),
    PRANK("整蛊"),
    ADMIN("管理"),
    MAIN_MENU("主菜单"),
    CHAIN("连锁"),
    HARVEST("丰收"),
    REPAIR("修复相关"),
    DAILY_TASK("每日任务"),
    CUSTOM_ITEM("自定义物品"),
    LOG("日志"),
    MULTITHREAD("多线程功能");

    private final String displayName;

    FeatureFlag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getKey() {
        return name().toLowerCase();
    }
}
