package moe.hinakusoft.funstart.manager;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public class BlockUtils {

    public static Set<Block> floodFill(Block origin, Predicate<Block> matcher, int max) {
        Set<Block> result = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(origin);
        result.add(origin);

        while (!queue.isEmpty() && result.size() < max) {
            Block current = queue.poll();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        if (result.size() >= max) return result;
                        Block neighbor = current.getRelative(x, y, z);
                        if (matcher.test(neighbor) && !result.contains(neighbor)) {
                            result.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        return result;
    }

    public static Set<Block> floodFill2D(Block origin, Predicate<Block> matcher, int max) {
        Set<Block> result = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(origin);
        result.add(origin);

        while (!queue.isEmpty() && result.size() < max) {
            Block current = queue.poll();
            for (int x = -1; x <= 1 && result.size() < max; x++) {
                for (int z = -1; z <= 1 && result.size() < max; z++) {
                    if (x == 0 && z == 0) continue;
                    Block neighbor = current.getRelative(x, 0, z);
                    if (matcher.test(neighbor) && !result.contains(neighbor)) {
                        result.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return result;
    }
}
