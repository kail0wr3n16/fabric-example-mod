package com.example.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class DeathMarkerModule extends Module {
    private boolean wasDead = false;
    private int deathCount = 0;

    public DeathMarkerModule() {
        super("deathmarker", ModuleCategory.QOL, true);
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) {
            wasDead = false;
            return;
        }

        boolean isDead = mc.player.isDeadOrDying();

        if (isDead && !wasDead) {
            // Just died — record position
            Vec3 pos = mc.player.position();
            deathCount++;
            String name = "Death #" + deathCount;

            WaypointsModule wm = WaypointsModule.getInstance();
            if (wm != null && wm.isEnabled()) {
                wm.addWaypoint(name, pos.x, pos.y, pos.z);
            }
        }

        wasDead = isDead;
    }
}
