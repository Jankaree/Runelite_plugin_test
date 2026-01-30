package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Hans finder")
public interface HansFinderConfig extends Config {

    @ConfigItem(
            keyName = "webhook",
            name = "Webhook URL",
            description = "Discord webhook url to send message + image to",
            position = 0
    )
    String webhook();
}
