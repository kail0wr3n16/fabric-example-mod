package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class AimAssistModule extends Module {
    private static AimAssistModule INSTANCE;

    private final NumberSetting fovCone;
    private final NumberSetting smoothing;
    private final NumberSetting maxRange;
    private final BooleanSetting players;
    private final BooleanSetting mobs;
    private final BooleanSetting onlyAttackable;

    public AimAssistModule() {
        super("aimassist", ModuleCategory.COMBAT, false);
        this.fovCone = addSetting(new NumberSetting("fovCone", 60.0, 5.0, 180.0));
        this.smoothing = addSetting(new NumberSetting("smoothing", 0.06, 0.01, 0.30));
        this.maxRange = addSetting(new NumberSetting("maxRange", 5.0, 1.0, 10.0));
        this.players = addSetting(new BooleanSetting("players", true));
        this.mobs = addSetting(new BooleanSetting("mobs", false));
        this.onlyAttackable = addSetting(new BooleanSetting("onlyAttackable", true));
        INSTANCE = this;
    }

    public static AimAssistModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) return;

        Entity target = findTarget(mc);
        if (target == null) return;

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 toTarget = target.getEyePosition().subtract(eyePos);

        double yaw = Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        double dist2D = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        double pitch = Math.toDegrees(-Math.atan2(toTarget.y, dist2D));

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        float dyaw = wrapDegrees((float)(yaw - currentYaw));
        float dpitch = (float)(pitch - currentPitch);

        float factor = smoothing.getValue().floatValue();
        mc.player.setYRot(currentYaw + dyaw * factor);
        mc.player.setXRot(Math.max(-90, Math.min(90, currentPitch + dpitch * factor)));
    }

    private Entity findTarget(Minecraft mc) {
        double halfFov = fovCone.getValue() / 2.0;
        double range = maxRange.getValue();
        Entity best = null;
        double bestAngle = Double.MAX_VALUE;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player) continue;
            if (!(e instanceof LivingEntity living)) continue;
            if (living.isDeadOrDying()) continue;
            if (e instanceof Player p) {
                if (!players.isEnabled()) continue;
                if (p.isCreative() || p.isSpectator()) continue;
            } else {
                if (!mobs.isEnabled()) continue;
            }
            if (onlyAttackable.isEnabled() && mc.player.distanceTo(e) > range) continue;

            Vec3 look = mc.player.getLookAngle();
            Vec3 toEntity = e.getEyePosition().subtract(mc.player.getEyePosition()).normalize();
            double dot = Math.max(-1, Math.min(1, look.dot(toEntity)));
            double angle = Math.toDegrees(Math.acos(dot));

            if (angle > halfFov) continue;
            if (angle < bestAngle) {
                bestAngle = angle;
                best = e;
            }
        }
        return best;
    }

    private float wrapDegrees(float degrees) {
        degrees = degrees % 360f;
        if (degrees >= 180f) degrees -= 360f;
        if (degrees < -180f) degrees += 360f;
        return degrees;
    }
}
