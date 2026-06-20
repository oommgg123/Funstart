package moe.hinakusoft.funstart.manager;

import moe.hinakusoft.funstart.model.PlayerData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DailyTaskManager {

    public static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    public static final List<TaskDef> TASKS = List.of(
        new TaskDef("挖掘方块", "挖掘指定数量的方块", 300, 30.0),
        new TaskDef("击杀生物", "击杀指定数量的生物", 15, 25.0),
        new TaskDef("已弃用", "Unknown", 0, 0.0)
    );

    public record TaskDef(String name, String description, int target, double reward) {}

    public static String getTodayDate() {
        return DATE_FMT.format(new Date());
    }

    public static void checkAndReset(PlayerData data) {
        String today = getTodayDate();
        if (!today.equals(data.getTaskDate())) {
            data.setTaskDate(today);
            data.setTaskProgress(new int[3]);
            data.setTaskCompleted(new boolean[3]);
        }
    }

    public static void incrementTask(PlayerData data, int index, int amount) {
        checkAndReset(data);
        if (index < 0 || index >= 3) return;
        if (data.getTaskCompleted()[index]) return;
        int[] progress = data.getTaskProgress();
        progress[index] = Math.min(progress[index] + amount, TASKS.get(index).target);
        if (progress[index] >= TASKS.get(index).target) {
            data.getTaskCompleted()[index] = true;
        }
    }

    public static boolean claimReward(PlayerData data, int index) {
        checkAndReset(data);
        if (index < 0 || index >= 3) return false;
        if (!data.getTaskCompleted()[index]) return false;
        data.getTaskCompleted()[index] = false;
        data.getTaskProgress()[index] = 0;
        data.addPoints(TASKS.get(index).reward);
        return true;
    }
}
