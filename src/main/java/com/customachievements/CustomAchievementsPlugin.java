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

import com.customachievements.events.ItemsValidated;
import com.customachievements.events.KilledNpc;
import com.customachievements.events.QuestStateChanged;
import com.customachievements.requirements.AbstractRequirement;
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
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NPCManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.loottracker.LootRecordType;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Custom Achievements",
	description = "Create custom achievements",
	tags = {"achievements", "goals"},
	enabledByDefault = false
)
@PluginDependency(LootTrackerPlugin.class)
public class CustomAchievementsPlugin extends Plugin
{
	@Getter
	private final List<AchievementElement> elements = new ArrayList<>();

	@Getter
	private final CustomAchievementsSerializer serializer = new CustomAchievementsSerializer();

	@Inject
	private ItemManager itemManager;

	@Inject
	private NPCManager npcManager;

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

	// NPC kill tracking
	private final Set<NPC> targetNpcs = new HashSet<>();
	private final Set<NPC> ignoredNpcs = new HashSet<>();
	private HitsplatApplied lastHit = new HitsplatApplied();
	private boolean validateFullHealth = false;

	// Quest state tracking
	private final Map<Integer, QuestState> questStateCache = new HashMap<>();

	// Tick timestamp on login
	private int loginTickCount = 0;

	private CustomAchievementsPanel panel;
	private NavigationButton navigationButton;

