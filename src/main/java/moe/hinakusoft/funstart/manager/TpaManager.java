package moe.hinakusoft.funstart.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {
    private final Map<UUID, TpaRequest> requests = new ConcurrentHashMap<UUID, TpaRequest>();

    public void createRequest(UUID requester, UUID target, TpaType type, int taskId) {
        long expireTime = System.currentTimeMillis() + 60000L;
        this.requests.put(requester, new TpaRequest(requester, target, type, expireTime, taskId));
    }

    public TpaRequest getRequestByRequester(UUID requester) {
        return this.requests.get(requester);
    }

    public TpaRequest getRequestByTarget(UUID target) {
        for (TpaRequest request : this.requests.values()) {
            if (!request.getTarget().equals(target)) continue;
            if (System.currentTimeMillis() >= request.getExpireTime()) {
                this.requests.remove(request.getRequester());
                continue;
            }
            return request;
        }
        return null;
    }

    public TpaRequest removeRequest(UUID requester) {
        return this.requests.remove(requester);
    }

    public List<TpaRequest> getRequestsByTarget(UUID target) {
        List<TpaRequest> result = new ArrayList<>();
        java.util.Iterator<Map.Entry<UUID, TpaRequest>> it = requests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TpaRequest> entry = it.next();
            TpaRequest req = entry.getValue();
            if (System.currentTimeMillis() >= req.getExpireTime()) {
                it.remove();
                continue;
            }
            if (req.getTarget().equals(target)) {
                result.add(req);
            }
        }
        return result;
    }

    public boolean hasPendingRequest(UUID requester) {
        TpaRequest req = this.requests.get(requester);
        if (req == null) {
            return false;
        }
        if (System.currentTimeMillis() >= req.getExpireTime()) {
            this.requests.remove(requester);
            return false;
        }
        return true;
    }

    public List<TpaRequest> removeAllRequestsByPlayer(UUID playerUuid) {
        ArrayList<TpaRequest> removed = new ArrayList<TpaRequest>();
        this.requests.values().removeIf(req -> {
            if (req.getRequester().equals(playerUuid) || req.getTarget().equals(playerUuid)) {
                removed.add(req);
                return true;
            }
            return false;
        });
        return removed;
    }

    public static class TpaRequest {
        private final UUID requester;
        private final UUID target;
        private final TpaType type;
        private final long expireTime;
        private final int taskId;

        public TpaRequest(UUID requester, UUID target, TpaType type, long expireTime, int taskId) {
            this.requester = requester;
            this.target = target;
            this.type = type;
            this.expireTime = expireTime;
            this.taskId = taskId;
        }

        public UUID getRequester() {
            return this.requester;
        }

        public UUID getTarget() {
            return this.target;
        }

        public TpaType getType() {
            return this.type;
        }

        public long getExpireTime() {
            return this.expireTime;
        }

        public int getTaskId() {
            return this.taskId;
        }
    }

    public static enum TpaType {
        TPA,
        TPAH;
    }
}
