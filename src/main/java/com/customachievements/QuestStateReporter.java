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

import com.customachievements.events.QuestStateChanged;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.task.Schedule;

import javax.inject.Inject;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class QuestStateReporter
{
	private final Client client;
	private final EventBus eventBus;

	private final Map<Integer, QuestState> questStateCache = new HashMap<>();

	@Inject
	public QuestStateReporter(final Client client, final EventBus eventBus)
	{
		this.client = client;
		this.eventBus = eventBus;
	}

	@Subscribe
	public void onWidgetLoaded(final WidgetLoaded widgetLoaded)
	{
		if (widgetLoaded.getGroupId() == WidgetID.QUEST_COMPLETED_GROUP_ID)
		{
			update();
		}
	}

	@Subscribe
	public void onScriptPostFired(final ScriptPostFired scriptPostFired)
	{
		if (scriptPostFired.getScriptId() == ScriptID.QUESTLIST_PROGRESS_LIST_SHOW)
		{
			for (Quest quest : Quest.values())
			{
				QuestState state = quest.getState(client);

				questStateCache.put(quest.getId(), state);
				eventBus.post(new QuestStateChanged(quest, state));
			}
		}
	}

	@Schedule(
			period = CustomAchievementsPlugin.UPDATE_FREQUENCY,
			unit = ChronoUnit.SECONDS
	)
	public void updateTask()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			update();
		}
	}

	public void update()
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
}