	@Provides
	CustomAchievementsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CustomAchievementsConfig.class);
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

	public void globalRefresh()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			ItemContainer container = client.getItemContainer(InventoryID.INVENTORY);

			if (container != null)
			{
				eventBus.post(new ItemContainerChanged(InventoryID.INVENTORY.getId(), container));
			}
		}

		updateAchievementElements(elements);

		SwingUtilities.invokeLater(panel::refresh);
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
			case ABSTRACT:
			default:
				return new AbstractRequirement("");
		}
	}

	@Schedule(
			period = 10,
			unit = ChronoUnit.SECONDS
	)
	public void updateTask()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			final List<NPC> npcs = client.getNpcs();

			final Predicate<NPC> canForgetNpc = npc -> {
				final boolean healthShowing = npc.getHealthScale() != -1;
				final boolean fullHealth = (npc.getHealthRatio() / npc.getHealthScale()) == 1;

				return (healthShowing && fullHealth) || !npcs.contains(npc);
			};

			targetNpcs.removeIf(canForgetNpc);
			ignoredNpcs.removeIf(canForgetNpc);

			updateQuestTracking();
		}
	}

	@Subscribe
	public void onGameTick(final GameTick _gameTick)
	{
		if (validateFullHealth)
		{
			final NPC npc = (NPC) lastHit.getActor();
			final Integer maxHealth = npcManager.getHealth(npc.getId());
			final int originalHealth = getNpcHealth(npc) + lastHit.getHitsplat().getAmount();

			if (maxHealth != null && originalHealth == maxHealth)
			{
				ignoredNpcs.remove(npc);
				targetNpcs.add(npc);
			}

			validateFullHealth = false;
		}
	}

	@Subscribe
	public void onHitsplatApplied(final HitsplatApplied hitsplatApplied)
	{
		if (hitsplatApplied.getActor() instanceof NPC)
		{
			final Hitsplat hitsplat = hitsplatApplied.getHitsplat();
			final NPC npc = (NPC) hitsplatApplied.getActor();

			if (hitsplat.isMine() && hitsplat.getHitsplatType() != Hitsplat.HitsplatType.BLOCK_ME)
			{
				if (!ignoredNpcs.contains(npc))
				{
					targetNpcs.add(npc);
				}
				else if (npc.getHealthScale() == -1)
				{
					// NPC health bar was hidden, so check to see if health was full on next game tick
					// and add the NPC to the target set if that's the case.
					validateFullHealth = true;
				}

				lastHit = hitsplatApplied;
			}
			else if (config.ironmanModeEnabled() && hitsplat.isOthers() && hitsplat.getHitsplatType() != Hitsplat.HitsplatType.BLOCK_OTHER)
			{
				targetNpcs.remove(npc);
				ignoredNpcs.add(npc);
			}
		}
	}

	@Subscribe
	public void onActorDeath(final ActorDeath actorDeath)
	{
		if (actorDeath.getActor() instanceof NPC)
		{
			final NPC npc = (NPC) actorDeath.getActor();

			if (!ignoredNpcs.remove(npc) && targetNpcs.remove(npc))
			{
				eventBus.post(new KilledNpc(npc));
			}
		}
	}

	@Subscribe
	public void onLootReceived(final LootReceived lootReceived)
	{
		if (config.ironmanModeEnabled() && lootReceived.getType() == LootRecordType.PLAYER)
		{
			return;
		}

		final ItemSource source = lootReceived.getType() == LootRecordType.PLAYER ?
				ItemSource.PLAYER_LOOT :
				ItemSource.LOOT;

		final Collection<NamedItem> items = lootReceived.getItems().stream()
				.map(itemStack -> createNamedItem(itemStack.getId(), itemStack.getQuantity()))
				.collect(Collectors.toList());

		eventBus.post(new ItemsValidated(source, items));
	}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged itemContainerChanged)
	{
		if (itemContainerChanged.getContainerId() == InventoryID.INVENTORY.getId())
		{
			final Collection<NamedItem> items = Arrays.stream(itemContainerChanged.getItemContainer().getItems())
					.map(item -> createNamedItem(item.getId(), item.getQuantity()))
					.collect(Collectors.toList());

			eventBus.post(new ItemsValidated(ItemSource.INVENTORY, items));
		}
	}

	@Subscribe
	public void onWidgetLoaded(final WidgetLoaded widgetLoaded)
	{
		if (widgetLoaded.getGroupId() == WidgetID.QUEST_COMPLETED_GROUP_ID)
		{
			updateQuestTracking();
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

	@Subscribe
	public void onScriptPostFired(final ScriptPostFired scriptPostFired)
	{
		if (scriptPostFired.getScriptId() == ScriptID.QUESTLIST_PROGRESS_LIST_SHOW)
		{
			for (Quest quest : Quest.values())
			{
				questStateCache.put(quest.getId(), quest.getState(client));
			}

			globalRefresh();
		}
	}

	@Subscribe
	public void onGameStateChanged(final GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			loginTickCount = client.getTickCount();
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
	}

	@Override
	protected void shutDown()
	{
		updateConfig();
		clear();
		clientToolbar.removeNavigation(navigationButton);
		questStateCache.clear();
	}

	private boolean ready()
	{
		int ticksElapsed = client.getTickCount() - loginTickCount;

		// Wait a few game ticks for everything to update
		return ticksElapsed > 2 && client.getGameState() == GameState.LOGGED_IN;
	}

	private void updateQuestTracking()
	{
		for (Quest quest : Quest.values())
		{
			if (questStateCache.get(quest.getId()) != QuestState.FINISHED)
			{
				QuestState state = quest.getState(client);

				if (questStateCache.get(quest.getId()) != state)
				{
					questStateCache.put(quest.getId(), state);
					eventBus.post(new QuestStateChanged(quest, state));
				}
			}
		}
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

	private NamedItem createNamedItem(int id, int quantity)
	{
		return new NamedItem(id, itemManager.getItemComposition(id).getName(), quantity);
	}

	private int getNpcHealth(NPC npc)
	{
		final Integer maxHealth = npcManager.getHealth(npc.getId());
		final float ratio = npc.getHealthRatio();
		final float scale = npc.getHealthScale();

		// This is the equivalent of the reverse operation done by the server to get the health.
		// The calculation done for the health ratio by the server is as follows (quoted from the opponentinfo plugin):
		//     ratio = 1 + ((scale - 1) * health) / maxHealth, if health > 0, otherwise 0
		return maxHealth == null ? 0 : (int) Math.ceil((maxHealth * Math.max(0, ratio - 1)) / (scale - 1));
	}

	@AllArgsConstructor
	private class AchievementElementStateListener implements AchievementStateListener
	{
		private final AchievementElement element;

		@Override
		public void onStateChanged(AchievementState status)
		{
			if (status == AchievementState.COMPLETE)
			{
				sendCompletionMessage(element.completionChatMessage(config));
			}

			updateConfig();
			SwingUtilities.invokeLater(panel::refresh);
		}
	}
}
