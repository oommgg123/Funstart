package moe.hinakusoft.funstart.manager;

import moe.hinakusoft.funstart.FunstartPlugin;
import moe.hinakusoft.funstart.model.FeatureFlag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MultiThreadManager {

    private final FunstartPlugin plugin;
    private final FeatureConfig featureConfig;
    private ExecutorService executor;
    private boolean enabled;

    public MultiThreadManager(FunstartPlugin plugin, FeatureConfig featureConfig) {
        this.plugin = plugin;
        this.featureConfig = featureConfig;
        this.enabled = featureConfig.isEnabled(FeatureFlag.MULTITHREAD);
        if (enabled) {
            this.executor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "Funstart-Worker");
                t.setDaemon(true);
                return t;
            });
            plugin.getLogger().info("多线程功能已启用，工作线程池已创建");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled && (executor == null || executor.isShutdown())) {
            this.executor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "Funstart-Worker");
                t.setDaemon(true);
                return t;
            });
        } else if (!enabled && executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void submit(Runnable task) {
        if (enabled && executor != null && !executor.isShutdown()) {
            executor.submit(task);
        } else {
            task.run();
        }
    }

    public <T> void submitBatch(Collection<T> items, Consumer<T> processor) {
        if (enabled && executor != null && !executor.isShutdown()) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (T item : items) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> processor.accept(item), executor);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } else {
            for (T item : items) {
                processor.accept(item);
            }
        }
    }

    public void runAsync(Runnable task) {
        if (enabled && executor != null && !executor.isShutdown()) {
            executor.submit(task);
        } else {
            task.run();
        }
    }

    public void runBukkitTask(Runnable task) {
        if (enabled && executor != null && !executor.isShutdown()) {
            executor.submit(() -> {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, task);
            });
        } else {
            task.run();
        }
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
