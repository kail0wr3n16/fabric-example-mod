package com.example.module;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;

public class PotionTimerHudModule extends Module {
    private final BooleanSetting showAmplifier;
    private final NumberSetting yPos;

    public PotionTimerHudModule() {
        super("potiontimer", ModuleCategory.RENDER, false);
        this.showAmplifier = addSetting(new BooleanSetting("showAmplifier", true));
        this.yPos = addSetting(new NumberSetting("yPos", 26.0, 5.0, 120.0));
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffectsMap().values());
        if (effects.isEmpty()) {
            return;
        }

        effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());

        List<String> lines = new ArrayList<>();
        for (MobEffectInstance effect : effects) {
            String name = effect.getEffect().value().getDescriptionId();
            String duration = formatDuration(effect.getDuration());
            if (showAmplifier.isEnabled() && effect.getAmplifier() > 0) {
                lines.add(name + " " + romanNumeral(effect.getAmplifier() + 1) + " " + duration);
            } else {
                lines.add(name + " " + duration);
            }
        }

        int width = 96;
        for (String line : lines) {
            width = Math.max(width, mc.font.width(line) + 8);
        }

        int x = guiGraphics.guiWidth() - width - 8;
        int y = yPos.getValue().intValue();
        int height = 8 + lines.size() * 10;

        guiGraphics.fill(x, y, x + width, y + height, 0xAA0A0C10);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF3A7FD4);

        int textY = y + 4;
        for (String line : lines) {
            guiGraphics.text(mc.font, Component.literal(line), x + 4, textY, 0xFFE8EEF8, true);
            textY += 10;
        }
    }

    private String formatDuration(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String romanNumeral(int amplifier) {
        return switch (amplifier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(amplifier);
        };
    }
}