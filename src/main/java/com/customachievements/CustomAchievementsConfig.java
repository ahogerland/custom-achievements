package com.customachievements;

import java.awt.Color;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("achievements")
public interface CustomAchievementsConfig extends Config
{
	@ConfigItem(
			position = 1,
			keyName = "inProgressEnabled",
			name = "Enable \"In Progress\" Status",
			description = "Achievements are labelled as \"In Progress\" when one or more requirements have been completed."
	)
	default boolean inProgressEnabled() { return true; }

	@ConfigItem(
			position = 2,
			keyName = "notificationsEnabled",
			name = "Enable Chat Notifications",
			description = "Enable notification messages in the chatbox."
	)
	default boolean notificationsEnabled() { return true; }

	@ConfigItem(
			position = 3,
			keyName = "notificationsColor",
			name = "Chat Notification Color",
			description = "The color applied to messages in the chatbox when unlocking an achievement."
	)
	default Color notificationsColor() { return Color.decode("#902020"); }

	@ConfigItem(
			keyName = "achievementsData",
			name = "",
			description = "",
			hidden = true
	)
	default String achievementsData() { return ""; }

	@ConfigItem(
			keyName = "achievementsData",
			name = "",
			description = ""
	)
	void achievementsData(String data);
}
