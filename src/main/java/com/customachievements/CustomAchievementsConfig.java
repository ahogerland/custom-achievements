/*
 * Copyright (c) 2020, Alec Hogerland <https://github.com/ahogerland>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.customachievements;

import java.awt.Color;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("achievements")
public interface CustomAchievementsConfig extends Config
{
	@ConfigItem(
			position = 0,
			keyName = "ironmanModeEnabled",
			name = "Ironman Mode",
			description = "Prevent player help for item drops and monster kills."
	)
	default boolean ironmanModeEnabled() { return true; }

	@ConfigItem(
			position = 1,
			keyName = "achievementInProgressEnabled",
			name = "Enable Achievement \"In Progress\" Status",
			description = "Achievements are labelled as \"In Progress\" when one or more requirements have been completed."
	)
	default boolean achievementInProgressEnabled() { return true; }

	@ConfigItem(
			position = 2,
			keyName = "requirementInProgressEnabled",
			name = "Enable Requirement \"In Progress\" Status",
			description = "Requirements are labelled as \"In Progress\" when partially completed (if applicable)."
	)
	default boolean requirementInProgressEnabled() { return true; }

	@ConfigItem(
			position = 3,
			keyName = "notificationsEnabled",
			name = "Enable Chat Notifications",
			description = "Enable notification messages in the chatbox."
	)
	default boolean notificationsEnabled() { return true; }

	@ConfigItem(
			position = 4,
			keyName = "notificationsColor",
			name = "Chat Notification Color",
			description = "The color applied to messages in the chatbox when unlocking an achievement."
	)
	default Color notificationsColor() { return new Color(120, 20, 120); }

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
