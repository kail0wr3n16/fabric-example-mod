package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ParkourAssistModule extends Module {
    private final NumberSetting edgeTriggerDist;
    private final BooleanSetting onlyWhenSprinting;

    public ParkourAssistModule() {
        super("parkourassist", ModuleCategory.MOVEMENT, false);
        this.edgeTriggerDist = addSetting(new NumberSetting("edgeTriggerDist", 0.4, 0.1, 1.0));
        this.onlyWhenSprinting = addSetting(new BooleanSetting("onlyWhenSprinting", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        if (!mc.player.onGround()) return;
        if (onlyWhenSprinting.isEnabled() && !mc.player.isSprinting()) return;

        // Check if moving forward
        double speed = mc.player.getDeltaMovement().horizontalDistance();
        if (speed < 0.05) return;

        // Check if near an edge (block below in movement direction is air)
        Vec3 pos = mc.player.position();
        Vec3 moveDir = mc.player.getDeltaMovement().normalize();
        double edge = edgeTriggerDist.getValue();

        Vec3 checkPos = pos.add(moveDir.x * edge, -0.1, moveDir.z * edge);
        BlockPos blockBelow = BlockPos.containing(checkPos.x, checkPos.y - 0.5, checkPos.z);

        if (mc.level.getBlockState(blockBelow).isAir()) {
            // Check that there's a block we're coming from (we're on an edge)
            BlockPos underPlayer = BlockPos.containing(pos.x, pos.y - 0.1, pos.z);
            if (!mc.level.getBlockState(underPlayer).isAir()) {
                mc.player.jumpFromGround();
            }
        }
    }
}
