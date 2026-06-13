package moe.hinakusoft.funstart.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private double points;
    private boolean autoHeal;
    private long lastAutoHealTime;
    private double totalDamageDealt;
    private boolean chainEnabled;
    private boolean harvestEnabled;
    private int maxChainBlocks;
    private String lastDeathLocation;
    private Set<String> acceptedShares;
    private boolean autoFix;
    private long lastAutoFixTime;
    private double totalPointsEarned;
    private int[] taskProgress = new int[3];
    private boolean[] taskCompleted = new boolean[3];
    private String taskDate = "";
    private boolean hasPanelClock = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.points = 0.0;
        this.autoHeal = false;
        this.lastAutoHealTime = 0L;
        this.totalDamageDealt = 0.0;
        this.chainEnabled = false;
        this.harvestEnabled = false;
        this.maxChainBlocks = 32;
        this.acceptedShares = new HashSet<>();
        this.autoFix = false;
        this.lastAutoFixTime = 0L;
    }

    public UUID getUuid() { return uuid; }

    public double getPoints() { return points; }
    public void setPoints(double points) { this.points = points; }

    public void addPoints(double amount) {
        this.points += amount;
        this.totalPointsEarned += amount;
        moe.hinakusoft.funstart.manager.DailyTaskManager.incrementTask(this, 2, (int) amount);
    }

    public boolean deductPoints(double amount) {
        if (this.points < amount) return false;
        this.points -= amount;
        return true;
    }

    public static String fmt(double value) {
        return String.format("%.3f", value);
    }

    public boolean isAutoHeal() { return autoHeal; }
    public void setAutoHeal(boolean autoHeal) { this.autoHeal = autoHeal; }
    public long getLastAutoHealTime() { return lastAutoHealTime; }
    public void setLastAutoHealTime(long lastAutoHealTime) { this.lastAutoHealTime = lastAutoHealTime; }

    public double getTotalDamageDealt() { return totalDamageDealt; }
    public void setTotalDamageDealt(double totalDamageDealt) { this.totalDamageDealt = totalDamageDealt; }
    public void addDamage(double damage) { this.totalDamageDealt += damage; }

    public boolean isChainEnabled() { return chainEnabled; }
    public void setChainEnabled(boolean chainEnabled) { this.chainEnabled = chainEnabled; }
    public boolean isHarvestEnabled() { return harvestEnabled; }
    public void setHarvestEnabled(boolean harvestEnabled) { this.harvestEnabled = harvestEnabled; }
    public int getMaxChainBlocks() { return maxChainBlocks; }
    public void setMaxChainBlocks(int maxChainBlocks) { this.maxChainBlocks = maxChainBlocks; }

    public String getLastDeathLocation() { return lastDeathLocation; }
    public void setLastDeathLocation(String lastDeathLocation) { this.lastDeathLocation = lastDeathLocation; }
    public Set<String> getAcceptedShares() { return acceptedShares; }
    public void setAcceptedShares(Set<String> acceptedShares) { this.acceptedShares = acceptedShares; }

    public boolean isAutoFix() { return autoFix; }
    public void setAutoFix(boolean autoFix) { this.autoFix = autoFix; }
    public long getLastAutoFixTime() { return lastAutoFixTime; }
    public void setLastAutoFixTime(long lastAutoFixTime) { this.lastAutoFixTime = lastAutoFixTime; }

    public double getTotalPointsEarned() { return totalPointsEarned; }
    public void setTotalPointsEarned(double totalPointsEarned) { this.totalPointsEarned = totalPointsEarned; }

    public int[] getTaskProgress() { return taskProgress; }
    public void setTaskProgress(int[] taskProgress) { this.taskProgress = taskProgress; }
    public boolean[] getTaskCompleted() { return taskCompleted; }
    public void setTaskCompleted(boolean[] taskCompleted) { this.taskCompleted = taskCompleted; }
    public String getTaskDate() { return taskDate; }
    public void setTaskDate(String taskDate) { this.taskDate = taskDate; }

    public boolean hasPanelClock() { return hasPanelClock; }
    public void setHasPanelClock(boolean hasPanelClock) { this.hasPanelClock = hasPanelClock; }
}
