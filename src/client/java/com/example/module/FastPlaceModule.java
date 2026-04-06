package com.example.module;

public class FastPlaceModule extends Module {
    private static FastPlaceModule INSTANCE;

    public FastPlaceModule() {
        super("fastplace", ModuleCategory.MISC, false);
        INSTANCE = this;
    }

    public static FastPlaceModule getInstance() {
        return INSTANCE;
    }
}
