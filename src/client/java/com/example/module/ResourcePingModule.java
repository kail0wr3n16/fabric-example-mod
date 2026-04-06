package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourcePingModule extends Module {
    private final NumberSetting scanRange;
    private final BooleanSetting ores;
    private final BooleanSetting chests;
    private final BooleanSetting spawners;
    private final BooleanSetting diamonds;
    private final BooleanSetting ancient;

    private final List<String> alerts = new ArrayList<>();
    private final Map<Block, Integer> nearbyCount = new HashMap<>();
    private int scanTick = 0;

    // Ore blocks to watch
    private static final Block[] ORE_BLOCKS = {
        Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.ANCIENT_DEBRIS,
        Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
        Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE
    };

    public ResourcePingModule() {
        super("resourceping", ModuleCategory.MISC, false);
        this.scanRange = addSetting(new NumberSetting("scanRange", 8.0, 4.0, 16.0));
        this.ores = addSetting(new BooleanSetting("ores", true));
        this.chests = addSetting(new BooleanSetting("chests", true));
        this.spawners = addSetting(new BooleanSetting("spawners", true));
        this.diamonds = addSetting(new BooleanSetting("diamondOnly", false));
        this.ancient = addSetting(new BooleanSetting("ancientDebris", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        scanTick++;
        if (scanTick < 20) return; // Scan every second
        scanTick = 0;

        int range = scanRange.asInt();
        BlockPos center = mc.player.blockPosition();
        nearbyCount.clear();
        int chestCount = 0;
        int spawnerCount = 0;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-range, -range, -range),
            center.offset(range, range, range))) {

            Block b = mc.level.getBlockState(pos).getBlock();

            if (ores.isEnabled()) {
                for (Block oreBlock : ORE_BLOCKS) {
                    if (b == oreBlock) {
                        if (diamonds.isEnabled() &&
                            b != Blocks.DIAMOND_ORE &&
                            b != Blocks.DEEPSLATE_DIAMOND_ORE) continue;
                        nearbyCount.merge(b, 1, Integer::sum);
                    }
                }
            }

            if (chests.isEnabled()) {
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be instanceof ChestBlockEntity) chestCount++;
            }

            if (spawners.isEnabled()) {
                BlockEntity be = mc.level.getBlockEntity(pos);
                if (be instanceof SpawnerBlockEntity) spawnerCount++;
            }
        }

        alerts.clear();
        if (chestCount > 0) alerts.add("Chests: " + chestCount);
        if (spawnerCount > 0) alerts.add("Spawners: " + spawnerCount);

        int diamondCount = nearbyCount.getOrDefault(Blocks.DIAMOND_ORE, 0)
            + nearbyCount.getOrDefault(Blocks.DEEPSLATE_DIAMOND_ORE, 0);
        if (diamondCount > 0) alerts.add("Diamonds: " + diamondCount);

        int ancientCount = nearbyCount.getOrDefault(Blocks.ANCIENT_DEBRIS, 0);
        if (ancientCount > 0 && ancient.isEnabled()) alerts.add("Ancient Debris: " + ancientCount);

        // Other ores summary
        int otherOres = 0;
        for (Map.Entry<Block, Integer> e : nearbyCount.entrySet()) {
            if (e.getKey() != Blocks.DIAMOND_ORE && e.getKey() != Blocks.DEEPSLATE_DIAMOND_ORE
                && e.getKey() != Blocks.ANCIENT_DEBRIS) {
                otherOres += e.getValue();
            }
        }
        if (otherOres > 0 && !diamonds.isEnabled()) alerts.add("Ores: " + otherOres);
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        if (alerts.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();

        int x = guiGraphics.guiWidth() - 110;
        int y = 40;
        int lineH = 10;
        int h = alerts.size() * lineH + 8;
        int w = 105;

        guiGraphics.fill(x - 2, y - 2, x + w + 2, y + h, 0x88000000);
        guiGraphics.text(mc.font, Component.literal("Nearby:"), x, y, 0xFFAAAAAA, false);
        y += lineH + 2;

        for (String alert : alerts) {
            int color = alert.startsWith("Diamond") ? 0xFF55FFFF :
                        alert.startsWith("Ancient") ? 0xFFFF5555 :
                        alert.startsWith("Chest") ? 0xFFFFAA00 :
                        alert.startsWith("Spawn") ? 0xFFFF55FF : 0xFFAAAAAA;
            guiGraphics.text(mc.font, Component.literal("  " + alert), x, y, color, false);
            y += lineH;
        }
    }
}
