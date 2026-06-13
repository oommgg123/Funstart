package moe.hinakusoft.funstart.model;

import java.util.UUID;

public class WarpPoint {
    private final String id;
    private final String name;
    private final UUID owner;
    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;

    public WarpPoint(String id, String name, UUID owner, String worldName,
                     double x, double y, double z, float yaw, float pitch) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}
