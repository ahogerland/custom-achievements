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

import com.customachievements.requirements.AbstractRequirement;
import com.customachievements.requirements.ChunkRequirement;
import com.customachievements.requirements.ItemRequirement;
import com.customachievements.requirements.QuestRequirement;
import com.customachievements.requirements.Requirement;
import com.customachievements.requirements.RequirementType;
import com.customachievements.requirements.SkillRequirement;
import com.customachievements.requirements.SkillTargetType;
import com.customachievements.requirements.SlayRequirement;
import com.google.common.base.Strings;
import com.google.inject.Provides;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@PluginDescriptor(
	name = "Custom Achievements",
	description = "Create custom achievements",
	tags = {"achievements", "goals"},
	enabledByDefault = false
)
@PluginDependency(LootTrackerPlugin.class)
public class CustomAchievementsPlugin extends Plugin
{
	// Scheduled update frequency (in seconds)
	public static final int UPDATE_FREQUENCY = 10;

	@Getter
	private final List<AchievementElement> elements = new ArrayList<>();

	@Getter
	private final CustomAchievementsSerializer serializer = new CustomAchievementsSerializer();

	@Inject
	private ConfigManager configManager;

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

	@Inject
	private ScheduledExecutorService executorService;

	@Inject
	private ItemReporter itemReporter;

	@Inject
	private NpcKillReporter npcKillReporter;

	@Inject
	private QuestStateReporter questStateReporter;

	@Inject
	private ChunkEnteredReporter chunkEnteredReporter;

	// Tick timestamp on login
	private int loginTickCount = 0;
	private boolean loggedOut = true;

	private CustomAchievementsPanel panel;
	private NavigationButton navigationButton;

