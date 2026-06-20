package moe.hinakusoft.funstart.model;

public class PlayerRank {
    private final String title;
    private final RankType type;

    public PlayerRank(String title, RankType type) {
        this.title = title;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public RankType getType() {
        return type;
    }

    public String getFormatted() {
        if (title == null || title.isEmpty()) return "";
        return type.getColor() + title;
    }

    public enum RankType {
        DEFAULT("default", "§a"),
        ZANZHU("zanzhu", "§a"),
        CUSTOM("custom", "§d");

        private final String key;
        private final String color;

        RankType(String key, String color) {
            this.key = key;
            this.color = color;
        }

        public static RankType fromKey(String key) {
            for (RankType t : values()) {
                if (t.key.equalsIgnoreCase(key)) return t;
            }
            return DEFAULT;
        }

        public String getKey() {
            return key;
        }

        public String getColor() {
            return color;
        }
    }
}
