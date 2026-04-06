package com.example.module;

public class InventoryMoveModule extends Module {
    private static InventoryMoveModule INSTANCE;

    public InventoryMoveModule() {
        super("inventorymove", ModuleCategory.MOVEMENT, false);
        INSTANCE = this;
    }

    public static InventoryMoveModule getInstance() {
        return INSTANCE;
    }
}