	@Provides
	CustomAchievementsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CustomAchievementsConfig.class);
	}

	@Subscribe
	public void onConfigChanged(final ConfigChanged configChanged)
	{
		if (configChanged.getGroup().equals(CustomAchievementsConfig.CONFIG_GROUP))
		{
			SwingUtilities.invokeLater(panel::refresh);
		}
	}

	@Subscribe
	public void onGameStateChanged(final GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
		{
			loggedOut = true;
		}
		else if (gameStateChanged.getGameState() == GameState.LOGGED_IN && loggedOut)
		{
			loggedOut = false;
			loginTickCount = client.getTickCount();

			// Wait for startup scripts and events to run before refreshing to avoid flicker
			executorService.schedule(this::globalRefresh, 1, TimeUnit.SECONDS);
		}
	}

	public void globalRefresh()
	{
		itemReporter.refresh();
		updateAchievementElements(elements);
		SwingUtilities.invokeLater(panel::refresh);
	}

	public void clear()
	{
		while (!elements.isEmpty())
		{
			remove(elements.get(elements.size() - 1));
		}
	}

	public void add(AchievementElement element)
	{
		elements.add(element);
		register(element);
		registerChildren(element);
	}

	public void add(AchievementElement parent, AchievementElement child)
	{
		parent.getChildren().add(child);
		register(child);
		registerChildren(child);
	}

	public void remove(AchievementElement element)
	{
		unregister(element);
		unregisterChildren(element);
		elements.remove(element);
	}

	public void remove(AchievementElement parent, AchievementElement child)
	{
		unregister(child);
		unregisterChildren(child);
		parent.getChildren().remove(child);
	}

	public void set(int index, AchievementElement element)
	{
		AchievementElement old = elements.get(index);

		unregister(old);
		unregisterChildren(old);
		elements.set(index, element);
		register(element);
		registerChildren(element);
	}

	public void set(int index, AchievementElement parent, AchievementElement child)
	{
		AchievementElement old = parent.getChildren().get(index);

		unregister(old);
		unregisterChildren(old);
		parent.getChildren().set(index, child);
		register(child);
		registerChildren(child);
	}

	public void register(AchievementElement element)
	{
		eventBus.register(element);
		element.setStateListener(new AchievementElementStateListener(element));
	}

	public void registerChildren(AchievementElement parent)
	{
		for (AchievementElement child : parent.getChildren())
		{
			registerChildren(child);
			register(child);
		}
	}

	public void unregister(AchievementElement element)
	{
		eventBus.unregister(element);
		element.setStateListener(null);
	}

	public void unregisterChildren(AchievementElement parent)
	{
		for (AchievementElement child : parent.getChildren())
		{
			unregisterChildren(child);
			unregister(child);
		}
	}

	public void updateConfig()
	{
		if (elements.isEmpty())
		{
			configManager.unsetConfiguration(
					CustomAchievementsConfig.CONFIG_GROUP,
					CustomAchievementsConfig.ELEMENTS);
			return;
		}

		final String json = serializer.toJson(elements);

		configManager.setConfiguration(
				CustomAchievementsConfig.CONFIG_GROUP,
				CustomAchievementsConfig.ELEMENTS,
				json);
	}

	public void loadConfig(String json)
	{
		if (Strings.isNullOrEmpty(json))
		{
			return;
		}

		List<AchievementElement> loaded = serializer.fromJson(json);

		if (loaded != null)
		{
			clear();

			for (AchievementElement element : loaded)
			{
				add(element);
			}
		}

		panel.refresh();
	}

	public void sendCompletionMessage(String message)
	{
		if (ready() && config.notificationsEnabled())
		{
			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.GAMEMESSAGE)
					.runeLiteFormattedMessage(message)
					.build());
		}
	}

	public Achievement createAchievement(String name)
	{
		return new Achievement(name);
	}

	public Requirement createRequirement(RequirementType type)
	{
		switch (type)
		{
			case SKILL:
				return new SkillRequirement(Skill.ATTACK, SkillTargetType.LEVEL, 1);
			case ITEM:
				return new ItemRequirement("", 1);
			case SLAY:
				return new SlayRequirement("", 1);
			case QUEST:
				return new QuestRequirement(Quest.COOKS_ASSISTANT);
			case CHUNK:
				return new ChunkRequirement(0, "");
			case ABSTRACT:
			default:
				return new AbstractRequirement("");
		}
	}

	@Override
	protected void startUp()
	{
		final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "achievements_icon.png");
		final String configJson = configManager.getConfiguration(
				CustomAchievementsConfig.CONFIG_GROUP,
				CustomAchievementsConfig.ELEMENTS);

		panel = new CustomAchievementsPanel(this, config);

		navigationButton = NavigationButton.builder()
				.tooltip("Custom Achievements")
				.icon(icon)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navigationButton);
		loadConfig(configJson);

		eventBus.register(itemReporter);
		eventBus.register(npcKillReporter);
		eventBus.register(questStateReporter);
		eventBus.register(chunkEnteredReporter);
	}

	@Override
	protected void shutDown()
	{
		eventBus.unregister(itemReporter);
		eventBus.unregister(npcKillReporter);
		eventBus.unregister(questStateReporter);
		eventBus.unregister(chunkEnteredReporter);

		updateConfig();
		clear();
		clientToolbar.removeNavigation(navigationButton);
	}

	private boolean ready()
	{
		int ticksElapsed = client.getTickCount() - loginTickCount;

		// Wait a few game ticks for everything to update
		return ticksElapsed > 4;
	}

	private void updateAchievementElements(List<AchievementElement> list)
	{
		for (AchievementElement element : list)
		{
			updateAchievementElements(element.getChildren());

			if (element instanceof Requirement)
			{
				((Requirement) element).forceUpdate(client);
			}

			element.refresh();
		}
	}

	@AllArgsConstructor
	private class AchievementElementStateListener implements AchievementStateListener
	{
		private final AchievementElement element;

		@Override
		public void onStateChanged(AchievementState status)
		{
			if (status == AchievementState.COMPLETE && !element.isForceComplete())
			{
				sendCompletionMessage(element.completionChatMessage(config));
			}

			updateConfig();
			SwingUtilities.invokeLater(panel::refresh);
		}
	}
}
