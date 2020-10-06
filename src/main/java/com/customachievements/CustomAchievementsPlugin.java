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

import com.customachievements.requirements.*;
import com.google.common.base.Strings;
import com.google.inject.Provides;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Custom Achievements",
	description = "Create custom achievements",
	tags = {"achievements", "goals"},
	enabledByDefault = false
)
public class CustomAchievementsPlugin extends Plugin
{
	@Getter
	private final List<Achievement> achievements = new ArrayList<>();

	@Getter
	private final AchievementSerializer serializer = new AchievementSerializer().setPrettyPrinting();

	@Inject
	private EventBus eventBus;

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private CustomAchievementsConfig config;

	private CustomAchievementsPanel panel;
	private NavigationButton navigationButton;

	@Provides
	CustomAchievementsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CustomAchievementsConfig.class);
	}

	public Achievement createAchievement(final String name)
	{
		return new Achievement(name);
	}

	public Requirement createRequirement(final RequirementType type)
	{
		switch (type)
		{
			case SKILL:
				return new SkillRequirement(Skill.ATTACK, SkillTargetType.LEVEL, 1);
			case ITEM:
			case QUEST:
			case CHUNK:
			case ABSTRACT:
			default:
				return new AbstractRequirement("New Requirement");
		}
	}

	public void clearAchievements()
	{
		while (!achievements.isEmpty())
		{
			removeAchievement(achievements.get(achievements.size() - 1));
		}
	}

	public void addAchievement(final Achievement achievement)
	{
		if (!achievements.contains(achievement))
		{
			achievements.add(achievement);
			achievement.setCompleteListener(() -> onAchievementComplete(achievement));
		}
	}

	public void removeAchievement(final Achievement achievement)
	{
		removeAllRequirements(achievement);
		achievements.remove(achievement);
	}

	public void addRequirement(final Achievement achievement, final Requirement requirement)
	{
		if (!achievement.getRequirements().contains(requirement))
		{
			eventBus.register(requirement);
			achievement.addRequirement(requirement);
			requirement.setCompleteListener(() -> onRequirementComplete(achievement, requirement));
		}
	}

	public void addAllRequirements(final Achievement achievement, final Collection<Requirement> requirements)
	{
		for (Requirement requirement : requirements)
		{
			addRequirement(achievement, requirement);
		}
	}

	public void removeRequirement(final Achievement achievement, final Requirement requirement)
	{
		eventBus.unregister(requirement);
		achievement.removeRequirement(requirement);
	}

	public void removeAllRequirements(final Achievement achievement)
	{
		for (Requirement requirement : achievement.getRequirements())
		{
			eventBus.unregister(requirement);
		}

		achievement.getRequirements().clear();
	}

	public void updateConfig()
	{
		config.achievementsData(serializer.toJson(achievements));
	}

	public void loadConfig(String json)
	{
		if (Strings.isNullOrEmpty(json))
		{
			return;
		}

		List<Achievement> loaded = serializer.fromJson(json);

		if (loaded != null)
		{
			clearAchievements();

			for (Achievement achievement : loaded)
			{
				achievements.add(achievement);
				achievement.setCompleteListener(() -> onAchievementComplete(achievement));

				for (Requirement requirement : achievement.getRequirements())
				{
					eventBus.register(requirement);
					requirement.setCompleteListener(() -> onRequirementComplete(achievement, requirement));
				}
			}
		}

		panel.refresh();
	}

	public void sendAchievementCompleteMessage(final Achievement achievement)
	{
		if (config.notificationsEnabled())
		{
			final Color notificationsColor = config.notificationsColor();
			final String message = new ChatMessageBuilder()
					.append(ChatColorType.HIGHLIGHT)
					.append(notificationsColor, "Congratulations! You have completed ")
					.append(notificationsColor, achievement.getName())
					.append(notificationsColor, ". Your Achievements have been updated.")
					.build();

			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.GAMEMESSAGE)
					.runeLiteFormattedMessage(message)
					.build());
		}
	}

	public void sendRequirementCompleteMessage(final Requirement requirement)
	{
		if (config.notificationsEnabled())
		{
			final String message = new ChatMessageBuilder()
					.append(ChatColorType.HIGHLIGHT)
					.append("Achievement Requirement complete! ")
					.append(config.notificationsColor(), requirement.toString())
					.build();

			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.GAMEMESSAGE)
					.runeLiteFormattedMessage(message)
					.build());
		}
	}

	@Subscribe
	public void onStatChanged(final StatChanged statChanged) {}

	@Subscribe
	public void onNpcLootReceived(final NpcLootReceived npcLootReceived) {}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged itemContainerChanged)
	{
		client.getItemContainer(InventoryID.INVENTORY);
	}

	@Subscribe
	public void onWidgetLoaded(final WidgetLoaded widgetLoaded)
	{
		switch (widgetLoaded.getGroupId())
		{
			case WidgetID.BARROWS_REWARD_GROUP_ID:
			case WidgetID.CHAMBERS_OF_XERIC_REWARD_GROUP_ID:
			case WidgetID.THEATRE_OF_BLOOD_REWARD_GROUP_ID:
			case WidgetID.CLUE_SCROLL_REWARD_GROUP_ID:
			case WidgetID.KINGDOM_GROUP_ID:
			case WidgetID.FISHING_TRAWLER_REWARD_GROUP_ID:
			case WidgetID.DRIFT_NET_FISHING_REWARD_GROUP_ID:
				break;
			default:
				return;
		}
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equals("achievements"))
		{
			SwingUtilities.invokeLater(panel::refresh);
		}
	}

	@Override
	protected void startUp()
	{
		final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "achievements_icon.png");

		panel = new CustomAchievementsPanel(this, config);

		navigationButton = NavigationButton.builder()
				.tooltip("Custom Achievements")
				.icon(icon)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navigationButton);
		loadConfig(config.achievementsData());
	}

	@Override
	protected void shutDown()
	{
		updateConfig();
		clearAchievements();
		clientToolbar.removeNavigation(navigationButton);
	}

	private void onAchievementComplete(final Achievement achievement)
	{
		sendAchievementCompleteMessage(achievement);

		updateConfig();
		SwingUtilities.invokeLater(panel::refresh);
	}

	private void onRequirementComplete(final Achievement achievement, final Requirement requirement)
	{
		// Don't bother sending a completion message if the Requirement was manually skipped
		if (!achievement.isComplete())
		{
			sendRequirementCompleteMessage(requirement);
			achievement.update();
		}

		updateConfig();
		SwingUtilities.invokeLater(panel::refresh);
	}
}
