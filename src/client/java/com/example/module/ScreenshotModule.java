package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Enhances the screenshot experience:
 * - Auto-copies the screenshot filename to the clipboard after taking one.
 * - Shows a brief HUD notification with the screenshot name.
 *
 * Vanilla F2 still takes the screenshot. This module post-processes it.
 */
public class ScreenshotModule extends Module {
    private final BooleanSetting autoCopy;
    private final BooleanSetting showNotification;

    private boolean prevScreenshotKeyDown = false;
    private long notificationShowTime = 0;
    private String notificationText = "";
    private static final long NOTIFICATION_DURATION_MS = 3000;

    public ScreenshotModule() {
        super("screenshot", ModuleCategory.QOL, false);
        this.autoCopy = addSetting(new BooleanSetting("autoCopy", true));
        this.showNotification = addSetting(new BooleanSetting("showNotification", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.options == null || mc.player == null) return;

        boolean ssKeyDown = mc.options.keyScreenshot.isDown();
        boolean justPressed = ssKeyDown && !prevScreenshotKeyDown;
        prevScreenshotKeyDown = ssKeyDown;

        if (justPressed) {
            // Schedule post-processing after the screenshot is written
            // We delay by scheduling work on next tick via a flag approach
            // Delay one tick so the screenshot file has been written before we read it
            mc.execute(() -> processLatestScreenshot(mc));
        }
    }

    private void processLatestScreenshot(Minecraft mc) {
        Path screenshotDir = mc.gameDirectory.toPath().resolve("screenshots");
        if (!Files.exists(screenshotDir)) return;

        try (Stream<Path> stream = Files.list(screenshotDir)) {
            Optional<Path> latest = stream
                .filter(p -> p.toString().endsWith(".png"))
                .max(Comparator.comparingLong(p -> {
                    try {
                        return Files.readAttributes(p, BasicFileAttributes.class).creationTime().toMillis();
                    } catch (IOException e) {
                        return 0L;
                    }
                }));

            latest.ifPresent(path -> {
                String filename = path.getFileName().toString();

                if (autoCopy.isEnabled()) {
                    try {
                        StringSelection sel = new StringSelection(path.toAbsolutePath().toString());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                    } catch (Exception ignored) {}
                }

                if (showNotification.isEnabled()) {
                    notificationText = "Screenshot: " + filename;
                    notificationShowTime = System.currentTimeMillis();
                }
            });
        } catch (IOException ignored) {}
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        if (!showNotification.isEnabled()) return;
        if (notificationText.isEmpty()) return;

        long elapsed = System.currentTimeMillis() - notificationShowTime;
        if (elapsed > NOTIFICATION_DURATION_MS) return;

        Minecraft mc = Minecraft.getInstance();
        float fadeOut = 1.0f - Math.max(0f, (elapsed - 2000) / 1000f);
        int alpha = (int) (180 * fadeOut);
        if (alpha <= 0) return;

        int sw = guiGraphics.guiWidth();
        int textW = mc.font.width(notificationText);
        int x = sw / 2 - textW / 2;
        int y = 30;

        guiGraphics.fill(x - 4, y - 2, x + textW + 4, y + 10, (alpha / 2) << 24 | 0x000000);
        guiGraphics.text(mc.font, Component.literal(notificationText), x, y, (alpha << 24) | 0xE8EEF8, false);
    }
}
