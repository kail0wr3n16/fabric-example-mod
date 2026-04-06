package com.example.module;

import com.example.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public class AutoJumpResetModule extends Module {
    private final NumberSetting jumpInterval;

    private boolean wasOnGround = false;
    private boolean wasMoving = false;
    private int ticksSinceLanded = 0;

    public AutoJumpResetModule() {
        super("jumpReset", ModuleCategory.QOL, false);
        this.jumpInterval = addSetting(new NumberSetting("jumpInterval", 1.0, 1.0, 5.0));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.screen != null) return;

        boolean onGround = mc.player.onGround();
        boolean isSprinting = mc.player.isSprinting();
        boolean isMoving = mc.player.getDeltaMovement().horizontalDistance() > 0.05;

        if (onGround && !wasOnGround) {
            // Just landed
            ticksSinceLanded = 0;
        }

        if (onGround) {
            ticksSinceLanded++;
        }

        // Auto jump on landing if sprinting and moving, after brief pause
        if (onGround && isSprinting && isMoving && ticksSinceLanded >= jumpInterval.asInt()) {
            mc.player.jumpFromGround();
            ticksSinceLanded = 0;
        }

        wasOnGround = onGround;
        wasMoving = isMoving;
    }

    @Override
    protected void onDisable() {
        wasOnGround = false;
        ticksSinceLanded = 0;
    }
}
