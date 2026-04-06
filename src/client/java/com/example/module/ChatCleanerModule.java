package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatCleanerModule extends Module {
    private static ChatCleanerModule INSTANCE;

    private final BooleanSetting filterJoinLeave;
    private final BooleanSetting filterAdvancement;
    private final BooleanSetting filterDeathMessages;
    private final BooleanSetting filterRepeat;
    private final NumberSetting repeatWindowSecs;

    // Custom user patterns (populated via command)
    private final List<Pattern> customFilters = new ArrayList<>();

    // Recent messages for repeat detection
    private final List<String> recentMessages = new ArrayList<>();
    private final List<Long> recentTimestamps = new ArrayList<>();

    public ChatCleanerModule() {
        super("chatcleaner", ModuleCategory.SOCIAL, false);
        this.filterJoinLeave = addSetting(new BooleanSetting("filterJoinLeave", false));
        this.filterAdvancement = addSetting(new BooleanSetting("filterAdvancement", true));
        this.filterDeathMessages = addSetting(new BooleanSetting("filterDeathMessages", false));
        this.filterRepeat = addSetting(new BooleanSetting("filterRepeat", true));
        this.repeatWindowSecs = addSetting(new NumberSetting("repeatWindowSecs", 5.0, 1.0, 30.0));
        INSTANCE = this;
    }

    public static ChatCleanerModule getInstance() {
        return INSTANCE;
    }

    /**
     * Called by the event handler in ExampleModClient for each incoming chat/system message.
     * Returns true if the message should be shown, false to filter it out.
     */
    public boolean shouldAllow(String rawMessage) {
        if (!isEnabled()) return true;

        // Filter advancements
        if (filterAdvancement.isEnabled()) {
            if (rawMessage.contains("has made the advancement") ||
                rawMessage.contains("has completed the challenge") ||
                rawMessage.contains("has reached the goal")) {
                return false;
            }
        }

        // Filter join/leave (common patterns)
        if (filterJoinLeave.isEnabled()) {
            if (rawMessage.matches(".*\\b(joined|left) the game\\b.*")) {
                return false;
            }
        }

        // Filter death messages (heuristic — common death verbs)
        if (filterDeathMessages.isEnabled()) {
            if (rawMessage.matches(".*(was slain|drowned|fell from|burned|blown up|killed|died|tried to swim).*")) {
                return false;
            }
        }

        // Custom filters
        for (Pattern pattern : customFilters) {
            if (pattern.matcher(rawMessage).find()) return false;
        }

        // Repeat filter
        if (filterRepeat.isEnabled()) {
            long now = System.currentTimeMillis();
            long windowMs = (long)(repeatWindowSecs.getValue() * 1000);

            // Clean old entries
            for (int i = recentMessages.size() - 1; i >= 0; i--) {
                if (now - recentTimestamps.get(i) > windowMs) {
                    recentMessages.remove(i);
                    recentTimestamps.remove(i);
                }
            }

            if (recentMessages.contains(rawMessage)) {
                return false;
            }

            recentMessages.add(rawMessage);
            recentTimestamps.add(now);
        }

        return true;
    }

    public void addCustomFilter(String regex) {
        try {
            customFilters.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        } catch (Exception ignored) {}
    }

    public void clearCustomFilters() {
        customFilters.clear();
    }
}
