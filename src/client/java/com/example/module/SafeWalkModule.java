package com.example.module;

public class SafeWalkModule extends Module {
    private static SafeWalkModule INSTANCE;

    public SafeWalkModule() {
        super("safewalk", ModuleCategory.ACCESSIBILITY, false);
        INSTANCE = this;
    }

    public static SafeWalkModule getInstance() {
        return INSTANCE;
    }
}
