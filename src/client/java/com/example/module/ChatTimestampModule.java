package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Prepends a timestamp to every incoming chat message.
 * e.g., "[14:32]  Hello world!"
 * A universally useful QoL feature present on every major client.
 */
public class ChatTimestampModule extends Module {
    private final BooleanSetting showSeconds;
    private boolean registered = false;

    public ChatTimestampModule() {
        super("chattimestamp", ModuleCategory.SOCIAL, true);
        this.showSeconds = addSetting(new BooleanSetting("showSeconds", false));
    }

    @Override
    protected void onEnable() {
        if (!registered) {
            ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> true);
            registered = true;
        }
    }

    /**
     * Called externally from the mixin/event — build the prefixed component.
     * Returns the message with a grey timestamp prepended.
     */
    public Component buildTimestamped(Component original) {
        if (!isEnabled()) return original;
        DateTimeFormatter fmt = showSeconds.isEnabled()
            ? DateTimeFormatter.ofPattern("HH:mm:ss")
            : DateTimeFormatter.ofPattern("HH:mm");
        String time = LocalTime.now().format(fmt);
        return Component.empty()
            .append(Component.literal("[" + time + "] ").withStyle(s -> s.withColor(0x556677)))
            .append(original);
    }

    /** Singleton access for the mixin. */
    private static ChatTimestampModule INSTANCE;

    public ChatTimestampModule(boolean _unused) {
        this();
        INSTANCE = this;
    }

    public static ChatTimestampModule getInstance() {
        return INSTANCE;
    }
}
