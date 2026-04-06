package com.example.module;

import com.example.module.setting.BooleanSetting;

/**
 * Prevents the FOV from changing when sprinting or drawing a bow.
 * This is a standard feature on Lunar Client, Badlion, and OptiFine.
 * No movement advantage — purely a visual comfort preference.
 */
public class FovMemoryModule extends Module {
    private final BooleanSetting lockSprint;
    private final BooleanSetting lockBow;

    private static FovMemoryModule INSTANCE;

    public FovMemoryModule() {
        super("fovmemory", ModuleCategory.QOL, false);
        this.lockSprint = addSetting(new BooleanSetting("lockSprint", true));
        this.lockBow = addSetting(new BooleanSetting("lockBow", true));
        INSTANCE = this;
    }

    public static FovMemoryModule getInstance() {
        return INSTANCE;
    }

    public boolean isLockSprint() {
        return lockSprint.isEnabled();
    }

    public boolean isLockBow() {
        return lockBow.isEnabled();
    }
}
